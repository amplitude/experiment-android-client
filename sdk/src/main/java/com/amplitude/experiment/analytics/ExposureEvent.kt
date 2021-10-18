package com.amplitude.experiment.analytics

import com.amplitude.experiment.ExperimentUser
import com.amplitude.experiment.Variant
import com.amplitude.experiment.VariantSource

/**
 * Event for tracking a user's exposure to a variant. This event will not count
 * towards your analytics event volume.
 */
class ExposureEvent(
    /**
     * The user exposed to the flag/experiment variant.
     */
    override val user: ExperimentUser,

    /**
     * The key of the flag/experiment that the user has been exposed to.
     */
    override val key: String,

    /**
     * The variant of the flag/experiment that the user has been exposed to.
     */
    override val variant: Variant,

    /**
     * The source of the determination of the variant.
     */
    val source: VariantSource
): ExperimentAnalyticsEvent {
    override val name: String = "[Experiment] Exposure"
    override val properties: Map<String, String?> = mapOf(
        "key" to key,
        "variant" to variant.value,
        "source" to source.toString(),
    )
    override val userProperties: Map<String, Any?> = mapOf(
        "[Experiment] $key" to variant.value
    )
    override val userProperty: String = "[Experiment] $key"
}
