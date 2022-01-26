package com.amplitude.experiment

import com.amplitude.core.AnalyticsConnector
import com.amplitude.core.AnalyticsEvent
import com.amplitude.experiment.analytics.ExperimentAnalyticsEvent
import com.amplitude.experiment.analytics.ExperimentAnalyticsProvider

internal class CoreAnalyticsProvider(
    private val analyticsConnector: AnalyticsConnector
): ExperimentAnalyticsProvider {

    override fun track(event: ExperimentAnalyticsEvent) {
        val eventProperties: Map<String, String> = event.properties
            .filterValues { it != null }
            .mapValues { it.value!! }
        analyticsConnector.logEvent(
            AnalyticsEvent(
                eventType = event.name,
                eventProperties = eventProperties
            )
        )
    }

    override fun setUserProperty(event: ExperimentAnalyticsEvent) {
        val variant = event.variant.value ?: return
        analyticsConnector.logEvent(
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
        analyticsConnector.logEvent(
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
