package com.amplitude.core

data class AnalyticsEvent(
    val eventType: String,
    val eventProperties: Map<String, Any>? = null,
    val userProperties: Map<String, Map<String, Any>>? = null,
)

interface AnalyticsConnector {

    fun logEvent(event: AnalyticsEvent)
    fun addEventListener(listener: (AnalyticsEvent) -> Unit)
    fun removeEventListener(listener: (AnalyticsEvent) -> Unit)
}

internal class AnalyticsConnectorImpl : AnalyticsConnector {

    private val listenersLock = Any()
    private val listeners: MutableSet<(AnalyticsEvent) -> Unit> = mutableSetOf()

    override fun logEvent(event: AnalyticsEvent) {
        val safeListeners = synchronized(listenersLock) {
            listeners
        }
        for (listener in safeListeners) {
            listener(event)
        }
    }

    override fun addEventListener(listener: (AnalyticsEvent) -> Unit) {
        synchronized(listenersLock) {
            listeners.add(listener)
        }
    }

    override fun removeEventListener(listener: (AnalyticsEvent) -> Unit) {
        synchronized(listenersLock) {
            listeners.remove(listener)
        }
    }
}