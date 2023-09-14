package com.amplitude.experiment

import com.amplitude.experiment.analytics.ExperimentAnalyticsEvent
import com.amplitude.experiment.analytics.ExperimentAnalyticsProvider
import com.amplitude.experiment.storage.SharedPrefsStorage
import com.amplitude.experiment.storage.Storage
import com.amplitude.experiment.util.Logger
import com.amplitude.experiment.util.SystemLogger
import okhttp3.OkHttpClient
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.ExecutionException
import org.mockito.Mockito

private const val API_KEY = "client-DvWljIjiiuqLbyjqdvBaLFfEBrAvGuA3"

private const val KEY = "sdk-ci-test"
private const val INITIAL_KEY = "initial-key"

class ExperimentClientTest {

    init {
        Logger.implementation = SystemLogger(true)
    }

    private val mockStorage = Mockito.mock(Storage::class.java)
    private val testUser = ExperimentUser(userId = "test_user")

    private val serverVariant = Variant(key = "on", value = "on", payload = "payload")
    private val fallbackVariant = Variant(key = "fallback", payload = "payload")
    private val initialVariant = Variant(key = "initial")

    private val initialVariants = mapOf(
        INITIAL_KEY to initialVariant,
        KEY to Variant(key = "off"),
    )

    private val client = DefaultExperimentClient(
        API_KEY,
        ExperimentConfig(
            debug = true,
            fallbackVariant = fallbackVariant,
            initialVariants = initialVariants,
        ),
        OkHttpClient(),
        mockStorage,
        Experiment.executorService,
    )

    private val timeoutClient = DefaultExperimentClient(
        API_KEY,
        ExperimentConfig(
            debug = true,
            fallbackVariant = fallbackVariant,
            initialVariants = initialVariants,
            fetchTimeoutMillis = 1,
        ),
        OkHttpClient(),
        mockStorage,
        Experiment.executorService,
    )

    private val initialVariantSourceClient = DefaultExperimentClient(
        API_KEY,
        ExperimentConfig(
            debug = true,
            source = Source.INITIAL_VARIANTS,
            initialVariants = initialVariants,
        ),
        OkHttpClient(),
        mockStorage,
        Experiment.executorService,
    )

    private val generalClient = DefaultExperimentClient(
        API_KEY,
        ExperimentConfig(
            debug = true,
        ),
        OkHttpClient(),
        mockStorage,
        Experiment.executorService,
    )

    @Test
    fun `fetch success`() {
        client.fetch(testUser).get()
        val variant = client.variant(KEY)
        Assert.assertNotNull(variant)
        Assert.assertEquals(serverVariant, variant)
    }

    @Test
    fun `fetch timeout`() {
        try {
            timeoutClient.fetch(testUser).get()
        } catch (e: ExecutionException) {
            // Timeout is expected
            val variant = timeoutClient.variant(KEY)
            Assert.assertEquals("off", variant.key)
            return
        }
        Assert.fail("expected timeout exception")
    }

    @Test
    fun `fetch timeout retry success`() {
        try {
            timeoutClient.fetch(testUser).get()
        } catch (e: ExecutionException) {
            // Timeout is expected
            val offVariant = timeoutClient.variant(KEY)
            Assert.assertEquals("off", offVariant.key)
            // Wait for retry to succeed and check updated variant
            Thread.sleep(1000)
            val variant = timeoutClient.variant(KEY)
            Assert.assertNotNull(variant)
            Assert.assertEquals(serverVariant, variant)
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

        variant = client.variant(INITIAL_KEY, firstFallback)
        Assert.assertEquals(firstFallback, variant)

        variant = client.variant(INITIAL_KEY)
        Assert.assertEquals(initialVariant, variant)

        client.fetch(testUser).get()

        variant = client.variant("asdf", firstFallback)
        Assert.assertEquals(firstFallback, variant)

        variant = client.variant("asdf")
        Assert.assertEquals(fallbackVariant, variant)

        variant = client.variant(INITIAL_KEY, firstFallback)
        Assert.assertEquals(firstFallback, variant)

        variant = client.variant(INITIAL_KEY)
        Assert.assertEquals(initialVariant, variant)

        variant = client.variant(KEY, firstFallback)
        Assert.assertEquals(serverVariant, variant)
    }

    @Test
    fun `initial variants returned`() {
        val variants = client.all()
        Assert.assertEquals(initialVariants, variants)
    }

    @Test
    fun `clear the flag config in storage`() {
        generalClient.fetch(testUser).get()
        val variant = generalClient.variant("sdk-ci-test")
        Assert.assertEquals(Variant(key = "on", value = "on", payload = "payload"), variant)
        generalClient.clear()
        val clearedVariants = generalClient.all()
        Assert.assertEquals(0, clearedVariants.entries.size)
    }

    @Test
    fun `initial variants source overrides fetch`() {
        var variant = initialVariantSourceClient.variant(KEY)
        Assert.assertNotNull(variant)
        initialVariantSourceClient.fetch(testUser).get()
        variant = initialVariantSourceClient.variant(KEY)
        Assert.assertNotNull(variant)
        Assert.assertEquals("off", variant.key)
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

    @Test
    fun `test fetch user with flag`() {
        client.fetch(testUser, FetchOptions(flagKeys = listOf(KEY))).get()
        val variant = client.variant(KEY)
        Assert.assertNotNull(variant)
        Assert.assertEquals(serverVariant, variant)
    }

    @Test
    fun `test fetch user with invalid flags`() {
        val invalidKey = "invalid"
        client.fetch(testUser, FetchOptions(flagKeys = listOf(KEY, INITIAL_KEY, invalidKey))).get()
        val variant = client.variant(KEY)
        Assert.assertNotNull(variant)
        Assert.assertEquals(serverVariant, variant)

        val firstFallback = Variant("first")
        val initialVariant = client.variant(INITIAL_KEY, firstFallback)
        Assert.assertEquals(firstFallback, initialVariant)

        val invalidVariant = client.variant(invalidKey)
        Assert.assertEquals(fallbackVariant, invalidVariant)
    }

    @Test
    fun `test exposure event through analytics provider when variant called`() {
        var didExposureGetTracked = false
        var didUserPropertyGetSet = false
        val analyticsProvider = object : ExperimentAnalyticsProvider {
            override fun track(event: ExperimentAnalyticsEvent) {
                Assert.assertEquals("[Experiment] Exposure", event.name)
                Assert.assertEquals(
                    mapOf(
                        "key" to KEY,
                        "variant" to serverVariant.key,
                        "source" to VariantSource.LOCAL_STORAGE.toString()
                    ),
                    event.properties
                )

                Assert.assertEquals(KEY, event.key)
                Assert.assertEquals(serverVariant, event.variant)
                didExposureGetTracked = true
            }

            override fun setUserProperty(event: ExperimentAnalyticsEvent) {
                Assert.assertEquals("[Experiment] $KEY", event.userProperty)
                Assert.assertEquals(serverVariant, event.variant)
                didUserPropertyGetSet = true
            }

            override fun unsetUserProperty(event: ExperimentAnalyticsEvent) {
                Assert.fail("analytics provider unset() should not be called")
            }
        }
        val analyticsProviderClient = DefaultExperimentClient(
            API_KEY,
            ExperimentConfig(
                debug = true,
                analyticsProvider = analyticsProvider,
            ),
            OkHttpClient(),
            mockStorage,
            Experiment.executorService,
        )
        analyticsProviderClient.fetch(testUser).get()
        analyticsProviderClient.variant(KEY)
        Assert.assertTrue(didExposureGetTracked)
        Assert.assertTrue(didUserPropertyGetSet)
    }

    @Test
    fun `test exposure event not tracked on fallback variant and unset called`() {
        var didExposureGetUnset = false
        val analyticsProvider = object : ExperimentAnalyticsProvider {
            override fun track(event: ExperimentAnalyticsEvent) {
                Assert.fail("analytics provider track() should not be called.")
            }

            override fun setUserProperty(event: ExperimentAnalyticsEvent) {
                Assert.fail("analytics provider setUserProperty() should not be called")
            }

            override fun unsetUserProperty(event: ExperimentAnalyticsEvent) {
                Assert.assertEquals(
                    event.userProperty,
                    "[Experiment] asdf"
                )
                Assert.assertEquals(
                    event.variant,
                    fallbackVariant
                )
                Assert.assertEquals(event.properties.get("source"), "fallback-config")
                didExposureGetUnset = true
            }
        }
        val analyticsProviderClient = DefaultExperimentClient(
            API_KEY,
            ExperimentConfig(
                debug = true,
                fallbackVariant = fallbackVariant,
                initialVariants = initialVariants,
                analyticsProvider = analyticsProvider,
            ),
            OkHttpClient(),
            mockStorage,
            Experiment.executorService,
        )
        analyticsProviderClient.fetch(testUser).get()
        analyticsProviderClient.variant("asdf")
        Assert.assertTrue(didExposureGetUnset)
    }

    @Test
    fun `test exposure event not tracked on secondary variant and unset not called`() {
        var didExposureGetUnset = false
        val analyticsProvider = object : ExperimentAnalyticsProvider {
            override fun track(event: ExperimentAnalyticsEvent) {
                Assert.assertEquals(
                    event.userProperty,
                    "[Experiment] $INITIAL_KEY"
                )
                Assert.assertEquals(
                    event.variant,
                    initialVariants.get(INITIAL_KEY)
                )
                Assert.assertEquals(event.properties.get("source"), "secondary-initial")
                didExposureGetUnset = true
            }

            override fun setUserProperty(event: ExperimentAnalyticsEvent) {
                Assert.fail("analytics provider set() should not be called.")
            }

            override fun unsetUserProperty(event: ExperimentAnalyticsEvent) {
                Assert.assertEquals("[Experiment] $INITIAL_KEY", event.userProperty)
            }
        }
        val analyticsProviderClient = DefaultExperimentClient(
            API_KEY,
            ExperimentConfig(
                debug = true,
                fallbackVariant = fallbackVariant,
                initialVariants = initialVariants,
                analyticsProvider = analyticsProvider,
            ),
            OkHttpClient(),
            mockStorage,
            Experiment.executorService,
        )
        analyticsProviderClient.fetch(testUser).get()
        analyticsProviderClient.variant(INITIAL_KEY)
        Assert.assertFalse(didExposureGetUnset)
    }

    @Test
    fun `test exposure event through analytics provider with user properties`() {
        var didExposureGetTracked = false
        var didUserPropertyGetSet = false
        val analyticsProvider = object : ExperimentAnalyticsProvider {
            override fun track(event: ExperimentAnalyticsEvent) {
                Assert.assertEquals(
                    event.userProperties,
                    mapOf("[Experiment] $KEY" to serverVariant.key)
                )
                didExposureGetTracked = true
            }

            override fun setUserProperty(event: ExperimentAnalyticsEvent) {
                Assert.assertEquals(
                    "[Experiment] $KEY",
                    event.userProperty
                )
                didUserPropertyGetSet = true
            }

            override fun unsetUserProperty(event: ExperimentAnalyticsEvent) {
                Assert.fail("analytics provider unset() should not be called")
            }
        }
        val analyticsProviderClient = DefaultExperimentClient(
            API_KEY,
            ExperimentConfig(
                debug = true,
                analyticsProvider = analyticsProvider,
            ),
            OkHttpClient(),
            mockStorage,
            Experiment.executorService,
        )
        analyticsProviderClient.fetch(testUser).get()
        analyticsProviderClient.variant(KEY)
        Assert.assertTrue(didExposureGetTracked)
        Assert.assertTrue(didUserPropertyGetSet)
    }

    @Test
    fun `test exposure through exposure tracking provider has experiment key from variant`() {
        var didTrack = false
        val exposureTrackingProvider = object : ExposureTrackingProvider {
            override fun track(exposure: Exposure) {
                Assert.assertEquals("flagKey", exposure.flagKey)
                Assert.assertEquals("variant", exposure.variant)
                Assert.assertEquals("experimentKey", exposure.experimentKey)
                didTrack = true
            }
        }
        val client = DefaultExperimentClient(
            API_KEY,
            ExperimentConfig(
                debug = true,
                exposureTrackingProvider = exposureTrackingProvider,
                source = Source.INITIAL_VARIANTS,
                initialVariants = mapOf("flagKey" to Variant(key = "variant", expKey = "experimentKey"))
            ),
            OkHttpClient(),
            mockStorage,
            Experiment.executorService,
        )
        client.variant("flagKey")
        Assert.assertTrue(didTrack)
    }
}
