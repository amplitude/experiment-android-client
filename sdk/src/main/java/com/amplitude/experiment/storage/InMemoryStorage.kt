package com.amplitude.experiment.storage

import com.amplitude.experiment.Variant
import java.util.concurrent.ConcurrentHashMap

internal class InMemoryStorage : Storage {

    private val data: MutableMap<String, Variant> = ConcurrentHashMap()

    override fun put(key: String, variant: Variant) {
        data[key] = variant
    }

    override operator fun get(key: String): Variant? {
        return data[key]
    }

    override fun getAll(): Map<String, Variant> {
        return data.toMap()
    }

    override fun clear() {
        data.clear()
    }
}