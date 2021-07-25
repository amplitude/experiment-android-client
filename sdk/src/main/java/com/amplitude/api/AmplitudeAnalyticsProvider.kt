package com.amplitude.api

import com.amplitude.experiment.analytics.ExperimentAnalyticsEvent
import com.amplitude.experiment.analytics.ExperimentAnalyticsProvider
import org.json.JSONObject

/**
 * Provides a tracking implementation for standard experiment events generated
 * by the client (e.g. exposure) using an [AmplitudeClient] instance.
 */
class AmplitudeAnalyticsProvider(
    private val amplitudeClient: AmplitudeClient,
): ExperimentAnalyticsProvider {

    override fun track(event: ExperimentAnalyticsEvent) {
        amplitudeClient.logEvent(event.name, JSONObject(event.properties))
    }
}