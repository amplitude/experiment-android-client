package com.amplitude.experiment

import android.app.Application
import com.amplitude.experiment.storage.SharedPrefsStorage
import com.amplitude.experiment.util.AndroidLogger
import com.amplitude.experiment.util.Logger
import okhttp3.OkHttpClient

object Experiment {

    private const val DEFAULT_INSTANCE = "\$default_instance"
    internal const val TAG = "Experiment"
    private val httpClient = OkHttpClient()
    private val instances = mutableMapOf<String, ExperimentClient>()

    /**
     * Initializes a singleton [ExperimentClient] instance. Subsequent calls will return the
     * same instance, regardless of api key or config. However, It is advised to inject the client
     * inside your application rather than re-initializing
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
        val instanceName = DEFAULT_INSTANCE
        return when (val instance = instances[instanceName]) {
            null -> {
                Logger.implementation = AndroidLogger(config.debug)
                val newInstance = DefaultExperimentClient(
                    apiKey,
                    config,
                    httpClient,
                    SharedPrefsStorage(application, apiKey, instanceName),
                )
                instances[instanceName] = newInstance
                newInstance
            }
            else -> instance
        }
    }
}