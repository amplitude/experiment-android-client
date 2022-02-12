package com.amplitude.experiment

import com.amplitude.analytics.connector.AnalyticsEvent
import com.amplitude.analytics.connector.AnalyticsEventReceiver
import com.amplitude.analytics.connector.EventBridge
import com.amplitude.experiment.util.SessionExposureTrackingProvider
import org.junit.Assert
import org.junit.Test

class TestEventBridge : EventBridge {
    var logEventCount = 0
    var recentEvent: AnalyticsEvent? = null
    override fun logEvent(event: AnalyticsEvent) {
        recentEvent = event
        logEventCount++
    }
    override fun setEventReceiver(receiver: AnalyticsEventReceiver?) {}
}

class ConnectorExposureTrackingProviderTest {

    @Test
    fun `track called once each per variant for different flag keys`() {
        val eventBridge = TestEventBridge()
        val connectorExposureTrackingProvider = SessionExposureTrackingProvider(ConnectorExposureTrackingProvider(eventBridge))

        // Track event with variant

        val exposureEvent1 = Exposure("test-key-1", "test")
        val expectedTrack1 = AnalyticsEvent("\$exposure", mapOf("flag_key" to "test-key-1", "variant" to "test"))

        connectorExposureTrackingProvider.track(exposureEvent1)
        Assert.assertEquals(expectedTrack1, eventBridge.recentEvent)
        Assert.assertEquals(eventBridge.logEventCount, 1)
        repeat(10) {
            eventBridge.recentEvent = null
            connectorExposureTrackingProvider.track(exposureEvent1)
            Assert.assertEquals(null, eventBridge.recentEvent)
        }
        Assert.assertEquals(eventBridge.logEventCount, 1)

        // Track new flag key event with same variant

        val exposureEvent2 = Exposure("test-key-2", "test")
        val expectedTrack2 = AnalyticsEvent("\$exposure", mapOf("flag_key" to "test-key-2", "variant" to "test"))

        eventBridge.recentEvent = null
        connectorExposureTrackingProvider.track(exposureEvent2)
        Assert.assertEquals(expectedTrack2, eventBridge.recentEvent)
        Assert.assertEquals(eventBridge.logEventCount, 2)
        repeat(10) {
            eventBridge.recentEvent = null
            connectorExposureTrackingProvider.track(exposureEvent2)
            Assert.assertEquals(null, eventBridge.recentEvent)
        }
        Assert.assertEquals(eventBridge.logEventCount, 2)
    }

    @Test
    fun `track called once each per variant for the same flag key`() {
        val eventBridge = TestEventBridge()
        val connectorExposureTrackingProvider = SessionExposureTrackingProvider(ConnectorExposureTrackingProvider(eventBridge))

        // Track event with variant

        val exposureEvent1 = Exposure("test-key", "test")
        val expectedTrack1 = AnalyticsEvent("\$exposure", mapOf("flag_key" to "test-key", "variant" to "test"))

        connectorExposureTrackingProvider.track(exposureEvent1)
        Assert.assertEquals(expectedTrack1, eventBridge.recentEvent)
        Assert.assertEquals(eventBridge.logEventCount, 1)
        repeat(10) {
            eventBridge.recentEvent = null
            connectorExposureTrackingProvider.track(exposureEvent1)
            Assert.assertEquals(null, eventBridge.recentEvent)
        }
        Assert.assertEquals(eventBridge.logEventCount, 1)

        // Track same flag key event with new variant

        val exposureEvent2 = Exposure("test-key", "test2")
        val expectedTrack2 = AnalyticsEvent("\$exposure", mapOf("flag_key" to "test-key", "variant" to "test2"))

        eventBridge.recentEvent = null
        connectorExposureTrackingProvider.track(exposureEvent2)
        Assert.assertEquals(expectedTrack2, eventBridge.recentEvent)
        Assert.assertEquals(eventBridge.logEventCount, 2)
        repeat(10) {
            eventBridge.recentEvent = null
            connectorExposureTrackingProvider.track(exposureEvent2)
            Assert.assertEquals(null, eventBridge.recentEvent)
        }
        Assert.assertEquals(eventBridge.logEventCount, 2)

        // Track event with no variant

        val exposureEvent3 = Exposure("test-key", null)
        val expectedTrack3 = AnalyticsEvent("\$exposure", mapOf("flag_key" to "test-key"))

        eventBridge.recentEvent = null
        connectorExposureTrackingProvider.track(exposureEvent3)
        Assert.assertEquals(expectedTrack3, eventBridge.recentEvent)
        Assert.assertEquals(eventBridge.logEventCount, 3)
        repeat(10) {
            eventBridge.recentEvent = null
            connectorExposureTrackingProvider.track(exposureEvent3)
            Assert.assertEquals(null, eventBridge.recentEvent)
        }
        Assert.assertEquals(eventBridge.logEventCount, 3)
    }
}
