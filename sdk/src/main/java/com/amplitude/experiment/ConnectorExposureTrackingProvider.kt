package com.amplitude.experiment

import com.amplitude.analytics.connector.EventBridge
import com.amplitude.analytics.connector.AnalyticsEvent

internal class ConnectorExposureTrackingProvider(
    private val eventBridge: EventBridge
): ExposureTrackingProvider {

    override fun track(exposure: Exposure) {
        eventBridge.logEvent(
            AnalyticsEvent(
                eventType = "\$exposure",
                eventProperties = mapOf(
                    "flag_key" to exposure.flagKey,
                    "variant" to exposure.variant,
                    "experiment_key" to exposure.experimentKey,
                ).filterNull()
            )
        )
    }
}

private fun Map<String, String?>.filterNull(): Map<String, String> {
    return filterValues { it != null }.mapValues { it.value!! }
}
