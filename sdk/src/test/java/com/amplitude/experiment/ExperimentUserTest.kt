package com.amplitude.experiment

import com.amplitude.experiment.ExperimentUser.Companion.builder
import com.amplitude.experiment.util.Logger
import com.amplitude.experiment.util.SystemLogger
import com.amplitude.experiment.util.merge
import com.amplitude.experiment.util.toJson
import org.json.JSONArray
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
            .groups(mapOf("groupType" to setOf("groupName")))
            .groupProperties(mapOf("groupType" to mapOf("groupName" to mapOf("k" to "v"))))
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
        expected.put("groups", JSONObject().apply {
            put("groupType", JSONArray().apply { put("groupName") })
        })
        expected.put("group_properties", JSONObject().apply {
            put("groupType", JSONObject().apply {
                put("groupName", JSONObject().apply {
                    put("k", "v")
                })
            })
        })
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
            .groups(mapOf(
                "gt2" to setOf("gn2"),
                "gt3" to setOf("gn3"),
                "gt4" to setOf("gn4"),
            ))
            .groupProperties(mapOf(
                "gt1" to mapOf(
                    "gn1" to mapOf(
                        "gp1" to "v2",
                        "gp3" to "v1"
                    ),
                    "gn3" to mapOf(
                        "gp1" to "v1",
                    ),
                ),
            ))
            .build()

        val user2 = builder()
            .country("newCountry")
            .version("newVersion")
            .userProperty("userPropertyKey2", "value2")
            .userProperty("userPropertyKey", "value2")
            .groups(mapOf(
                "gt1" to setOf("gn1"),
                "gt2" to setOf("difference"),
                "gt4" to setOf("gn4"),
            ))
            .groupProperties(mapOf(
                "gt1" to mapOf(
                    "gn1" to mapOf(
                        "gp1" to "v1",
                        "gp2" to "v1"
                    ),
                    "gn2" to mapOf(
                        "gp1" to "v1",
                    ),
                ),
                "gt2" to mapOf(
                    "gn1" to mapOf(
                        "gp1" to "v1",
                    ),
                ),
            ))
            .build()

        val user = user2.merge(user1)

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
            .userProperty("userPropertyKey", "value2")
            .userProperty("userPropertyKey2", "value2")
            .groups(mapOf(
                "gt1" to setOf("gn1"),
                "gt2" to setOf("difference"),
                "gt3" to setOf("gn3"),
                "gt4" to setOf("gn4"),
            ))
            .groupProperties(mapOf(
                "gt1" to mapOf(
                    "gn1" to mapOf(
                        "gp1" to "v1",
                        "gp2" to "v1",
                        "gp3" to "v1"
                    ),
                    "gn2" to mapOf(
                        "gp1" to "v1",
                    ),
                    "gn3" to mapOf(
                        "gp1" to "v1",
                    ),
                ),
                "gt2" to mapOf(
                    "gn1" to mapOf(
                        "gp1" to "v1",
                    ),
                ),
            ))

            .build()

        Assert.assertEquals(expected, user)

        val user3 = user1.merge(null)
        Assert.assertEquals(user1, user3)
    }

    @Test
    fun `test group and group property builder`() {
        val user = builder().apply {
            group("gt1", "gn1")
            group("gt2", "gn2")
            groupProperty("gt1", "gn1", "k", "v")
            groupProperty("gt1", "gn1", "k2", "v2")
            groupProperty("gt1", "gn2", "k", "v")
            groupProperty("gt2", "gn1", "k", "v")
        }.build()

        val expected = builder().apply {
            groups(mapOf("gt1" to setOf("gn1"), "gt2" to setOf("gn2")))
            groupProperties(mapOf(
                "gt1" to mapOf(
                    "gn1" to mapOf(
                        "k" to "v",
                        "k2" to "v2",
                    ),
                    "gn2" to mapOf(
                        "k" to "v"
                    )
                ),
                "gt2" to mapOf(
                    "gn1" to mapOf(
                        "k" to "v"
                    )
                )
            ))
        }.build()

        Assert.assertEquals(expected, user)
    }
}
