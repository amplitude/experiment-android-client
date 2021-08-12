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
        if (event.userProperties != null) {
            amplitudeClient.setUserProperties(JSONObject(event.userProperties!!.toMutableMap()))
        }
        amplitudeClient.logEvent(event.name, JSONObject(event.properties))
    }
}
