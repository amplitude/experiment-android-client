package com.amplitude.experiment

data class Variant @JvmOverloads constructor(
    @JvmField val value: String? = null,
    @JvmField val payload: Any? = null,
)
