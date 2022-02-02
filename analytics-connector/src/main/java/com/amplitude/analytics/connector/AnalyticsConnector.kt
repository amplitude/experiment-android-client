package com.amplitude.analytics.connector

class AnalyticsConnector private constructor() {

    companion object {

        private val instancesLock = Any()
        private val instances = mutableMapOf<String, AnalyticsConnector>()

        @JvmStatic
        fun getInstance(instanceName: String): AnalyticsConnector {
            return synchronized(instancesLock) {
                instances.getOrPut(instanceName) {
                    AnalyticsConnector()
                }
            }
        }
    }

    val identityStore: IdentityStore = IdentityStoreImpl()
    val eventBridge: EventBridge = EventBridgeImpl()
}
