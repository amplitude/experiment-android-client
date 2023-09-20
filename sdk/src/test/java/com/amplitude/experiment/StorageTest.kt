package com.amplitude.experiment

import com.amplitude.experiment.storage.transformVariantFromStorage
import org.junit.Assert
import org.junit.Test

class TransformVariantFromStorageTest {

    @Test
    fun `v0 variant transformation`() {
        val storedVariant = "on"
        Assert.assertEquals(
            Variant(key = "on", value = "on"),
            transformVariantFromStorage(storedVariant)
        )
    }

    @Test
    fun `v1 variant transformation`() {
        val storedVariant = mapOf("value" to "on")
        Assert.assertEquals(
            Variant(key = "on", value = "on"),
            transformVariantFromStorage(storedVariant)
        )
    }

    @Test
    fun `v1 variant transformation with payload`() {
        val storedVariant = mapOf(
            "value" to "on",
            "payload" to mapOf("k" to "v")
        )
        Assert.assertEquals(
            Variant(key = "on", value = "on", payload = mapOf("k" to "v")),
            transformVariantFromStorage(storedVariant)
        )
    }

    @Test
    fun `v1 variant transformation with payload and experiment key`() {
        val storedVariant = mapOf(
            "value" to "on",
            "payload" to mapOf("k" to "v"),
            "expKey" to "exp-1"
        )
        Assert.assertEquals(
            Variant(
                key = "on",
                value = "on",
                payload = mapOf("k" to "v"),
                expKey = "exp-1",
                metadata = mapOf("experimentKey" to "exp-1")
            ),
            transformVariantFromStorage(storedVariant)
        )
    }

    @Test
    fun `v2 variant transformation`() {
        val storedVariant = mapOf(
            "key" to "treatment",
            "value" to "on"
        )
        Assert.assertEquals(
            Variant(key = "treatment", value = "on"),
            transformVariantFromStorage(storedVariant)
        )
    }

    @Test
    fun `v2 variant transformation with payload`() {
        val storedVariant = mapOf(
            "key" to "treatment",
            "value" to "on",
            "payload" to mapOf("k" to "v")
        )
        Assert.assertEquals(
            Variant(
                key = "treatment",
                value = "on",
                payload = mapOf("k" to "v")
            ),
            transformVariantFromStorage(storedVariant)
        )
    }

    @Test
    fun `v2 variant transformation with payload and experiment key`() {
        val storedVariant = mapOf(
            "key" to "treatment",
            "value" to "on",
            "payload" to mapOf("k" to "v"),
            "expKey" to "exp-1"
        )
        Assert.assertEquals(
            Variant(
                key = "treatment",
                value = "on",
                payload = mapOf("k" to "v"),
                expKey = "exp-1",
                metadata = mapOf("experimentKey" to "exp-1")
            ),
            transformVariantFromStorage(storedVariant)
        )
    }

    @Test
    fun `v2 variant transformation with payload and experiment key metadata`() {
        val storedVariant = mapOf(
            "key" to "treatment",
            "value" to "on",
            "payload" to mapOf("k" to "v"),
            "metadata" to mapOf("experimentKey" to "exp-1")
        )
        Assert.assertEquals(
            Variant(
                key = "treatment",
                value = "on",
                payload = mapOf("k" to "v"),
                expKey = "exp-1",
                metadata = mapOf("experimentKey" to "exp-1")
            ),
            transformVariantFromStorage(storedVariant)
        )
    }
}
