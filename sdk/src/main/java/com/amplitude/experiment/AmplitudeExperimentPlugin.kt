package com.amplitude.experiment

import android.content.Context
import com.amplitude.analytics.connector.AnalyticsConnector
import com.amplitude.analytics.connector.Identity
import com.amplitude.analytics.connector.IdentityListener
import com.amplitude.core.AmplitudeContext
import com.amplitude.core.AnalyticsClient
import com.amplitude.core.AnalyticsIdentity
import com.amplitude.core.events.AnalyticsEvent
import com.amplitude.core.platform.UniversalPlugin
import com.amplitude.experiment.util.AmpLogger
import java.util.concurrent.Future
import com.amplitude.core.ServerZone as CoreServerZone

/**
 * A [UniversalPlugin] that hosts Experiment inside a unified Amplitude analytics client.
 *
 * Register with `amplitude.add(plugin)`. Pass an application [Context] for storage and
 * device metadata; [AmplitudeContext] does not include one.
 *
 * In owned mode (the default [context] constructor), creates the Experiment client in [setup]
 * and calls [ExperimentClient.start] once the host has a device id and session. Stops polling
 * on opt-out and starts again on opt-in. Clears assignments on host reset. [teardown] stops
 * and drops the client.
 *
 * In wrap mode ([experiment] constructor), exposes an existing client for host lookup
 * without modifying its configuration, start/stop lifecycle, or identity synchronization.
 *
 * Do not also call [Experiment.initialize] or [Experiment.initializeWithAmplitudeAnalytics]
 * for the same instance name and API key when using owned mode. That starts a second client
 * and duplicates fetches, polling, and exposure tracking.
 *
 * [ExperimentClient.stop] only stops flag polling; [ExperimentClient.variant] still
 * evaluates locally while the host is opted out.
 */
class AmplitudeExperimentPlugin private constructor(
    private val applicationContext: Context?,
    private val config: ExperimentConfig?,
    private val deploymentKey: String?,
    private val externalExperimentClient: ExperimentClient?,
) : UniversalPlugin {
    @JvmOverloads
    constructor(
        context: Context,
        config: ExperimentConfig = ExperimentConfig(),
        deploymentKey: String? = null,
    ) : this(context.applicationContext ?: context, config, deploymentKey, null)

    /**
     * Wraps an existing [ExperimentClient] without changing its configuration or lifecycle.
     *
     * The plugin does not call [ExperimentClient.start], [ExperimentClient.stop], or [ExperimentClient.clear],
     * and does not synchronize identity, session, or opt-out state from the analytics host.
     */
    constructor(experiment: ExperimentClient) : this(
        applicationContext = null,
        config = null,
        deploymentKey = (experiment as? DefaultExperimentClient)?.apiKey,
        externalExperimentClient = experiment,
    )

    override val name: String? =
        when {
            externalExperimentClient != null && deploymentKey == null -> null
            else -> pluginName(deploymentKey)
        }

    private val ownsExperimentClient: Boolean = externalExperimentClient == null
    private val lifecycleLock = Any()

    @Volatile
    var experimentClient: ExperimentClient? = externalExperimentClient
        private set

    @Volatile
    private var analyticsClient: AnalyticsClient? = null

    @Volatile
    private var userProvider: AnalyticsClientUserProvider? = null

    @Volatile
    private var stoppedDueToOptOut: Boolean = false

    @Volatile
    private var started: Boolean = false

    private var inFlightOperation: Future<ExperimentClient>? = null

    @Volatile
    private var connectorInstanceName: String? = null

    @Volatile
    private var connectorIdentityListener: IdentityListener? = null

    @Volatile
    private var lastConnectorUserId: String? = null

    @Volatile
    private var lastConnectorDeviceId: String? = null

    @Volatile
    private var lastConnectorUserProperties: Map<String, Any?> = emptyMap()

    override fun setup(
        client: AnalyticsClient,
        context: AmplitudeContext,
    ) {
        synchronized(lifecycleLock) {
            teardownLocked()
            analyticsClient = client
            if (!ownsExperimentClient) {
                experimentClient = externalExperimentClient
                return
            }
            val applicationContext = checkNotNull(applicationContext)
            val config = checkNotNull(config)
            stoppedDueToOptOut = client.optOut
            connectorInstanceName = context.instanceName
            val provider =
                AnalyticsClientUserProvider(
                    applicationContext,
                    { analyticsClient },
                    context.instanceName,
                ).also {
                    it.sessionId = client.sessionId.takeUnless { sessionId -> sessionId == UNINITIALIZED_SESSION_ID }
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
                    .automaticFetchOnAmplitudeIdentityChange(
                        config.automaticFetchOnAmplitudeIdentityChange,
                    )
                    .build()
            experimentClient = createExperimentClient(apiKey, experimentConfig)
            if (config.automaticFetchOnAmplitudeIdentityChange) {
                // Identify $set updates commit to the connector only; UniversalPlugin
                // onIdentityChanged does not receive user-property maps on Amplitude-Kotlin.
                snapshotConnectorIdentity(context.instanceName)
                val listener: IdentityListener = { identity: Identity ->
                    synchronized(lifecycleLock) {
                        handleConnectorIdentity(identity)
                    }
                }
                connectorIdentityListener = listener
                AnalyticsConnector.getInstance(context.instanceName)
                    .identityStore
                    .addIdentityListener(listener)
            }
            maybeStartLocked()
        }
    }

    override fun <T : AnalyticsEvent> execute(event: T): T? = event

    override fun onIdentityChanged(identity: AnalyticsIdentity) {
        synchronized(lifecycleLock) {
            if (!ownsExperimentClient) return
            val client = experimentClient ?: return
            val user = buildUser(identity, analyticsClient?.sessionId)
            client.setUser(user)
            if (!maybeStartLocked() &&
                config?.automaticFetchOnAmplitudeIdentityChange == true &&
                started &&
                !stoppedDueToOptOut
            ) {
                replaceInFlightOperationLocked { client.fetch(user) }
            }
        }
    }

    override fun onSessionIdChanged(sessionId: Long) {
        synchronized(lifecycleLock) {
            if (!ownsExperimentClient) return
            userProvider?.sessionId = sessionId.takeUnless { it == UNINITIALIZED_SESSION_ID }
            val client = experimentClient ?: return
            val user = buildUser(analyticsClient?.identity, sessionId)
            client.setUser(user)
            maybeStartLocked()
        }
    }

    override fun onOptOutChanged(optOut: Boolean) {
        synchronized(lifecycleLock) {
            if (!ownsExperimentClient) return
            if (optOut) {
                cancelInFlightOperationLocked()
                experimentClient?.stop()
                stoppedDueToOptOut = true
                started = false
                connectorInstanceName?.let { snapshotConnectorIdentity(it) }
                return
            }
            if (!stoppedDueToOptOut) {
                return
            }
            stoppedDueToOptOut = false
            connectorInstanceName?.let { snapshotConnectorIdentity(it) }
            maybeStartLocked()
        }
    }

    override fun onReset() {
        synchronized(lifecycleLock) {
            if (!ownsExperimentClient) return
            val experiment = experimentClient ?: return
            // Host reset notifies onIdentityChanged then onReset. Clear assignments that
            // belonged to the previous user; fetch only when automatic identity fetch is on.
            cancelInFlightOperationLocked()
            experiment.clear()
            val user = buildUser(analyticsClient?.identity, analyticsClient?.sessionId)
            experiment.setUser(user)
            if (config?.automaticFetchOnAmplitudeIdentityChange == true && !stoppedDueToOptOut) {
                inFlightOperation = experiment.fetch(user)
            }
        }
    }

    override fun teardown() {
        synchronized(lifecycleLock) {
            teardownLocked()
        }
    }

    private fun createExperimentClient(
        apiKey: String,
        experimentConfig: ExperimentConfig,
    ): ExperimentClient = Experiment.createClient(checkNotNull(applicationContext), apiKey, experimentConfig)

    private fun maybeStartLocked(): Boolean {
        if (started || stoppedDueToOptOut) {
            return false
        }
        val experiment = experimentClient ?: return false
        val analytics = analyticsClient ?: return false
        if (analytics.optOut || !isHostIdentityReady(analytics)) {
            return false
        }
        val provider = userProvider ?: return false
        provider.sessionId = analytics.sessionId
        val user = buildUser(analytics.identity, analytics.sessionId)
        experiment.setUser(user)
        replaceInFlightOperationLocked { experiment.start(user) }
        started = true
        return true
    }

    private fun isHostIdentityReady(client: AnalyticsClient): Boolean {
        val deviceId = client.identity.deviceId
        return !deviceId.isNullOrEmpty() && client.sessionId != UNINITIALIZED_SESSION_ID
    }

    private fun snapshotConnectorIdentity(instanceName: String) {
        val identity =
            try {
                AnalyticsConnector.getInstance(instanceName).identityStore.getIdentity()
            } catch (_: Exception) {
                return
            }
        lastConnectorUserId = identity.userId
        lastConnectorDeviceId = identity.deviceId
        lastConnectorUserProperties = identity.userProperties.toMap()
    }

    private fun handleConnectorIdentity(identity: Identity) {
        val userIdChanged = identity.userId != lastConnectorUserId
        val deviceIdChanged = identity.deviceId != lastConnectorDeviceId
        val propertiesChanged = identity.userProperties != lastConnectorUserProperties
        lastConnectorUserId = identity.userId
        lastConnectorDeviceId = identity.deviceId
        lastConnectorUserProperties = identity.userProperties.toMap()
        if (!started || stoppedDueToOptOut) return
        val client = experimentClient ?: return
        // User/device id changes are handled by UniversalPlugin.onIdentityChanged.
        if (userIdChanged || deviceIdChanged || !propertiesChanged) {
            return
        }
        val user = buildUser(analyticsClient?.identity, analyticsClient?.sessionId)
        client.setUser(user)
        replaceInFlightOperationLocked { client.fetch(user) }
    }

    private fun replaceInFlightOperationLocked(operation: () -> Future<ExperimentClient>) {
        cancelInFlightOperationLocked()
        inFlightOperation = operation()
    }

    private fun cancelInFlightOperationLocked() {
        inFlightOperation?.cancel(true)
        inFlightOperation = null
        (experimentClient as? DefaultExperimentClient)?.cancelPendingFetches()
    }

    private fun teardownLocked() {
        val listener = connectorIdentityListener
        val instanceName = connectorInstanceName
        if (listener != null && instanceName != null) {
            AnalyticsConnector.getInstance(instanceName)
                .identityStore
                .removeIdentityListener(listener)
        }
        connectorIdentityListener = null
        connectorInstanceName = null
        lastConnectorUserId = null
        lastConnectorDeviceId = null
        lastConnectorUserProperties = emptyMap()
        if (ownsExperimentClient) {
            cancelInFlightOperationLocked()
            experimentClient?.stop()
            experimentClient = null
        } else {
            experimentClient = externalExperimentClient
        }
        analyticsClient = null
        userProvider = null
        stoppedDueToOptOut = false
        started = false
    }

    private fun buildUser(
        identity: AnalyticsIdentity?,
        sessionId: Long?,
    ): ExperimentUser {
        val resolvedSessionId =
            (sessionId ?: analyticsClient?.sessionId)
                ?.takeUnless { it == UNINITIALIZED_SESSION_ID }
        resolvedSessionId?.let { userProvider?.sessionId = it }
        // Prefer the user provider so connector-backed user properties are preserved when
        // AnalyticsIdentity.userProperties is empty (Amplitude-Kotlin today).
        val builder = (userProvider?.getUser() ?: ExperimentUser()).copyToBuilder()
        if (identity != null) {
            builder.userId(identity.userId).deviceId(identity.deviceId)
            if (identity.userProperties.isNotEmpty()) {
                builder.userProperties(identity.userProperties)
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
        private const val UNINITIALIZED_SESSION_ID = -1L

        internal fun pluginName(deploymentKey: String?): String = deploymentKey?.let { "${PLUGIN_NAME}_$it" } ?: PLUGIN_NAME
    }
}
