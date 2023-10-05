package com.amplitude.experiment

import com.amplitude.experiment.evaluation.EvaluationFlag
import com.amplitude.experiment.evaluation.json
import com.amplitude.experiment.util.AsyncFuture
import com.amplitude.experiment.util.Logger
import kotlinx.serialization.decodeFromString
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

internal data class GetFlagsOptions(
    val libraryName: String,
    val libraryVersion: String,
    val evaluationMode: String? = null,
    val timeoutMillis: Long = ExperimentConfig.Defaults.FETCH_TIMEOUT_MILLIS
)

internal interface FlagApi {
    fun getFlags(options: GetFlagsOptions? = null, callback: ((Map<String, EvaluationFlag>) -> Unit)? = null): Future<Map<String, EvaluationFlag>>
}

internal class SdkFlagApi(
    private val deploymentKey: String,
    private val serverUrl: String,
    private val httpClient: OkHttpClient
) : FlagApi {
    override fun getFlags(
        options: GetFlagsOptions?,
        callback: ((Map<String, EvaluationFlag>) -> Unit)?
    ): Future<Map<String, EvaluationFlag>> {
        val url = serverUrl.toHttpUrl().newBuilder()
            .addPathSegments("sdk/v2/flags")
            .build()

        val builder = Request.Builder()
            .get()
            .url(url).addHeader("Authorization", "Api-Key $deploymentKey")

        options?.let {
            if (it.libraryName.isNotEmpty() && it.libraryVersion.isNotEmpty()) {
                builder.addHeader("X-Amp-Exp-Library", "${it.libraryName}/${it.libraryVersion}")
            }
        }

        val request = builder.build()
        val call = httpClient.newCall(request)
        if (options != null) {
            call.timeout().timeout(options.timeoutMillis, TimeUnit.MILLISECONDS)
        }
        val future = AsyncFuture(call, callback)
        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                try {
                    Logger.d("Received fetch flags response: $response")
                    val body = response.body?.string() ?: ""
                    val flags = json.decodeFromString<List<EvaluationFlag>>(body)
                        .associateBy { it.key }
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
