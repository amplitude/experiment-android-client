package com.amplitude.core

import org.junit.Assert
import org.junit.Test

class AnalyticsConnectorTest {

    @Test
    fun `test addEventListener, logEvent, listener called`() {
        val testEvent = AnalyticsEvent("test")
        val analyticsConnector = AnalyticsConnectorImpl()
        analyticsConnector.addEventListener {
            Assert.assertEquals(testEvent, it)
        }
        analyticsConnector.logEvent(testEvent)
    }

    @Test
    fun `test multiple logEvent, late addEventListener, listener called`() {
        val testEvent0 = AnalyticsEvent("test0")
        val testEvent1 = AnalyticsEvent("test1")
        val testEvent2 = AnalyticsEvent("test2")
        val analyticsConnector = AnalyticsConnectorImpl()
        analyticsConnector.logEvent(testEvent0)
        analyticsConnector.logEvent(testEvent1)
        var eventCount = 0
        analyticsConnector.addEventListener {
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