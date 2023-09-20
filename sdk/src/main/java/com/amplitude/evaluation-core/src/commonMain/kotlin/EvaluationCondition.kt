package com.amplitude.experiment.evaluation

import kotlinx.serialization.Serializable

@Serializable
data class EvaluationCondition(
    // How to select the property from the evaluation state.
    val selector: List<String>,

    // The operator.
    val op: String,

    // The values to compare to.
    val values: Set<String>
)
