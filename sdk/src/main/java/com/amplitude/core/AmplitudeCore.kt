package com.amplitude.core

class AmplitudeCore private constructor(
    instanceName: String,
) {

    companion object {
        @JvmStatic
        fun getInstance(instanceName: String): AmplitudeCore { TODO() }
    }

    val identityStore: IdentityStore = IdentityStoreImpl()
    val analyticsConnector: AnalyticsConnector = TODO()
}