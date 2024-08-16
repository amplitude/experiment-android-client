package com.amplitude.experiment

import com.amplitude.analytics.connector.AnalyticsEvent
import com.amplitude.analytics.connector.EventBridge

internal class ConnectorExposureTrackingProvider(
    private val eventBridge: EventBridge,
) : ExposureTrackingProvider {
    override fun track(exposure: Exposure) {
        eventBridge.logEvent(
            AnalyticsEvent(
                eventType = "\$exposure",
                eventProperties =
                    mapOf(
                        "flag_key" to exposure.flagKey,
                        "variant" to exposure.variant,
                        "experiment_key" to exposure.experimentKey,
                        "metadata" to exposure.metadata,
                    ).filterNull(),
            ),
        )
    }
}

private fun <T> Map<String, T?>.filterNull(): Map<String, T> {
    return filterValues { it != null }.mapValues { it.value!! }
}
