package com.amplitude.experiment

data class Variant(
    @JvmField val value: String,
    @JvmField val payload: Any? = null,
)
