package com.amplitude.experiment

import com.amplitude.experiment.analytics.ExperimentAnalyticsProvider

enum class Source {
    LOCAL_STORAGE,
    INITIAL_VARIANTS,
}

enum class ServerZone {
    US,
    EU,
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
    val initialFlags: String? = Defaults.INITIAL_FLAGS,
    @JvmField
    val initialVariants: Map<String, Variant> = Defaults.INITIAL_VARIANTS,
    @JvmField
    val source: Source = Defaults.SOURCE,
    @JvmField
    val serverUrl: String = Defaults.SERVER_URL,
    @JvmField
    val flagsServerUrl: String = Defaults.FLAGS_SERVER_URL,
    @JvmField
    val serverZone: ServerZone = Defaults.SERVER_ZONE,
    @JvmField
    val fetchTimeoutMillis: Long = Defaults.FETCH_TIMEOUT_MILLIS,
    @JvmField
    val retryFetchOnFailure: Boolean = Defaults.RETRY_FETCH_ON_FAILURE,
    @JvmField
    val automaticExposureTracking: Boolean = Defaults.AUTOMATIC_EXPOSURE_TRACKING,
    @JvmField
    val pollOnStart: Boolean = Defaults.POLL_ON_START,
    @JvmField
    val flagConfigPollingIntervalMillis: Long = Defaults.FLAG_CONFIG_POLLING_INTERVAL_MILLIS,
    @JvmField
    val fetchOnStart: Boolean = Defaults.FETCH_ON_START,
    @JvmField
    val automaticFetchOnAmplitudeIdentityChange: Boolean = Defaults.AUTOMATIC_FETCH_ON_AMPLITUDE_IDENTITY_CHANGE,
    @JvmField
    val userProvider: ExperimentUserProvider? = Defaults.USER_PROVIDER,
    @JvmField
    @Deprecated("Use the exposureTrackingProvider configuration")
    val analyticsProvider: ExperimentAnalyticsProvider? = Defaults.ANALYTICS_PROVIDER,
    @JvmField
    val exposureTrackingProvider: ExposureTrackingProvider? = Defaults.EXPOSURE_TRACKING_PROVIDER,
    @JvmField
    val customRequestHeaders: ((ExperimentUser) -> Map<String, String>)? = Defaults.CUSTOM_REQUEST_HEADERS
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
         * null
         */
        val INITIAL_FLAGS: String? = null

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
         * "https://flag.lab.amplitude.com/"
         */
        const val FLAGS_SERVER_URL = "https://flag.lab.amplitude.com/"

        /**
         * ServerZone.US
         */
        val SERVER_ZONE = ServerZone.US

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
         * 300000
         */
        const val FLAG_CONFIG_POLLING_INTERVAL_MILLIS = 300000L

        /**
         * null
         */
        const val FETCH_ON_START: Boolean = true

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

        /**
         * null
         */
        val CUSTOM_REQUEST_HEADERS: ((ExperimentUser) -> Map<String, String>)? = null
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
        private var initialFlags = Defaults.INITIAL_FLAGS
        private var initialVariants = Defaults.INITIAL_VARIANTS
        private var source = Defaults.SOURCE
        private var serverUrl = Defaults.SERVER_URL
        private var flagsServerUrl = Defaults.FLAGS_SERVER_URL
        private var serverZone = Defaults.SERVER_ZONE
        private var fetchTimeoutMillis = Defaults.FETCH_TIMEOUT_MILLIS
        private var retryFetchOnFailure = Defaults.RETRY_FETCH_ON_FAILURE
        private var automaticExposureTracking = Defaults.AUTOMATIC_EXPOSURE_TRACKING
        private var pollOnStart = Defaults.POLL_ON_START
        private var flagConfigPollingIntervalMillis = Defaults.FLAG_CONFIG_POLLING_INTERVAL_MILLIS
        private var fetchOnStart = Defaults.FETCH_ON_START
        private var automaticFetchOnAmplitudeIdentityChange = Defaults.AUTOMATIC_FETCH_ON_AMPLITUDE_IDENTITY_CHANGE
        private var userProvider = Defaults.USER_PROVIDER
        private var analyticsProvider = Defaults.ANALYTICS_PROVIDER
        private var exposureTrackingProvider = Defaults.EXPOSURE_TRACKING_PROVIDER
        private var customRequestHeaders = Defaults.CUSTOM_REQUEST_HEADERS

        fun debug(debug: Boolean) =
            apply {
                this.debug = debug
            }

        fun instanceName(instanceName: String) =
            apply {
                this.instanceName = instanceName
            }

        fun fallbackVariant(fallbackVariant: Variant) =
            apply {
                this.fallbackVariant = fallbackVariant
            }

        fun initialFlags(initialFlags: String?) =
            apply {
                this.initialFlags = initialFlags
            }

        fun initialVariants(initialVariants: Map<String, Variant>) =
            apply {
                this.initialVariants = initialVariants
            }

        fun source(source: Source) =
            apply {
                this.source = source
            }

        fun serverUrl(serverUrl: String) =
            apply {
                this.serverUrl = serverUrl
            }

        fun flagsServerUrl(flagsServerUrl: String) =
            apply {
                this.flagsServerUrl = flagsServerUrl
            }

        fun serverZone(serverZone: ServerZone) =
            apply {
                this.serverZone = serverZone
            }

        fun fetchTimeoutMillis(fetchTimeoutMillis: Long) =
            apply {
                this.fetchTimeoutMillis = fetchTimeoutMillis
            }

        fun retryFetchOnFailure(retryFetchOnFailure: Boolean) =
            apply {
                this.retryFetchOnFailure = retryFetchOnFailure
            }

        fun automaticExposureTracking(automaticExposureTracking: Boolean) =
            apply {
                this.automaticExposureTracking = automaticExposureTracking
            }

        fun pollOnStart(pollOnStart: Boolean) =
            apply {
                this.pollOnStart = pollOnStart
            }

        fun flagConfigPollingIntervalMillis(flagConfigPollingIntervalMillis: Long) =
            apply {
                this.flagConfigPollingIntervalMillis = flagConfigPollingIntervalMillis
            }

        fun fetchOnStart(fetchOnStart: Boolean?) =
            apply {
                this.fetchOnStart = fetchOnStart ?: true
            }

        fun automaticFetchOnAmplitudeIdentityChange(automaticFetchOnAmplitudeIdentityChange: Boolean) =
            apply {
                this.automaticFetchOnAmplitudeIdentityChange = automaticFetchOnAmplitudeIdentityChange
            }

        fun userProvider(userProvider: ExperimentUserProvider?) =
            apply {
                this.userProvider = userProvider
            }

        @Deprecated("Use the exposureTrackingProvider instead")
        fun analyticsProvider(analyticsProvider: ExperimentAnalyticsProvider?) =
            apply {
                this.analyticsProvider = analyticsProvider
            }

        fun exposureTrackingProvider(exposureTrackingProvider: ExposureTrackingProvider?) =
            apply {
                this.exposureTrackingProvider = exposureTrackingProvider
            }

        fun customRequestHeaders(customRequestHeaders: ((ExperimentUser) -> Map<String, String>)?) =
            apply {
                this.customRequestHeaders = customRequestHeaders
            }

        fun build(): ExperimentConfig {
            return ExperimentConfig(
                debug = debug,
                instanceName = instanceName,
                fallbackVariant = fallbackVariant,
                initialFlags = initialFlags,
                initialVariants = initialVariants,
                source = source,
                serverUrl = serverUrl,
                flagsServerUrl = flagsServerUrl,
                serverZone = serverZone,
                fetchTimeoutMillis = fetchTimeoutMillis,
                retryFetchOnFailure = retryFetchOnFailure,
                automaticExposureTracking = automaticExposureTracking,
                pollOnStart = pollOnStart,
                flagConfigPollingIntervalMillis = flagConfigPollingIntervalMillis,
                fetchOnStart = fetchOnStart,
                automaticFetchOnAmplitudeIdentityChange = automaticFetchOnAmplitudeIdentityChange,
                userProvider = userProvider,
                analyticsProvider = analyticsProvider,
                exposureTrackingProvider = exposureTrackingProvider,
                customRequestHeaders = customRequestHeaders
            )
        }
    }

    internal fun copyToBuilder(): Builder {
        return builder()
            .debug(debug)
            .instanceName(instanceName)
            .fallbackVariant(fallbackVariant)
            .initialFlags(initialFlags)
            .initialVariants(initialVariants)
            .source(source)
            .serverUrl(serverUrl)
            .flagsServerUrl(flagsServerUrl)
            .serverZone(serverZone)
            .fetchTimeoutMillis(fetchTimeoutMillis)
            .retryFetchOnFailure(retryFetchOnFailure)
            .automaticExposureTracking(automaticExposureTracking)
            .pollOnStart(pollOnStart)
            .flagConfigPollingIntervalMillis(flagConfigPollingIntervalMillis)
            .fetchOnStart(fetchOnStart)
            .automaticFetchOnAmplitudeIdentityChange((automaticFetchOnAmplitudeIdentityChange))
            .userProvider(userProvider)
            .analyticsProvider(analyticsProvider)
            .exposureTrackingProvider(exposureTrackingProvider)
            .customRequestHeaders(customRequestHeaders)
    }
}
