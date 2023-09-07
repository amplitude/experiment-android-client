package com.amplitude.experiment.storage

import com.amplitude.experiment.Variant
import com.amplitude.experiment.util.toMap
import org.json.JSONObject

class LoadStoreCache<V>(
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

fun getVariantStorage(deploymentKey: String, instanceName: String, storage: Storage): LoadStoreCache<Variant> {
    val truncatedDeployment = deploymentKey.takeLast(6)
    val namespace = "amp-exp-$instanceName-$truncatedDeployment"
    return LoadStoreCache(namespace, storage, ::transformVariantFromStorage)
}

fun getFlagStorage(deploymentKey: String, instanceName: String, storage: Storage): LoadStoreCache<Any> {
    val truncatedDeployment = deploymentKey.takeLast(6)
    val namespace = "amp-exp-$instanceName-$truncatedDeployment-flags"
    return LoadStoreCache(namespace, storage)
}


fun transformVariantFromStorage(storageValue: Any?): Variant {
    return when (storageValue) {
        is String -> {
            // From v0 string format
            Variant(
                key = storageValue,
                value = storageValue
            )
        }

        is Map<*, *> -> {
            // From v1 or v2 object format
            val key = storageValue["key"] as? String
            val value = storageValue["value"] as? String
            val payload = storageValue["payload"]
            var metadata: MutableMap<String, Any>? = (storageValue["metadata"] as? Map<String, Any>)?.toMutableMap()
            var experimentKey: String? = storageValue["expKey"] as? String

            if (metadata != null && metadata["experimentKey"] != null) {
                experimentKey = metadata["experimentKey"] as? String
            } else if (experimentKey != null) {
                metadata = metadata ?: HashMap()
                metadata["experimentKey"] = experimentKey
            }

            val variant = Variant()

            if (key != null) {
                variant.key = key
            } else if (value != null) {
                variant.key = value
            }

            value?.let { variant.value = it }
            metadata?.let { variant.metadata = it }
            payload?.let { variant.payload = it }
            experimentKey?.let { variant.expKey = it }

            variant
        }

        else -> Variant()
    }
}
