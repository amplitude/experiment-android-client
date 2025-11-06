package com.amplitude.experiment.util

import android.util.Log

enum class LogLevel(val priority: Int) {
    DISABLE(0),
    ERROR(1),
    WARN(2),
    INFO(3),
    DEBUG(4),
    VERBOSE(5),
}

interface LoggerProvider {
    fun verbose(msg: String)

    fun debug(msg: String)

    fun info(msg: String)

    fun warn(msg: String)

    fun error(msg: String)
}

internal object AmpLogger : LoggerProvider {
    internal var logLevel: LogLevel = LogLevel.ERROR
    internal var loggerProvider: LoggerProvider? = null

    internal fun configure(
        logLevel: LogLevel,
        provider: LoggerProvider?,
    ) {
        this.logLevel = logLevel
        this.loggerProvider = provider
    }

    override fun verbose(msg: String) {
        if (shouldLog(LogLevel.VERBOSE)) {
            this.loggerProvider?.verbose(msg)
        }
    }

    override fun debug(msg: String) {
        if (shouldLog(LogLevel.DEBUG)) {
            this.loggerProvider?.debug(msg)
        }
    }

    override fun info(msg: String) {
        if (shouldLog(LogLevel.INFO)) {
            this.loggerProvider?.info(msg)
        }
    }

    override fun warn(msg: String) {
        if (shouldLog(LogLevel.WARN)) {
            this.loggerProvider?.warn(msg)
        }
    }

    override fun error(msg: String) {
        if (shouldLog(LogLevel.ERROR)) {
            loggerProvider?.error(msg)
        }
    }

    private fun shouldLog(logLevel: LogLevel): Boolean {
        return logLevel.priority <= this.logLevel.priority
    }
}

class AndroidLoggerProvider() : LoggerProvider {
    private val tag = "Experiment"

    override fun verbose(msg: String) {
        Log.v(tag, msg)
    }

    override fun debug(msg: String) {
        Log.d(tag, msg)
    }

    override fun info(msg: String) {
        Log.i(tag, msg)
    }

    override fun warn(msg: String) {
        Log.w(tag, msg)
    }

    override fun error(msg: String) {
        Log.e(tag, msg)
    }
}
