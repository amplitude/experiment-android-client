package com.amplitude.core

import java.util.concurrent.LinkedBlockingQueue

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

    private val lock = Any()
    private val listeners: MutableSet<(AnalyticsEvent) -> Unit> = mutableSetOf()
    private val queue = LinkedBlockingQueue<AnalyticsEvent>(256)

    override fun logEvent(event: AnalyticsEvent) {
        val safeListeners = synchronized(lock) {
            if (listeners.isEmpty()) {
                queue.offer(event)
            }
            listeners.toSet()
        }
        for (listener in safeListeners) {
            listener(event)
        }
    }

    override fun addEventListener(listener: (AnalyticsEvent) -> Unit) {
        val events = synchronized(lock) {
            listeners.add(listener)
            mutableListOf<AnalyticsEvent>().apply {
                queue.drainTo(this)
            }
        }
        for (event in events) {
            listener(event)
        }
    }

    override fun removeEventListener(listener: (AnalyticsEvent) -> Unit) {
        synchronized(lock) {
            listeners.remove(listener)
        }
    }
}