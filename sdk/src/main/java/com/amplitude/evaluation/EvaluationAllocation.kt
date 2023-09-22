package com.amplitude.experiment.evaluation

import kotlinx.serialization.Serializable

@Serializable
internal data class EvaluationAllocation(
    val range: List<Int>,
    val distributions: List<EvaluationDistribution>,
)
