package com.amplitude.experiment.storage

import com.amplitude.experiment.EvaluationFlag
import com.amplitude.experiment.EvaluationSegment
import com.amplitude.experiment.EvaluationVariant
import com.amplitude.experiment.Variant
import com.amplitude.experiment.util.toMap
import org.json.JSONObject

internal class LoadStoreCache<V>(
    private val namespace: String,
    private val storage: Storage,
    private val transformer: ((value: Any) -> V)
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
        if (rawValues == null) {
            clear()
            return
        }
        val jsonValues = JSONObject(rawValues).toMap()
        val values = jsonValues.mapNotNull { entry ->
            try {
                entry.key to transformer.invoke(entry.value!!)
            } catch (e: Exception) {
                null
            }
        }.toMap()
        clear()
        putAll(values)
    }

    fun store(values: Map<String, V> = cache) = synchronized(cache) {
        storage.put(namespace, JSONObject(values).toString())
    }
}

internal fun getVariantStorage(deploymentKey: String, instanceName: String, storage: Storage): LoadStoreCache<Variant> {
    val truncatedDeployment = deploymentKey.takeLast(6)
    val namespace = "amp-exp-$instanceName-$truncatedDeployment"
    return LoadStoreCache(namespace, storage, ::transformVariantFromStorage)
}

//fun getFlagStorage(deploymentKey: String, instanceName: String, storage: Storage): LoadStoreCache<Any> {
//    val truncatedDeployment = deploymentKey.takeLast(6)
//    val namespace = "amp-exp-$instanceName-$truncatedDeployment-flags"
//    return LoadStoreCache(namespace, storage)
//}

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
