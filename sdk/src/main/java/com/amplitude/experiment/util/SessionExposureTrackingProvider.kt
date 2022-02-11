package com.amplitude.experiment.util

import com.amplitude.experiment.ExposureEvent
import com.amplitude.experiment.ExposureTrackingProvider

internal class SessionExposureTrackingProvider(
    private val trackingProvider: ExposureTrackingProvider,
): ExposureTrackingProvider {

    private val lock = Any()
    private val tracked = mutableSetOf<ExposureEvent>()

    override fun track(exposureEvent: ExposureEvent) {
        synchronized(lock) {
            if (tracked.contains(exposureEvent)) {
                return@track
            } else {
                tracked.add(exposureEvent)
            }
        }
        trackingProvider.track(exposureEvent)
    }
}
