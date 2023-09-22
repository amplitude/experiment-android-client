package com.amplitude.experiment.storage

import com.amplitude.experiment.Variant
import com.amplitude.experiment.evaluation.EvaluationFlag
import com.amplitude.experiment.util.toFlag
import com.amplitude.experiment.util.toVariant
import com.amplitude.experiment.util.toJson

internal class LoadStoreCache<V : Any>(
    private val namespace: String,
    private val storage: Storage,
    private val transformer: ((value: String) -> V?)
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
        val values = rawValues.mapNotNull { entry ->
            try {
                val value = transformer.invoke(entry.value)
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

    fun store(values: MutableMap<String, V> = cache) = synchronized(cache) {
        val stringValues = values.mapNotNull { entry ->
            try {
                val value = transformStringFromV(entry.value)
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
    return LoadStoreCache(namespace, storage, ::transformVariantFromStorage)
}

internal fun getFlagStorage(
    deploymentKey: String,
    instanceName: String,
    storage: Storage
): LoadStoreCache<EvaluationFlag> {
    val truncatedDeployment = deploymentKey.takeLast(6)
    val namespace = "amp-exp-$instanceName-$truncatedDeployment-flags"
    return LoadStoreCache(namespace, storage, ::transformFlagFromStorage)
}

internal fun transformVariantFromStorage(storageValue: String): Variant? {
    return storageValue.toVariant()
}

internal fun transformFlagFromStorage(storageValue: String): EvaluationFlag? {
    return storageValue.toFlag()
}

internal fun transformStringFromV(value: Any): String? {
    return when (value) {
        is Variant -> value.toJson()
        is EvaluationFlag -> value.toJson()
        else -> null
    }
}

