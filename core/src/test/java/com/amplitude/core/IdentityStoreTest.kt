package com.amplitude.core

import org.junit.Assert
import org.junit.Test

class IdentityStoreTest {
    @Test
    fun `test edit identity, setUserId setDeviceId, getIdentity, success`() {
        val identityStore = IdentityStoreImpl()
        identityStore.editIdentity()
            .setUserId("user_id")
            .setDeviceId("device_id")
            .commit()
        val identity = identityStore.getIdentity()
        Assert.assertEquals(Identity("user_id", "device_id"), identity)
    }
}