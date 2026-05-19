package com.amplitude.experiment.util

import com.amplitude.analytics.connector.Identity
import com.amplitude.experiment.ExperimentConfig
import com.amplitude.experiment.ExperimentUser
import com.amplitude.experiment.Exposure
import com.amplitude.experiment.ExposureTrackingProvider

internal class UserSessionExposureTracker(
    private val trackingProvider: ExposureTrackingProvider,
    private val ttlMillis: Long = ExperimentConfig.Defaults.EXPOSURE_DEDUP_CACHE_TTL_MILLIS,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private val lock = Any()
    private val tracked = mutableMapOf<Exposure, Long>()
    private var identity = Identity()

    fun track(
        exposure: Exposure,
        user: ExperimentUser? = null,
    ) {
        synchronized(lock) {
            val now = clock()
            val newIdentity = user.toIdentity()
            if (!identity.identityEquals(newIdentity)) {
                tracked.clear()
            }
            identity = newIdentity
            tracked.entries.removeAll { now - it.value > ttlMillis }
            if (tracked.containsKey(exposure)) {
                return@track
            }
            tracked[exposure] = now
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
