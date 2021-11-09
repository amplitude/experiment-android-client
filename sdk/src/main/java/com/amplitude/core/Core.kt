package com.amplitude.core

//
// Identity
//

data class Identity(
    val userId: String? = null,
    val deviceId: String? = null,
    val userProperties: Map<String, Any>? = null,
)

interface IdentityStore {

    interface Editor {

        fun setUserId(userId: String): Editor
        fun setDeviceId(deviceId: String): Editor
        fun updateUserProperties(action: String, key: String, value: Any): Editor
        fun commit()
    }

    fun editIdentity(): Editor
    fun setIdentity(identity: Identity)
    fun getIdentity(): Identity
    fun addIdentityListener(listener: (Identity) -> Unit)
    fun removeIdentityListener(listener: (Identity) -> Unit)
}

//
// Analytics
//

data class AnalyticsEvent(
    val eventType: String,
    val eventProperties: Map<String, Any>? = null,
    val userProperties: Map<String, Map<String, Any>>? = null,
)

interface AnalyticsConnector {

    fun logEvent(event: AnalyticsEvent)
    fun addEventListener(listener: (AnalyticsEvent) -> Unit)
    fun removeEventListener(listener: (AnalyticsEvent) -> Unit)
}

//
// Component
//

enum class Component {
    ANALYTICS,
    EXPERIMENT
}

enum class ComponentState {
    REGISTERED,
    INITIALIZED
}

interface ComponentRegistrar {

    fun getState(component: Component): ComponentState?
    fun setState(component: Component, state: ComponentState)
    fun addComponentListener(listener: (Component, ComponentState) -> Unit)
    fun removeComponentListener(listener: (Component, ComponentState) -> Unit)
}

//
// Core
//

class AmplitudeCore private constructor(
    instanceName: String,
) {

    companion object {
        @JvmStatic
        fun getInstance(instanceName: String): AmplitudeCore { TODO() }
    }

    val identityStore: IdentityStore = TODO()
    val analyticsConnector: AnalyticsConnector = TODO()
    val componentRegistrar: ComponentRegistrar = TODO()
}

