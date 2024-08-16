package com.amplitude.experiment.storage

internal interface Storage {
    fun get(key: String): Map<String, String>

    fun put(
        key: String,
        value: Map<String, String>,
    )

    fun delete(key: String)
}
