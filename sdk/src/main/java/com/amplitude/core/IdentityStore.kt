package com.amplitude.core

import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

data class Identity(
    val userId: String? = null,
    val deviceId: String? = null,
    val userProperties: JSONObject? = null,
) {
    fun isUnidentified(): Boolean {
        return userId.isNullOrEmpty() && deviceId.isNullOrEmpty()
    }
}

interface IdentityStore {

    interface Editor {

        fun setUserId(userId: String?): Editor
        fun setDeviceId(deviceId: String?): Editor
        fun setUserProperties(userProperties: JSONObject?): Editor
        fun updateUserProperties(actions: Map<String, JSONObject>): Editor
        fun commit()
    }

    fun editIdentity(): Editor
    fun setIdentity(identity: Identity)
    fun getIdentity(): Identity
    fun addIdentityListener(listener: (Identity) -> Unit)
    fun removeIdentityListener(listener: (Identity) -> Unit)
}

internal class IdentityStoreImpl: IdentityStore {

    private val identityLock = ReentrantLock(true)

    private var userId: String? = null
    private var deviceId: String? = null
    private var userProperties: JSONObject? = null

    private val listenersLock = Any()
    private val listeners: MutableSet<(Identity) -> Unit> = mutableSetOf()

    override fun editIdentity(): IdentityStore.Editor {
        identityLock.lock()
        val originalIdentity = Identity(userId, deviceId, userProperties)
        return object : IdentityStore.Editor {
            override fun setUserId(userId: String?): IdentityStore.Editor {
                this@IdentityStoreImpl.userId = userId
                return this
            }

            override fun setDeviceId(deviceId: String?): IdentityStore.Editor {
                this@IdentityStoreImpl.deviceId = deviceId
                return this
            }

            override fun setUserProperties(userProperties: JSONObject?): IdentityStore.Editor {
                this@IdentityStoreImpl.userProperties = userProperties
                return this
            }

            override fun updateUserProperties(actions: Map<String, JSONObject>): IdentityStore.Editor {
                var actingProperties = this@IdentityStoreImpl.userProperties ?: JSONObject()
                for (actionEntry in actions.entries) {
                    val action = actionEntry.key
                    val properties = actionEntry.value
                    when (action) {
                        "\$set" -> {
                            properties.forEach { key, value ->
                                actingProperties.put(key, value)
                            }
                        }
                        "\$unset" -> {
                            properties.forEach { key, _ ->
                                actingProperties.remove(key)
                            }
                        }
                        "\$prepend" -> {
                            properties.forEach { key, value ->
                                if (value is JSONArray) {
                                    val originalArray = actingProperties.optJSONArray(key)
                                    if (originalArray == null) {
                                        actingProperties.put(key, value)
                                    } else {
                                        actingProperties.put(key, value.append(originalArray))
                                    }
                                }
                            }
                        }
                        "\$append" -> {
                            properties.forEach { key, value ->
                                if (value is JSONArray) {
                                    val originalArray = actingProperties.optJSONArray(key)
                                    if (originalArray == null) {
                                        actingProperties.put(key, value)
                                    } else {
                                        actingProperties.put(key, originalArray.append(value))
                                    }
                                }
                            }
                        }
                        "\$clearAll" -> {
                            actingProperties = JSONObject()
                        }
                        "\$setOnce" -> {
                            properties.forEach { key, value ->
                                if (actingProperties.has(key)) {
                                    actingProperties.put(key, value)
                                }
                            }
                        }
                    }
                }
                if (actingProperties.length() == 0) {
                    this@IdentityStoreImpl.userProperties = null
                } else {
                    this@IdentityStoreImpl.userProperties = actingProperties
                }
                return this
            }

            override fun commit() {
                val newIdentity = Identity(userId, deviceId, userProperties)
                identityLock.unlock()
                if (newIdentity != originalIdentity) {
                    val safeListeners = synchronized(listenersLock) {
                        listeners
                    }
                    for (listener in safeListeners) {
                        listener(newIdentity)
                    }
                }
            }
        }
    }

    override fun setIdentity(identity: Identity) {
        identityLock.withLock {
            this.userId = identity.userId
            this.deviceId = identity.deviceId
            this.userProperties = identity.userProperties
        }
    }

    override fun getIdentity(): Identity {
        identityLock.withLock {
            return Identity(
                userId = userId,
                deviceId = deviceId,
                userProperties = userProperties,
            )
        }
    }

    override fun addIdentityListener(listener: (Identity) -> Unit) {
        synchronized(listenersLock) {
            listeners.add(listener)
        }
    }

    override fun removeIdentityListener(listener: (Identity) -> Unit) {
        synchronized(listenersLock) {
            listeners.remove(listener)
        }
    }
}

private fun JSONObject.forEach(action: (key: String, value: Any) -> Unit) {
    for (key in keys()) {
        action(key, get(key))
    }
}

/**
 * Append JSON arrays. Returns a new array rather than modifying the original.
 */
private fun JSONArray.append(other: JSONArray): JSONArray {
    val result = JSONArray()
    for (i in 0 until this.length()) {
        result.put(this[i])
    }
    for (i in 0 until other.length()) {
        result.put(other[i])
    }
    return result
}