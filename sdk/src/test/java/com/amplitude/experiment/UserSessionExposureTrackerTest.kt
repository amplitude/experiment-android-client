package com.amplitude.experiment

import com.amplitude.experiment.util.TestExposureTrackingProvider
import com.amplitude.experiment.util.UserSessionExposureTracker
import org.junit.Assert
import org.junit.Test

class UserSessionExposureTrackerTest {

    @Test
    fun `test track called once per flag`() {
        val provider = TestExposureTrackingProvider()
        val tracker = UserSessionExposureTracker(provider)

        val exposure = Exposure("flag", "variant", null)
        repeat(10) {
            tracker.track(exposure)
        }
        Assert.assertEquals(exposure, provider.lastExposure)
        Assert.assertEquals(1, provider.trackCount)

        val exposure2 = Exposure("flag2", "variant", null)
        repeat(10) {
            tracker.track(exposure2)
        }
        Assert.assertEquals(exposure2, provider.lastExposure)
        Assert.assertEquals(2, provider.trackCount)
    }

    @Test
    fun `test track called again on same flag with variant change, value to null`() {
        val provider = TestExposureTrackingProvider()
        val tracker = UserSessionExposureTracker(provider)

        val exposure = Exposure("flag", "variant", null)
        repeat(10) {
            tracker.track(exposure)
        }
        val exposure2 = Exposure("flag", null, null)
        repeat(10) {
            tracker.track(exposure2)
        }

        Assert.assertEquals(exposure2, provider.lastExposure)
        Assert.assertEquals(2, provider.trackCount)
    }

    @Test
    fun `test track called again on same flag with variant change, value to different value`() {
        val provider = TestExposureTrackingProvider()
        val tracker = UserSessionExposureTracker(provider)

        val exposure = Exposure("flag", "variant", null)
        repeat(10) {
            tracker.track(exposure)
        }
        val exposure2 = Exposure("flag", "variant2", null)
        repeat(10) {
            tracker.track(exposure2)
        }

        Assert.assertEquals(exposure2, provider.lastExposure)
        Assert.assertEquals(2, provider.trackCount)
    }

    @Test
    fun `test track called again on user id change, null to value`() {
        val provider = TestExposureTrackingProvider()
        val tracker = UserSessionExposureTracker(provider)
        val exposure = Exposure("flag", "variant", null)
        repeat(10) {
            tracker.track(exposure)
        }
        repeat(10) {
            tracker.track(exposure, ExperimentUser(userId = "uid"))
        }
        Assert.assertEquals(exposure, provider.lastExposure)
        Assert.assertEquals(2, provider.trackCount)
    }

    @Test
    fun `test track called again on device id change, null to value`() {
        val provider = TestExposureTrackingProvider()
        val tracker = UserSessionExposureTracker(provider)
        val exposure = Exposure("flag", "variant", null)
        repeat(10) {
            tracker.track(exposure)
        }
        repeat(10) {
            tracker.track(exposure, ExperimentUser(deviceId = "did"))
        }
        Assert.assertEquals(exposure, provider.lastExposure)
        Assert.assertEquals(2, provider.trackCount)
    }

    @Test
    fun `test track called again on user id change, value to null`() {
        val provider = TestExposureTrackingProvider()
        val tracker = UserSessionExposureTracker(provider)
        val exposure = Exposure("flag", "variant", null)
        repeat(10) {
            tracker.track(exposure, ExperimentUser(userId = "uid"))
        }
        repeat(10) {
            tracker.track(exposure, ExperimentUser())
        }
        Assert.assertEquals(exposure, provider.lastExposure)
        Assert.assertEquals(2, provider.trackCount)
    }

    @Test
    fun `test track called again on device id change, value to null`() {
        val provider = TestExposureTrackingProvider()
        val tracker = UserSessionExposureTracker(provider)
        val exposure = Exposure("flag", "variant", null)
        repeat(10) {
            tracker.track(exposure, ExperimentUser(deviceId = "did"))
        }
        repeat(10) {
            tracker.track(exposure, ExperimentUser())
        }
        Assert.assertEquals(exposure, provider.lastExposure)
        Assert.assertEquals(2, provider.trackCount)
    }

    @Test
    fun `test track called again on user id change, value to different value`() {
        val provider = TestExposureTrackingProvider()
        val tracker = UserSessionExposureTracker(provider)
        val exposure = Exposure("flag", "variant", null)
        repeat(10) {
            tracker.track(exposure, ExperimentUser(userId = "uid"))
        }
        repeat(10) {
            tracker.track(exposure, ExperimentUser(userId = "uid2"))
        }
        Assert.assertEquals(exposure, provider.lastExposure)
        Assert.assertEquals(2, provider.trackCount)
    }

    @Test
    fun `test track called again on device id change, value to different value`() {
        val provider = TestExposureTrackingProvider()
        val tracker = UserSessionExposureTracker(provider)
        val exposure = Exposure("flag", "variant", null)
        repeat(10) {
            tracker.track(exposure, ExperimentUser(deviceId = "did"))
        }
        repeat(10) {
            tracker.track(exposure, ExperimentUser(deviceId = "did2"))
        }
        Assert.assertEquals(exposure, provider.lastExposure)
        Assert.assertEquals(2, provider.trackCount)
    }

    @Test
    fun `test track called again on user id and device id change, null to value`() {
        val provider = TestExposureTrackingProvider()
        val tracker = UserSessionExposureTracker(provider)
        val exposure = Exposure("flag", "variant", null)
        repeat(10) {
            tracker.track(exposure)
        }
        repeat(10) {
            tracker.track(exposure, ExperimentUser(userId = "uid", deviceId = "did"))
        }
        Assert.assertEquals(exposure, provider.lastExposure)
        Assert.assertEquals(2, provider.trackCount)
    }

    @Test
    fun `test track called again on user id and device id change, value to null`() {
        val provider = TestExposureTrackingProvider()
        val tracker = UserSessionExposureTracker(provider)
        val exposure = Exposure("flag", "variant", null)
        repeat(10) {
            tracker.track(exposure, ExperimentUser(userId = "uid", deviceId = "did"))
        }
        repeat(10) {
            tracker.track(exposure, ExperimentUser())
        }
        Assert.assertEquals(exposure, provider.lastExposure)
        Assert.assertEquals(2, provider.trackCount)
    }

    @Test
    fun `test track called again on user id and device id change, value to different value`() {
        val provider = TestExposureTrackingProvider()
        val tracker = UserSessionExposureTracker(provider)
        val exposure = Exposure("flag", "variant", null)
        repeat(10) {
            tracker.track(exposure, ExperimentUser(userId = "uid", deviceId = "did"))
        }
        repeat(10) {
            tracker.track(exposure, ExperimentUser(userId = "uid2", deviceId = "did2"))
        }
        Assert.assertEquals(exposure, provider.lastExposure)
        Assert.assertEquals(2, provider.trackCount)
    }
}
