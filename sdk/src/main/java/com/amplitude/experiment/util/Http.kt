package com.amplitude.experiment.util

import kotlinx.coroutines.channels.Channel
import okhttp3.*
import java.io.IOException

internal suspend fun OkHttpClient.call(request: Request): Response {
    val responseChannel = Channel<Response>(1)
    newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            responseChannel.close(e)
        }

        override fun onResponse(call: Call, response: Response) {
            responseChannel.offer(response)
        }
    })
    return responseChannel.receive()
}