package com.amplitude.analytics.connector

import com.amplitude.api.Identify
import com.amplitude.analytics.connector.util.toUpdateUserPropertiesMap
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class IdentityStoreTest {

    @Test
    fun `test editIdentity, setUserId setDeviceId, getIdentity, success`() {
        val identityStore = IdentityStoreImpl()
        identityStore.editIdentity()
            .setUserId("user_id")
            .setDeviceId("device_id")
            .commit()
        val identity = identityStore.getIdentity()
        Assert.assertEquals(Identity("user_id", "device_id"), identity)
    }

    @Test
    fun `test editIdentity, setUserId setDeviceId, identity listener called`() {
        val expectedIdentity = Identity("user_id", "device_id")
        val identityStore = IdentityStoreImpl()
        var listenerCalled = false
        identityStore.addIdentityListener {
            Assert.assertEquals(expectedIdentity, it)
            listenerCalled = true
        }
        identityStore.editIdentity()
            .setUserId("user_id")
            .setDeviceId("device_id")
            .commit()
        Assert.assertTrue(listenerCalled)
    }

    @Test
    fun `test setIdentity, getIdentity, success`() {
        val expectedIdentity = Identity("user_id", "device_id")
        val identityStore = IdentityStoreImpl()
        identityStore.setIdentity(expectedIdentity)
        val identity = identityStore.getIdentity()
        Assert.assertEquals(expectedIdentity, identity)
    }

    @Test
    fun `test setIdentity, identity listener called`() {
        val expectedIdentity = Identity("user_id", "device_id")
        val identityStore = IdentityStoreImpl()
        var listenerCalled = false
        identityStore.addIdentityListener {
            Assert.assertEquals(expectedIdentity, it)
            listenerCalled = true
        }
        identityStore.setIdentity(expectedIdentity)
        Assert.assertTrue(listenerCalled)
    }

    @Test
    fun `test setIdentity with unchanged identity, identity listener not called`() {
        val expectedIdentity = Identity("user_id", "device_id")
        val identityStore = IdentityStoreImpl()
        identityStore.setIdentity(expectedIdentity)
        identityStore.addIdentityListener {
            Assert.fail("identity listener should not be called")
        }
        identityStore.setIdentity(expectedIdentity)
    }

    @Test
    fun `test updateUserProperties, set`() {
        val identityStore = IdentityStoreImpl()
        identityStore.editIdentity()
            .updateUserProperties(mapOf("\$set" to mapOf("key1" to "value1")))
            .commit()
        val identity = identityStore.getIdentity()
        Assert.assertEquals(
            Identity(userProperties = mapOf("key1" to "value1")),
            identity
        )
    }

    @Test
    fun `test updateUserProperties, unset`() {
        val identityStore = IdentityStoreImpl()
        identityStore.setIdentity(
            Identity(userProperties = mapOf(
            "key" to "value",
            "other" to true,
            "final" to 4.2
        ))
        )
        identityStore.editIdentity()
            .updateUserProperties(
                mapOf("\$unset" to mapOf("other" to "-", "final" to "-"))
            ).commit()
        val identity = identityStore.getIdentity()
        Assert.assertEquals(
            Identity(userProperties = mapOf("key" to "value")),
            identity,
        )
    }

    @Test
    fun `test updateUserProperties, clearAll`() {
        val identityStore = IdentityStoreImpl()
        identityStore.setIdentity(
            Identity(userProperties = mapOf(
            "key" to listOf(-3, -2, -1, 0),
            "key2" to mapOf("wow" to "cool"),
            "key3" to 3,
            "key4" to false,
        ))
        )
        identityStore.editIdentity()
            .updateUserProperties(
                mapOf("\$clearAll" to mapOf())
            ).commit()
        val identity = identityStore.getIdentity()
        Assert.assertEquals(Identity(), identity)
    }

    @Test
    fun `test identify to user properties map, set`() {
        val identityStore = IdentityStoreImpl()
        val identify = Identify()
            .set("string", "string")
            .set("int", 32)
            .set("bool", true)
            .set("double", 4.2)
            .set("jsonArray", JSONArray().put(0).put(1.1).put(true).put("three"))
            .set("jsonObject", JSONObject().put("key", "value"))
        identityStore.editIdentity()
            .updateUserProperties(identify.userPropertiesOperations.toUpdateUserPropertiesMap())
            .commit()
        val identity = identityStore.getIdentity()
        Assert.assertEquals(
            Identity(userProperties = mapOf(
                "string" to "string",
                "int" to 32,
                "bool" to true,
                "double" to 4.2,
                "jsonArray" to listOf(0, 1.1, true, "three"),
                "jsonObject" to mapOf("key" to "value"),
            )),
            identity
        )
    }

    @Test
    fun `test identify to user properties map, unset`() {
        val identityStore = IdentityStoreImpl()
        identityStore.setIdentity(Identity(userProperties = mapOf("key" to "value")))
        val identify = Identify().unset("key")
        identityStore.editIdentity()
            .updateUserProperties(identify.userPropertiesOperations.toUpdateUserPropertiesMap())
            .commit()
        val identity = identityStore.getIdentity()
        Assert.assertEquals(Identity(), identity)
    }

    @Test
    fun `test identify to user properties map, clearAll`() {
        val identityStore = IdentityStoreImpl()
        val identify = Identify()
            .set("string", "string")
            .set("int", 32)
            .set("bool", true)
            .set("double", 4.2)
            .set("jsonArray", JSONArray().put(0).put(1.1).put(true).put("three"))
            .set("jsonObject", JSONObject().put("key", "value"))
        identityStore.editIdentity()
            .updateUserProperties(identify.userPropertiesOperations.toUpdateUserPropertiesMap())
            .commit()
        val clear = Identify().clearAll()
        identityStore.editIdentity()
            .updateUserProperties(clear.userPropertiesOperations.toUpdateUserPropertiesMap())
            .commit()
        val identity = identityStore.getIdentity()
        Assert.assertEquals(Identity(), identity)
    }
}
