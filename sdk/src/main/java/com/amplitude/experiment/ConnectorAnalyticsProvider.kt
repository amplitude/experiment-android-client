package com.amplitude.experiment

import com.amplitude.analytics.connector.EventBridge
import com.amplitude.analytics.connector.AnalyticsEvent
import com.amplitude.experiment.analytics.ExperimentAnalyticsEvent
import com.amplitude.experiment.analytics.ExperimentAnalyticsProvider
import com.amplitude.experiment.analytics.ExposureEvent

internal class ConnectorAnalyticsProvider(
    private val eventBridge: EventBridge
): ExperimentAnalyticsProvider {

    override fun track(event: ExperimentAnalyticsEvent) {
        if (event !is ExposureEvent) {
            return
        }
        val variant = if (event.source.isFallback()) {
            null
        } else {
            event.variant.value
        }
        val eventName = "\$exposure"
        val eventProperties = mapOf(
            "flag_key" to event.key,
            "variant" to variant
        ).filterNull()
        eventBridge.logEvent(
            AnalyticsEvent(
                eventType = eventName,
                eventProperties = eventProperties
            )
        )
    }

    override fun setUserProperty(event: ExperimentAnalyticsEvent) {
    }

    override fun unsetUserProperty(event: ExperimentAnalyticsEvent) {
        track(event)
    }
}

private fun Map<String, String?>.filterNull(): Map<String, String> {
    return filterValues { it != null }.mapValues { it.value!! }
}
