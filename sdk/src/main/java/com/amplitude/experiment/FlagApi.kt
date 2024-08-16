package com.amplitude.experiment

import com.amplitude.experiment.evaluation.EvaluationFlag
import com.amplitude.experiment.evaluation.json
import com.amplitude.experiment.util.AsyncFuture
import com.amplitude.experiment.util.Logger
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

internal data class GetFlagsOptions(
    val libraryName: String,
    val libraryVersion: String,
    val evaluationMode: String? = null,
    val timeoutMillis: Long = ExperimentConfig.Defaults.FETCH_TIMEOUT_MILLIS,
)

internal interface FlagApi {
    fun getFlags(
        options: GetFlagsOptions? = null,
        callback: ((Map<String, EvaluationFlag>) -> Unit)? = null,
    ): Future<Map<String, EvaluationFlag>>
}

internal class SdkFlagApi(
    private val deploymentKey: String,
    private val serverUrl: HttpUrl,
    private val httpClient: OkHttpClient,
) : FlagApi {
    override fun getFlags(
        options: GetFlagsOptions?,
        callback: ((Map<String, EvaluationFlag>) -> Unit)?,
    ): Future<Map<String, EvaluationFlag>> {
        val url =
            serverUrl.newBuilder()
                .addPathSegments("sdk/v2/flags")
                .addQueryParameter("v", "0")
                .build()

        val builder =
            Request.Builder()
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
        call.enqueue(
            object : Callback {
                override fun onResponse(
                    call: Call,
                    response: Response,
                ) {
                    try {
                        Logger.d("Received fetch flags response: $response")
                        if (response.isSuccessful) {
                            val body = response.body?.string() ?: ""
                            val flags =
                                json.decodeFromString<List<EvaluationFlag>>(body)
                                    .associateBy { it.key }
                            future.complete(flags)
                        } else {
                            Logger.e("Non-successful response: ${response.code}")
                            future.completeExceptionally(IOException("Non-successful response: ${response.code}"))
                        }
                    } catch (e: IOException) {
                        onFailure(call, e)
                    } catch (e: SerializationException) {
                        Logger.e("Error decoding JSON: ${e.message}")
                        future.completeExceptionally(e)
                    }
                }

                override fun onFailure(
                    call: Call,
                    e: IOException,
                ) {
                    future.completeExceptionally(e)
                }
            },
        )
        return future
    }
}
