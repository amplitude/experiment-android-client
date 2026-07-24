package com.amplitude.experiment

import android.content.Context
import com.amplitude.core.AmplitudeContext
import com.amplitude.core.AnalyticsClient
import com.amplitude.core.AnalyticsIdentity
import com.amplitude.core.RestrictedAmplitudeFeature
import com.amplitude.core.platform.UniversalPlugin
import com.amplitude.core.remoteconfig.RemoteConfigClient
import com.amplitude.experiment.storage.SharedPrefsStorage
import com.amplitude.experiment.util.AmpLogger
import okhttp3.OkHttpClient
import com.amplitude.core.ServerZone as CoreServerZone

/**
 * A [UniversalPlugin] that hosts Experiment inside the unified Amplitude analytics client.
 *
 * Register with `amplitude.add(plugin)` on the unified client. Because [AmplitudeContext] does
 * not carry an Android [Context], pass an application [Context] to this plugin's constructor for
 * Experiment storage and device metadata.
 *
 * This is an additive entry point that parallels [Experiment.initializeWithAmplitudeAnalytics]
 * without replacing it.
 */
class AmplitudeExperimentPlugin
    @JvmOverloads
    constructor(
        context: Context,
        private val config: ExperimentConfig = ExperimentConfig(),
        private val deploymentKey: String? = null,
        private val remoteConfigWaitTimeoutMs: Long = DEFAULT_REMOTE_CONFIG_WAIT_TIMEOUT_MS,
    ) : UniversalPlugin {
        override val name: String =
            deploymentKey?.let { "${PLUGIN_NAME}_$it" } ?: PLUGIN_NAME

        private val applicationContext: Context = context.applicationContext ?: context
        private val lifecycleLock = Any()

        @Volatile
        var experimentClient: ExperimentClient? = null
            private set

        @Volatile
        private var analyticsClient: AnalyticsClient? = null

        @Volatile
        private var userProvider: AnalyticsClientUserProvider? = null

        @Volatile
        private var setupGeneration: Int = 0

        @Volatile
        private var startedGeneration: Int = -1

        @Volatile
        private var stoppedDueToOptOut: Boolean = false

        @OptIn(RestrictedAmplitudeFeature::class)
        @Volatile
        private var remoteConfigCallback: RemoteConfigClient.RemoteConfigCallback? = null

        private val httpClient = OkHttpClient()

        override fun setup(
            client: AnalyticsClient,
            context: AmplitudeContext,
        ) {
            synchronized(lifecycleLock) {
                invalidateCurrentGeneration()
                val generation = setupGeneration
                analyticsClient = client
                stoppedDueToOptOut = client.optOut
                val provider =
                    AnalyticsClientUserProvider(applicationContext) { analyticsClient }.also {
                        it.sessionId = client.sessionId
                        userProvider = it
                    }
                val apiKey = deploymentKey ?: context.apiKey
                AmpLogger.configure(config.logLevel, config.loggerProvider)
                val experimentConfig =
                    config
                        .copyToBuilder()
                        .instanceName(context.instanceName)
                        .serverZone(context.serverZone.toExperimentServerZone())
                        .userProvider(provider)
                        .exposureTrackingProvider(AnalyticsClientExposureTrackingProvider { analyticsClient })
                        .automaticFetchOnAmplitudeIdentityChange(false)
                        .build()
                val experiment =
                    DefaultExperimentClient(
                        apiKey,
                        experimentConfig,
                        httpClient,
                        SharedPrefsStorage(applicationContext),
                        Experiment.executorService,
                    )
                experimentClient = experiment
                subscribeForRemoteConfigAndStart(context, generation, experiment, provider, client)
            }
        }

        override fun onIdentityChanged(identity: AnalyticsIdentity) {
            synchronized(lifecycleLock) {
                val client = experimentClient ?: return
                val user = buildUser(identity, analyticsClient?.sessionId)
                client.setUser(user)
                if (config.automaticFetchOnAmplitudeIdentityChange &&
                    startedGeneration == setupGeneration &&
                    !stoppedDueToOptOut
                ) {
                    client.fetch(user)
                }
            }
        }

        override fun onSessionIdChanged(sessionId: Long) {
            synchronized(lifecycleLock) {
                userProvider?.sessionId = sessionId
                val client = experimentClient ?: return
                val user = buildUser(analyticsClient?.identity, sessionId)
                client.setUser(user)
            }
        }

        override fun onOptOutChanged(optOut: Boolean) {
            synchronized(lifecycleLock) {
                if (optOut) {
                    experimentClient?.stop()
                    stoppedDueToOptOut = true
                    return
                }
                if (!stoppedDueToOptOut) {
                    return
                }
                stoppedDueToOptOut = false
                val experiment = experimentClient ?: return
                val analytics = analyticsClient ?: return
                if (startedGeneration != setupGeneration) {
                    return
                }
                val provider = userProvider ?: return
                provider.sessionId = analytics.sessionId
                val user = buildUser(analytics.identity, analytics.sessionId)
                experiment.setUser(user)
                experiment.start(user)
            }
        }

        override fun onReset() {
            synchronized(lifecycleLock) {
                experimentClient?.clear()
                experimentClient?.setUser(buildUser(analyticsClient?.identity, analyticsClient?.sessionId))
            }
        }

        override fun teardown() {
            synchronized(lifecycleLock) {
                invalidateCurrentGeneration()
            }
        }

        @OptIn(RestrictedAmplitudeFeature::class)
        private fun invalidateCurrentGeneration() {
            setupGeneration++
            remoteConfigCallback = null
            experimentClient?.stop()
            experimentClient = null
            analyticsClient = null
            userProvider = null
            startedGeneration = -1
            stoppedDueToOptOut = false
        }

        @OptIn(RestrictedAmplitudeFeature::class)
        private fun subscribeForRemoteConfigAndStart(
            context: AmplitudeContext,
            generation: Int,
            experiment: DefaultExperimentClient,
            provider: AnalyticsClientUserProvider,
            client: AnalyticsClient,
        ) {
            // TODO(SDKA-5): switch to RemoteConfigClient.Key.Experiment once the dedicated key
            // lands on analytics-core.
            val callback =
                RemoteConfigClient.RemoteConfigCallback { _, _, _ ->
                    if (generation != setupGeneration) {
                        return@RemoteConfigCallback
                    }
                    startAfterRemoteConfig(generation, experiment, provider, client)
                }
            remoteConfigCallback = callback
            context.remoteConfigClient.subscribe(
                RemoteConfigClient.Key.Custom(EXPERIMENT_REMOTE_CONFIG_KEY),
                RemoteConfigClient.DeliveryMode.WaitForRemote(remoteConfigWaitTimeoutMs),
                callback,
            )
        }

        private fun startAfterRemoteConfig(
            generation: Int,
            experiment: DefaultExperimentClient,
            provider: AnalyticsClientUserProvider,
            client: AnalyticsClient,
        ) {
            synchronized(lifecycleLock) {
                if (generation != setupGeneration || startedGeneration == generation) {
                    return
                }
                startedGeneration = generation
                if (client.optOut) {
                    stoppedDueToOptOut = true
                    return
                }
                provider.sessionId = client.sessionId
                val user = buildUser(client.identity, client.sessionId)
                experiment.setUser(user)
                experiment.start(user)
            }
        }

        private fun buildUser(
            identity: AnalyticsIdentity?,
            sessionId: Long?,
        ): ExperimentUser {
            val resolvedSessionId = sessionId ?: analyticsClient?.sessionId
            resolvedSessionId?.let { userProvider?.sessionId = it }
            val builder = (userProvider?.deviceUser() ?: ExperimentUser()).copyToBuilder()
            if (identity != null) {
                builder
                    .userId(identity.userId)
                    .deviceId(identity.deviceId)
                    .userProperties(identity.userProperties)
            } else {
                analyticsClient?.identity?.let { analyticsIdentity ->
                    builder
                        .userId(analyticsIdentity.userId)
                        .deviceId(analyticsIdentity.deviceId)
                        .userProperties(analyticsIdentity.userProperties)
                }
            }
            resolvedSessionId?.let {
                builder.userProperty(AnalyticsClientUserProvider.SESSION_ID_USER_PROPERTY, it)
            }
            return builder.build()
        }

        private fun CoreServerZone.toExperimentServerZone(): ServerZone =
            when (this) {
                CoreServerZone.EU -> ServerZone.EU
                CoreServerZone.US -> ServerZone.US
            }

        companion object {
            const val PLUGIN_NAME = "com.amplitude.experiment"
            const val DEFAULT_REMOTE_CONFIG_WAIT_TIMEOUT_MS = 3_000L

            // TODO(SDKA-5): replace with RemoteConfigClient.Key.Experiment when available.
            private const val EXPERIMENT_REMOTE_CONFIG_KEY = "experiment.androidSDK"
        }
    }
