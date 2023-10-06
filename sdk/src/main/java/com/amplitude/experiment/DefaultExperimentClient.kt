package com.amplitude.experiment

import com.amplitude.experiment.evaluation.EvaluationEngineImpl
import com.amplitude.experiment.evaluation.EvaluationFlag
import com.amplitude.experiment.evaluation.topologicalSort
import com.amplitude.experiment.storage.LoadStoreCache
import com.amplitude.experiment.storage.Storage
import com.amplitude.experiment.analytics.ExposureEvent as OldExposureEvent
import com.amplitude.experiment.storage.getVariantStorage
import com.amplitude.experiment.storage.getFlagStorage
import com.amplitude.experiment.util.*
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

internal const val euServerUrl = "https://api.lab.eu.amplitude.com/"
internal const val euFlagsServerUrl = "https://flag.lab.eu.amplitude.com/"

internal class DefaultExperimentClient internal constructor(
    private val apiKey: String,
    private val config: ExperimentConfig,
    private val httpClient: OkHttpClient,
    storage: Storage,
    private val executorService: ScheduledExecutorService,
) : ExperimentClient {

    private var user: ExperimentUser? = null
    private val engine = EvaluationEngineImpl()

    private val variants: LoadStoreCache<Variant> = getVariantStorage(
        this.apiKey,
        this.config.instanceName,
        storage,
    )
    private val flags: LoadStoreCache<EvaluationFlag> = getFlagStorage(
        this.apiKey,
        this.config.instanceName,
        storage,
    )

    init {
        this.variants.load()
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
    private val flagPollerIntervalMillis: Long = 60000

    private val poller: Poller = Poller(this.executorService, ::doFlags, flagPollerIntervalMillis)


    internal val serverUrl: HttpUrl =
        if (config.serverUrl == ExperimentConfig.Defaults.SERVER_URL && config.flagsServerUrl == ExperimentConfig.Defaults.FLAGS_SERVER_URL && config.serverZone == ServerZone.EU) {
            euServerUrl.toHttpUrl()
        } else {
            config.serverUrl.toHttpUrl()
        }

    internal val flagsServerUrl: HttpUrl =
        if (config.serverUrl == ExperimentConfig.Defaults.SERVER_URL && config.flagsServerUrl == ExperimentConfig.Defaults.FLAGS_SERVER_URL && config.serverZone == ServerZone.EU) {
            euFlagsServerUrl.toHttpUrl()
        } else {
            config.flagsServerUrl.toHttpUrl()
        }

    private val flagApi = SdkFlagApi(this.apiKey, flagsServerUrl, httpClient)

    @Deprecated("moved to experiment config")
    private var userProvider: ExperimentUserProvider? = config.userProvider

    private val analyticsProvider: SessionAnalyticsProvider? = config.analyticsProvider?.let {
        SessionAnalyticsProvider(it)
    }
    private val userSessionExposureTracker: UserSessionExposureTracker? =
        config.exposureTrackingProvider?.let {
            UserSessionExposureTracker(it)
        }

    private val isRunningLock = Any()
    private var isRunning = false

    override fun start(user: ExperimentUser?): Future<ExperimentClient>? {
        synchronized(isRunningLock) {
            if (isRunning) {
                return null
            } else {
                isRunning = true
            }
            if (config.pollOnStart) {
                this.poller.start()
            }
        }
        this.user = user
        return this.executorService.submit(Callable {
            val flagsFuture = doFlags()
            var remoteFlags = config.fetchOnStart
                ?: allFlags().values.any { it.isRemoteEvaluationMode() }
            if (remoteFlags) {
                flagsFuture.get()
                fetchInternal(getUserMergedWithProviderOrWait(10000), config.fetchTimeoutMillis, config.retryFetchOnFailure, null)
            } else {
                flagsFuture.get()
                remoteFlags = config.fetchOnStart
                    ?: allFlags().values.any { it.isRemoteEvaluationMode() }
                if (remoteFlags) {
                    fetchInternal(getUserMergedWithProviderOrWait(10000), config.fetchTimeoutMillis, config.retryFetchOnFailure, null)
                }
            }
            this
        })
    }

    /**
     * Stop the local flag configuration poller.
     */
    override fun stop() {
        synchronized(isRunningLock) {
            if (!isRunning) {
                return
            }
            poller.stop()
            isRunning = false
        }
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
        if (config.automaticExposureTracking) {
            exposureInternal(key, variantAndSource)
        }
        return variantAndSource.variant
    }

    override fun exposure(key: String) {
        val variantAndSource = resolveVariantAndSource(key)
        exposureInternal(key, variantAndSource)
    }

    override fun all(): Map<String, Variant> {
        val evaluationResults = this.evaluate(emptySet())
        val evaluatedVariants = synchronized(flags) {
            evaluationResults.filter { entry ->
                this.flags.get(entry.key).isLocalEvaluationMode()
            }
        }
        return secondaryVariants() + sourceVariants() + evaluatedVariants
    }

    override fun clear() {
        synchronized(variants) {
            this.variants.clear()
            this.variants.store()
        }
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


    internal fun allFlags(): Map<String, EvaluationFlag> {
        return synchronized(flags) { this.flags.getAll() }
    }

    private fun exposureInternal(key: String, variantAndSource: VariantAndSource) {
        legacyExposureInternal(key, variantAndSource.variant, variantAndSource.source)

        // Do not track exposure for fallback variants that are not associated with a default variant.
        val fallback = isFallback(variantAndSource.source)
        if (fallback && !variantAndSource.hasDefaultVariant) {
            return
        }

        val experimentKey = variantAndSource.variant.expKey
        val metadata = variantAndSource.variant.metadata
        val variant = if (!fallback && !variantAndSource.variant.isDefaultVariant()) {
            variantAndSource.variant.key ?: variantAndSource.variant.value
        } else {
            null
        }

        val exposure = Exposure(key, variant, experimentKey, metadata)

        userSessionExposureTracker?.track(exposure)
    }


    private fun legacyExposureInternal(key: String, variant: Variant, source: VariantSource) {
        val exposedUser = getUserMergedWithProvider()
        val event = OldExposureEvent(exposedUser, key, variant, source)
        // Track the exposure event if an analytics provider is set
        if (source.isFallback() || variant.key == null) {
            analyticsProvider?.unsetUserProperty(event)
        } else {
            analyticsProvider?.setUserProperty(event)
            analyticsProvider?.track(event)
        }
    }

    private fun isFallback(source: VariantSource?): Boolean {
        return source == null || source.isFallback()
    }

    private fun resolveVariantAndSource(key: String, fallback: Variant? = null): VariantAndSource {
        var variantAndSource: VariantAndSource
        variantAndSource = when (config.source) {
            Source.LOCAL_STORAGE -> localStorageVariantAndSource(key, fallback)
            Source.INITIAL_VARIANTS -> initialVariantsVariantAndSource(key, fallback)

        }
        val flag = synchronized(flags) { this.flags.get(key) }
        if (flag != null && (flag.isLocalEvaluationMode() || variantAndSource.variant.isNullOrEmpty())) {
            variantAndSource = this.localEvaluationVariantAndSource(key, flag, fallback)
        }
        return variantAndSource
    }

    @Throws
    internal fun fetchInternal(user: ExperimentUser, timeoutMillis: Long, retry: Boolean, options: FetchOptions?) {
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
            .addPathSegments("sdk/v2/vardata")
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
                    Logger.d("Received fetch variants response: $response")
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

    private fun doFlags(): Future<Map<String, EvaluationFlag>> {
        return flagApi.getFlags(
            GetFlagsOptions(
                libraryName = "experiment-js-client",
                libraryVersion = BuildConfig.VERSION_NAME,
                timeoutMillis = config.fetchTimeoutMillis
            )
        ) { flags ->
            synchronized(this.flags) {
                this.flags.clear()
                this.flags.putAll(flags)
                this.flags.store()
            }
        }
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
        synchronized(this.variants) {
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
    }

    private fun sourceVariants(): Map<String, Variant> {
        return when (config.source) {
            Source.LOCAL_STORAGE -> synchronized(variants) { this.variants.getAll() }
            Source.INITIAL_VARIANTS -> config.initialVariants
        }
    }

    private fun secondaryVariants(): Map<String, Variant> {
        return when (config.source) {
            Source.LOCAL_STORAGE -> config.initialVariants
            Source.INITIAL_VARIANTS -> synchronized(variants) { this.variants.getAll() }
        }
    }

    private fun getUserMergedWithProvider(): ExperimentUser {
        val user = this.user ?: ExperimentUser()
        return user.copyToBuilder()
            .library("experiment-android-client/${BuildConfig.VERSION_NAME}")
            .build().merge(config.userProvider?.getUser())
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

    private fun evaluate(flagKeys: Set<String>): Map<String, Variant> {
        val user = getUserMergedWithProvider()
        val flags = try {
            topologicalSort(synchronized(flags) { this.flags.getAll() }, flagKeys)
        } catch (e: Exception) {
            Logger.w("Error during topological sort of flags", e)
            return emptyMap()
        }
        val context = user.toEvaluationContext()
        val evaluationVariants = this.engine.evaluate(context, flags)
        return evaluationVariants.mapValues { it.value.convertToVariant() }
    }

    /**
     * This function assumes the flag exists and is local evaluation mode. For
     * local evaluation, fallback order goes:
     *
     *  1. Local evaluation
     *  2. Inline function fallback
     *  3. Initial variants
     *  4. Config fallback
     *
     * If there is a default variant and no fallback, return the default variant.
     */
    private fun localEvaluationVariantAndSource(
        key: String,
        flag: EvaluationFlag,
        fallback: Variant? = null
    ): VariantAndSource {
        var defaultVariantAndSource = VariantAndSource()
        // Local evaluation
        val variant = evaluate(setOf(flag.key))[key]
        val source = VariantSource.LOCAL_EVALUATION
        val isLocalEvaluationDefault = variant?.metadata?.get("default") as? Boolean
        if (variant != null && isLocalEvaluationDefault != true) {
            return VariantAndSource(
                variant = variant,
                source = source,
                hasDefaultVariant = false
            )
        } else if (isLocalEvaluationDefault == true) {
            defaultVariantAndSource = VariantAndSource(
                variant = variant,
                source = source,
                hasDefaultVariant = true
            )
        }
        // Inline fallback
        if (fallback != null) {
            return VariantAndSource(
                variant = fallback,
                source = VariantSource.FALLBACK_INLINE,
                hasDefaultVariant = defaultVariantAndSource.hasDefaultVariant
            )
        }
        // Initial variants
        val initialVariant = config.initialVariants[key]
        if (initialVariant != null) {
            return VariantAndSource(
                variant = initialVariant,
                source = VariantSource.SECONDARY_INITIAL_VARIANTS,
                hasDefaultVariant = defaultVariantAndSource.hasDefaultVariant
            )
        }
        // Configured fallback, or default variant
        val fallbackVariant = config.fallbackVariant
        val fallbackVariantAndSource = VariantAndSource(
            variant = fallbackVariant,
            source = VariantSource.FALLBACK_CONFIG,
            hasDefaultVariant = defaultVariantAndSource.hasDefaultVariant
        )
        if (!fallbackVariant.isNullOrEmpty()) {
            return fallbackVariantAndSource
        }
        return defaultVariantAndSource
    }

    /**
     * For Source.LocalStorage, fallback order goes:
     *
     *  1. Local Storage
     *  2. Inline function fallback
     *  3. InitialFlags
     *  4. Config fallback
     *
     * If there is a default variant and no fallback, return the default variant.
     */
    private fun localStorageVariantAndSource(
        key: String,
        fallback: Variant?
    ): VariantAndSource {
        var defaultVariantAndSource = VariantAndSource()
        // Local storage
        val localStorageVariant = synchronized(variants) { variants.getAll()[key] }
        val isLocalStorageDefault = localStorageVariant?.metadata?.get("default") as? Boolean
        if (localStorageVariant != null && isLocalStorageDefault != true) {
            return VariantAndSource(
                variant = localStorageVariant,
                source = VariantSource.LOCAL_STORAGE,
                hasDefaultVariant = false
            )
        } else if (isLocalStorageDefault == true) {
            defaultVariantAndSource = VariantAndSource(
                variant = localStorageVariant,
                source = VariantSource.LOCAL_STORAGE,
                hasDefaultVariant = true
            )
        }
        // Inline fallback
        if (fallback != null) {
            return VariantAndSource(
                variant = fallback,
                source = VariantSource.FALLBACK_INLINE,
                hasDefaultVariant = defaultVariantAndSource.hasDefaultVariant
            )
        }
        // Initial variants
        val initialVariant = config.initialVariants[key]
        if (initialVariant != null) {
            return VariantAndSource(
                variant = initialVariant,
                source = VariantSource.SECONDARY_INITIAL_VARIANTS,
                hasDefaultVariant = defaultVariantAndSource.hasDefaultVariant
            )
        }
        // Configured fallback, or default variant
        val fallbackVariant = config.fallbackVariant
        val fallbackVariantAndSource = VariantAndSource(
            variant = fallbackVariant,
            source = VariantSource.FALLBACK_CONFIG,
            hasDefaultVariant = defaultVariantAndSource.hasDefaultVariant
        )
        if (!fallbackVariant.isNullOrEmpty()) {
            return fallbackVariantAndSource
        }
        return defaultVariantAndSource
    }

    /**
     * For Source.InitialVariants, fallback order goes:
     *
     *  1. Initial variants
     *  2. Local storage
     *  3. Inline function fallback
     *  4. Config fallback
     *
     * If there is a default variant and no fallback, return the default variant.
     */
    private fun initialVariantsVariantAndSource(
        key: String,
        fallback: Variant? = null
    ): VariantAndSource {
        var defaultVariantAndSource = VariantAndSource()
        // Initial variants
        val initialVariantsVariant = config.initialVariants[key]
        if (initialVariantsVariant != null) {
            return VariantAndSource(
                variant = initialVariantsVariant,
                source = VariantSource.INITIAL_VARIANTS,
                hasDefaultVariant = false
            )
        }
        // Local storage
        val localStorageVariant = synchronized(variants) { variants.getAll()[key] }
        val isLocalStorageDefault = localStorageVariant?.metadata?.get("default") as? Boolean
        if (localStorageVariant != null && isLocalStorageDefault != true) {
            return VariantAndSource(
                variant = localStorageVariant,
                source = VariantSource.LOCAL_STORAGE,
                hasDefaultVariant = false
            )
        } else if (isLocalStorageDefault == true) {
            defaultVariantAndSource = VariantAndSource(
                variant = localStorageVariant,
                source = VariantSource.LOCAL_STORAGE,
                hasDefaultVariant = true
            )
        }
        // Inline fallback
        if (fallback != null) {
            return VariantAndSource(
                variant = fallback,
                source = VariantSource.FALLBACK_INLINE,
                hasDefaultVariant = defaultVariantAndSource.hasDefaultVariant
            )
        }
        // Configured fallback, or default variant
        val fallbackVariant = config.fallbackVariant
        val fallbackVariantAndSource = VariantAndSource(
            variant = fallbackVariant,
            source = VariantSource.FALLBACK_CONFIG,
            hasDefaultVariant = defaultVariantAndSource.hasDefaultVariant
        )
        if (!fallbackVariant.isNullOrEmpty()) {
            return fallbackVariantAndSource
        }
        return defaultVariantAndSource
    }


}

data class VariantAndSource(
    val variant: Variant = Variant(),
    val source: VariantSource = VariantSource.FALLBACK_CONFIG,
    val hasDefaultVariant: Boolean = false
)

enum class VariantSource(val type: String) {
    LOCAL_STORAGE("storage"),
    INITIAL_VARIANTS("initial"),
    SECONDARY_LOCAL_STORAGE("secondary-storage"),
    SECONDARY_INITIAL_VARIANTS("secondary-initial"),
    FALLBACK_INLINE("fallback-inline"),
    FALLBACK_CONFIG("fallback-config"),
    LOCAL_EVALUATION("local-evaluation");

    override fun toString(): String {
        return type
    }

    fun isFallback(): Boolean {
        return this == FALLBACK_INLINE ||
                this == FALLBACK_CONFIG ||
                this == SECONDARY_INITIAL_VARIANTS
    }
}
