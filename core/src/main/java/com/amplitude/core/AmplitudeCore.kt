package com.amplitude.core

class AmplitudeCore private constructor() {

    companion object {

        private val instancesLock = Any()
        private val instances = mutableMapOf<String, AmplitudeCore>()

        @JvmStatic
        fun getInstance(instanceName: String): AmplitudeCore {
            return synchronized(instancesLock) {
                instances.getOrPut(instanceName) {
                    AmplitudeCore()
                }
            }
        }
    }

    val identityStore: IdentityStore = IdentityStoreImpl()
    val analyticsConnector: AnalyticsConnector = AnalyticsConnectorImpl()
}
