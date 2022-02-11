package com.amplitude.experiment.util

import com.amplitude.experiment.Exposure
import com.amplitude.experiment.ExposureTrackingProvider

internal class SessionExposureTrackingProvider(
    private val trackingProvider: ExposureTrackingProvider,
): ExposureTrackingProvider {

    private val lock = Any()
    private val tracked = mutableSetOf<Exposure>()

    override fun track(exposure: Exposure) {
        synchronized(lock) {
            if (tracked.contains(exposure)) {
                return@track
            } else {
                tracked.add(exposure)
            }
        }
        trackingProvider.track(exposure)
    }
}
