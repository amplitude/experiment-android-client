package com.amplitude.experiment

import com.amplitude.experiment.analytics.ExperimentAnalyticsProvider

enum class Source {
    LOCAL_STORAGE,
    INITIAL_VARIANTS,
}

/**
 * Configuration options. This is an immutable object that can be created using
 * a [ExperimentConfig.Builder]. Example usage:
 *
 *`ExperimentConfig.builder().serverUrl("https://api.lab.amplitude.com/").build()`
 */
class ExperimentConfig internal constructor(
    @JvmField
    val debug: Boolean = Defaults.DEBUG,
    @JvmField
    val instanceName: String = Defaults.INSTANCE_NAME,
    @JvmField
    val fallbackVariant: Variant = Defaults.FALLBACK_VARIANT,
    @JvmField
    val initialVariants: Map<String, Variant> = Defaults.INITIAL_VARIANTS,
    @JvmField
    val source: Source = Defaults.SOURCE,
    @JvmField
    val serverUrl: String = Defaults.SERVER_URL,
    @JvmField
    val fetchTimeoutMillis: Long = Defaults.FETCH_TIMEOUT_MILLIS,
    @JvmField
    val retryFetchOnFailure: Boolean = Defaults.RETRY_FETCH_ON_FAILURE,
    @JvmField
    val automaticExposureTracking: Boolean = Defaults.AUTOMATIC_EXPOSURE_TRACKING,
    @JvmField
    val pollOnStart: Boolean = Defaults.POLL_ON_START,
    @JvmField
    val fetchOnStart: Boolean? = Defaults.FETCH_ON_START,
    @JvmField
    val automaticFetchOnAmplitudeIdentityChange: Boolean = Defaults.AUTOMATIC_FETCH_ON_AMPLITUDE_IDENTITY_CHANGE,
    @JvmField
    val userProvider: ExperimentUserProvider? = Defaults.USER_PROVIDER,
    @JvmField
    @Deprecated("Use the exposureTrackingProvider configuration")
    val analyticsProvider: ExperimentAnalyticsProvider? = Defaults.ANALYTICS_PROVIDER,
    @JvmField
    val exposureTrackingProvider: ExposureTrackingProvider? = Defaults.EXPOSURE_TRACKING_PROVIDER,
) {

    /**
     * Construct the default [ExperimentConfig].
     */
    constructor() : this(debug = Defaults.DEBUG)

    /**
     * Defaults for [ExperimentConfig]
     */
    object Defaults {

        /**
         * false
         */
        const val DEBUG = false

        /**
         * $default_instance
         */
        const val INSTANCE_NAME = "\$default_instance"

        /**
         * Variant(null,  null)
         */
        val FALLBACK_VARIANT: Variant = Variant()

        /**
         * Empty Map<String, Variant>
         */
        val INITIAL_VARIANTS: Map<String, Variant> = emptyMap()

        /**
         * Source.LOCAL_STORAGE
         */
        val SOURCE = Source.LOCAL_STORAGE

        /**
         * "https://api.lab.amplitude.com/"
         */
        const val SERVER_URL = "https://api.lab.amplitude.com/"

        /**
         * 10000
         */
        const val FETCH_TIMEOUT_MILLIS = 10000L

        /**
         * true
         */
        const val RETRY_FETCH_ON_FAILURE = true

        /**
         * true
         */
        const val AUTOMATIC_EXPOSURE_TRACKING = true

        /**
         * true
         */
        const val POLL_ON_START = true

        /**
         * null
         */
        val FETCH_ON_START: Boolean? = null

        /**
         * false
         */
        const val AUTOMATIC_FETCH_ON_AMPLITUDE_IDENTITY_CHANGE = false

        /**
         * null
         */
        val USER_PROVIDER: ExperimentUserProvider? = null

        /**
         * null
         */
        @Deprecated("Use ExposureTrackingProvider instead")
        val ANALYTICS_PROVIDER: ExperimentAnalyticsProvider? = null

        /**
         * null
         */
        val EXPOSURE_TRACKING_PROVIDER: ExposureTrackingProvider? = null
    }

    companion object {
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }

    class Builder {

        private var debug = Defaults.DEBUG
        private var instanceName = Defaults.INSTANCE_NAME
        private var fallbackVariant = Defaults.FALLBACK_VARIANT
        private var initialVariants = Defaults.INITIAL_VARIANTS
        private var source = Defaults.SOURCE
        private var serverUrl = Defaults.SERVER_URL
        private var fetchTimeoutMillis = Defaults.FETCH_TIMEOUT_MILLIS
        private var retryFetchOnFailure = Defaults.RETRY_FETCH_ON_FAILURE
        private var automaticExposureTracking = Defaults.AUTOMATIC_EXPOSURE_TRACKING
        private var pollOnStart = Defaults.POLL_ON_START
        private var fetchOnStart = Defaults.FETCH_ON_START
        private var automaticFetchOnAmplitudeIdentityChange = Defaults.AUTOMATIC_FETCH_ON_AMPLITUDE_IDENTITY_CHANGE
        private var userProvider = Defaults.USER_PROVIDER
        private var analyticsProvider = Defaults.ANALYTICS_PROVIDER
        private var exposureTrackingProvider = Defaults.EXPOSURE_TRACKING_PROVIDER

        fun debug(debug: Boolean) = apply {
            this.debug = debug
        }

        fun instanceName(instanceName: String) = apply {
            this.instanceName = instanceName
        }

        fun fallbackVariant(fallbackVariant: Variant) = apply {
            this.fallbackVariant = fallbackVariant
        }

        fun initialVariants(initialVariants: Map<String, Variant>) = apply {
            this.initialVariants = initialVariants
        }

        fun source(source: Source) = apply {
            this.source = source
        }

        fun serverUrl(serverUrl: String) = apply {
            this.serverUrl = serverUrl
        }

        fun fetchTimeoutMillis(fetchTimeoutMillis: Long) = apply {
            this.fetchTimeoutMillis = fetchTimeoutMillis
        }

        fun retryFetchOnFailure(retryFetchOnFailure: Boolean) = apply {
            this.retryFetchOnFailure = retryFetchOnFailure
        }

        fun automaticExposureTracking(automaticExposureTracking: Boolean) = apply {
            this.automaticExposureTracking = automaticExposureTracking
        }

        fun pollOnStart(pollOnStart: Boolean) = apply {
            this.pollOnStart = pollOnStart
        }

        fun fetchOnStart(fetchOnStart: Boolean?) = apply {
            this.fetchOnStart = fetchOnStart
        }

        fun automaticFetchOnAmplitudeIdentityChange(automaticFetchOnAmplitudeIdentityChange: Boolean) = apply {
            this.automaticFetchOnAmplitudeIdentityChange = automaticFetchOnAmplitudeIdentityChange
        }

        fun userProvider(userProvider: ExperimentUserProvider?) = apply {
            this.userProvider = userProvider
        }

        @Deprecated("Use the exposureTrackingProvider instead")
        fun analyticsProvider(analyticsProvider: ExperimentAnalyticsProvider?) = apply {
            this.analyticsProvider = analyticsProvider
        }

        fun exposureTrackingProvider(exposureTrackingProvider: ExposureTrackingProvider?) = apply {
            this.exposureTrackingProvider = exposureTrackingProvider
        }

        fun build(): ExperimentConfig {
            return ExperimentConfig(
                debug = debug,
                instanceName = instanceName,
                fallbackVariant = fallbackVariant,
                initialVariants = initialVariants,
                source = source,
                serverUrl = serverUrl,
                fetchTimeoutMillis = fetchTimeoutMillis,
                retryFetchOnFailure = retryFetchOnFailure,
                automaticExposureTracking = automaticExposureTracking,
                pollOnStart = pollOnStart,
                fetchOnStart = fetchOnStart,
                automaticFetchOnAmplitudeIdentityChange = automaticFetchOnAmplitudeIdentityChange,
                userProvider = userProvider,
                analyticsProvider = analyticsProvider,
                exposureTrackingProvider = exposureTrackingProvider,
            )
        }
    }

    internal fun copyToBuilder(): Builder {
        return builder()
            .debug(debug)
            .instanceName(instanceName)
            .fallbackVariant(fallbackVariant)
            .initialVariants(initialVariants)
            .source(source)
            .serverUrl(serverUrl)
            .fetchTimeoutMillis(fetchTimeoutMillis)
            .retryFetchOnFailure(retryFetchOnFailure)
            .automaticExposureTracking(automaticExposureTracking)
            .pollOnStart(pollOnStart)
            .fetchOnStart(fetchOnStart)
            .automaticFetchOnAmplitudeIdentityChange((automaticFetchOnAmplitudeIdentityChange))
            .userProvider(userProvider)
            .analyticsProvider(analyticsProvider)
            .exposureTrackingProvider(exposureTrackingProvider)
    }
}
