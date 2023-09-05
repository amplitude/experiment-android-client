package com.amplitude.experiment.storage

import com.amplitude.experiment.EvaluationFlag
import com.amplitude.experiment.EvaluationSegment
import com.amplitude.experiment.EvaluationVariant
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

internal fun getFlagStorage(deploymentKey: String, instanceName: String, storage: Storage): LoadStoreCache<EvaluationFlag> {
    val truncatedDeployment = deploymentKey.takeLast(6)
    val namespace = "amp-exp-$instanceName-$truncatedDeployment-flags"
    return LoadStoreCache(namespace, storage, ::transformFlagFromStorage)
}

private fun transformFlagFromStorage(storageValue: Any?): EvaluationFlag {
    return when (storageValue) {
        is Map<*, *> -> {
            // From v1 or v2 object format
            val key = storageValue["key"] as? String
            val variants = storageValue["variants"] as? Map<String, EvaluationVariant>
            val segments = storageValue["segments"] as? List<EvaluationSegment>
            val dependencies = storageValue["dependencies"] as? List<String>
            var metadata: MutableMap<String, Any>? = (storageValue["metadata"] as? Map<String, Any>)?.toMutableMap()

            val flag = EvaluationFlag()

            key?.let { flag.key = it }
            variants?.let { flag.variants = it }
            segments?.let { flag.segments = it }
            dependencies?.let { flag.dependencies = it }
            metadata?.let { flag.metadata = it }

            flag
        }

        else -> EvaluationFlag()
    }
}
