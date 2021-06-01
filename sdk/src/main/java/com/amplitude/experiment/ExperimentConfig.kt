package com.amplitude.experiment

enum class Source {
    LOCAL_STORAGE,
    INITIAL_VARIANTS,
}

/**
 * Configuration options. This is an immutable object that can be created using
 * a [ExperimentConfig.Builder]. Example usage:
 *
 *`ExperimentConfig.builder().setServerUrl("https://api.lab.amplitude.com/").build()`
 */
class ExperimentConfig internal constructor(
    @JvmField
    val debug: Boolean = Defaults.DEBUG,
    @JvmField
    val fallbackVariant: Variant? = Defaults.FALLBACK_VARIANT,
    @JvmField
    val initialVariants: Map<String, Variant>? = Defaults.INITIAL_VARIANTS,
    @JvmField
    val source: Source = Defaults.SOURCE,
    @JvmField
    val serverUrl: String = Defaults.SERVER_URL,
    @JvmField
    val fetchTimeoutMillis: Long = Defaults.FETCH_TIMEOUT_MILLIS,
//    val retryFetchOnFailure: Boolean = Defaults.RETRY_FETCH_ON_FAILURE,
) {

    /**
     * Defaults for [ExperimentConfig]
     */
    object Defaults {

        /**
         * false
         */
        const val DEBUG = false

        /**
         * null
         */
        val FALLBACK_VARIANT: Variant? = null

        /**
         * null
         */
        val INITIAL_VARIANTS: Map<String, Variant>? = null

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

//        const val RETRY_FETCH_ON_FAILURE = true
    }

    companion object {
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }

    class Builder {

        private var debug = Defaults.DEBUG
        private var fallbackVariant = Defaults.FALLBACK_VARIANT
        private var initialVariants = Defaults.INITIAL_VARIANTS
        private var source = Defaults.SOURCE
        private var serverUrl = Defaults.SERVER_URL
        private var fetchTimeoutMillis = Defaults.FETCH_TIMEOUT_MILLIS
//        private var retryFetchOnFailure = Defaults.RETRY_FETCH_ON_FAILURE

        fun debug(debug: Boolean) = apply {
            this.debug = debug
        }

        fun fallbackVariant(fallbackVariant: Variant?) = apply {
            this.fallbackVariant = fallbackVariant
        }

        fun initialVariants(initialVariants: Map<String, Variant>?) = apply {
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

//        fun retryFetchOnFailure(retryFetchOnFailure: Boolean) = apply {
//            this.retryFetchOnFailure = retryFetchOnFailure
//        }

        fun build(): ExperimentConfig {
            return ExperimentConfig(
                debug = debug,
                fallbackVariant = fallbackVariant,
                initialVariants = initialVariants,
                source = source,
                serverUrl = serverUrl,
                fetchTimeoutMillis = fetchTimeoutMillis,
//                retryFetchOnFailure = retryFetchOnFailure,
            )
        }
    }
}
