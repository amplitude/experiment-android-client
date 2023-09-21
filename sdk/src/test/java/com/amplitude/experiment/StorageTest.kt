package com.amplitude.experiment

import com.amplitude.experiment.storage.transformVariantFromStorage
import com.amplitude.experiment.util.toJson
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class TransformVariantFromStorageTest {

    private val payload = "payload"

    @Test
    fun `v0 variant transformation`() {
        val storedVariant =  Variant("on").toJson()
        Assert.assertEquals(
            Variant(key = "on", value = "on"),
            transformVariantFromStorage(storedVariant)
        )
    }

    @Test
    fun `v1 variant transformation`() {
        val storedVariant = JSONObject(mapOf("value" to "on")).toString()
        Assert.assertEquals(
            Variant(key = "on", value = "on"),
            transformVariantFromStorage(storedVariant)
        )
    }

    @Test
    fun `v1 variant transformation with payload`() {
        val storedVariant = JSONObject(
            mapOf(
                "value" to "on",
                "payload" to payload
            )
        ).toString()
        Assert.assertEquals(
            Variant(key = "on", value = "on", payload = payload),
            transformVariantFromStorage(storedVariant)
        )
    }

    @Test
    fun `v1 variant transformation with payload and experiment key`() {
        val storedVariant = JSONObject(
            mapOf(
                "value" to "on",
                "payload" to payload,
                "expKey" to "exp-1"
            )
        ).toString()
        Assert.assertEquals(
            Variant(
                key = "on",
                value = "on",
                payload = payload,
                expKey = "exp-1",
                metadata = mapOf("experimentKey" to "exp-1")
            ),
            transformVariantFromStorage(storedVariant)
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
            transformVariantFromStorage(storedVariant)
        )
    }

    @Test
    fun `v2 variant transformation with payload`() {
        val storedVariant = JSONObject(
            mapOf(
                "key" to "treatment",
                "value" to "on",
                "payload" to payload
            )
        ).toString()
        Assert.assertEquals(
            Variant(
                key = "treatment",
                value = "on",
                payload = payload
            ),
            transformVariantFromStorage(storedVariant)
        )
    }

    @Test
    fun `v2 variant transformation with payload and experiment key`() {
        val storedVariant = JSONObject(
            mapOf(
                "key" to "treatment",
                "value" to "on",
                "payload" to payload,
                "expKey" to "exp-1"
            )
        ).toString()
        Assert.assertEquals(
            Variant(
                key = "treatment",
                value = "on",
                payload = payload,
                expKey = "exp-1",
                metadata = mapOf("experimentKey" to "exp-1")
            ),
            transformVariantFromStorage(storedVariant)
        )
    }

    @Test
    fun `v2 variant transformation with payload and experiment key metadata`() {
        val storedVariant = JSONObject(
            mapOf(
                "key" to "treatment",
                "value" to "on",
                "payload" to payload,
                "metadata" to mapOf("experimentKey" to "exp-1")
            )
        ).toString()
        Assert.assertEquals(
            Variant(
                key = "treatment",
                value = "on",
                payload = payload,
                expKey = "exp-1",
                metadata = mapOf("experimentKey" to "exp-1")
            ),
            transformVariantFromStorage(storedVariant)
        )
    }
}
