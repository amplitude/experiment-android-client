package com.amplitude.experiment.storage

import com.amplitude.experiment.Variant

internal interface Storage {
    fun put(key: String, variant: Variant)
    fun get(key: String): Variant?
    fun getAll(): Map<String, Variant>
    fun clear()
}