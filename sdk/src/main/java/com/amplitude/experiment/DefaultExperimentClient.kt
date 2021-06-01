package com.amplitude.experiment

import com.amplitude.experiment.storage.Storage
import com.amplitude.experiment.util.Logger
import com.amplitude.experiment.util.call
import com.amplitude.experiment.util.merge
import com.amplitude.experiment.util.toJson
import com.amplitude.experiment.util.toVariant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

internal class DefaultExperimentClient internal constructor(
    private val apiKey: String,
    private val config: ExperimentConfig,
    httpClient: OkHttpClient,
    private val storage: Storage,
) : ExperimentClient {

    private val serverUrl: HttpUrl = config.serverUrl.toHttpUrl()
    private val httpClient = httpClient.newBuilder()
        .callTimeout(config.fetchTimeoutMillis, TimeUnit.MILLISECONDS)
        .build()

    private val supervisor = CoroutineScope(SupervisorJob())
    private val storageMutex = Mutex()
    private var user: ExperimentUser = ExperimentUser()
    private var userProvider: ExperimentUserProvider? = null

    override fun getUser(): ExperimentUser {
        return user
    }

    override fun setUser(user: ExperimentUser) {
        this.user = user
    }

    override fun fetch(user: ExperimentUser?): Future<ExperimentClient> {
        this.user = user ?: this.user
        val fetchUser = this.user.merge(userProvider?.getUser())
        val future = AsyncFuture<ExperimentClient>()
        // Launch a coroutine to fetch and store the variants.
        supervisor.async {
            val variants = doFetch(fetchUser)
            storeVariants(variants)
        }.invokeOnCompletion { throwable ->
            // Complete the future when the coroutine completes.
            if (throwable != null) {
                Logger.e("fetch failed", throwable)
                future.completeExceptionally(throwable)
            } else {
                future.complete(this)
            }
        }
        return future
    }

    override fun variant(key: String, fallback: Variant?): Variant? {
        return all()[key] ?: fallback ?: config.fallbackVariant
    }

    override fun all(): Map<String, Variant> {
        val initialVariants = config.initialVariants ?: emptyMap()
        return when (config.source) {
            Source.LOCAL_STORAGE -> {
                return initialVariants.plus(storage.getAll())
            }
            Source.INITIAL_VARIANTS -> {
                storage.getAll().plus(initialVariants)
            }
        }
    }

    override fun setUserProvider(provider: ExperimentUserProvider?): ExperimentClient {
        this.userProvider = provider
        return this
    }

    private suspend fun doFetch(user: ExperimentUser): Map<String, Variant> {
        if (user.userId == null && user.deviceId == null) {
            Logger.w("user id and device id are null; amplitude may not resolve identity")
        }
        Logger.d("Fetch variants for user: $user")
        // Build request to fetch variants for the user
        val userJsonBytes = user.toJson().toByteArray(Charsets.UTF_8)
        val userBase64 = userJsonBytes.toByteString().base64Url()
        val url = serverUrl.newBuilder()
            .addPathSegments("sdk/vardata/$userBase64")
            .build()
        val request = Request.Builder()
            .get()
            .url(url)
            .addHeader("Authorization", "Api-Key $apiKey")
            .build()

        // Execute request and handle response
        httpClient.call(request).use { response ->
            Logger.d("Received fetch response: $response")
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
            Logger.d("Received variants: $variants")
            return variants
        }
    }

    private suspend fun storeVariants(variants: Map<String, Variant>) {
        storageMutex.withLock {
            variants.forEach { (key, variant) ->
                storage.put(key, variant)
            }
            Logger.d("Stored variants: $variants")
        }
    }
}
