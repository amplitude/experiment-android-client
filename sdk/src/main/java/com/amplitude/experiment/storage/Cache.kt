package com.amplitude.experiment.storage
import org.json.JSONObject

internal class LoadStoreCache<V>(
    private val namespace: String,
    private val storage: Storage,
    private val transformer: ((value: Any) -> V)? = null
) {
    private val cache: MutableMap<String, V> = mutableMapOf()

    fun get(key: String): V? {
        return cache[key]
    }

    fun getAll(): Map<String, V> {
        return HashMap(cache)
    }

    fun put(key: String, value: V) {
        cache[key] = value
    }

    private fun putAll(values: Map<String, V>) {
        cache.putAll(values)
    }

    fun remove(key: String) {
        cache.remove(key)
    }

    private fun clear() {
        cache.clear()
    }

    suspend fun load() {
        val rawValues = storage.get(namespace)
        val jsonValues: Map<String, Any> = try {
            JSONObject(rawValues) as Map<String, Any>? ?: emptyMap()
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

    suspend fun store(values: Map<String, V> = cache) {
        storage.put(namespace, JSONObject(values).toString())
    }
}
