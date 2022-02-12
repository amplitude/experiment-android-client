package com.amplitude.experiment.analytics

import com.amplitude.experiment.ExperimentUser
import com.amplitude.experiment.Variant

/**
 * Analytics event for tracking events generated from the experiment SDK client.
 * These events are sent to the implementation provided by an
 * [ExperimentAnalyticsProvider].
 */
@Deprecated("Use ExposureTrackingProvider instead")
interface ExperimentAnalyticsEvent {
    /**
     * The name of the event. Should be passed as the event tracking name to the
     * analytics implementation provided by the [ExperimentAnalyticsProvider].
     */
    val name: String

    /**
     * Properties for the analytics event. Should be passed as the event
     * properties to the analytics implementation provided by the
     * [ExperimentAnalyticsProvider].
     */
    val properties: Map<String, String?>

    /**
     * User properties to identify with the user prior to sending the event.
     */
    val userProperties: Map<String, Any?>?

    /**
     * The user exposed to the flag/experiment variant.
     */
    val user: ExperimentUser

    /**
     * The key of the flag/experiment that the user has been exposed to.
     */
    val key: String

    /**
     * The variant of the flag/experiment that the user has been exposed to.
     */
    val variant: Variant

    /**
     * The user property for the flag/experiment (auto-generated from the key)
     */
    val userProperty: String
}
