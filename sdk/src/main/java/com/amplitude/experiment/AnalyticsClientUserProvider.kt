package com.amplitude.experiment

import android.content.Context
import com.amplitude.core.AnalyticsClient

internal class AnalyticsClientUserProvider(
    context: Context,
    private val clientProvider: () -> AnalyticsClient?,
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
                .userProperties(identity.userProperties)
        sessionId?.let { builder.userProperty(SESSION_ID_USER_PROPERTY, it) }
        return builder.build()
    }

    private fun baseUser(): ExperimentUser = baseUserProvider?.getUser() ?: ExperimentUser()

    internal fun deviceUser(): ExperimentUser = baseUser()

    internal companion object {
        const val SESSION_ID_USER_PROPERTY = "session_id"
    }
}
