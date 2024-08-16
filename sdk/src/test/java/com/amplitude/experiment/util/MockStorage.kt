package com.amplitude.experiment.util

import com.amplitude.experiment.storage.Storage

internal class MockStorage() : Storage {
    private val store: MutableMap<String, Map<String, String>> = mutableMapOf()

    override fun get(key: String): Map<String, String> =
        synchronized(this) {
            return store[key] ?: mutableMapOf<String, String>()
        }

    override fun put(
        key: String,
        value: Map<String, String>,
    ): Unit =
        synchronized(this) {
            store[key] = value
        }

    override fun delete(key: String): Unit =
        synchronized(this) {
            store.remove(key)
        }
}
