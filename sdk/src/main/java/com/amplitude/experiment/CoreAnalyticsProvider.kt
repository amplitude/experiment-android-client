package com.amplitude.experiment

import com.amplitude.analytics.connector.EventBridge
import com.amplitude.analytics.connector.AnalyticsEvent
import com.amplitude.experiment.analytics.ExperimentAnalyticsEvent
import com.amplitude.experiment.analytics.ExperimentAnalyticsProvider

internal class CoreAnalyticsProvider(
    private val eventBridge: EventBridge
): ExperimentAnalyticsProvider {

    override fun track(event: ExperimentAnalyticsEvent) {
        val eventProperties: Map<String, String> = event.properties
            .filterValues { it != null }
            .mapValues { it.value!! }
        eventBridge.logEvent(
            AnalyticsEvent(
                eventType = event.name,
                eventProperties = eventProperties
            )
        )
    }

    override fun setUserProperty(event: ExperimentAnalyticsEvent) {
        val variant = event.variant.value ?: return
        eventBridge.logEvent(
            AnalyticsEvent(
                "\$identify",
                null,
                mapOf(
                    "\$set" to mapOf(
                        event.userProperty to variant
                    )
                )
            )
        )
    }

    override fun unsetUserProperty(event: ExperimentAnalyticsEvent) {
        eventBridge.logEvent(
            AnalyticsEvent(
                "\$identify",
                null,
                mapOf(
                    "\$unset" to mapOf(
                        event.userProperty to "-"
                    )
                )
            )
        )
    }
}
