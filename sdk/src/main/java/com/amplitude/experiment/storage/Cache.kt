package com.amplitude.experiment.storage

import com.amplitude.experiment.Variant
import com.amplitude.experiment.evaluation.EvaluationFlag
import com.amplitude.experiment.evaluation.json
import com.amplitude.experiment.util.toJson
import com.amplitude.experiment.util.toVariant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

internal class LoadStoreCache<V>(
    private val namespace: String,
    private val storage: Storage,
    private val decoder: ((value: String) -> V?),
    private val encoder: ((value: V) -> String),
    private val onLoad: (() -> Unit)? = null,
) {
    private val cache: MutableMap<String, V> = mutableMapOf()
    private var isLoaded = false

    fun get(key: String): V? {
        if (!isLoaded) load()
        return cache[key]
    }

    fun getAll(): Map<String, V> {
        if (!isLoaded) load()
        return HashMap(cache)
    }

    fun put(
        key: String,
        value: V,
    ) {
        cache[key] = value
    }

    fun putAll(values: Map<String, V>) {
        cache.putAll(values)
    }

    fun remove(key: String) {
        cache.remove(key)
    }

    fun clear() {
        cache.clear()
    }

    fun load() {
        val rawValues = storage.get(namespace)
        val values =
            rawValues.mapNotNull { entry ->
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
        isLoaded = true
        onLoad?.invoke()
    }

    fun store(values: MutableMap<String, V> = cache) {
        val stringValues =
            values.mapNotNull { entry ->
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

internal class SingleValueStoreCache<V>(
    private val namespace: String,
    private val storage: Storage,
    private val decoder: ((value: String) -> V?),
    private val encoder: ((value: V) -> String),
) {
    private var cachedValue: V? = null
    private var isLoaded = false

    fun get(): V? {
        if (!isLoaded) load()
        return cachedValue
    }

    fun put(value: V) {
        cachedValue = value
    }

    fun load() {
        val rawValue = storage.getSingle(namespace)
        cachedValue = rawValue?.let { decoder.invoke(it) }
        isLoaded = true
    }

    fun store() {
        cachedValue?.let { value ->
            try {
                val stringValue = encoder(value)
                storage.putSingle(namespace, stringValue)
            } catch (e: Exception) {
                // Handle encoding error
            }
        }
    }
}

internal fun getVariantStorage(
    deploymentKey: String,
    instanceName: String,
    storage: Storage,
): LoadStoreCache<Variant> {
    val truncatedDeployment = deploymentKey.takeLast(6)
    val namespace = "amp-exp-$instanceName-$truncatedDeployment"
    return LoadStoreCache(namespace, storage, ::decodeVariantFromStorage, ::encodeVariantToStorage)
}

internal fun getFlagStorage(
    deploymentKey: String,
    instanceName: String,
    storage: Storage,
    merger: () -> Unit,
): LoadStoreCache<EvaluationFlag> {
    val truncatedDeployment = deploymentKey.takeLast(6)
    val namespace = "amp-exp-$instanceName-$truncatedDeployment-flags"
    return LoadStoreCache(namespace, storage, ::decodeFlagFromStorage, ::encodeFlagToStorage, merger)
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

internal fun getTrackAssignmentEventStorage(
    deploymentKey: String,
    instanceName: String,
    storage: Storage,
): SingleValueStoreCache<Boolean> {
    val truncatedDeployment = deploymentKey.takeLast(6)
    val namespace = "amp-exp-$instanceName-$truncatedDeployment-track-assignment"
    return SingleValueStoreCache(namespace, storage, ::decodeBooleanFromStorage, ::encodeBooleanToStorage)
}

internal fun decodeBooleanFromStorage(storageValue: String): Boolean? {
    return storageValue.toBooleanStrictOrNull()
}

internal fun encodeBooleanToStorage(value: Boolean): String {
    return value.toString()
}
