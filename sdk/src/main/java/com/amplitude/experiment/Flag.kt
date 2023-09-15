package com.amplitude.experiment

data class EvaluationFlag(
    var key: String? = null,
    var variants: Map<String, EvaluationVariant>? = null,
    var segments: List<EvaluationSegment>? = null,
    var dependencies: List<String>? = null,
    var metadata: Map<String, Any>? = null
)

data class EvaluationVariant(
    val key: String? = null,
    val value: Any? = null,
    val payload: Any? = null,
    val metadata: Map<String, Any>? = null
)

data class EvaluationSegment(
    val bucket: EvaluationBucket? = null,
    val conditions: List<List<EvaluationCondition>>? = null,
    val variant: String? = null,
    val metadata: Map<String, Any>? = null
)

data class EvaluationBucket(
    val selector: List<String>,
    val salt: String,
    val allocations: List<EvaluationAllocation>
)

data class EvaluationCondition(
    val selector: List<String>,
    val op: String,
    val values: List<String>
)

data class EvaluationAllocation(
    val range: List<Int>,
    val distributions: List<EvaluationDistribution>
)

data class EvaluationDistribution(
    val variant: String,
    val range: List<Int>
)
