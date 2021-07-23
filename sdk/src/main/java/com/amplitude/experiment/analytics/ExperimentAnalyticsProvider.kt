package com.amplitude.experiment.analytics

/**
 * Provides a analytics implementation for standard experiment events generated
 * by the client (e.g. [ExposureEvent]).
 */
interface ExperimentAnalyticsProvider {
    fun track(event: ExperimentAnalyticsEvent)
}