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

    override suspend fun get(key: String): String? = synchronized(this) {
        return appContext.getSharedPreferences(key, Context.MODE_PRIVATE).getString(key, "")
    }

    override suspend fun put(key: String, value: String): Unit = synchronized(this) {
        appContext.getSharedPreferences(key, Context.MODE_PRIVATE).edit().putString(key, value)
    }


    override suspend fun delete(key: String): Unit = synchronized(this) {
        appContext.getSharedPreferences(key, Context.MODE_PRIVATE).edit().remove(key)
    }
}
