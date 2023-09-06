package com.amplitude.experiment.storage

import com.amplitude.experiment.util.toMap
import org.json.JSONObject

internal class LoadStoreCache<V>(
    private val namespace: String,
    private val storage: Storage,
    private val transformer: ((value: Any) -> V)? = null
) {
    private val cache: MutableMap<String, V> = mutableMapOf()

    fun get(key: String): V? = synchronized(cache) {
        return cache[key]
    }

    fun getAll(): Map<String, V> = synchronized(cache) {
        return HashMap(cache)
    }

    fun put(key: String, value: V) = synchronized(cache) {
        cache[key] = value
    }

    fun putAll(values: Map<String, V>) = synchronized(cache) {
        cache.putAll(values)
    }

    fun remove(key: String) = synchronized(cache) {
        cache.remove(key)
    }

    fun clear() = synchronized(cache) {
        cache.clear()
    }

    fun load() = synchronized(cache) {
        val rawValues = storage.get(namespace)
        val jsonValues: Map<String, Any?> = try {
            JSONObject(rawValues).toMap()
        } catch (e: Exception) {
            emptyMap()
        }
        val values: MutableMap<String, V> = mutableMapOf()
        for (key in jsonValues.keys) {
            try {
                val value: V = if (transformer != null) {
                    transformer?.let { it(jsonValues[key]!!) }
                } else {
                    jsonValues[key] as V
                }
                if (value != null) {
                    values[key] = value
                }
            } catch (e: Exception) {
                // Do nothing
            }
        }
        clear()
        putAll(values)
    }

    fun store(values: Map<String, V> = cache) = synchronized(cache) {
        storage.put(namespace, JSONObject(values).toString())
    }
}
