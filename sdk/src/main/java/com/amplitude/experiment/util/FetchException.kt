package com.amplitude.experiment.util

import okio.IOException

internal class FetchException internal constructor(
    val statusCode: Int,
    message: String
) : IOException(message)
