package com.amplitude.experiment

import android.content.Context
import com.amplitude.analytics.connector.AnalyticsConnector
import com.amplitude.core.AnalyticsClient
import com.amplitude.core.AnalyticsIdentity

internal class AnalyticsClientUserProvider(
    context: Context,
    private val clientProvider: () -> AnalyticsClient?,
    private val instanceName: String = ExperimentConfig.Defaults.INSTANCE_NAME,
) : ExperimentUserProvider {
    private val baseUserProvider: ExperimentUserProvider? =
        try {
            DefaultUserProvider(context)
        } catch (e: Exception) {
            null
        }

    @Volatile
    var sessionId: Long? = null

    override fun getUser(): ExperimentUser {
        val client = clientProvider() ?: return baseUser()
        val identity = client.identity
        val builder =
            baseUser()
                .copyToBuilder()
                .userId(identity.userId)
                .deviceId(identity.deviceId)
                .userProperties(resolveUserProperties(identity))
        sessionId?.let { builder.userProperty(SESSION_ID_USER_PROPERTY, it) }
        return builder.build()
    }

    /**
     * Prefer [AnalyticsIdentity.userProperties] when the host maintains a live map (iOS-style).
     * Amplitude-Kotlin currently leaves that empty, so fall back to analytics-connector's
     * [com.amplitude.analytics.connector.IdentityStore] — the same source
     * [Experiment.initializeWithAmplitudeAnalytics] uses.
     */
    private fun resolveUserProperties(identity: AnalyticsIdentity): Map<String, Any?> {
        if (identity.userProperties.isNotEmpty()) {
            return identity.userProperties
        }
        return connectorUserProperties()
    }

    private fun connectorUserProperties(): Map<String, Any?> {
        return try {
            AnalyticsConnector.getInstance(instanceName).identityStore.getIdentity().userProperties
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun baseUser(): ExperimentUser = baseUserProvider?.getUser() ?: ExperimentUser()

    internal companion object {
        const val SESSION_ID_USER_PROPERTY = "session_id"
    }
}
