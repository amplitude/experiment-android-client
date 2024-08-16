package com.amplitude.experiment.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val format = SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT)

internal fun timestamp(): String {
    return format.format(Date())
}

// For Testing
internal class SystemLogger(private val debug: Boolean) : ILogger {
    override fun v(msg: String) {
        if (debug) {
            println("[${timestamp()}] VERBOSE: $msg")
        }
    }

    override fun d(msg: String) {
        if (debug) {
            println("[${timestamp()}] DEBUG: $msg")
        }
    }

    override fun i(msg: String) {
        if (debug) {
            println("[${timestamp()}] INFO: $msg")
        }
    }

    override fun w(
        msg: String,
        e: Throwable?,
    ) {
        if (e == null) {
            println("[${timestamp()}] WARN: $msg")
        } else {
            println("[${timestamp()}] WARN: $msg\n${e.printStackTrace()}")
        }
    }

    override fun e(
        msg: String,
        e: Throwable?,
    ) {
        if (e == null) {
            println("[${timestamp()}] ERROR: $msg")
        } else {
            println("[${timestamp()}] ERROR: $msg\n$e")
        }
    }
}
