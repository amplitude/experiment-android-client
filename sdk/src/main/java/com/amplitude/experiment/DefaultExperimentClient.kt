package com.amplitude.experiment

import com.amplitude.experiment.storage.Storage
import com.amplitude.experiment.util.Logger
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
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

internal class DefaultExperimentClient internal constructor(
    private val apiKey: String,
    private val config: ExperimentConfig,
    httpClient: OkHttpClient,
    private val storage: Storage,
    private val executorService: ExecutorService,
) : ExperimentClient {

    private val storageLock = Any()
    private val serverUrl: HttpUrl = config.serverUrl.toHttpUrl()
    private val httpClient = httpClient.newBuilder()
        .callTimeout(config.fetchTimeoutMillis, TimeUnit.MILLISECONDS)
        .build()

    private var user: ExperimentUser? = null
    private var userProvider: ExperimentUserProvider? = null

    override fun getUser(): ExperimentUser? {
        return user
    }

    override fun setUser(user: ExperimentUser) {
        this.user = user
    }

    override fun fetch(user: ExperimentUser?): Future<ExperimentClient> {
        this.user = user ?: this.user
        val fetchUser = this.user.merge(userProvider?.getUser())
        return executorService.submit(Callable {
            val variants = doFetch(fetchUser).get()
            storeVariants(variants)
            this
        })
    }

    override fun variant(key: String, fallback: Variant?): Variant {
        return all()[key] ?: fallback ?: config.fallbackVariant
    }

    override fun all(): Map<String, Variant> {
        val initialVariants = config.initialVariants
        return when (config.source) {
            Source.LOCAL_STORAGE -> {
                return initialVariants.plus(storage.getAll())
            }
            Source.INITIAL_VARIANTS -> {
                storage.getAll().plus(initialVariants)
            }
        }
    }

    override fun getUserProvider(): ExperimentUserProvider? {
        return this.userProvider
    }

    override fun setUserProvider(provider: ExperimentUserProvider?): ExperimentClient {
        this.userProvider = provider
        return this
    }

    private fun doFetch(
        user: ExperimentUser,
    ): Future<Map<String, Variant>> {
        if (user.userId == null && user.deviceId == null) {
            Logger.w("user id and device id are null; amplitude may not resolve identity")
        }
        Logger.d("Fetch variants for user: $user")
        val future = AsyncFuture<Map<String, Variant>>()
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
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                future.completeExceptionally(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    Logger.d("Received fetch response: $response")
                    val variants = parseResponse(response)
                    Logger.d("Parsed variants: $variants")
                    future.complete(variants)
                } catch (e: Exception) {
                    future.completeExceptionally(e)
                }
            }
        })
        return future
    }

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
}
