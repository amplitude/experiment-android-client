package com.amplitude.experiment

import com.amplitude.core.AnalyticsConnector
import com.amplitude.core.AnalyticsEvent
import com.amplitude.experiment.analytics.ExperimentAnalyticsEvent
import com.amplitude.experiment.analytics.ExperimentAnalyticsProvider

internal class CoreAnalyticsProvider(
    private val analyticsConnector: AnalyticsConnector
): ExperimentAnalyticsProvider {

    private val setProperties = mutableMapOf<String, String>()
    private val unsetProperties = mutableSetOf<String>()

    override fun track(event: ExperimentAnalyticsEvent) {
        val variant = event.variant.value ?: return
        if (setProperties[event.key] == variant) {
            return
        } else {
            setProperties[event.key] = variant
            unsetProperties.remove(event.key)
        }
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
        if (setProperties[event.key] == variant) {
            return
        }
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
        if (unsetProperties.contains(event.key)) {
            return
        } else {
            unsetProperties.add(event.key)
            setProperties.remove(event.key)
        }
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
