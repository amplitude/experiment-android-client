package com.amplitude.experiment.analytics

import com.amplitude.experiment.ExperimentUser
import com.amplitude.experiment.Variant

/**
 * Event for tracking a user's exposure to a variant. This event will not count
 * towards your analytics event volume.
 */
class ExposureEvent(
    /**
     * The user exposed to the flag/experiment variant.
     */
    val user: ExperimentUser,

    /**
     * The key of the flag/experiment that the user has been exposed to.
     */
    val key: String,

    /**
     * The variant of the flag/experiment that the user has been exposed to.
     */
    val variant: Variant,
): ExperimentAnalyticsEvent {
    override val name: String = "[Experiment] Exposure"
    override val properties: Map<String, String?> = mapOf(
        "key" to key,
        "variant" to variant.value,
    )
    override val userProperties: Map<String, Any?> = mapOf(
        "[Experiment] $key" to variant.value
    )
}
