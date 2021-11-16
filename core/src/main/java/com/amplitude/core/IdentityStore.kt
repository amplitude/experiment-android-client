package com.amplitude.core

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

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

    private val identityLock = ReentrantReadWriteLock(true)

    private var identity = Identity()

    private val listenersLock = Any()
    private val listeners: MutableSet<(Identity) -> Unit> = mutableSetOf()

    override fun editIdentity(): IdentityStore.Editor {
        val originalIdentity = getIdentity()
        return object : IdentityStore.Editor {

            private var userId: String? = originalIdentity.userId
            private var deviceId: String? = originalIdentity.deviceId
            private var userProperties: Map<String, Any?> = originalIdentity.userProperties

            override fun setUserId(userId: String?): IdentityStore.Editor {
                this.userId = userId
                return this
            }

            override fun setDeviceId(deviceId: String?): IdentityStore.Editor {
                this.deviceId = deviceId
                return this
            }

            override fun setUserProperties(userProperties: Map<String, Any?>): IdentityStore.Editor {
                this.userProperties = userProperties
                return this
            }

            override fun updateUserProperties(actions: Map<String, Map<String, Any?>>): IdentityStore.Editor {
                val actingProperties = this.userProperties.toMutableMap()
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
                this.userProperties = actingProperties
                return this
            }

            override fun commit() {
                val newIdentity = Identity(userId, deviceId, userProperties)
                setIdentity(newIdentity)
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
        identityLock.write {
            this.identity = identity
        }
    }

    override fun getIdentity(): Identity {
        return identityLock.read {
            this.identity
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
