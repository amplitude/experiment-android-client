package com.amplitude.experiment.storage

import com.amplitude.experiment.Variant
import com.amplitude.experiment.evaluation.EvaluationFlag
import com.amplitude.experiment.evaluation.json
import com.amplitude.experiment.util.toVariant
import com.amplitude.experiment.util.toJson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

internal class LoadStoreCache<V>(
    private val namespace: String,
    private val storage: Storage,
    private val decoder: ((value: String) -> V?),
    private val encoder: ((value: V) -> String)
) {
    private val cache: MutableMap<String, V> = mutableMapOf()

    fun get(key: String): V?  {
        return cache[key]
    }

    fun getAll(): Map<String, V>  {
        return HashMap(cache)
    }

    fun put(key: String, value: V)  {
        cache[key] = value
    }

    fun putAll(values: Map<String, V>)  {
        cache.putAll(values)
    }

    fun remove(key: String)  {
        cache.remove(key)
    }

    fun clear()  {
        cache.clear()
    }

    fun load()  {
        val rawValues = storage.get(namespace)
        val values = rawValues.mapNotNull { entry ->
            try {
                val value = decoder.invoke(entry.value)
                if (value != null) {
                    entry.key to value
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }.toMap()
        clear()
        putAll(values)
    }

    fun store(values: MutableMap<String, V> = cache)  {
        val stringValues = values.mapNotNull { entry ->
            try {
                val value = encoder(entry.value)
                if (value != null) {
                    entry.key to value
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }.toMap()
        storage.put(namespace, stringValues)
    }
}

internal fun getVariantStorage(deploymentKey: String, instanceName: String, storage: Storage): LoadStoreCache<Variant> {
    val truncatedDeployment = deploymentKey.takeLast(6)
    val namespace = "amp-exp-$instanceName-$truncatedDeployment"
    return LoadStoreCache(namespace, storage, ::decodeVariantFromStorage, ::encodeVariantToStorage)
}

internal fun getFlagStorage(
    deploymentKey: String,
    instanceName: String,
    storage: Storage
): LoadStoreCache<EvaluationFlag> {
    val truncatedDeployment = deploymentKey.takeLast(6)
    val namespace = "amp-exp-$instanceName-$truncatedDeployment-flags"
    return LoadStoreCache(namespace, storage, ::decodeFlagFromStorage, ::encodeFlagToStorage)
}

internal fun decodeVariantFromStorage(storageValue: String): Variant? {
    return storageValue.toVariant()
}

internal fun decodeFlagFromStorage(storageValue: String): EvaluationFlag? {
    return json.decodeFromString<EvaluationFlag>(storageValue)
}

internal fun encodeVariantToStorage(value: Variant): String {
    return value.toJson()
}

internal fun encodeFlagToStorage(value: EvaluationFlag): String {
    return json.encodeToString(value)
}
