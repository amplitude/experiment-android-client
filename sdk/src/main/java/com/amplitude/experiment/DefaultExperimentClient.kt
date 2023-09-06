package com.amplitude.experiment

import com.amplitude.experiment.storage.LoadStoreCache
import com.amplitude.experiment.analytics.ExposureEvent as OldExposureEvent
import com.amplitude.experiment.storage.Storage
import com.amplitude.experiment.storage.getFlagStorage
import com.amplitude.experiment.storage.getVariantStorage
import com.amplitude.experiment.util.AsyncFuture
import com.amplitude.experiment.util.Backoff
import com.amplitude.experiment.util.BackoffConfig
import com.amplitude.experiment.util.Logger
import com.amplitude.experiment.util.SessionAnalyticsProvider
import com.amplitude.experiment.util.UserSessionExposureTracker
import com.amplitude.experiment.util.backoff
import com.amplitude.experiment.util.merge
import com.amplitude.experiment.util.toJson
import com.amplitude.experiment.util.toVariant
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.io.IOException
import java.lang.IllegalStateException
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.jvm.Throws
import org.json.JSONArray

internal class DefaultExperimentClient internal constructor(
    private val apiKey: String,
    private val config: ExperimentConfig,
    private val httpClient: OkHttpClient,
    private val storage: Storage,
    private val executorService: ScheduledExecutorService,
) : ExperimentClient {

    private var user: ExperimentUser? = null
    private val variants: LoadStoreCache<Variant> = getVariantStorage(
        this.apiKey,
        this.config.instanceName,
        storage,
    );
    private val flags: LoadStoreCache<EvaluationFlag> = getFlagStorage(
        this.apiKey,
        this.config.instanceName,
        storage,
    );
    init {
        this.flags.load()
    }

    private val backoffLock = Any()
    private var backoff: Backoff? = null
    private val fetchBackoffTimeoutMillis = 10000L
    private val backoffConfig = BackoffConfig(
        attempts = 8,
        min = 500,
        max = 10000,
        scalar = 1.5f,
    )

    private val serverUrl: HttpUrl = config.serverUrl.toHttpUrl()

    @Deprecated("moved to experiment config")
    private var userProvider: ExperimentUserProvider? = config.userProvider

    private val analyticsProvider: SessionAnalyticsProvider? = config.analyticsProvider?.let {
        SessionAnalyticsProvider(it)
    }
    private val userSessionExposureTracker: UserSessionExposureTracker? =
        config.exposureTrackingProvider?.let {
            UserSessionExposureTracker(it)
        }

    override fun fetch(user: ExperimentUser?): Future<ExperimentClient> {
        return fetch(user, null)
    }

    override fun fetch(user: ExperimentUser?, options: FetchOptions?): Future<ExperimentClient> {
        this.user = user ?: this.user
        return executorService.submit(Callable {
            val fetchUser = getUserMergedWithProviderOrWait(10000)
            fetchInternal(fetchUser, config.fetchTimeoutMillis, config.retryFetchOnFailure, options)
            this
        })
    }

    override fun variant(key: String): Variant {
        return variant(key, null)
    }

    override fun variant(key: String, fallback: Variant?): Variant {
        val variantAndSource = resolveVariantAndSource(key, fallback)
        val variant = variantAndSource.variant
        val source = variantAndSource.source
        if (config.automaticExposureTracking) {
            exposureInternal(key, variant, source)
        }
        return variant
    }

    override fun exposure(key: String) {
        val variantAndSource = resolveVariantAndSource(key, null)
        exposureInternal(key, variantAndSource.variant, variantAndSource.source)
    }

    private fun exposureInternal(key: String, variant: Variant, source: VariantSource) {
        val exposedUser = getUserMergedWithProvider()
        val event = OldExposureEvent(exposedUser, key, variant, source)
        // Track the exposure event if an analytics provider is set
        if (source.isFallback() || variant.value == null) {
            userSessionExposureTracker?.track(Exposure(key, null, variant.expKey), exposedUser)
            analyticsProvider?.unsetUserProperty(event)
        } else {
            userSessionExposureTracker?.track(Exposure(key, variant.value, variant.expKey), exposedUser)
            analyticsProvider?.setUserProperty(event)
            analyticsProvider?.track(event)
        }
    }

    private fun resolveVariantAndSource(key: String, fallback: Variant?): VariantAndSource {
        val sourceVariant = sourceVariants()[key]
        when (config.source) {
            Source.LOCAL_STORAGE -> {
                // for source = LocalStorage, fallback order goes:
                // 1. Local Storage
                // 2. Function fallback
                // 3. InitialFlags
                // 4. Config fallback
                if (sourceVariant != null) {
                    return VariantAndSource(sourceVariant, VariantSource.LOCAL_STORAGE)
                }
                if (fallback != null) {
                    return VariantAndSource(fallback, VariantSource.FALLBACK_INLINE)
                }
                val secondaryVariant = secondaryVariants()[key]
                if (secondaryVariant != null) {
                    return VariantAndSource(secondaryVariant, VariantSource.SECONDARY_INITIAL_VARIANTS)
                }
                return VariantAndSource(config.fallbackVariant, VariantSource.FALLBACK_CONFIG)
            }

            Source.INITIAL_VARIANTS -> {
                // for source = InitialVariants, fallback order goes:
                // 1. InitialFlags
                // 2. Local Storage
                // 3. Function fallback
                // 4. Config fallback
                if (sourceVariant != null) {
                    return VariantAndSource(sourceVariant, VariantSource.INITIAL_VARIANTS)
                }
                val secondaryVariant = secondaryVariants()[key]
                if (secondaryVariant != null) {
                    return VariantAndSource(secondaryVariant, VariantSource.SECONDARY_LOCAL_STORAGE)
                }
                if (fallback != null) {
                    return VariantAndSource(fallback, VariantSource.FALLBACK_INLINE)
                }
                return VariantAndSource(config.fallbackVariant, VariantSource.FALLBACK_CONFIG)
            }
        }
    }

    override fun all(): Map<String, Variant> {
        return secondaryVariants() + sourceVariants()
    }

    override fun clear() {
        this.variants.clear()
        this.variants.store()
    }

    override fun getUser(): ExperimentUser? {
        return user
    }

    override fun setUser(user: ExperimentUser) {
        this.user = user
    }

    override fun getUserProvider(): ExperimentUserProvider? {
        return this.userProvider
    }

    override fun setUserProvider(provider: ExperimentUserProvider?): ExperimentClient {
        this.userProvider = provider
        return this
    }

    @Throws
    private fun fetchInternal(user: ExperimentUser, timeoutMillis: Long, retry: Boolean, options: FetchOptions?) {
        if (retry) {
            stopRetries()
        }
        try {
            val variants = doFetch(user, timeoutMillis, options).get()
            storeVariants(variants, options)
        } catch (e: Exception) {
            if (retry) {
                startRetries(user, options)
            }
            throw e
        }
    }

    private fun doFetch(
        user: ExperimentUser,
        timeoutMillis: Long,
        options: FetchOptions?
    ): Future<Map<String, Variant>> {
        if (user.userId == null && user.deviceId == null) {
            Logger.w("user id and device id are null; amplitude may not resolve identity")
        }
        Logger.d("Fetch variants for user: $user")
        // Build request to fetch variants for the user
        val userBase64 = user.toJson()
            .toByteArray(Charsets.UTF_8)
            .toByteString()
            .base64Url()
        val url = serverUrl.newBuilder()
            .addPathSegments("sdk/vardata")
            .build()
        val builder = Request.Builder()
            .get()
            .url(url)
            .addHeader("Authorization", "Api-Key $apiKey")
            .addHeader("X-Amp-Exp-User", userBase64)
        if (!options?.flagKeys.isNullOrEmpty()) {
            val flagKeysBase64 = JSONArray(options?.flagKeys)
                .toString()
                .toByteArray(Charsets.UTF_8)
                .toByteString()
                .base64()
            builder.addHeader("X-Amp-Exp-Flag-Keys", flagKeysBase64)
        }
        val request = builder.build()
        val call = httpClient.newCall(request)
        call.timeout().timeout(timeoutMillis, TimeUnit.MILLISECONDS)
        val future = AsyncFuture<Map<String, Variant>>(call)
        // Execute request and handle response
        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                try {
                    Logger.d("Received fetch response: $response")
                    val variants = parseResponse(response)
                    future.complete(variants)
                } catch (e: IOException) {
                    onFailure(call, e)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                future.completeExceptionally(e)
            }
        })
        return future
    }

    private fun startRetries(user: ExperimentUser, options: FetchOptions?) = synchronized(backoffLock) {
        backoff?.cancel()
        backoff = executorService.backoff(backoffConfig) {
            fetchInternal(user, fetchBackoffTimeoutMillis, false, options)
        }
    }

    private fun stopRetries() = synchronized(backoffLock) {
        backoff?.cancel()
    }

    @Throws(IOException::class)
    private fun parseResponse(response: Response): Map<String, Variant> = response.use {
        if (!response.isSuccessful) {
            throw IOException("fetch error response: $response")
        }
        val body = response.body?.string() ?: ""
        val json = JSONObject(body)
        val variants = mutableMapOf<String, Variant>()
        json.keys().forEach { key ->
            val variant = json.getJSONObject(key).toVariant()
            if (variant != null) {
                variants[key] = variant
            }
        }
        return variants
    }

    private fun storeVariants(variants: Map<String, Variant>, options: FetchOptions?) = synchronized(variants) {
        val failedFlagKeys = options?.flagKeys?.toMutableList() ?: mutableListOf()
        if (options?.flagKeys == null) {
            this.variants.clear()
        }
        for (entry in variants.entries) {
            this.variants.put(entry.key, entry.value)
            failedFlagKeys.remove(entry.key)
        }
        for (key in failedFlagKeys) {
            this.variants.remove(key)
        }

        this.variants.store()
        Logger.d("Stored variants: $variants")
    }

    private fun sourceVariants(): Map<String, Variant> {
        return when (config.source) {
            Source.LOCAL_STORAGE -> this.variants.getAll()
            Source.INITIAL_VARIANTS -> config.initialVariants
        }
    }

    private fun secondaryVariants(): Map<String, Variant> {
        return when (config.source) {
            Source.LOCAL_STORAGE -> config.initialVariants
            Source.INITIAL_VARIANTS -> this.variants.getAll()
        }
    }

    private fun getUserMergedWithProvider(): ExperimentUser {
        val user = this.user ?: ExperimentUser()
        return user.copyToBuilder()
            .library("experiment-android-client/${BuildConfig.VERSION_NAME}")
            .build().merge(userProvider?.getUser())
    }

    @Throws(IllegalStateException::class)
    private fun getUserMergedWithProviderOrWait(ms: Long): ExperimentUser {
        val safeUserProvider = userProvider
        val providedUser = if (safeUserProvider is ConnectorUserProvider) {
            try {
                safeUserProvider.getUserOrWait(ms)
            } catch (e: TimeoutException) {
                throw IllegalStateException(e)
            }
        } else {
            safeUserProvider?.getUser()
        }
        val user = this.user ?: ExperimentUser()
        return user.copyToBuilder()
            .library("experiment-android-client/${BuildConfig.VERSION_NAME}")
            .build().merge(providedUser)
    }
}

data class VariantAndSource(val variant: Variant, val source: VariantSource)

enum class VariantSource(val type: String) {
    LOCAL_STORAGE("storage"),
    INITIAL_VARIANTS("initial"),
    SECONDARY_LOCAL_STORAGE("secondary-storage"),
    SECONDARY_INITIAL_VARIANTS("secondary-initial"),
    FALLBACK_INLINE("fallback-inline"),
    FALLBACK_CONFIG("fallback-config");

    override fun toString(): String {
        return type
    }

    fun isFallback(): Boolean {
        return this == FALLBACK_INLINE ||
                this == FALLBACK_CONFIG ||
                this == SECONDARY_INITIAL_VARIANTS
    }
}
