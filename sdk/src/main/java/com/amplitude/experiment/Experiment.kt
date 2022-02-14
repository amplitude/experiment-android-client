package com.amplitude.experiment

import android.app.Application
import com.amplitude.analytics.connector.AnalyticsConnector
import com.amplitude.experiment.storage.SharedPrefsStorage
import com.amplitude.experiment.util.AndroidLogger
import com.amplitude.experiment.util.Logger
import okhttp3.OkHttpClient
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory

object Experiment {

    private val daemonThreadFactory =
        ThreadFactory { r ->
            Executors.defaultThreadFactory().newThread(r).apply {
                isDaemon = true
            }
        }
    internal val executorService = ScheduledThreadPoolExecutor(0, daemonThreadFactory)

    private val httpClient = OkHttpClient()
    private val instances = mutableMapOf<String, ExperimentClient>()

    /**
     * Initializes a singleton [ExperimentClient] identified by the configured
     * instance name.
     *
     * @param application The Android Application context
     * @param apiKey  The API key. This can be found in the Experiment settings and should not be null or empty.
     * @param config see [ExperimentConfig] for configuration options
     */
    @JvmStatic
    fun initialize(
        application: Application,
        apiKey: String,
        config: ExperimentConfig
    ): ExperimentClient = synchronized(instances) {
        val instanceName = config.instanceName
        val instanceKey = "$instanceName.$apiKey"
        return when (val instance = instances[instanceKey]) {
            null -> {
                Logger.implementation = AndroidLogger(config.debug)
                var mergedConfig = config
                if (config.userProvider == null) {
                    mergedConfig = config.copyToBuilder()
                        .userProvider(DefaultUserProvider(application))
                        .build()
                }
                val newInstance = DefaultExperimentClient(
                    apiKey,
                    mergedConfig,
                    httpClient,
                    SharedPrefsStorage(application, apiKey, instanceName),
                    executorService,
                )
                instances[instanceKey] = newInstance
                newInstance
            }
            else -> instance
        }
    }

    /**
     * Initialize a singleton [ExperimentClient] which automatically
     * integrates with the installed and initialized instance of the amplitude
     * analytics SDK.
     *
     * You must be using Amplitude-Android SDK version 2.36.0+ for this
     * integration to work.
     *
     * @param application The Android Application context
     * @param apiKey  The API key. This can be found in the Experiment settings and should not be null or empty.
     * @param config See [ExperimentConfig] for configuration options. If null, uses default config.
     * You should not have to set userProvider or exposureTrackingProvider since those will be set
     * up for you.
     */
    @JvmStatic
    fun initializeWithAmplitudeAnalytics(
        application: Application,
        apiKey: String,
        config: ExperimentConfig? = null
    ): ExperimentClient = synchronized(instances) {
        val nonNullConfig = config ?: ExperimentConfig()
        val instanceName = nonNullConfig.instanceName
        val instanceKey = "$instanceName.$apiKey"
        val connector = AnalyticsConnector.getInstance(instanceName)
        val instance = when (val instance = instances[instanceKey]) {
            null -> {
                Logger.implementation = AndroidLogger(nonNullConfig.debug)
                val configBuilder = nonNullConfig.copyToBuilder()
                if (nonNullConfig.userProvider == null) {
                    configBuilder.userProvider(
                        ConnectorUserProvider(application, connector.identityStore)
                    )
                }
                if (nonNullConfig.exposureTrackingProvider == null) {
                    configBuilder.exposureTrackingProvider(
                        ConnectorExposureTrackingProvider(connector.eventBridge)
                    )
                }
                val newInstance = DefaultExperimentClient(
                    apiKey,
                    configBuilder.build(),
                    httpClient,
                    SharedPrefsStorage(application, apiKey, instanceName),
                    executorService,
                )
                instances[instanceKey] = newInstance
                if (nonNullConfig.automaticFetchOnAmplitudeIdentityChange) {
                    connector.identityStore.addIdentityListener {
                        newInstance.fetch()
                    }
                }
                newInstance
            }
            else -> instance
        }
        return instance
    }
}
