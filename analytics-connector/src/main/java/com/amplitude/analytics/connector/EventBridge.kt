package com.amplitude.analytics.connector

import java.util.concurrent.ArrayBlockingQueue

typealias AnalyticsEventReceiver = (AnalyticsEvent) -> Unit

data class AnalyticsEvent(
    val eventType: String,
    val eventProperties: Map<String, Any>? = null,
    val userProperties: Map<String, Map<String, Any>>? = null,
)

interface EventBridge {

    fun logEvent(event: AnalyticsEvent)
    fun setEventReceiver(receiver: AnalyticsEventReceiver?)
}

internal class EventBridgeImpl : EventBridge {

    private val lock = Any()
    private var receiver: AnalyticsEventReceiver? = null
    private val queue = ArrayBlockingQueue<AnalyticsEvent>(512)

    override fun logEvent(event: AnalyticsEvent) {
        synchronized(lock) {
            if (this.receiver == null) {
                queue.offer(event)
            }
            this.receiver
        }?.invoke(event)
    }

    override fun setEventReceiver(receiver: AnalyticsEventReceiver?) {
        synchronized(lock) {
            this.receiver = receiver
            mutableListOf<AnalyticsEvent>().apply {
                queue.drainTo(this)
            }
        }.forEach { event ->
            receiver?.invoke(event)
        }
    }
}
