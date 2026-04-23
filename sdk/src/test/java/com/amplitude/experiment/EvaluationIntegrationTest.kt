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

class EvaluationIntegrationTest {
    private val engine = EvaluationEngineImpl()

    companion object {
        private lateinit var flags: List<EvaluationFlag>

        @BeforeClass
        @JvmStatic
        fun fetchFlags() {
            try {
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
                    check(response.isSuccessful) { "Flags request failed: ${response.code}" }
                    val body = response.body?.string() ?: error("Empty flags response body")
                    flags = json.decodeFromString(body)
                }
            } catch (e: Exception) {
                throw IllegalStateException(
                    "Failed to fetch integration-test flags from $FLAGS_URL. " +
                        "Check network connectivity and that the deployment key is still active.",
                    e,
                )
            }
        }
    }

    private fun assertVariant(
        flagKey: String,
        user: EvaluationContext,
        expectedVariant: String?,
        expectedSegmentName: String? = null,
        flagsFilter: ((EvaluationFlag) -> Boolean)? = null,
    ) {
        val scope = if (flagsFilter == null) flags else flags.filter(flagsFilter)
        val result = engine.evaluate(user, scope)[flagKey]
        assertEquals(expectedVariant, result?.key)
        if (expectedSegmentName != null) {
            assertEquals(expectedSegmentName, result?.metadata?.get("segmentName"))
        }
    }

    // Basic Tests

    @Test
    fun `test off`() = assertVariant("test-off", userContext(userId = "user_id", deviceId = "device_id"), "off")

    @Test
    fun `test on`() = assertVariant("test-on", userContext(userId = "user_id", deviceId = "device_id"), "on")

    // Opinionated Segment Tests

    @Test
    fun `test individual inclusions match`() {
        assertVariant("test-individual-inclusions", userContext(userId = "user_id"), "on", "individual-inclusions")
        assertVariant("test-individual-inclusions", userContext(deviceId = "device_id"), "on", "individual-inclusions")
        assertVariant("test-individual-inclusions", userContext(userId = "not_user_id"), "off")
        assertVariant("test-individual-inclusions", userContext(deviceId = "not_device_id"), "off")
    }

    @Test
    fun `test flag dependencies on`() =
        assertVariant(
            "test-flag-dependencies-on",
            userContext(userId = "user_id", deviceId = "device_id"),
            "on",
        )

    @Test
    fun `test flag dependencies off`() =
        assertVariant(
            "test-flag-dependencies-off",
            userContext(userId = "user_id", deviceId = "device_id"),
            "off",
            "flag-dependencies",
        )

    @Test
    fun `test sticky bucketing`() {
        fun ctx(overrideVariant: String) =
            userContext(
                userId = "user_id",
                deviceId = "device_id",
                userProperties = mapOf("[Experiment] test-sticky-bucketing" to overrideVariant),
            )
        assertVariant("test-sticky-bucketing", ctx("on"), "on", "sticky-bucketing")
        assertVariant("test-sticky-bucketing", ctx("off"), "off", "All Other Users")
        assertVariant("test-sticky-bucketing", ctx("not-a-variant"), "off")
    }

    // Experiment and Flag Segment Tests

    @Test
    fun `test experiment`() {
        val result = engine.evaluate(userContext(userId = "user_id", deviceId = "device_id"), flags)["test-experiment"]
        assertEquals("on", result?.key)
        assertEquals("exp-1", result?.metadata?.get("experimentKey"))
    }

    @Test
    fun `test flag`() {
        val result = engine.evaluate(userContext(userId = "user_id", deviceId = "device_id"), flags)["test-flag"]
        assertEquals("on", result?.key)
        assertEquals(null, result?.metadata?.get("experimentKey"))
    }

    // Conditional Logic Tests

    @Test
    fun `test multiple conditions and values`() {
        assertVariant(
            "test-multiple-conditions-and-values",
            userContext(
                userProperties =
                    mapOf(
                        "key-1" to "value-1",
                        "key-2" to "value-2",
                        "key-3" to "value-3",
                    ),
            ),
            "on",
        )
        assertVariant(
            "test-multiple-conditions-and-values",
            userContext(
                userProperties =
                    mapOf(
                        "key-1" to "value-1",
                        "key-2" to "value-2",
                    ),
            ),
            "off",
        )
    }

    // Condition Property Targeting Tests

    @Test
    fun `test amplitude property targeting`() = assertVariant("test-amplitude-property-targeting", userContext(userId = "user_id"), "on")

    @Test
    fun `test cohort targeting`() {
        assertVariant("test-cohort-targeting", userContext(cohortIds = setOf("u0qtvwla", "12345678")), "on")
        assertVariant("test-cohort-targeting", userContext(cohortIds = setOf("12345678", "87654321")), "off")
    }

    @Test
    fun `test group name targeting`() = assertVariant("test-group-name-targeting", groupContext("org name", "amplitude"), "on")

    @Test
    fun `test group property targeting`() =
        assertVariant(
            "test-group-property-targeting",
            groupContext("org name", "amplitude", mapOf("org plan" to "enterprise2")),
            "on",
        )

    // Bucketing Tests

    @Test
    fun `test amplitude id bucketing`() = assertVariant("test-amplitude-id-bucketing", userContext(amplitudeId = "1234567890"), "on")

    @Test
    fun `test user id bucketing`() = assertVariant("test-user-id-bucketing", userContext(userId = "user_id"), "on")

    @Test
    fun `test device id bucketing`() = assertVariant("test-device-id-bucketing", userContext(deviceId = "device_id"), "on")

    @Test
    fun `test custom user property bucketing`() =
        assertVariant("test-custom-user-property-bucketing", userContext(userProperties = mapOf("key" to "value")), "on")

    @Test
    fun `test group name bucketing`() = assertVariant("test-group-name-bucketing", groupContext("org name", "amplitude"), "on")

    @Test
    fun `test group property bucketing`() =
        assertVariant(
            "test-group-property-bucketing",
            groupContext("org name", "amplitude", mapOf("org plan" to "enterprise2")),
            "on",
        )

    // Bucketing Allocation / Distribution Tests
    //
    // The exact counts below are a function of each flag's {salt, flagVersion, allocation config}
    // on the `server-VVhLULXCxxY0xqmszXouXxiEzoeJWmSh` deployment. If the deployment is re-baked
    // the numbers drift — update the constants here in lockstep with the sibling
    // experiment-jvm-server / experiment-ios-client integration suites, which all assert the same
    // reference counts to guarantee cross-SDK bucket parity.

    @Test
    fun `test 1 percent allocation`() {
        var on = 0
        repeat(10000) { i ->
            if (engine.evaluate(userContext(deviceId = "${i + 1}"), flags)["test-1-percent-allocation"]?.key == "on") on++
        }
        assertEquals(107, on)
    }

    @Test
    fun `test 50 percent allocation`() {
        var on = 0
        repeat(10000) { i ->
            if (engine.evaluate(userContext(deviceId = "${i + 1}"), flags)["test-50-percent-allocation"]?.key == "on") on++
        }
        assertEquals(5009, on)
    }

    @Test
    fun `test 99 percent allocation`() {
        var on = 0
        repeat(10000) { i ->
            if (engine.evaluate(userContext(deviceId = "${i + 1}"), flags)["test-99-percent-allocation"]?.key == "on") on++
        }
        assertEquals(9900, on)
    }

    @Test
    fun `test 1 percent distribution`() {
        val (control, treatment) = countDistribution("test-1-percent-distribution")
        assertEquals(106, control)
        assertEquals(9894, treatment)
    }

    @Test
    fun `test 50 percent distribution`() {
        val (control, treatment) = countDistribution("test-50-percent-distribution")
        assertEquals(4990, control)
        assertEquals(5010, treatment)
    }

    @Test
    fun `test 99 percent distribution`() {
        val (control, treatment) = countDistribution("test-99-percent-distribution")
        assertEquals(9909, control)
        assertEquals(91, treatment)
    }

    @Test
    fun `test multiple distributions`() {
        val counts = mutableMapOf("a" to 0, "b" to 0, "c" to 0, "d" to 0)
        repeat(10000) { i ->
            val key =
                engine.evaluate(userContext(deviceId = "${i + 1}"), flags)["test-multiple-distributions"]?.key
                    ?: throw RuntimeException("Unexpected null variant")
            counts[key] = (counts[key] ?: throw RuntimeException("Unexpected variant $key")) + 1
        }
        assertEquals(2444, counts["a"])
        assertEquals(2634, counts["b"])
        assertEquals(2447, counts["c"])
        assertEquals(2475, counts["d"])
    }

    private fun countDistribution(flagKey: String): Pair<Int, Int> {
        var control = 0
        var treatment = 0
        repeat(10000) { i ->
            when (engine.evaluate(userContext(deviceId = "${i + 1}"), flags)[flagKey]?.key) {
                "control" -> control++
                "treatment" -> treatment++
                else -> throw RuntimeException("Unexpected variant for $flagKey at i=$i")
            }
        }
        return control to treatment
    }

    // Operator Tests

    @Test
    fun `test is`() = assertVariant("test-is", userContext(userProperties = mapOf("key" to "value")), "on")

    @Test
    fun `test is not`() = assertVariant("test-is-not", userContext(userProperties = mapOf("key" to "value")), "on")

    @Test
    fun `test contains`() = assertVariant("test-contains", userContext(userProperties = mapOf("key" to "value")), "on")

    @Test
    fun `test does not contain`() = assertVariant("test-does-not-contain", userContext(userProperties = mapOf("key" to "value")), "on")

    @Test
    fun `test less`() = assertVariant("test-less", userContext(userProperties = mapOf("key" to "-1")), "on")

    @Test
    fun `test less or equal`() = assertVariant("test-less-or-equal", userContext(userProperties = mapOf("key" to "0")), "on")

    @Test
    fun `test greater`() = assertVariant("test-greater", userContext(userProperties = mapOf("key" to "1")), "on")

    @Test
    fun `test greater or equal`() = assertVariant("test-greater-or-equal", userContext(userProperties = mapOf("key" to "0")), "on")

    @Test
    fun `test version less`() =
        assertVariant(
            "test-version-less",
            freeformUserContext(mapOf("version" to "1.9.0")),
            "on",
            flagsFilter = { it.key == "test-version-less" },
        )

    @Test
    fun `test version less or equal`() =
        assertVariant("test-version-less-or-equal", freeformUserContext(mapOf("version" to "1.10.0")), "on")

    @Test
    fun `test version greater`() = assertVariant("test-version-greater", freeformUserContext(mapOf("version" to "1.10.0")), "on")

    @Test
    fun `test version greater or equal`() =
        assertVariant("test-version-greater-or-equal", freeformUserContext(mapOf("version" to "1.9.0")), "on")

    @Test
    fun `test set is`() = assertVariant("test-set-is", userContext(userProperties = mapOf("key" to listOf("1", "2", "3"))), "on")

    @Test
    fun `test set is json array`() = assertVariant("test-set-is", userContext(userProperties = mapOf("key" to """["1", "2", "3"]""")), "on")

    @Test
    fun `test set is not`() = assertVariant("test-set-is-not", userContext(userProperties = mapOf("key" to listOf("1", "2"))), "on")

    @Test
    fun `test set contains`() =
        assertVariant("test-set-contains", userContext(userProperties = mapOf("key" to listOf("1", "2", "3", "4"))), "on")

    @Test
    fun `test set does not contain`() =
        assertVariant("test-set-does-not-contain", userContext(userProperties = mapOf("key" to listOf("1", "2", "4"))), "on")

    @Test
    fun `test set contains any`() = assertVariant("test-set-contains-any", userContext(cohortIds = setOf("u0qtvwla", "12345678")), "on")

    @Test
    fun `test set does not contain any`() =
        assertVariant("test-set-does-not-contain-any", userContext(cohortIds = setOf("12345678", "87654321")), "on")

    @Test
    fun `test glob match`() = assertVariant("test-glob-match", userContext(userProperties = mapOf("key" to "/path/1/2/3/end")), "on")

    @Test
    fun `test glob does not match`() =
        assertVariant("test-glob-does-not-match", userContext(userProperties = mapOf("key" to "/path/1/2/3")), "on")

    // Test specific functionality

    @Test
    fun `test is with booleans`() {
        assertVariant(
            "test-is-with-booleans",
            userContext(userProperties = mapOf("true" to "TRUE", "false" to "FALSE")),
            "on",
        )
        assertVariant(
            "test-is-with-booleans",
            userContext(userProperties = mapOf("true" to "True", "false" to "False")),
            "on",
        )
        assertVariant(
            "test-is-with-booleans",
            userContext(userProperties = mapOf("true" to "true", "false" to "false")),
            "on",
        )
    }

    @Test
    fun `test version compare falls back on string comparison`() =
        assertVariant(
            "test-version-less",
            freeformUserContext(mapOf("version" to "1.10.")),
            "on",
            flagsFilter = { it.key == "test-version-less" },
        )

    // Multi-value / array property support tests

    @Test
    fun `test is with array values`() =
        assertVariant("test-is-array", userContext(userProperties = mapOf("key" to listOf("value1", "value2"))), "on")

    @Test
    fun `test is not with array values`() =
        assertVariant("test-is-not-array", userContext(userProperties = mapOf("key" to listOf("value3", "value4"))), "on")

    @Test
    fun `test contains with array values`() =
        assertVariant(
            "test-contains-array",
            userContext(userProperties = mapOf("key" to listOf("has-target-value", "has", "value"))),
            "on",
        )

    @Test
    fun `test does not contain with array values`() =
        assertVariant(
            "test-does-not-contain-array",
            userContext(userProperties = mapOf("key" to listOf("has-value", "has", "value"))),
            "on",
        )

    @Test
    fun `test is with json array value`() =
        assertVariant(
            "test-is-array",
            userContext(userProperties = mapOf("key" to """["value1", "value2"]""")),
            "on",
        )

    @Test
    fun `test does not contain with json array values`() =
        assertVariant(
            "test-does-not-contain-array",
            userContext(userProperties = mapOf("key" to """["has-value", "has", "value"]""")),
            "on",
        )
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
