package com.amplitude.experiment

import com.amplitude.analytics.connector.EventBridge
import com.amplitude.analytics.connector.AnalyticsEvent
import com.amplitude.experiment.analytics.ExperimentAnalyticsEvent
import com.amplitude.experiment.analytics.ExperimentAnalyticsProvider

internal class ConnectorAnalyticsProvider(
    private val eventBridge: EventBridge
): ExperimentAnalyticsProvider {

    override fun track(event: ExperimentAnalyticsEvent) {
        eventBridge.logEvent(
            AnalyticsEvent(
                eventType = "\$exposure",
                eventProperties = mapOf(
                    "flag_key" to event.key,
                    "variant" to event.variant.value
                ).filterNull()
            )
        )
    }

    override fun setUserProperty(event: ExperimentAnalyticsEvent) {
    }

    override fun unsetUserProperty(event: ExperimentAnalyticsEvent) {
        eventBridge.logEvent(
            AnalyticsEvent(
                eventType = "\$exposure",
                eventProperties = mapOf(
                    "flag_key" to event.key,
                ).filterNull()
            )
        )
    }
}

private fun Map<String, String?>.filterNull(): Map<String, String> {
    return filterValues { it != null }.mapValues { it.value!! }
}
