package com.amplitude.experiment.util

import com.amplitude.experiment.Exposure
import com.amplitude.experiment.ExposureTrackingProvider

class TestExposureTrackingProvider : ExposureTrackingProvider {
    var trackCount = 0
    var lastExposure: Exposure? = null
    override fun track(exposure: Exposure) {
        trackCount++
        lastExposure = exposure
    }
}
