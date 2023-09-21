@file:UseSerializers(AnySerializer::class)

package com.amplitude.experiment.evaluation

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class EvaluationSegment(
    // How to bucket the user given a matching condition.
    val bucket: EvaluationBucket? = null,

    // The targeting conditions. On match, bucket the user. The outer list
    // is operated with "OR" and the inner list is operated with "AND".
    val conditions: List<List<EvaluationCondition>>? = null,

    // The default variant if the conditions match but either no bucket is set,
    // or the bucket does not produce a variant.
    val variant: String? = null,

    // An object of metadata for this segment. For example, contains the
    // segment name and may contain the experiment key associated with this
    // segment. The bucketing segment's metadata is passed back in the
    // evaluation result along with the flag metadata.
    val metadata: Map<String, Any?>? = null
)
