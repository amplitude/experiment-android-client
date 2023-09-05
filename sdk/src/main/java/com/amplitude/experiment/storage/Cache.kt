package com.amplitude.experiment.storage

import com.amplitude.experiment.util.toMap
import org.json.JSONObject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class LoadStoreCache<V>(
    private val namespace: String,
    private val storage: Storage,
    private val transformer: ((value: Any) -> V)? = null
) {
    private val cache: MutableMap<String, V> = mutableMapOf()
    private val mutex: Mutex = Mutex()

    suspend fun get(key: String): V? {
        mutex.withLock {
            return cache[key]
        }
    }

    suspend fun getAll(): Map<String, V> {
        mutex.withLock {
            return HashMap(cache)
        }
    }

    suspend fun put(key: String, value: V) {
        mutex.withLock {
            cache[key] = value
        }
    }

    suspend fun putAll(values: Map<String, V>) {
        mutex.withLock {
            cache.putAll(values)
        }
    }

    suspend fun remove(key: String) {
        mutex.withLock {
            cache.remove(key)
        }
    }

    suspend fun clear() {
        mutex.withLock {
            cache.clear()
        }
    }

    suspend fun load() {
        mutex.withLock {
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
    }

    suspend fun store(values: Map<String, V> = cache) {
        mutex.withLock {
            storage.put(namespace, JSONObject(values).toString())
        }
    }
}
