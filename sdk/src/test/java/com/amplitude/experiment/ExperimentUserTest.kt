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
            .userId("userId")
            .deviceId("deviceId")
            .country("country")
            .city("city")
            .region("region")
            .dma("dma")
            .language("language")
            .platform("platform")
            .version("version")
            .os("os")
            .library("library")
            .deviceBrand("deviceBrand")
            .deviceManufacturer("deviceManufacturer")
            .deviceModel("deviceModel")
            .carrier("carrier")
            .userProperty("userPropertyKey", "value")
            .build()

        // Ordering matters here, based on toJson() extension function
        val expected = JSONObject()
        expected.put("user_id", "userId")
        expected.put("device_id", "deviceId")
        expected.put("country", "country")
        expected.put("city", "city")
        expected.put("region", "region")
        expected.put("dma", "dma")
        expected.put("language", "language")
        expected.put("platform", "platform")
        expected.put("version", "version")
        expected.put("os", "os")
        expected.put("device_brand", "deviceBrand")
        expected.put("device_manufacturer", "deviceManufacturer")
        expected.put("device_model", "deviceModel")
        expected.put("carrier", "carrier")
        expected.put("library", "library")
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
            .city("city")
            .region("region")
            .dma("dma")
            .language("language")
            .platform("platform")
            .os("os")
            .library("library")
            .deviceBrand("deviceBrand")
            .deviceManufacturer("deviceManufacturer")
            .deviceModel("deviceModel")
            .carrier("carrier")
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
            .language("language")
            .city("city")
            .region("region")
            .dma("dma")
            .language("language")
            .platform("platform")
            .os("os")
            .library("library")
            .deviceBrand("deviceBrand")
            .deviceManufacturer("deviceManufacturer")
            .deviceModel("deviceModel")
            .carrier("carrier")
            .userProperty("userPropertyKey2", "value2")
            .build()

        Assert.assertEquals(expected, user)

        val user3 = user1.merge(null)
        Assert.assertEquals(user1, user3)
    }
}
