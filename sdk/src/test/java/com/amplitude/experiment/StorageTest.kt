package com.amplitude.experiment

import com.amplitude.experiment.storage.decodeVariantFromStorage
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class StorageTest {

    @Test
    fun `v1 variant transformation`() {
        val storedVariant = JSONObject(mapOf("value" to "on")).toString()
        Assert.assertEquals(
            Variant(key = "on", value = "on"),
            decodeVariantFromStorage(storedVariant)
        )
    }

    @Test
    fun `v1 variant transformation with payload`() {
        val storedVariant = JSONObject(
            mapOf(
                "value" to "on",
                "payload" to mapOf("k" to "v")
            )
        ).toString()
        Assert.assertEquals(
            Variant(key = "on", value = "on", payload = mapOf("k" to "v")),
            decodeVariantFromStorage(storedVariant)
        )
    }

    @Test
    fun `v1 variant transformation with payload and experiment key`() {
        val storedVariant = JSONObject(
            mapOf(
                "value" to "on",
                "payload" to mapOf("k" to "v"),
                "expKey" to "exp-1"
            )
        ).toString()
        Assert.assertEquals(
            Variant(
                key = "on",
                value = "on",
                payload = mapOf("k" to "v"),
                expKey = "exp-1",
                metadata = mapOf("experimentKey" to "exp-1")
            ),
            decodeVariantFromStorage(storedVariant)
        )
    }

    @Test
    fun `v2 variant transformation`() {
        val storedVariant = JSONObject(
            mapOf(
                "key" to "treatment",
                "value" to "on"
            )
        ).toString()
        Assert.assertEquals(
            Variant(key = "treatment", value = "on"),
            decodeVariantFromStorage(storedVariant)
        )
    }

    @Test
    fun `v2 variant transformation with payload`() {
        val storedVariant = JSONObject(
            mapOf(
                "key" to "treatment",
                "value" to "on",
                "payload" to mapOf("k" to "v")
            )
        ).toString()
        Assert.assertEquals(
            Variant(
                key = "treatment",
                value = "on",
                payload = mapOf("k" to "v")
            ),
            decodeVariantFromStorage(storedVariant)
        )
    }

    @Test
    fun `v2 variant transformation with payload and experiment key`() {
        val storedVariant = JSONObject(
            mapOf(
                "key" to "treatment",
                "value" to "on",
                "payload" to mapOf("k" to "v"),
                "expKey" to "exp-1"
            )
        ).toString()
        Assert.assertEquals(
            Variant(
                key = "treatment",
                value = "on",
                payload = mapOf("k" to "v"),
                expKey = "exp-1",
                metadata = mapOf("experimentKey" to "exp-1")
            ),
            decodeVariantFromStorage(storedVariant)
        )
    }

    @Test
    fun `v2 variant transformation with payload and experiment key metadata`() {
        val storedVariant = JSONObject(
            mapOf(
                "key" to "treatment",
                "value" to "on",
                "payload" to mapOf("k" to "v"),
                "metadata" to mapOf("experimentKey" to "exp-1")
            )
        ).toString()
        Assert.assertEquals(
            Variant(
                key = "treatment",
                value = "on",
                payload = mapOf("k" to "v"),
                expKey = "exp-1",
                metadata = mapOf("experimentKey" to "exp-1")
            ),
            decodeVariantFromStorage(storedVariant)
        )
    }
}
