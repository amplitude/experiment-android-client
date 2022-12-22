package com.amplitude.experiment.storage

import android.content.Context
import android.content.SharedPreferences
import com.amplitude.experiment.Variant
import com.amplitude.experiment.util.toJson
import com.amplitude.experiment.util.toVariant

private const val SHARED_PREFS_PREFIX = "amplitude-experiment"

/**
 * Simple SharedPrefs backed storage for caching assigned variant values locally.
 *
 * This is not multi-process safe.
 */
internal class SharedPrefsStorage(
    appContext: Context,
    apiKey: String,
    instanceName: String
) : Storage {

    private val sharedPrefs: SharedPreferences
    private val map: MutableMap<String, Variant> = mutableMapOf()

    init {
        val sharedPrefsKey = "$SHARED_PREFS_PREFIX-$instanceName-${apiKey.takeLast(6)}"
        sharedPrefs = appContext.getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE)
        load()
    }

    override fun put(key: String, variant: Variant) = synchronized(this) {
        map[key] = variant
        sharedPrefs.edit().putString(key, variant.toJson()).apply()
    }

    override fun get(key: String): Variant? = synchronized(this) {
        return map[key]
    }

    override fun remove(key: String) = synchronized(this) {
        map.remove(key)
        sharedPrefs.edit().remove(key).apply()
    }

    override fun getAll(): Map<String, Variant> = synchronized(this) {
        return linkedMapOf<String, Variant>().apply { putAll(map) }
    }

    override fun clear() = synchronized(this) {
        map.clear()
        sharedPrefs.edit().clear().apply()
    }

    private fun load() = synchronized(this) {
        val result = mutableMapOf<String, Variant>()
        for ((key, value) in sharedPrefs.all) {
            if (value is String) {
                val variant = value.toVariant()
                if (variant == null) {
                    sharedPrefs.edit().remove(key).apply()
                } else {
                    result[key] = variant
                }
            }
        }
        map.clear()
        map.putAll(result)
    }
}
