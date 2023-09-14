package com.amplitude.experiment.util

import com.amplitude.experiment.analytics.ExperimentAnalyticsEvent
import com.amplitude.experiment.analytics.ExperimentAnalyticsProvider

internal class SessionAnalyticsProvider(
    private val analyticsProvider: ExperimentAnalyticsProvider,
): ExperimentAnalyticsProvider {

    private val lock = Any()
    private val setProperties = mutableMapOf<String, String>()
    private val unsetProperties = mutableSetOf<String>()

    override fun track(event: ExperimentAnalyticsEvent) {
        val variant = event.variant.key ?: return
        synchronized(lock) {
            if (setProperties[event.key] == variant) {
                return
            } else {
                setProperties[event.key] = variant
                unsetProperties.remove(event.key)
            }
        }
        analyticsProvider.track(event)
    }

    override fun setUserProperty(event: ExperimentAnalyticsEvent) {
        val variant = event.variant.key ?: return
        synchronized(lock) {
            if (setProperties[event.key] == variant) {
                return@setUserProperty
            }
        }
        analyticsProvider.setUserProperty(event)
    }

    override fun unsetUserProperty(event: ExperimentAnalyticsEvent) {
        synchronized(lock) {
            if (unsetProperties.contains(event.key)) {
                return@unsetUserProperty
            } else {
                unsetProperties.add(event.key)
                setProperties.remove(event.key)
            }
        }
        analyticsProvider.unsetUserProperty(event)
    }
}
