package com.amplitude.experiment.evaluation

import kotlinx.serialization.Serializable

@Serializable
data class EvaluationAllocation(
    val range: List<Int>,
    val distributions: List<EvaluationDistribution>,
)
