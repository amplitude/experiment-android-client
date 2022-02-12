package com.amplitude.experiment

/**
 * Interface for enabling tracking [Exposure]s through the
 * [ExperimentClient].
 *
 * If you're using the Amplitude Analytics SDK for tracking you do not need
 * to implement this interface. Simply upgrade your analytics SDK version to
 * 2.36.0+ and initialize experiment using the
 * [Experiment.initializeWithAmplitudeAnalytics] function.
 *
 * If you're using a 3rd party analytics implementation then you'll need to
 * implement the sending of the analytics event yourself. The implementation
 * should result in the following event getting sent to amplitude:
 *
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
 * For example, if you're using Segment for analytics:
 *
 * ```
 * Analytics.with(context).track(
 *   "$exposure",
 *   new Properties()
 *     .putValue("flag_key", exposure.flagKey)
 *     .putValue("variant", exposure.variant)
 * );
 * ```
 */
interface ExposureTrackingProvider {
    /**
     * Called when the [ExperimentClient] intends to track an exposure event;
     * either when [ExperimentClient.variant] serves a variant (and
     * [ExperimentConfig.automaticExposureTracking] is `true`) or if
     * [ExperimentClient.exposure] is called.
     *
     * The implementation should result in the following event getting sent to
     * amplitude:
     *
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
    fun track(exposure: Exposure)
}
