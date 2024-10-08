package com.amplitude.experiment

import com.amplitude.experiment.util.Logger
import com.amplitude.experiment.util.SystemLogger
import com.amplitude.experiment.util.toJson
import com.amplitude.experiment.util.toVariant
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class VariantTest {
    init {
        Logger.implementation = SystemLogger(true)
    }

    @Test
    fun `empty json object to variant`() {
        val jsonObject = JSONObject()
        val variant = jsonObject.toVariant()
        Assert.assertNull(variant)
    }

    @Test
    fun `json object to variant`() {
        val jsonObject = JSONObject()
        jsonObject.put("key", "key")
        jsonObject.put("value", "value")
        jsonObject.put("payload", "payload")
        jsonObject.put("expKey", "expKey")
        jsonObject.put("metadata", emptyMap<Any, Any>())
        val variant = jsonObject.toVariant()
        Assert.assertNotNull(variant)
        Assert.assertEquals("key", variant!!.key)
        Assert.assertEquals("value", variant.value)
        Assert.assertEquals("payload", variant.payload)
        Assert.assertEquals("expKey", variant.expKey)
        Assert.assertEquals(mapOf("experimentKey" to "expKey"), variant.metadata)
    }

    @Test
    fun `variant to json object`() {
        run {
            val variant = Variant("value", null, "expKey", "key", emptyMap())
            val jsonObject = JSONObject()
            jsonObject.put("key", "key")
            jsonObject.put("value", "value")
            jsonObject.put("expKey", "expKey")
            jsonObject.put("metadata", emptyMap<Any, Any>())
            Assert.assertEquals(jsonObject.toString(), variant.toJson())
        }
    }
}
