package com.amplitude.experiment.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val format = SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT)

internal fun timestamp(): String {
    return format.format(Date())
}

// For Testing
internal class SystemLoggerProvider(private val debug: Boolean) : LoggerProvider {
    override fun verbose(msg: String) {
        if (debug) {
            println("[${timestamp()}] VERBOSE: $msg")
        }
    }

    override fun debug(msg: String) {
        if (debug) {
            println("[${timestamp()}] DEBUG: $msg")
        }
    }

    override fun info(msg: String) {
        if (debug) {
            println("[${timestamp()}] INFO: $msg")
        }
    }

    override fun warn(
        msg: String,
    ) {
        println("[${timestamp()}] WARN: $msg")
    }

    override fun error(
        msg: String,
    ) {
        println("[${timestamp()}] ERROR: $msg")
    }
}
