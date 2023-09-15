package com.amplitude.experiment

import com.amplitude.experiment.util.AsyncFuture
import com.amplitude.experiment.util.Logger
import com.amplitude.experiment.util.toFlag
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

data class GetFlagsOptions(
    val libraryName: String,
    val libraryVersion: String,
    val evaluationMode: String? = null,
    val timeoutMillis: Long? = null
)

interface FlagApi {
    suspend fun getFlags(options: GetFlagsOptions? = null): Future<Map<String, EvaluationFlag>>
}

class SdkFlagApi(
    private val deploymentKey: String,
    private val serverUrl: String,
    private val httpClient: OkHttpClient
) : FlagApi {
    override suspend fun getFlags(options: GetFlagsOptions?): Future<Map<String, EvaluationFlag>> {
        val url = serverUrl.toHttpUrl().newBuilder()
            .addPathSegments("sdk/v2/flags")
            .build()

        val builder = Request.Builder()
            .get()
            .url(url).addHeader("Authorization", "Api-Key $deploymentKey")

        options?.let {
            if (it.libraryName.isNotEmpty() && it.libraryVersion.isNotEmpty()) {
                builder.addHeader("X-Amp-Exp-Library","${it.libraryName}/${it.libraryVersion}")
            }
        }

        val request = builder.build()
        val call = httpClient.newCall(request)
        val timeout = if (options == null || options.timeoutMillis == null) 0 else options.timeoutMillis
        call.timeout().timeout(timeout, TimeUnit.MILLISECONDS)
        val future = AsyncFuture<Map<String, EvaluationFlag>>(call)
        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                try {
                    Logger.d("Received fetch response: $response")
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val flags = mutableMapOf<String, EvaluationFlag>()
                    json.keys().forEach { key ->
                        val flag = json.getJSONObject(key).toFlag()
                        if (flag != null) {
                            flags[key] = flag
                        }
                    }
                    future.complete(flags)
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
}
