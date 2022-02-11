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

class ConnectorAnalyticsProviderTest {

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
        eventType = "\$exposure",
        eventProperties = mapOf(
            "flag_key" to "test-key",
            "variant" to "test",
        ),
    )
    private val expectedSet2 = AnalyticsEvent(
        eventType = "\$exposure",
        eventProperties = mapOf(
            "flag_key" to "test-key",
            "variant" to "test2",
        ),
    )
    private val expectedTrack1 = AnalyticsEvent(
        eventType = "\$exposure",
        eventProperties = mapOf(
            "flag_key" to "test-key",
            "variant" to "test",
        ),
    )

    private val expectedTrack2 = AnalyticsEvent(
        eventType = "\$exposure",
        eventProperties = mapOf(
            "flag_key" to "test-key",
            "variant" to "test2",
        ),
    )
    private val expectedUnset = AnalyticsEvent(
        eventType = "\$exposure",
        eventProperties = mapOf(
            "flag_key" to "test-key",
        ),
    )

    @Test
    fun `set and track called once each per variant`() {
        val eventBridge = TestEventBridge()
        val coreAnalyticsProvider = SessionAnalyticsProvider(ConnectorAnalyticsProvider(eventBridge))

        coreAnalyticsProvider.setUserProperty(exposureEvent1)
        Assert.assertEquals(null, eventBridge.recentEvent)
        coreAnalyticsProvider.track(exposureEvent1)
        Assert.assertEquals(expectedTrack1, eventBridge.recentEvent)
        Assert.assertEquals(eventBridge.logEventCount, 1)
        repeat(10) {
            eventBridge.recentEvent = null
            coreAnalyticsProvider.setUserProperty(exposureEvent1)
            Assert.assertEquals(null, eventBridge.recentEvent)
            coreAnalyticsProvider.track(exposureEvent1)
            Assert.assertEquals(null, eventBridge.recentEvent)
        }
        Assert.assertEquals(eventBridge.logEventCount, 1)

        eventBridge.recentEvent = null
        coreAnalyticsProvider.setUserProperty(exposureEvent2)
        Assert.assertEquals(null, eventBridge.recentEvent)
        coreAnalyticsProvider.track(exposureEvent2)
        Assert.assertEquals(expectedTrack2, eventBridge.recentEvent)
        repeat(10) {
            eventBridge.recentEvent = null
            coreAnalyticsProvider.setUserProperty(exposureEvent2)
            Assert.assertEquals(null, eventBridge.recentEvent)
            coreAnalyticsProvider.track(exposureEvent2)
            Assert.assertEquals(null, eventBridge.recentEvent)
        }
        Assert.assertEquals(eventBridge.logEventCount, 2)
    }

    @Test
    fun `unset called once per key`() {
        val eventBridge = TestEventBridge()
        val coreAnalyticsProvider = SessionAnalyticsProvider(ConnectorAnalyticsProvider(eventBridge))
        val exposureEvent1 = ExposureEvent(ExperimentUser(), "test-key", Variant("off"), VariantSource.FALLBACK_INLINE)
        repeat(10) {
            coreAnalyticsProvider.unsetUserProperty(exposureEvent1)
            Assert.assertEquals(expectedUnset, eventBridge.recentEvent)
        }
        Assert.assertEquals(1, eventBridge.logEventCount)
    }

    @Test
    fun `test property set tracked, unset, set tracked`() {
        val eventBridge = TestEventBridge()
        val coreAnalyticsProvider = ConnectorAnalyticsProvider(eventBridge)
        val unsetEvent = ExposureEvent(ExperimentUser(), "test-key", Variant(), VariantSource.FALLBACK_CONFIG)
        repeat(10) {
            eventBridge.recentEvent = null
            // set (not called in ConnectorUserProvider)
            coreAnalyticsProvider.setUserProperty(exposureEvent1)
            Assert.assertEquals(null, eventBridge.recentEvent)
            // track
            coreAnalyticsProvider.track(exposureEvent1)
            Assert.assertEquals(expectedTrack1, eventBridge.recentEvent)
            // unset
            coreAnalyticsProvider.unsetUserProperty(unsetEvent)
            Assert.assertEquals(expectedUnset, eventBridge.recentEvent)
        }
        Assert.assertEquals(eventBridge.logEventCount, 20)
    }
}