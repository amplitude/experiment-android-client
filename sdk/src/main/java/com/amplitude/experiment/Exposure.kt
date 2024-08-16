package com.amplitude.experiment

/**
 * Improved exposure event for tracking exposures to Amplitude Experiment.
 *
 * This object contains all the required information to send an `$exposure`
 * event through any SDK or CDP to experiment. The resulting exposure event
 * must follow the following definition:
 * ```
 * {
 *   "event_type": "$exposure",
 *   "event_properties": {
 *     "flag_key": "<flagKey>",
 *     "variant": "<variant>"
 *   }
 * }
 * ```
 *
 * Where `<flagKey>` and `<variant>` are the [flagKey] and [variant] members on
 * this class.
 *
 * For example, if you're using Segment for analytics:
 *
 * ```
 * Analytics.with(context).track(
 *   "$exposure",
 *   new Properties()
 *     .putValue("flag_key", exposureEvent.flagKey)
 *     .putValue("variant", exposureEvent.variant)
 * );
 * ```
 */
data class Exposure internal constructor(
    val flagKey: String,
    val variant: String?,
    val experimentKey: String?,
    val metadata: Map<String, Any?>? = null,
)
