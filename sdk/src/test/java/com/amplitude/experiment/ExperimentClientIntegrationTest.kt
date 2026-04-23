package com.amplitude.experiment

import com.amplitude.experiment.evaluation.EvaluationContext
import com.amplitude.experiment.evaluation.EvaluationEngineImpl
import com.amplitude.experiment.evaluation.EvaluationFlag
import com.amplitude.experiment.util.json
import kotlinx.serialization.decodeFromString
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.TimeUnit

private const val DEPLOYMENT_KEY = "server-VVhLULXCxxY0xqmszXouXxiEzoeJWmSh"
private const val FLAGS_URL = "https://flag.lab.amplitude.com/sdk/v2/flags?eval_mode=remote"

class ExperimentClientIntegrationTest {
    private val engine = EvaluationEngineImpl()

    companion object {
        private lateinit var flags: List<EvaluationFlag>

        @BeforeClass
        @JvmStatic
        fun fetchFlags() {
            val http =
                OkHttpClient
                    .Builder()
                    .callTimeout(20, TimeUnit.SECONDS)
                    .build()
            val request =
                Request
                    .Builder()
                    .url(FLAGS_URL)
                    .addHeader("Authorization", "Api-Key $DEPLOYMENT_KEY")
                    .get()
                    .build()
            http.newCall(request).execute().use { response ->
                require(response.isSuccessful) { "Flags request failed: ${response.code}" }
                val body = response.body?.string() ?: error("Empty flags response body")
                flags = json.decodeFromString(body)
            }
        }
    }

    // Basic Tests

    @Test
    fun `test off`() {
        val user = userContext(userId = "user_id", deviceId = "device_id")
        val result = engine.evaluate(user, flags)["test-off"]
        assertEquals("off", result?.key)
    }

    @Test
    fun `test on`() {
        val user = userContext(userId = "user_id", deviceId = "device_id")
        val result = engine.evaluate(user, flags)["test-on"]
        assertEquals("on", result?.key)
    }

    // Opinionated Segment Tests

    @Test
    fun `test individual inclusions match`() {
        // Match User ID
        var user = userContext(userId = "user_id")
        var result = engine.evaluate(user, flags)["test-individual-inclusions"]
        assertEquals("on", result?.key)
        assertEquals("individual-inclusions", result?.metadata?.get("segmentName"))
        // Match Device ID
        user = userContext(deviceId = "device_id")
        result = engine.evaluate(user, flags)["test-individual-inclusions"]
        assertEquals("on", result?.key)
        assertEquals("individual-inclusions", result?.metadata?.get("segmentName"))
        // Doesn't Match User ID
        user = userContext(userId = "not_user_id")
        result = engine.evaluate(user, flags)["test-individual-inclusions"]
        assertEquals("off", result?.key)
        // Doesn't Match Device ID
        user = userContext(deviceId = "not_device_id")
        result = engine.evaluate(user, flags)["test-individual-inclusions"]
        assertEquals("off", result?.key)
    }

    @Test
    fun `test flag dependencies on`() {
        val user = userContext(userId = "user_id", deviceId = "device_id")
        val result = engine.evaluate(user, flags)["test-flag-dependencies-on"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test flag dependencies off`() {
        val user = userContext(userId = "user_id", deviceId = "device_id")
        val result = engine.evaluate(user, flags)["test-flag-dependencies-off"]
        assertEquals("off", result?.key)
        assertEquals("flag-dependencies", result?.metadata?.get("segmentName"))
    }

    @Test
    fun `test sticky bucketing`() {
        // On
        var user =
            userContext(
                userId = "user_id",
                deviceId = "device_id",
                userProperties = mapOf("[Experiment] test-sticky-bucketing" to "on"),
            )
        var result = engine.evaluate(user, flags)["test-sticky-bucketing"]
        assertEquals("on", result?.key)
        assertEquals("sticky-bucketing", result?.metadata?.get("segmentName"))
        // Off
        user =
            userContext(
                userId = "user_id",
                deviceId = "device_id",
                userProperties = mapOf("[Experiment] test-sticky-bucketing" to "off"),
            )
        result = engine.evaluate(user, flags)["test-sticky-bucketing"]
        assertEquals("off", result?.key)
        assertEquals("All Other Users", result?.metadata?.get("segmentName"))
        // Non-variant
        user =
            userContext(
                userId = "user_id",
                deviceId = "device_id",
                userProperties = mapOf("[Experiment] test-sticky-bucketing" to "not-a-variant"),
            )
        result = engine.evaluate(user, flags)["test-sticky-bucketing"]
        assertEquals("off", result?.key)
    }

    // Experiment and Flag Segment Tests

    @Test
    fun `test experiment`() {
        val user = userContext(userId = "user_id", deviceId = "device_id")
        val result = engine.evaluate(user, flags)["test-experiment"]
        assertEquals("on", result?.key)
        assertEquals("exp-1", result?.metadata?.get("experimentKey"))
    }

    @Test
    fun `test flag`() {
        val user = userContext(userId = "user_id", deviceId = "device_id")
        val result = engine.evaluate(user, flags)["test-flag"]
        assertEquals("on", result?.key)
        assertEquals(null, result?.metadata?.get("experimentKey"))
    }

    // Conditional Logic Tests

    @Test
    fun `test multiple conditions and values`() {
        // All match, on
        var user =
            userContext(
                userProperties =
                    mapOf(
                        "key-1" to "value-1",
                        "key-2" to "value-2",
                        "key-3" to "value-3",
                    ),
            )
        var result = engine.evaluate(user, flags)["test-multiple-conditions-and-values"]
        assertEquals("on", result?.key)
        // Some match, off
        user =
            userContext(
                userProperties =
                    mapOf(
                        "key-1" to "value-1",
                        "key-2" to "value-2",
                    ),
            )
        result = engine.evaluate(user, flags)["test-multiple-conditions-and-values"]
        assertEquals("off", result?.key)
    }

    // Condition Property Targeting Tests

    @Test
    fun `test amplitude property targeting`() {
        val user = userContext(userId = "user_id")
        val result = engine.evaluate(user, flags)["test-amplitude-property-targeting"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test cohort targeting`() {
        // User in cohort
        var user = userContext(cohortIds = setOf("u0qtvwla", "12345678"))
        var result = engine.evaluate(user, flags)["test-cohort-targeting"]
        assertEquals("on", result?.key)
        // User not in cohort
        user = userContext(cohortIds = setOf("12345678", "87654321"))
        result = engine.evaluate(user, flags)["test-cohort-targeting"]
        assertEquals("off", result?.key)
    }

    @Test
    fun `test group name targeting`() {
        val user = groupContext("org name", "amplitude")
        val result = engine.evaluate(user, flags)["test-group-name-targeting"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test group property targeting`() {
        val user = groupContext("org name", "amplitude", mapOf("org plan" to "enterprise2"))
        val result = engine.evaluate(user, flags)["test-group-property-targeting"]
        assertEquals("on", result?.key)
    }

    // Bucketing Tests

    @Test
    fun `test amplitude id bucketing`() {
        val user = userContext(amplitudeId = "1234567890")
        val result = engine.evaluate(user, flags)["test-amplitude-id-bucketing"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test user id bucketing`() {
        val user = userContext(userId = "user_id")
        val result = engine.evaluate(user, flags)["test-user-id-bucketing"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test device id bucketing`() {
        val user = userContext(deviceId = "device_id")
        val result = engine.evaluate(user, flags)["test-device-id-bucketing"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test custom user property bucketing`() {
        val user = userContext(userProperties = mapOf("key" to "value"))
        val result = engine.evaluate(user, flags)["test-custom-user-property-bucketing"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test group name bucketing`() {
        val user = groupContext("org name", "amplitude")
        val result = engine.evaluate(user, flags)["test-group-name-bucketing"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test group property bucketing`() {
        val user = groupContext("org name", "amplitude", mapOf("org plan" to "enterprise2"))
        val result = engine.evaluate(user, flags)["test-group-property-bucketing"]
        assertEquals("on", result?.key)
    }

    // Bucketing Allocation Tests

    @Test
    fun `test 1 percent allocation`() {
        var on = 0
        repeat(10000) { i ->
            val user = userContext(deviceId = "${i + 1}")
            val result = engine.evaluate(user, flags)["test-1-percent-allocation"]
            if (result?.key == "on") on++
        }
        assertEquals(107, on)
    }

    @Test
    fun `test 50 percent allocation`() {
        var on = 0
        repeat(10000) { i ->
            val user = userContext(deviceId = "${i + 1}")
            val result = engine.evaluate(user, flags)["test-50-percent-allocation"]
            if (result?.key == "on") on++
        }
        assertEquals(5009, on)
    }

    @Test
    fun `test 99 percent allocation`() {
        var on = 0
        repeat(10000) { i ->
            val user = userContext(deviceId = "${i + 1}")
            val result = engine.evaluate(user, flags)["test-99-percent-allocation"]
            if (result?.key == "on") on++
        }
        assertEquals(9900, on)
    }

    // Bucketing Distribution Tests

    @Test
    fun `test 1 percent distribution`() {
        var control = 0
        var treatment = 0
        repeat(10000) { i ->
            val user = userContext(deviceId = "${i + 1}")
            val result = engine.evaluate(user, flags)["test-1-percent-distribution"]
            when (result?.key) {
                "control" -> control++
                "treatment" -> treatment++
                else -> throw RuntimeException("Unexpected variant ${result?.key}")
            }
        }
        assertEquals(106, control)
        assertEquals(9894, treatment)
    }

    @Test
    fun `test 50 percent distribution`() {
        var control = 0
        var treatment = 0
        repeat(10000) { i ->
            val user = userContext(deviceId = "${i + 1}")
            val result = engine.evaluate(user, flags)["test-50-percent-distribution"]
            when (result?.key) {
                "control" -> control++
                "treatment" -> treatment++
                else -> throw RuntimeException("Unexpected variant ${result?.key}")
            }
        }
        assertEquals(4990, control)
        assertEquals(5010, treatment)
    }

    @Test
    fun `test 99 percent distribution`() {
        var control = 0
        var treatment = 0
        repeat(10000) { i ->
            val user = userContext(deviceId = "${i + 1}")
            val result = engine.evaluate(user, flags)["test-99-percent-distribution"]
            when (result?.key) {
                "control" -> control++
                "treatment" -> treatment++
                else -> throw RuntimeException("Unexpected variant ${result?.key}")
            }
        }
        assertEquals(9909, control)
        assertEquals(91, treatment)
    }

    @Test
    fun `test multiple distributions`() {
        var a = 0
        var b = 0
        var c = 0
        var d = 0
        repeat(10000) { i ->
            val user = userContext(deviceId = "${i + 1}")
            val result = engine.evaluate(user, flags)["test-multiple-distributions"]
            when (result?.key) {
                "a" -> a++
                "b" -> b++
                "c" -> c++
                "d" -> d++
                else -> throw RuntimeException("Unexpected variant ${result?.key}")
            }
        }
        assertEquals(2444, a)
        assertEquals(2634, b)
        assertEquals(2447, c)
        assertEquals(2475, d)
    }

    // Operator Tests

    @Test
    fun `test is`() {
        val user = userContext(userProperties = mapOf("key" to "value"))
        val result = engine.evaluate(user, flags)["test-is"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test is not`() {
        val user = userContext(userProperties = mapOf("key" to "value"))
        val result = engine.evaluate(user, flags)["test-is-not"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test contains`() {
        val user = userContext(userProperties = mapOf("key" to "value"))
        val result = engine.evaluate(user, flags)["test-contains"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test does not contain`() {
        val user = userContext(userProperties = mapOf("key" to "value"))
        val result = engine.evaluate(user, flags)["test-does-not-contain"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test less`() {
        val user = userContext(userProperties = mapOf("key" to "-1"))
        val result = engine.evaluate(user, flags)["test-less"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test less or equal`() {
        val user = userContext(userProperties = mapOf("key" to "0"))
        val result = engine.evaluate(user, flags)["test-less-or-equal"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test greater`() {
        val user = userContext(userProperties = mapOf("key" to "1"))
        val result = engine.evaluate(user, flags)["test-greater"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test greater or equal`() {
        val user = userContext(userProperties = mapOf("key" to "0"))
        val result = engine.evaluate(user, flags)["test-greater-or-equal"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test version less`() {
        val user = freeformUserContext(mapOf("version" to "1.9.0"))
        val result = engine.evaluate(user, flags.filter { it.key == "test-version-less" })["test-version-less"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test version less or equal`() {
        val user = freeformUserContext(mapOf("version" to "1.10.0"))
        val result = engine.evaluate(user, flags)["test-version-less-or-equal"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test version greater`() {
        val user = freeformUserContext(mapOf("version" to "1.10.0"))
        val result = engine.evaluate(user, flags)["test-version-greater"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test version greater or equal`() {
        val user = freeformUserContext(mapOf("version" to "1.9.0"))
        val result = engine.evaluate(user, flags)["test-version-greater-or-equal"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test set is`() {
        val user = userContext(userProperties = mapOf("key" to listOf("1", "2", "3")))
        val result = engine.evaluate(user, flags)["test-set-is"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test set is json array`() {
        val user = userContext(userProperties = mapOf("key" to """["1", "2", "3"]"""))
        val result = engine.evaluate(user, flags)["test-set-is"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test set is not`() {
        val user = userContext(userProperties = mapOf("key" to listOf("1", "2")))
        val result = engine.evaluate(user, flags)["test-set-is-not"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test set contains`() {
        val user = userContext(userProperties = mapOf("key" to listOf("1", "2", "3", "4")))
        val result = engine.evaluate(user, flags)["test-set-contains"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test set does not contain`() {
        val user = userContext(userProperties = mapOf("key" to listOf("1", "2", "4")))
        val result = engine.evaluate(user, flags)["test-set-does-not-contain"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test set contains any`() {
        val user = userContext(cohortIds = setOf("u0qtvwla", "12345678"))
        val result = engine.evaluate(user, flags)["test-set-contains-any"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test set does not contain any`() {
        val user = userContext(cohortIds = setOf("12345678", "87654321"))
        val result = engine.evaluate(user, flags)["test-set-does-not-contain-any"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test glob match`() {
        val user = userContext(userProperties = mapOf("key" to "/path/1/2/3/end"))
        val result = engine.evaluate(user, flags)["test-glob-match"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test glob does not match`() {
        val user = userContext(userProperties = mapOf("key" to "/path/1/2/3"))
        val result = engine.evaluate(user, flags)["test-glob-does-not-match"]
        assertEquals("on", result?.key)
    }

    // Test specific functionality

    @Test
    fun `test is with booleans`() {
        var user = userContext(userProperties = mapOf("true" to "TRUE", "false" to "FALSE"))
        var result = engine.evaluate(user, flags)["test-is-with-booleans"]
        assertEquals("on", result?.key)
        user = userContext(userProperties = mapOf("true" to "True", "false" to "False"))
        result = engine.evaluate(user, flags)["test-is-with-booleans"]
        assertEquals("on", result?.key)
        user = userContext(userProperties = mapOf("true" to "true", "false" to "false"))
        result = engine.evaluate(user, flags)["test-is-with-booleans"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test version compare falls back on string comparison`() {
        val user = freeformUserContext(mapOf("version" to "1.10."))
        val result = engine.evaluate(user, flags.filter { it.key == "test-version-less" })["test-version-less"]
        assertEquals("on", result?.key)
    }

    // Multi-value / array property support tests (SKY-9582 fix)

    @Test
    fun `test is with array values`() {
        val user = userContext(userProperties = mapOf("key" to listOf("value1", "value2")))
        val result = engine.evaluate(user, flags)["test-is-array"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test is not with array values`() {
        val user = userContext(userProperties = mapOf("key" to listOf("value3", "value4")))
        val result = engine.evaluate(user, flags)["test-is-not-array"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test contains with array values`() {
        val user = userContext(userProperties = mapOf("key" to listOf("has-target-value", "has", "value")))
        val result = engine.evaluate(user, flags)["test-contains-array"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test does not contain with array values`() {
        val user = userContext(userProperties = mapOf("key" to listOf("has-value", "has", "value")))
        val result = engine.evaluate(user, flags)["test-does-not-contain-array"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test is with json array value`() {
        val user = userContext(userProperties = mapOf("key" to """["value1", "value2"]"""))
        val result = engine.evaluate(user, flags)["test-is-array"]
        assertEquals("on", result?.key)
    }

    @Test
    fun `test does not contain with json array values`() {
        val user = userContext(userProperties = mapOf("key" to """["has-value", "has", "value"]"""))
        val result = engine.evaluate(user, flags)["test-does-not-contain-array"]
        assertEquals("on", result?.key)
    }
}

private fun userContext(
    userId: String? = null,
    deviceId: String? = null,
    amplitudeId: String? = null,
    userProperties: Map<String, Any?>? = null,
    cohortIds: Set<String>? = null,
): EvaluationContext =
    EvaluationContext().apply {
        put(
            "user",
            mutableMapOf<String, Any?>().apply {
                if (userId != null) put("user_id", userId)
                if (deviceId != null) put("device_id", deviceId)
                if (amplitudeId != null) put("amplitude_id", amplitudeId)
                if (userProperties != null) put("user_properties", userProperties)
                if (cohortIds != null) put("cohort_ids", cohortIds)
            },
        )
    }

private fun freeformUserContext(user: Map<String, Any?>): EvaluationContext =
    EvaluationContext().apply {
        put("user", user)
    }

private fun groupContext(
    groupType: String,
    groupName: String,
    groupProperties: Map<String, Any?>? = null,
): EvaluationContext =
    EvaluationContext().apply {
        put(
            "groups",
            mutableMapOf<String, Any?>().apply {
                put(
                    groupType,
                    mutableMapOf<String, Any?>().apply {
                        put("group_name", groupName)
                        if (groupProperties != null) put("group_properties", groupProperties)
                    },
                )
            },
        )
    }
