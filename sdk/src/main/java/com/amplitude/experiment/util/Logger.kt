package com.amplitude.experiment.util

import android.util.Log

internal interface ILogger {
    fun v(msg: String)
    fun d(msg: String)
    fun i(msg: String)
    fun w(msg: String, e: Throwable? = null)
    fun e(msg: String, e: Throwable? = null)
}

internal object Logger : ILogger {

    internal var implementation: ILogger? = null

    override fun v(msg: String) {
        implementation?.v(msg)
    }

    override fun d(msg: String) {
        implementation?.d(msg)
    }

    override fun i(msg: String) {
        implementation?.i(msg)
    }

    override fun w(msg: String, e: Throwable?) {
        implementation?.w(msg)
    }

    override fun e(msg: String, e: Throwable?) {
        implementation?.e(msg, e)
    }
}

internal class AndroidLogger(private val debug: Boolean) : ILogger {

    private val tag = "Experiment"

    override fun v(msg: String) {
        if (debug) {
            Log.v(tag, msg)
        }
    }

    override fun d(msg: String) {
        if (debug) {
            Log.d(tag, msg)
        }
    }

    override fun i(msg: String) {
        if (debug) {
            Log.i(tag, msg)
        }
    }

    override fun w(msg: String, e: Throwable?) {
        Log.w(tag, msg)
    }

    override fun e(msg: String, e: Throwable?) {
        Log.e(tag, msg, e)
    }
}
