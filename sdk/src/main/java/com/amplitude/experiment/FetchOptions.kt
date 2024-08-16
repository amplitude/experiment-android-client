package com.amplitude.experiment

data class FetchOptions
    @JvmOverloads
    constructor(
        @JvmField val flagKeys: List<String>? = null,
    )
