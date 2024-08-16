package com.amplitude.experiment.util

import com.amplitude.analytics.connector.Identity
import com.amplitude.experiment.ExperimentUser
import com.amplitude.experiment.Exposure
import com.amplitude.experiment.ExposureTrackingProvider

internal class UserSessionExposureTracker(
    private val trackingProvider: ExposureTrackingProvider,
) {
    private val lock = Any()
    private val tracked = mutableSetOf<Exposure>()
    private var identity = Identity()

    fun track(
        exposure: Exposure,
        user: ExperimentUser? = null,
    ) {
        synchronized(lock) {
            val newIdentity = user.toIdentity()
            if (!identity.identityEquals(newIdentity)) {
                tracked.clear()
            }
            identity = newIdentity
            if (tracked.contains(exposure)) {
                return@track
            } else {
                tracked.add(exposure)
            }
        }
        trackingProvider.track(exposure)
    }
}

private fun ExperimentUser?.toIdentity() =
    Identity(
        userId = this?.userId,
        deviceId = this?.deviceId,
    )

private fun Identity.identityEquals(other: Identity): Boolean = this.userId == other.userId && this.deviceId == other.deviceId
