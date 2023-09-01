package com.amplitude.experiment.storage

import com.amplitude.experiment.Variant

internal interface Storage {
    suspend fun get(key: String): String?
    suspend fun put(key: String, value: String)
    suspend fun delete(key: String)
}
