package com.amplitude.experiment.storage

internal interface Storage {
    suspend fun get(key: String): String?
    suspend fun put(key: String, value: String)
    suspend fun delete(key: String)
}
