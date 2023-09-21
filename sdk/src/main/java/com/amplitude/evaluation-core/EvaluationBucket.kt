package com.amplitude.experiment.evaluation

import kotlinx.serialization.Serializable

@Serializable
data class EvaluationBucket(
    // How to select the prop from the context.
    val selector: List<String>,

    // A random string used to salt the bucketing value prior to hashing.
    val salt: String,

    // Determines which variant, if any, should be returned based on the
    // result of the hash functions.
    val allocations: List<EvaluationAllocation>,
)
