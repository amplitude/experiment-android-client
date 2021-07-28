package com.amplitude.experiment

import com.amplitude.experiment.analytics.ExposureEvent
import com.amplitude.experiment.storage.Storage
import com.amplitude.experiment.util.AsyncFuture
import com.amplitude.experiment.util.Backoff
import com.amplitude.experiment.util.BackoffConfig
import com.amplitude.experiment.util.Logger
import com.amplitude.experiment.util.backoff
import com.amplitude.experiment.util.merge
import com.amplitude.experiment.util.toJson
import com.amplitude.experiment.util.toVariant
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.jvm.Throws

internal class DefaultExperimentClient internal constructor(
    private val apiKey: String,
    private val config: ExperimentConfig,
    private val httpClient: OkHttpClient,
    private val storage: Storage,
    private val executorService: ScheduledExecutorService,
) : ExperimentClient {

    private var user: ExperimentUser? = null

    private val backoffLock = Any()
    private var backoff: Backoff? = null
    private val fetchBackoffTimeoutMillis = 10000L
    private val backoffConfig = BackoffConfig(
        attempts = 8,
        min = 500,
        max = 10000,
        scalar = 1.5f,
    )

    private val storageLock = Any()
    private val serverUrl: HttpUrl = config.serverUrl.toHttpUrl()

    @Deprecated("moved to experiment config")
    private var userProvider: ExperimentUserProvider? = config.userProvider

    override fun fetch(user: ExperimentUser?): Future<ExperimentClient> {
        this.user = user ?: this.user
        val fetchUser = getUserMergedWithProvider()
        return executorService.submit(Callable {
            fetchInternal(fetchUser, config.fetchTimeoutMillis, config.retryFetchOnFailure)
            this
        })
    }

    override fun variant(key: String): Variant {
        return variant(key, null)
    }

    override fun variant(key: String, fallback: Variant?): Variant {
        val sourceVariant = sourceVariants()[key]
        // Track the exposure event if an analytics provider is set
        if (sourceVariant?.value != null) {
            val exposedUser = user.merge(userProvider?.getUser())
            config.analyticsProvider?.track(ExposureEvent(exposedUser, key, sourceVariant))
        }
        return sourceVariant
            ?: fallback
            ?: secondaryVariants()[key]
            ?: config.fallbackVariant
    }

    override fun all(): Map<String, Variant> {
        return secondaryVariants() + sourceVariants()
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
    private fun fetchInternal(user: ExperimentUser, timeoutMillis: Long, retry: Boolean) {
        if (retry) {
            stopRetries()
        }
        try {
            val variants = doFetch(user, timeoutMillis).get()
            storeVariants(variants)
        } catch (e: Exception) {
            if (retry) {
                startRetries(user)
            }
            throw e
        }
    }

    private fun doFetch(
        user: ExperimentUser,
        timeoutMillis: Long,
    ): Future<Map<String, Variant>> {
        if (user.userId == null && user.deviceId == null) {
            Logger.w("user id and device id are null; amplitude may not resolve identity")
        }
        Logger.d("Fetch variants for user: $user")
        // Build request to fetch variants for the user
        val body = user.toJson()
            .toByteArray(Charsets.UTF_8)
            .toRequestBody("application/json".toMediaType())
        val url = serverUrl.newBuilder()
            .addPathSegments("sdk/vardata")
            .build()
        val request = Request.Builder()
            .post(body)
            .url(url)
            .addHeader("Authorization", "Api-Key $apiKey")
            .build()
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

    private fun startRetries(user: ExperimentUser) = synchronized(backoffLock) {
        backoff?.cancel()
        backoff = executorService.backoff(backoffConfig) {
            fetchInternal(user, fetchBackoffTimeoutMillis, false)
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

    private fun storeVariants(variants: Map<String, Variant>) = synchronized(storageLock) {
        storage.clear()
        variants.forEach { (key, variant) ->
            storage.put(key, variant)
        }
        Logger.d("Stored variants: $variants")
    }

    private fun sourceVariants(): Map<String, Variant> {
        return when (config.source) {
            Source.LOCAL_STORAGE -> storage.getAll()
            Source.INITIAL_VARIANTS -> config.initialVariants
        }
    }

    private fun secondaryVariants(): Map<String, Variant> {
        return when (config.source) {
            Source.LOCAL_STORAGE -> config.initialVariants
            Source.INITIAL_VARIANTS -> storage.getAll()
        }
    }

    private fun getUserMergedWithProvider(): ExperimentUser {
        return this.user?.copyToBuilder()
            ?.library("experiment-android-client/${BuildConfig.VERSION_NAME}")
            ?.build().merge(userProvider?.getUser())
    }
}
