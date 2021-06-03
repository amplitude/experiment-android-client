package com.amplitude.experiment

import com.amplitude.experiment.storage.InMemoryStorage
import com.amplitude.experiment.util.Logger
import com.amplitude.experiment.util.SystemLogger
import okhttp3.OkHttpClient
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.ExecutionException

private const val API_KEY = "client-DvWljIjiiuqLbyjqdvBaLFfEBrAvGuA3"
private const val KEY = "sdk-ci-test"
private const val ON_VARIANT_VALUE = "on"
private const val ON_VARIANT_PAYLOAD = "payload"

class ExperimentClientTest {

    init {
        Logger.implementation = SystemLogger(true)
    }

    private val testUser = ExperimentUser(userId = "test_user")

    private val fallbackVariant = Variant("fallback", "payload")

    private val initialVariants = mapOf(
        "initial1" to Variant("initial1", mapOf("abc" to "cdf")),
        "initial2" to Variant("initial2"),
        KEY to Variant("off"),
    )

    private val client = DefaultExperimentClient(
        API_KEY,
        ExperimentConfig(
            debug = true,
            fallbackVariant = fallbackVariant,
            initialVariants = initialVariants,
        ),
        OkHttpClient(),
        InMemoryStorage(),
        Experiment.executorService,
    )

    private val timeoutClient = DefaultExperimentClient(
        API_KEY,
        ExperimentConfig(debug = true, fetchTimeoutMillis = 1),
        OkHttpClient(),
        InMemoryStorage(),
        Experiment.executorService,
    )

    private val initialVariantSourceClient = DefaultExperimentClient(
        API_KEY,
        ExperimentConfig(
            debug = true,
            source = Source.INITIAL_VARIANTS,
            initialVariants = initialVariants
        ),
        OkHttpClient(),
        InMemoryStorage(),
        Experiment.executorService,
    )

    @Test
    fun `fetch success`() {
        client.fetch(testUser).get()
        val variant = client.variant(KEY)
        Assert.assertNotNull(variant)
        Assert.assertEquals(ON_VARIANT_VALUE, variant.value)
        Assert.assertEquals(ON_VARIANT_PAYLOAD, variant.payload)
    }

    @Test
    fun `fetch timeout`() {
        try {
            timeoutClient.fetch(testUser).get()
        } catch (e: ExecutionException) {
            // Timeout is expected
            val variant = client.variant(KEY)
            Assert.assertEquals("off", variant.value)
            return
        }
        Assert.fail("expected timeout exception")
    }

    @Test
    fun `fallback variant returned in correct order`() {
        val firstFallback = Variant("first")
        var variant = client.variant("asdf", firstFallback)
        Assert.assertEquals(firstFallback, variant)

        variant = client.variant("asdf")
        Assert.assertEquals(fallbackVariant, variant)
    }

    @Test
    fun `initial variants returned`() {
        val variants = client.all()
        Assert.assertEquals(initialVariants, variants)
    }

    @Test
    fun `initial variants source overrides fetch`() {
        var variant = initialVariantSourceClient.variant(KEY)
        Assert.assertNotNull(variant)
        initialVariantSourceClient.fetch(testUser).get()
        variant = initialVariantSourceClient.variant(KEY)
        Assert.assertNotNull(variant)
        Assert.assertEquals("off", variant.value)
        Assert.assertNull(variant.payload)
    }

    @Test
    fun `test fetch sets user and setUser overwrites`() {
        client.fetch(testUser)
        Assert.assertEquals(testUser, client.getUser())
        val newUser = testUser.copyToBuilder().userId("different_user").build()
        client.setUser(newUser)
        Assert.assertEquals(newUser, client.getUser())
    }
}
