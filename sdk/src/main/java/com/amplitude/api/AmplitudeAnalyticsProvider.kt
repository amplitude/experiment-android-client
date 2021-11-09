package com.amplitude.api

import com.amplitude.experiment.analytics.ExperimentAnalyticsEvent
import com.amplitude.experiment.analytics.ExperimentAnalyticsProvider
import org.json.JSONObject

/**
 * Provides a tracking implementation for standard experiment events generated
 * by the client (e.g. exposure) using an [AmplitudeClient] instance.
 */
@Deprecated("Update your version of the amplitude analytics SDK to X.X.X+ and for seamless " +
    "integration with the amplitude analytics SDK") // TODO X.X.X+
class AmplitudeAnalyticsProvider(
    private val amplitudeClient: AmplitudeClient,
): ExperimentAnalyticsProvider {

    override fun track(event: ExperimentAnalyticsEvent) {
        amplitudeClient.logEvent(event.name, JSONObject(event.properties))
    }

    override fun setUserProperty(event: ExperimentAnalyticsEvent) {
        amplitudeClient.setUserProperties(JSONObject(event.userProperties!!.toMutableMap()))
    }

    override fun unsetUserProperty(event: ExperimentAnalyticsEvent) {
        amplitudeClient.identify(Identify().unset(event.userProperty));
    }
}
