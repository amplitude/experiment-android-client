package com.amplitude.experiment.storage

import android.content.Context

/**
 * Simple SharedPrefs backed storage for caching assigned variant values locally.
 *
 * This is not multiprocess safe.
 */
internal class SharedPrefsStorage(
    appContext: Context,
) : Storage {
    private val appContext: Context

    init {
        this.appContext = appContext
    }

    override fun get(key: String): Map<String, String> =
        synchronized(this) {
            val sharedPrefs = appContext.getSharedPreferences(key, Context.MODE_PRIVATE)
            val result = mutableMapOf<String, String>()
            for ((spKey, spValue) in sharedPrefs.all) {
                if (spValue is String) {
                    result[spKey] = spValue
                }
            }
            return result
        }

    override fun put(
        key: String,
        value: Map<String, String>,
    ): Unit =
        synchronized(this) {
            val editor = appContext.getSharedPreferences(key, Context.MODE_PRIVATE).edit()
            editor.clear()
            for ((k, v) in value) {
                editor.putString(k, v)
            }
            editor.commit()
        }

    override fun delete(key: String): Unit =
        synchronized(this) {
            appContext.getSharedPreferences(key, Context.MODE_PRIVATE).edit().remove(key).commit()
        }

    override fun getSingle(key: String): String? =
        synchronized(this) {
            val sharedPrefs = appContext.getSharedPreferences(key, Context.MODE_PRIVATE)
            return sharedPrefs.getString("value", null)
        }

    override fun putSingle(
        key: String,
        value: String,
    ): Unit =
        synchronized(this) {
            val editor = appContext.getSharedPreferences(key, Context.MODE_PRIVATE).edit()
            editor.putString("value", value)
            editor.commit()
        }
}
