package com.amplitude.experiment

import com.amplitude.core.AnalyticsClient

internal class AnalyticsClientExposureTrackingProvider(
    private val clientProvider: () -> AnalyticsClient?,
) : ExposureTrackingProvider {
    override fun track(exposure: Exposure) {
        val client = clientProvider() ?: return
        client.track(
            "\$exposure",
            mapOf(
                "flag_key" to exposure.flagKey,
                "variant" to exposure.variant,
                "experiment_key" to exposure.experimentKey,
                "metadata" to exposure.metadata,
            ).filterNullValues(),
        )
    }
}

private fun <T> Map<String, T?>.filterNullValues(): Map<String, T> {
    return filterValues { it != null }.mapValues { it.value!! }
}
