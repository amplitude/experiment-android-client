package com.amplitude.experiment.util

import okio.IOException

class FetchException internal constructor(
    val statusCode: Int,
    message: String
) : IOException(message)
