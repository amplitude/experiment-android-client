package com.amplitude.experiment

import android.app.Application
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

    private const val DEFAULT_INSTANCE = "\$default_instance"
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
                    executorService
                )
                instances[instanceName] = newInstance
                newInstance
            }
            else -> instance
        }
    }
}
