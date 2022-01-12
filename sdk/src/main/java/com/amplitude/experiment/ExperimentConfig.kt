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
    val automaticClientSideExposureTracking: Boolean = Defaults.AUTOMATIC_CLIENT_SIDE_EXPOSURE_TRACKING,
    @JvmField
    val userProvider: ExperimentUserProvider? = Defaults.USER_PROVIDER,
    @JvmField
    val analyticsProvider: ExperimentAnalyticsProvider? = Defaults.ANALYTICS_PROVIDER,
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
        const val AUTOMATIC_CLIENT_SIDE_EXPOSURE_TRACKING = true

        /**
         * null
         */
        val USER_PROVIDER: ExperimentUserProvider? = null

        /**
         * null
         */
        val ANALYTICS_PROVIDER: ExperimentAnalyticsProvider? = null
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
        private var automaticClientSideExposureTracking = Defaults.AUTOMATIC_CLIENT_SIDE_EXPOSURE_TRACKING
        private var userProvider = Defaults.USER_PROVIDER
        private var analyticsProvider = Defaults.ANALYTICS_PROVIDER

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

        fun automaticClientSideExposureTracking(automaticClientSideExposureTracking: Boolean) = apply {
            this.automaticClientSideExposureTracking = automaticClientSideExposureTracking
        }

        fun userProvider(userProvider: ExperimentUserProvider?) = apply {
            this.userProvider = userProvider
        }
        fun analyticsProvider(analyticsProvider: ExperimentAnalyticsProvider?) = apply {
            this.analyticsProvider = analyticsProvider
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
                userProvider = userProvider,
                analyticsProvider = analyticsProvider,
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
            .automaticClientSideExposureTracking(automaticClientSideExposureTracking)
            .userProvider(userProvider)
            .analyticsProvider(analyticsProvider)
    }
}
