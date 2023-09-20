package com.amplitude.experiment.evaluation

import kotlinx.serialization.Serializable

@Serializable
data class EvaluationDistribution(
    // The key of the variant to deliver if this range matches.
    val variant: String,

    // The distribution range [start, end), where the max value is 42949672.
    // E.g. [0, 42949673] = [0%, 100%]
    val range: List<Int>,
)
