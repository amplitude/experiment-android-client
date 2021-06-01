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

    init {
        val sharedPrefsKey = "$SHARED_PREFS_PREFIX-$instanceName-${apiKey.takeLast(6)}"
        sharedPrefs = appContext.getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE)
    }

    override fun put(key: String, variant: Variant) {
        sharedPrefs.edit().putString(key, variant.toJson()).apply()
    }

    override fun get(key: String): Variant? {
        return sharedPrefs.getString(key, null).toVariant()
    }

    override fun getAll(): Map<String, Variant> {
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
        return result
    }

    override fun clear() {
        sharedPrefs.edit().clear().apply()
    }
}
