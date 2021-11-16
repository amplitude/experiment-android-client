package com.amplitude.core

import com.amplitude.api.Identify
import com.amplitude.core.util.toUpdateUserPropertiesMap
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
        identityStore.setIdentity(Identity(userProperties = mapOf(
            "key" to "value",
            "other" to true,
            "final" to 4.2
        )))
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

    // TODO: Float inputs fail to retain precision. This wont happen when using
    //       toUpdateUserPropertiesMap from JSON Object since floating point
    //       precision is kept. May need to handle this use case later.
    @Test
    fun `test updateUserProperties, add`() {
        val identityStore = IdentityStoreImpl()
        identityStore.setIdentity(Identity(userProperties = mapOf(
            "byte" to 1.toByte(),
            "short" to 1.toShort(),
            "int" to 1,
            "long" to 1L,
            "double" to 1.1,
        )))
        identityStore.editIdentity()
            .updateUserProperties(
                mapOf("\$add" to mapOf(
                    "byte" to 1.1,
                    "short" to 1.1,
                    "int" to 1.1,
                    "long" to 1.1,
                    "double" to 1,
                ))
            ).commit()
        val identity = identityStore.getIdentity()
        Assert.assertEquals(
            Identity(userProperties = mapOf(
                "byte" to 2.1,
                "short" to 2.1,
                "int" to 2.1,
                "long" to 2.1,
                "double" to 2.1,
            )),
            identity,
        )
    }

    @Test
    fun `test updateUserProperties, prepend`() {
        val identityStore = IdentityStoreImpl()
        identityStore.setIdentity(Identity(userProperties = mapOf(
            "key" to listOf(0, 1, 2, 3)
        )))
        identityStore.editIdentity()
            .updateUserProperties(
                mapOf("\$prepend" to mapOf("key" to listOf(-3, -2, -1)))
            ).commit()
        val identity = identityStore.getIdentity()
        Assert.assertEquals(
            Identity(userProperties = mapOf("key" to listOf(-3, -2, -1, 0, 1, 2, 3))),
            identity,
        )
    }

    @Test
    fun `test updateUserProperties, append`() {
        val identityStore = IdentityStoreImpl()
        identityStore.setIdentity(Identity(userProperties = mapOf(
            "key" to listOf(-3, -2, -1, 0)
        )))
        identityStore.editIdentity()
            .updateUserProperties(
                mapOf("\$append" to mapOf("key" to listOf(1, 2, 3)))
            ).commit()
        val identity = identityStore.getIdentity()
        Assert.assertEquals(
            Identity(userProperties = mapOf("key" to listOf(-3, -2, -1, 0, 1, 2, 3))),
            identity,
        )
    }

    @Test
    fun `test updateUserProperties, clearAll`() {
        val identityStore = IdentityStoreImpl()
        identityStore.setIdentity(Identity(userProperties = mapOf(
            "key" to listOf(-3, -2, -1, 0),
            "key2" to mapOf("wow" to "cool"),
            "key3" to 3,
            "key4" to false,
        )))
        identityStore.editIdentity()
            .updateUserProperties(
                mapOf("\$clearAll" to mapOf())
            ).commit()
        val identity = identityStore.getIdentity()
        Assert.assertEquals(Identity(), identity)
    }

    @Test
    fun `test updateUserProperties, setOnce`() {
        val identityStore = IdentityStoreImpl()
        identityStore.editIdentity()
            .updateUserProperties(mapOf("\$setOnce" to mapOf("key1" to "value1")))
            .commit()
        var identity = identityStore.getIdentity()
        Assert.assertEquals(
            Identity(userProperties = mapOf("key1" to "value1")),
            identity
        )
        identityStore.editIdentity()
            .updateUserProperties(mapOf("\$setOnce" to mapOf("key1" to "value2")))
            .commit()
        identity = identityStore.getIdentity()
        Assert.assertEquals(
            Identity(userProperties = mapOf("key1" to "value1")),
            identity
        )
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
    fun `test identify to user properties map, add`() {
        val identityStore = IdentityStoreImpl()
        val identify = Identify()
            .set("int", 1)
            .set("long", 1.toLong())
            .set("float", 1.1f)
            .set("double", 1.1)
        identityStore.editIdentity()
            .updateUserProperties(identify.userPropertiesOperations.toUpdateUserPropertiesMap())
            .commit()
        val add = Identify()
            .add("int", 1.1)
            .add("long", 1.1f)
            .add("float", 1)
            .add("double", 1L)
        identityStore.editIdentity()
            .updateUserProperties(add.userPropertiesOperations.toUpdateUserPropertiesMap())
            .commit()
        val identity = identityStore.getIdentity()
        Assert.assertEquals(
            Identity(userProperties = mapOf(
                "int" to 2.1,
                "long" to 2.1,
                "float" to 2.1,
                "double" to 2.1,
            )),
            identity,
        )
    }

    @Test
    fun `test identify to user properties map, append`() {
        val identityStore = IdentityStoreImpl()
        identityStore.setIdentity(Identity(userProperties = mapOf("key" to listOf(-3, -2, -1, 0))))
        val identify = Identify().append("key", intArrayOf(1, 2, 3))
        identityStore.editIdentity()
            .updateUserProperties(identify.userPropertiesOperations.toUpdateUserPropertiesMap())
            .commit()
        val identity = identityStore.getIdentity()
        Assert.assertEquals(
            Identity(userProperties = mapOf(
                "key" to listOf(-3, -2, -1, 0, 1, 2, 3)
            )),
            identity
        )
    }

    @Test
    fun `test identify to user properties map, prepend`() {
        val identityStore = IdentityStoreImpl()
        identityStore.setIdentity(Identity(userProperties = mapOf("key" to listOf(0, 1, 2, 3))))
        val identify = Identify().prepend("key", intArrayOf(-3, -2, -1))
        identityStore.editIdentity()
            .updateUserProperties(identify.userPropertiesOperations.toUpdateUserPropertiesMap())
            .commit()
        val identity = identityStore.getIdentity()
        Assert.assertEquals(
            Identity(userProperties = mapOf(
                "key" to listOf(-3, -2, -1, 0, 1, 2, 3)
            )),
            identity
        )
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
