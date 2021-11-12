package com.amplitude.experiment

import android.content.Context
import com.amplitude.core.Identity
import com.amplitude.core.IdentityStore
import com.amplitude.experiment.util.LockResult


internal class CoreUserProvider(
    context: Context,
    private val identityStore: com.amplitude.core.IdentityStore,
): ExperimentUserProvider {

    private val base = DefaultUserProvider(context)

    override fun getUser(): ExperimentUser {
        val identity = identityStore.getIdentityOrWait()
        return base.getUser().copyToBuilder()
            .userId(identity.userId)
            .deviceId(identity.deviceId)
            .userProperties(identity.userProperties)
            .build()
    }
}

/**
 * Get the identity from the identity store or wait until the identity is
 * available.
 *
 * More complex to assure that no race conditions between getting the identity
 * directly and adding a listener.
 */
private fun com.amplitude.core.IdentityStore.getIdentityOrWait(): com.amplitude.core.Identity {
    val lock = Lock<com.amplitude.core.Identity>()
    val callback: (com.amplitude.core.Identity) -> Unit = { id ->
        lock.notify(LockResult.Success(id))
    }
    addIdentityListener(callback)
    val immediateIdentity = getIdentity()
    val result = if (immediateIdentity.isUnidentified()) {
        when(val result = lock.wait()) {
            is LockResult.Success -> result.value
            is LockResult.Error -> com.amplitude.core.Identity()
        }
    } else {
        immediateIdentity
    }
    removeIdentityListener(callback)
    return result
}