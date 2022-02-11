package com.amplitude.experiment

import com.amplitude.analytics.connector.EventBridge
import com.amplitude.analytics.connector.AnalyticsEvent

internal class ConnectorExposureTrackingProvider(
    private val eventBridge: EventBridge
): ExposureTrackingProvider {

    override fun track(exposureEvent: ExposureEvent) {
        eventBridge.logEvent(
            AnalyticsEvent(
                eventType = "\$exposure",
                eventProperties = mapOf(
                    "flag_key" to exposureEvent.flagKey,
                    "variant" to exposureEvent.variant,
                ).filterNull()
            )
        )
    }
}

private fun Map<String, String?>.filterNull(): Map<String, String> {
    return filterValues { it != null }.mapValues { it.value!! }
}
