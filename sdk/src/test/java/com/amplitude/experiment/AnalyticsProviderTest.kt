package com.amplitude.experiment

import com.amplitude.analytics.connector.AnalyticsEvent
import com.amplitude.analytics.connector.AnalyticsEventReceiver
import com.amplitude.analytics.connector.EventBridge
import com.amplitude.experiment.analytics.ExposureEvent
import com.amplitude.experiment.util.SessionAnalyticsProvider
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

class AnalyticsProviderTest {

    private val exposureEvent1 = ExposureEvent(
        user = ExperimentUser(),
        key = "test-key",
        variant = Variant("test"),
        source = VariantSource.LOCAL_STORAGE,
    )
    private val exposureEvent2 = ExposureEvent(
        user = ExperimentUser(),
        key = "test-key",
        variant = Variant("test2"),
        source = VariantSource.LOCAL_STORAGE,
    )
    private val expectedSet1 = AnalyticsEvent(
        eventType = "\$identify",
        eventProperties = null,
        userProperties = mapOf(
            "\$set" to mapOf("[Experiment] test-key" to "test")
        )
    )
    private val expectedSet2 = AnalyticsEvent(
        eventType = "\$identify",
        eventProperties = null,
        userProperties = mapOf(
            "\$set" to mapOf("[Experiment] test-key" to "test2")
        )
    )
    private val expectedTrack1 = AnalyticsEvent(
        eventType = "[Experiment] Exposure",
        eventProperties = exposureEvent1.properties
            .filterValues { it != null }
            .mapValues { it.value!! },
        userProperties = null,
    )

    private val expectedTrack2 = AnalyticsEvent(
        eventType = "[Experiment] Exposure",
        eventProperties = exposureEvent2.properties
            .filterValues { it != null }
            .mapValues { it.value!! },
        userProperties = null,
    )
    private val expectedUnset = AnalyticsEvent(
        eventType = "\$identify",
        eventProperties = null,
        userProperties = mapOf(
            "\$unset" to mapOf("[Experiment] test-key" to "-")
        )
    )


    @Test
    fun `set and track called once each per variant`() {
        val eventBridge = TestEventBridge()
        val coreAnalyticsProvider = SessionAnalyticsProvider(CoreAnalyticsProvider(eventBridge))

        coreAnalyticsProvider.setUserProperty(exposureEvent1)
        Assert.assertEquals(expectedSet1, eventBridge.recentEvent)
        coreAnalyticsProvider.track(exposureEvent1)
        Assert.assertEquals(expectedTrack1, eventBridge.recentEvent)
        repeat(10) {
            coreAnalyticsProvider.setUserProperty(exposureEvent1)
            Assert.assertEquals(expectedTrack1, eventBridge.recentEvent)
            coreAnalyticsProvider.track(exposureEvent1)
            Assert.assertEquals(expectedTrack1, eventBridge.recentEvent)
        }
        Assert.assertEquals(eventBridge.logEventCount, 2)

        coreAnalyticsProvider.setUserProperty(exposureEvent2)
        Assert.assertEquals(expectedSet2, eventBridge.recentEvent)
        coreAnalyticsProvider.track(exposureEvent2)
        Assert.assertEquals(expectedTrack2, eventBridge.recentEvent)
        repeat(10) {
            coreAnalyticsProvider.setUserProperty(exposureEvent2)
            Assert.assertEquals(expectedTrack2, eventBridge.recentEvent)
            coreAnalyticsProvider.track(exposureEvent2)
            Assert.assertEquals(expectedTrack2, eventBridge.recentEvent)
        }
        Assert.assertEquals(eventBridge.logEventCount, 4)
    }

    @Test
    fun `unset called once per key`() {
        val eventBridge = TestEventBridge()
        val coreAnalyticsProvider = SessionAnalyticsProvider(CoreAnalyticsProvider(eventBridge))
        val exposureEvent1 = ExposureEvent(ExperimentUser(), "test-key", Variant("test"), VariantSource.LOCAL_STORAGE)
        repeat(10) {
            coreAnalyticsProvider.unsetUserProperty(exposureEvent1)
            Assert.assertEquals(expectedUnset, eventBridge.recentEvent)
        }
        Assert.assertEquals(1, eventBridge.logEventCount)
    }

    @Test
    fun `test property set tracked, unset, set tracked`() {
        val eventBridge = TestEventBridge()
        val coreAnalyticsProvider = CoreAnalyticsProvider(eventBridge)
        repeat(10) {
            // set
            coreAnalyticsProvider.setUserProperty(exposureEvent1)
            Assert.assertEquals(expectedSet1, eventBridge.recentEvent)
            // track
            coreAnalyticsProvider.track(exposureEvent1)
            Assert.assertEquals(expectedTrack1, eventBridge.recentEvent)
            // unset
            coreAnalyticsProvider.unsetUserProperty(exposureEvent1)
            Assert.assertEquals(expectedUnset, eventBridge.recentEvent)
        }
        Assert.assertEquals(eventBridge.logEventCount, 30)
    }
}