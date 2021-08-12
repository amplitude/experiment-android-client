package com.amplitude.experiment.analytics

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
     * User properties to identify with the user prior to sending the event.
     */
    val userProperties: Map<String, Any?>?
}
