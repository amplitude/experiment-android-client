package com.amplitude.core

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal const val ID_OP_SET = "\$set"
internal const val ID_OP_UNSET = "\$unset"
internal const val ID_OP_SET_ONCE = "\$setOnce"
internal const val ID_OP_ADD = "\$add"
internal const val ID_OP_APPEND = "\$append"
internal const val ID_OP_PREPEND = "\$prepend"
internal const val ID_OP_CLEAR_ALL = "\$clearAll"

data class Identity(
    val userId: String? = null,
    val deviceId: String? = null,
    val userProperties: Map<String, Any?> = mapOf(),
) {
    fun isUnidentified(): Boolean {
        return userId.isNullOrEmpty() && deviceId.isNullOrEmpty()
    }
}

interface IdentityStore {

    interface Editor {

        fun setUserId(userId: String?): Editor
        fun setDeviceId(deviceId: String?): Editor
        fun setUserProperties(userProperties: Map<String, Any?>): Editor
        fun updateUserProperties(actions: Map<String, Map<String, Any?>>): Editor
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
    private var userProperties: Map<String, Any?> = mapOf()

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

            override fun setUserProperties(userProperties: Map<String, Any?>): IdentityStore.Editor {
                this@IdentityStoreImpl.userProperties = userProperties
                return this
            }

            override fun updateUserProperties(actions: Map<String, Map<String, Any?>>): IdentityStore.Editor {
                val actingProperties = this@IdentityStoreImpl.userProperties.toMutableMap()
                for (actionEntry in actions.entries) {
                    val action = actionEntry.key
                    val properties = actionEntry.value
                    when (action) {
                        ID_OP_SET -> {
                            actingProperties.putAll(properties)
                        }
                        ID_OP_UNSET -> {
                            for (entry in properties.entries) {
                                actingProperties.remove(entry.key)
                            }
                        }
                        ID_OP_SET_ONCE -> {
                            for (entry in properties.entries) {
                                actingProperties.getOrPut(entry.key) {
                                    entry.value
                                }
                            }
                        }
                        ID_OP_ADD -> {
                            for (entry in properties.entries) {
                                val value = entry.value
                                val actingValue = actingProperties[entry.key] ?: 0
                                // All lesser numbers can be represented as doubles for simplicity
                                // TODO: Float values will lose precision when converted to double.
                                // The current move from (Float | Double) -> BigInteger from org.json
                                // makes this check unnecessary since all floats will be precisely
                                // converted.
                                if (value is Number && actingValue is Number) {
                                    actingProperties[entry.key] = value.toDouble() + actingValue.toDouble()
                                }
                            }
                        }
                        ID_OP_APPEND -> {
                            for (entry in properties.entries) {
                                val actingValue = actingProperties[entry.key]
                                val value = entry.value
                                if (value is List<Any?> && actingValue is List<Any?>) {
                                    actingProperties[entry.key] = actingValue + value.toMutableList()
                                }
                            }
                        }
                        ID_OP_PREPEND -> {
                            for (entry in properties.entries) {
                                val actingValue = actingProperties[entry.key]
                                val value = entry.value
                                if (value is List<Any?> && actingValue is List<Any?>) {
                                    actingProperties[entry.key] = value.toMutableList() + actingValue
                                }
                            }
                        }
                        ID_OP_CLEAR_ALL -> {
                            actingProperties.clear()
                        }

                    }
                }
                this@IdentityStoreImpl.userProperties = actingProperties
                return this
            }

            override fun commit() {
                val newIdentity = Identity(userId, deviceId, userProperties)
                identityLock.unlock()
                if (newIdentity != originalIdentity) {
                    val safeListeners = synchronized(listenersLock) {
                        listeners.toSet()
                    }
                    for (listener in safeListeners) {
                        listener(newIdentity)
                    }
                }
            }
        }
    }

    override fun setIdentity(identity: Identity) {
        editIdentity()
            .setUserId(identity.userId)
            .setDeviceId(identity.deviceId)
            .setUserProperties(identity.userProperties)
            .commit()
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
