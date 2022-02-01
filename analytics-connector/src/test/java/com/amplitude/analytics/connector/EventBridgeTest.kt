package com.amplitude.analytics.connector

import com.amplitude.analytics.connector.AnalyticsEvent
import com.amplitude.analytics.connector.EventBridgeImpl
import org.junit.Assert
import org.junit.Test

class EventBridgeTest {

    @Test
    fun `test addEventListener, logEvent, listener called`() {
        val testEvent = AnalyticsEvent("test")
        val analyticsConnector = EventBridgeImpl()
        analyticsConnector.setEventReceiver {
            Assert.assertEquals(testEvent, it)
        }
        analyticsConnector.logEvent(testEvent)
    }

    @Test
    fun `test multiple logEvent, late addEventListener, listener called`() {
        val testEvent0 = AnalyticsEvent("test0")
        val testEvent1 = AnalyticsEvent("test1")
        val testEvent2 = AnalyticsEvent("test2")
        val analyticsConnector = EventBridgeImpl()
        analyticsConnector.logEvent(testEvent0)
        analyticsConnector.logEvent(testEvent1)
        var eventCount = 0
        analyticsConnector.setEventReceiver {
            when (eventCount) {
                0 ->  Assert.assertEquals(testEvent0, it)
                1 ->  Assert.assertEquals(testEvent1, it)
                2 ->  Assert.assertEquals(testEvent2, it)
                else -> Assert.fail("unexpected event")
            }
            eventCount++
        }
        analyticsConnector.logEvent(testEvent2)
        Assert.assertEquals(3, eventCount)
    }
}
