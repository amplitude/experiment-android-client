package com.amplitude.experiment

import com.amplitude.experiment.ExperimentUser.Companion.builder
import com.amplitude.experiment.util.Logger
import com.amplitude.experiment.util.SystemLogger
import com.amplitude.experiment.util.merge
import com.amplitude.experiment.util.toJson
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class ExperimentUserTest {

    init {
        Logger.implementation = SystemLogger(true)
    }

    @Test
    fun `user to json`() {
        val user = builder()
            .userId("user_id")
            .deviceId("device_id")
            .country("country")
            .version(null)
            .userProperty("userPropertyKey", "value")
            .build()

        val expected = JSONObject()
        expected.put("user_id", "user_id")
        expected.put("device_id", "device_id")
        expected.put("country", "country")
        val expectedUserProperties = JSONObject()
        expectedUserProperties.put("userPropertyKey", "value")
        expected.put("user_properties", expectedUserProperties)

        Assert.assertEquals(expected.toString(), user.toJson())
    }

    @Test
    fun `user merge`() {
        val user1 = builder()
            .userId("user_id")
            .deviceId("device_id")
            .country("country")
            .city("test")
            .region("test")
            .dma("test")
            .language("test")
            .platform("test")
            .os("test")
            .library("test")
            .deviceBrand("test")
            .deviceManufacturer("test")
            .deviceModel("test")
            .carrier("test")
            .userProperty("userPropertyKey", "value")
            .build()

        val user2 = builder()
            .country("newCountry")
            .version("newVersion")
            .userProperty("userPropertyKey2", "value2")
            .build()

        val user = user2.merge(user1, false)

        val expected = builder()
            .userId("user_id")
            .deviceId("device_id")
            .country("newCountry") // overwrites value
            .version("newVersion") // overwrites null
            .language("test")
            .city("test")
            .region("test")
            .dma("test")
            .language("test")
            .platform("test")
            .os("test")
            .library("test")
            .deviceBrand("test")
            .deviceManufacturer("test")
            .deviceModel("test")
            .carrier("test")
            .userProperty("userPropertyKey2", "value2")
            .build()

        Assert.assertEquals(expected, user)
    }
}