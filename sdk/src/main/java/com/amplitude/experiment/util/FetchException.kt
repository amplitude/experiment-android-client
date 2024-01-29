package com.amplitude.experiment.util

import okio.IOException

internal class FetchException(
    val statusCode: Int,
    message: String
) : IOException(message)
