package com.amplitude.experiment.analytics

import com.amplitude.experiment.ExperimentUser
import com.amplitude.experiment.ExperimentClient
import com.amplitude.experiment.ExperimentUserProvider

/**
 * Analytics event for tracking events generated from the experiment SDK client.
 * These events are sent to the implementation provided by an
 * [ExperimentAnalyticsProvider].
 */
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
     * Custom user properties from the [ExperimentUser] stored by the
     * [ExperimentClient]. Also includes merged properties provided by the
     * [ExperimentUserProvider].
     */
    val userProperties: Map<String, Any?>?
}
