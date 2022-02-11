package com.amplitude.experiment.analytics

/**
 * Provides a analytics implementation for standard experiment events generated
 * by the client (e.g. [ExposureEvent]).
 */
@Deprecated("")
interface ExperimentAnalyticsProvider {

    /**
     * Wraps an analytics event track call. This is typically called by the
     * experiment client after setting user properties to track an
     * "[Experiment] Exposure" event
     * @param event see [ExperimentAnalyticsEvent]
     */
    fun track(event: ExperimentAnalyticsEvent)

    /**
     * Wraps an analytics identify or set user property call. This is typically
     * called by the experiment client before sending an
     * "[Experiment] Exposure" event.
     * @param event see [ExperimentAnalyticsEvent]
     */
    fun setUserProperty(event: ExperimentAnalyticsEvent)

    /**
     * Wraps an analytics unset user property call. This is typically
     * called by the experiment client when a user has been evaluated to use
     * a fallback variant.
     * @param event see [ExperimentAnalyticsEvent]
     */
    fun unsetUserProperty(event: ExperimentAnalyticsEvent)
}
