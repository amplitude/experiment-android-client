package com.amplitude.experiment.storage

interface Storage {
    fun get(key: String): String?
    fun put(key: String, value: String)
    fun delete(key: String)
}
