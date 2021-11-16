package com.amplitude.experiment

import com.amplitude.core.AnalyticsConnector
import com.amplitude.core.AnalyticsEvent
import com.amplitude.experiment.analytics.ExperimentAnalyticsEvent
import com.amplitude.experiment.analytics.ExperimentAnalyticsProvider

internal class CoreAnalyticsProvider(
    private val analyticsConnector: com.amplitude.core.AnalyticsConnector
): ExperimentAnalyticsProvider {

    override fun track(event: ExperimentAnalyticsEvent) {
        val eventProperties: Map<String, String> = event.properties
            .filterValues { it != null }
            .mapValues { it.value!! }
        analyticsConnector.logEvent(
            com.amplitude.core.AnalyticsEvent(
                eventType = event.name,
                eventProperties = eventProperties
            )
        )
    }

    override fun setUserProperty(event: ExperimentAnalyticsEvent) {
        analyticsConnector.logEvent(
            com.amplitude.core.AnalyticsEvent(
                "\$identify",
                null,
                mapOf(
                    "\$set" to mapOf(
                        event.userProperty to event.variant
                    )
                )
            )
        )
    }

    override fun unsetUserProperty(event: ExperimentAnalyticsEvent) {
        analyticsConnector.logEvent(
            com.amplitude.core.AnalyticsEvent(
                "\$identify",
                null,
                mapOf(
                    "\$unset" to mapOf(
                        event.userProperty to event.variant
                    )
                )
            )
        )
    }
}