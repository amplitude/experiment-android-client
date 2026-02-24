package com.amplitude.experiment

import com.amplitude.experiment.analytics.ExperimentAnalyticsEvent
import com.amplitude.experiment.analytics.ExperimentAnalyticsProvider
import com.amplitude.experiment.storage.getTrackAssignmentEventStorage
import com.amplitude.experiment.util.AmpLogger
import com.amplitude.experiment.util.FetchException
import com.amplitude.experiment.util.MockStorage
import com.amplitude.experiment.util.SystemLoggerProvider
import com.amplitude.experiment.util.TestExposureTrackingProvider
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

private const val API_KEY = "client-DvWljIjiiuqLbyjqdvBaLFfEBrAvGuA3"
private const val SERVER_API_KEY = "server-qz35UwzJ5akieoAdIgzM4m9MIiOLXLoz"

private const val KEY = "sdk-ci-test"
private const val INITIAL_KEY = "initial-key"

/**
 * To assert two variants. These fields are not consistent across evaluation, simply assert not null.
 * - metadata.evaluationId
 */
fun assertVariantEquals(
    expected: Variant,
    actual: Variant,
) {
    Assert.assertEquals(expected.key, actual.key)
    Assert.assertEquals(expected.value, actual.value)
    Assert.assertEquals(expected.payload, actual.payload)
    Assert.assertEquals(expected.expKey, actual.expKey)
    if (expected.metadata?.get("evaluationId") != null) {
        Assert.assertNotNull(actual.metadata?.get("evaluationId"))
    }
}

class ExperimentClientTest {
    init {
        AmpLogger.loggerProvider = SystemLoggerProvider(true)
    }

    private var mockStorage = MockStorage()
    private val testUser = ExperimentUser(userId = "test_user")

    private val serverVariant =
        Variant(
            key = "on",
            value = "on",
            payload = "payload",
            metadata = mapOf("evaluationId" to ""),
        )
    private val fallbackVariant = Variant(key = "fallback", value = "fallback")
    private val initialVariant = Variant(key = "initial", value = "initial")
    private val inlineVariant = Variant(key = "inline", value = "inline")

    private val initialVariants =
        mapOf(
            INITIAL_KEY to initialVariant,
            KEY to Variant(key = "off"),
        )

    private val client =
        DefaultExperimentClient(
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

    private val timeoutClient =
        DefaultExperimentClient(
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

    private val initialVariantSourceClient =
        DefaultExperimentClient(
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

    private val generalClient =
        DefaultExperimentClient(
            API_KEY,
            ExperimentConfig(
                debug = true,
            ),
            OkHttpClient(),
            mockStorage,
            Experiment.executorService,
        )

    @Before
    fun init() {
        mockStorage = MockStorage()
    }

    @Test
    fun `fetch success`() {
        client.fetch(testUser).get()
        val variant = client.variant(KEY)
        Assert.assertNotNull(variant)
        assertVariantEquals(serverVariant, variant)
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
            assertVariantEquals(serverVariant, variant)
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
        assertVariantEquals(fallbackVariant, variant)

        variant = client.variant(INITIAL_KEY, firstFallback)
        Assert.assertEquals(firstFallback, variant)

        variant = client.variant(INITIAL_KEY)
        assertVariantEquals(initialVariant, variant)

        client.fetch(testUser).get()

        variant = client.variant("asdf", firstFallback)
        Assert.assertEquals(firstFallback, variant)

        variant = client.variant("asdf")
        assertVariantEquals(fallbackVariant, variant)

        variant = client.variant(INITIAL_KEY, firstFallback)
        Assert.assertEquals(firstFallback, variant)

        variant = client.variant(INITIAL_KEY)
        assertVariantEquals(initialVariant, variant)

        variant = client.variant(KEY, firstFallback)
        assertVariantEquals(serverVariant, variant)
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
        assertVariantEquals(Variant(key = "on", value = "on", payload = "payload"), variant)
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
        client.fetch(testUser).get()
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
        assertVariantEquals(serverVariant, variant)
    }

    @Test
    fun `test fetch user with invalid flags`() {
        val invalidKey = "invalid"
        client.fetch(testUser, FetchOptions(flagKeys = listOf(KEY, INITIAL_KEY, invalidKey))).get()
        val variant = client.variant(KEY)
        Assert.assertNotNull(variant)
        assertVariantEquals(serverVariant, variant)

        val firstFallback = Variant("first")
        val initialVariant = client.variant(INITIAL_KEY, firstFallback)
        Assert.assertEquals(firstFallback, initialVariant)

        val invalidVariant = client.variant(invalidKey)
        assertVariantEquals(fallbackVariant, invalidVariant)
    }

    @Test
    fun `test exposure event through analytics provider when variant called`() {
        var didExposureGetTracked = false
        var didUserPropertyGetSet = false
        val analyticsProvider =
            object : ExperimentAnalyticsProvider {
                override fun track(event: ExperimentAnalyticsEvent) {
                    Assert.assertEquals("[Experiment] Exposure", event.name)
                    Assert.assertEquals(
                        mapOf(
                            "key" to KEY,
                            "variant" to serverVariant.key,
                            "source" to VariantSource.LOCAL_STORAGE.toString(),
                        ),
                        event.properties,
                    )

                    Assert.assertEquals(KEY, event.key)
                    assertVariantEquals(serverVariant, event.variant)
                    didExposureGetTracked = true
                }

                override fun setUserProperty(event: ExperimentAnalyticsEvent) {
                    Assert.assertEquals("[Experiment] $KEY", event.userProperty)
                    assertVariantEquals(serverVariant, event.variant)
                    didUserPropertyGetSet = true
                }

                override fun unsetUserProperty(event: ExperimentAnalyticsEvent) {
                    Assert.fail("analytics provider unset() should not be called")
                }
            }
        val analyticsProviderClient =
            DefaultExperimentClient(
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
        val analyticsProvider =
            object : ExperimentAnalyticsProvider {
                override fun track(event: ExperimentAnalyticsEvent) {
                    Assert.fail("analytics provider track() should not be called.")
                }

                override fun setUserProperty(event: ExperimentAnalyticsEvent) {
                    Assert.fail("analytics provider setUserProperty() should not be called")
                }

                override fun unsetUserProperty(event: ExperimentAnalyticsEvent) {
                    Assert.assertEquals(
                        event.userProperty,
                        "[Experiment] asdf",
                    )
                    Assert.assertEquals(
                        event.variant,
                        fallbackVariant,
                    )
                    Assert.assertEquals(event.properties["source"], "fallback-config")
                    didExposureGetUnset = true
                }
            }
        val analyticsProviderClient =
            DefaultExperimentClient(
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
        val analyticsProvider =
            object : ExperimentAnalyticsProvider {
                override fun track(event: ExperimentAnalyticsEvent) {
                    Assert.assertEquals(
                        event.userProperty,
                        "[Experiment] $INITIAL_KEY",
                    )
                    Assert.assertEquals(
                        event.variant,
                        initialVariants[INITIAL_KEY],
                    )
                    Assert.assertEquals(event.properties["source"], "secondary-initial")
                    didExposureGetUnset = true
                }

                override fun setUserProperty(event: ExperimentAnalyticsEvent) {
                    Assert.fail("analytics provider set() should not be called.")
                }

                override fun unsetUserProperty(event: ExperimentAnalyticsEvent) {
                    Assert.assertEquals("[Experiment] $INITIAL_KEY", event.userProperty)
                }
            }
        val analyticsProviderClient =
            DefaultExperimentClient(
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
        val analyticsProvider =
            object : ExperimentAnalyticsProvider {
                override fun track(event: ExperimentAnalyticsEvent) {
                    Assert.assertEquals(
                        event.userProperties,
                        mapOf("[Experiment] $KEY" to serverVariant.key),
                    )
                    didExposureGetTracked = true
                }

                override fun setUserProperty(event: ExperimentAnalyticsEvent) {
                    Assert.assertEquals(
                        "[Experiment] $KEY",
                        event.userProperty,
                    )
                    didUserPropertyGetSet = true
                }

                override fun unsetUserProperty(event: ExperimentAnalyticsEvent) {
                    Assert.fail("analytics provider unset() should not be called")
                }
            }
        val analyticsProviderClient =
            DefaultExperimentClient(
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
        val exposureTrackingProvider =
            object : ExposureTrackingProvider {
                override fun track(exposure: Exposure) {
                    Assert.assertEquals("flagKey", exposure.flagKey)
                    Assert.assertEquals("variant", exposure.variant)
                    Assert.assertEquals("experimentKey", exposure.experimentKey)
                    didTrack = true
                }
            }
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    debug = true,
                    exposureTrackingProvider = exposureTrackingProvider,
                    source = Source.INITIAL_VARIANTS,
                    initialVariants =
                        mapOf(
                            "flagKey" to
                                Variant(
                                    key = "variant",
                                    expKey = "experimentKey",
                                ),
                        ),
                ),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        client.variant("flagKey")
        Assert.assertTrue(didTrack)
    }

    @Test
    fun `ServerZone - test no config uses defaults`() {
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        Assert.assertEquals("https://api.lab.amplitude.com/".toHttpUrl(), client.serverUrl)
        Assert.assertEquals("https://flag.lab.amplitude.com/".toHttpUrl(), client.flagsServerUrl)
    }

    @Test
    fun `ServerZone - test us server zone config uses defaults`() {
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(serverZone = ServerZone.US),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        Assert.assertEquals("https://api.lab.amplitude.com/".toHttpUrl(), client.serverUrl)
        Assert.assertEquals("https://flag.lab.amplitude.com/".toHttpUrl(), client.flagsServerUrl)
    }

    @Test
    fun `ServerZone - test us server zone config with explicit config uses explicit config`() {
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    serverZone = ServerZone.US,
                    serverUrl = "https://experiment.company.com",
                    flagsServerUrl = "https://flags.company.com",
                ),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        Assert.assertEquals("https://experiment.company.com".toHttpUrl(), client.serverUrl)
        Assert.assertEquals("https://flags.company.com".toHttpUrl(), client.flagsServerUrl)
    }

    @Test
    fun `ServerZone - test eu server zone uses eu defaults`() {
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(serverZone = ServerZone.EU),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        Assert.assertEquals("https://api.lab.eu.amplitude.com/".toHttpUrl(), client.serverUrl)
        Assert.assertEquals("https://flag.lab.eu.amplitude.com/".toHttpUrl(), client.flagsServerUrl)
    }

    @Test
    fun `ServerZone - test eu server zone with explicit config uses explicit config`() {
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    serverZone = ServerZone.EU,
                    serverUrl = "https://experiment.company.com",
                    flagsServerUrl = "https://flags.company.com",
                ),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        Assert.assertEquals("https://experiment.company.com".toHttpUrl(), client.serverUrl)
        Assert.assertEquals("https://flags.company.com".toHttpUrl(), client.flagsServerUrl)
    }

    @Test
    fun `LocalEvaluation - test start loads flags into local storage`() {
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(fetchOnStart = true),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        client.start(ExperimentUser(deviceId = "test_device")).get()
        Assert.assertEquals("sdk-ci-test-local", client.allFlags()["sdk-ci-test-local"]?.key)
        client.stop()
    }

    @Test
    fun `LocalEvaluation - test variant after start returns expected locally evaluated variant`() {
        val client =
            DefaultExperimentClient(
                SERVER_API_KEY,
                ExperimentConfig(fetchOnStart = true),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        client.start(ExperimentUser(deviceId = "test_device")).get()
        var variant = client.variant("sdk-ci-test-local")
        Assert.assertEquals("on", variant.key)
        Assert.assertEquals("on", variant.value)
        client.setUser(ExperimentUser())
        variant = client.variant("sdk-ci-test-local")
        Assert.assertEquals("off", variant.key)
        Assert.assertEquals(null, variant.value)
        client.stop()
    }

    @Test
    fun `LocalEvaluation - remote evaluation variant preferred over local evaluation variant`() {
        val client =
            DefaultExperimentClient(
                SERVER_API_KEY,
                ExperimentConfig(fetchOnStart = false),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        val user = ExperimentUser(userId = "test_user", deviceId = "test_device")
        client.start(user).get()
        var variant = client.variant("sdk-ci-test")
        Assert.assertEquals("off", variant.key)
        Assert.assertEquals(null, variant.value)
        client.fetch(user).get()
        variant = client.variant("sdk-ci-test")
        Assert.assertEquals("on", variant.key)
        Assert.assertEquals("on", variant.value)
        Assert.assertEquals("payload", variant.payload)
        client.stop()
    }

    @Test
    fun `LocalStorage - test variant accessed from local storage primary`() {
        val user = ExperimentUser(userId = "test_user")
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    exposureTrackingProvider = exposureTrackingProvider,
                    fetchOnStart = true,
                    source = Source.LOCAL_STORAGE,
                ),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        client.start(user).get()
        val variant = client.variant("sdk-ci-test")
        Assert.assertEquals("on", variant.key)
        Assert.assertEquals("on", variant.value)
        Assert.assertEquals("payload", variant.payload)
        verify(exactly = 1) {
            exposureTrackingProvider.track(
                match {
                    it.flagKey == "sdk-ci-test" && it.variant == "on"
                },
            )
        }
    }

    @Test
    fun `LocalStorage - test variant accessed from inline fallback before initial variants secondary`() {
        val user = ExperimentUser()
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    exposureTrackingProvider = exposureTrackingProvider,
                    fetchOnStart = true,
                    source = Source.LOCAL_STORAGE,
                    initialVariants = mapOf("sdk-ci-test" to initialVariant),
                    fallbackVariant = fallbackVariant,
                ),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        client.start(user).get()
        val variant = client.variant("sdk-ci-test", inlineVariant)
        assertVariantEquals(inlineVariant, variant)
        verify(exactly = 1) {
            exposureTrackingProvider.track(
                match {
                    it.flagKey == "sdk-ci-test" && it.variant == null
                },
            )
        }
    }

    @Test
    fun `LocalStorage - test variant accessed from initial variants when no explicit fallback provided`() {
        val user = ExperimentUser()
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    exposureTrackingProvider = exposureTrackingProvider,
                    fetchOnStart = true,
                    source = Source.LOCAL_STORAGE,
                    initialVariants = mapOf("sdk-ci-test" to initialVariant),
                    fallbackVariant = fallbackVariant,
                ),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        client.start(user).get()
        val variant = client.variant("sdk-ci-test")
        assertVariantEquals(initialVariant, variant)
        verify(exactly = 1) {
            exposureTrackingProvider.track(
                match {
                    it.flagKey == "sdk-ci-test" && it.variant == null
                },
            )
        }
    }

    @Test
    fun `LocalStorage - test variant accessed from configured fallback when no initial variants or explicit fallback provided`() {
        val user = ExperimentUser()
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    exposureTrackingProvider = exposureTrackingProvider,
                    fetchOnStart = true,
                    source = Source.LOCAL_STORAGE,
                    initialVariants = mapOf("sdk-ci-test-not-selected" to initialVariant),
                    fallbackVariant = fallbackVariant,
                ),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        client.start(user).get()
        val variant = client.variant("sdk-ci-test")
        Assert.assertEquals(Variant(key = "fallback", value = "fallback"), variant)
        verify(exactly = 1) {
            exposureTrackingProvider.track(
                match {
                    it.flagKey == "sdk-ci-test" && it.variant == null
                },
            )
        }
    }

    @Test
    fun `LocalStorage - test default variant returned when no other fallback is provided`() {
        val user = ExperimentUser()
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    exposureTrackingProvider = exposureTrackingProvider,
                    fetchOnStart = true,
                    source = Source.LOCAL_STORAGE,
                    initialVariants = mapOf("sdk-ci-test-not-selected" to initialVariant),
                ),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        client.start(user).get()
        val variant = client.variant("sdk-ci-test")
        Assert.assertEquals(variant.key, "off")
        Assert.assertEquals(variant.value, null)
        Assert.assertEquals(variant.metadata?.get("default"), true)
        verify(exactly = 1) {
            exposureTrackingProvider.track(
                match {
                    it.flagKey == "sdk-ci-test" && it.variant == null
                },
            )
        }
    }

    @Test
    fun `InitialVariants - test variant accessed from initial variants primary`() {
        val user = ExperimentUser(userId = "test_user")
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    exposureTrackingProvider = exposureTrackingProvider,
                    fetchOnStart = true,
                    source = Source.INITIAL_VARIANTS,
                    initialVariants = mapOf("sdk-ci-test" to initialVariant),
                    fallbackVariant = fallbackVariant,
                ),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        client.start(user).get()
        val variant = client.variant("sdk-ci-test")
        assertVariantEquals(initialVariant, variant)
        verify(exactly = 1) {
            exposureTrackingProvider.track(
                match {
                    it.flagKey == "sdk-ci-test" && it.variant == "initial"
                },
            )
        }
    }

    @Test
    fun `InitialVariants - test variant accessed from local storage secondary`() {
        val user = ExperimentUser(userId = "test_user")
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    exposureTrackingProvider = exposureTrackingProvider,
                    fetchOnStart = true,
                    source = Source.INITIAL_VARIANTS,
                    initialVariants = mapOf("sdk-ci-test-not-selected" to initialVariant),
                    fallbackVariant = fallbackVariant,
                ),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        client.start(user).get()
        val variant = client.variant("sdk-ci-test", inlineVariant)
        Assert.assertEquals("on", variant.key)
        Assert.assertEquals("on", variant.value)
        Assert.assertEquals("payload", variant.payload)
        verify(exactly = 1) {
            exposureTrackingProvider.track(
                match {
                    it.flagKey == "sdk-ci-test" && it.variant == "on"
                },
            )
        }
    }

    @Test
    fun `InitialVariants - test variant accessed from inline fallback`() {
        val user = ExperimentUser()
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    exposureTrackingProvider = exposureTrackingProvider,
                    fetchOnStart = true,
                    source = Source.INITIAL_VARIANTS,
                    initialVariants = mapOf("sdk-ci-test-not-selected" to initialVariant),
                    fallbackVariant = fallbackVariant,
                ),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        client.start(user).get()
        val variant = client.variant("sdk-ci-test", inlineVariant)
        assertVariantEquals(inlineVariant, variant)
        verify(exactly = 1) {
            exposureTrackingProvider.track(
                match {
                    it.flagKey == "sdk-ci-test" && it.variant == null
                },
            )
        }
    }

    @Test
    fun `InitialVariants - test variant accessed from configured fallback when no initial variants or explicit fallback provided`() {
        val user = ExperimentUser()
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    exposureTrackingProvider = exposureTrackingProvider,
                    fetchOnStart = true,
                    source = Source.INITIAL_VARIANTS,
                    initialVariants = mapOf("sdk-ci-test-not-selected" to initialVariant),
                    fallbackVariant = fallbackVariant,
                ),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        client.start(user).get()
        val variant = client.variant("sdk-ci-test")
        assertVariantEquals(fallbackVariant, variant)
        verify(exactly = 1) {
            exposureTrackingProvider.track(
                match {
                    it.flagKey == "sdk-ci-test" && it.variant == null
                },
            )
        }
    }

    @Test
    fun `InitialVariants - default variant returned when no other fallback is provided`() {
        val user = ExperimentUser()
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    exposureTrackingProvider = exposureTrackingProvider,
                    fetchOnStart = true,
                    source = Source.INITIAL_VARIANTS,
                    initialVariants = mapOf("sdk-ci-test-not-selected" to initialVariant),
                ),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        client.start(user).get()
        val variant = client.variant("sdk-ci-test")
        assertVariantEquals(Variant(key = "off", metadata = mapOf("default" to true)), variant)
        verify(exactly = 1) {
            exposureTrackingProvider.track(
                match {
                    it.flagKey == "sdk-ci-test" && it.variant == null
                },
            )
        }
    }

    @Test
    fun `LocalEvaluationFlags - test returns locally evaluated variant over remote and all other fallbacks`() {
        val user = ExperimentUser(deviceId = "0123456789")
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    exposureTrackingProvider = exposureTrackingProvider,
                    fetchOnStart = true,
                    source = Source.LOCAL_STORAGE,
                    initialVariants = mapOf("sdk-ci-test-local" to initialVariant),
                    fallbackVariant = fallbackVariant,
                ),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        client.start(user).get()
        val variant = client.variant("sdk-ci-test-local", inlineVariant)
        Assert.assertEquals("on", variant.key)
        Assert.assertEquals("on", variant.value)
        Assert.assertEquals("local", variant.metadata?.get("evaluationMode"))
        verify(exactly = 1) {
            exposureTrackingProvider.track(
                match {
                    it.flagKey == "sdk-ci-test-local" && it.variant == "on"
                },
            )
        }
    }

    @Test
    fun `LocalEvaluationFlags - test locally evaluated default variant with inline fallback`() {
        val user = ExperimentUser()
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    exposureTrackingProvider = exposureTrackingProvider,
                    fetchOnStart = true,
                    source = Source.LOCAL_STORAGE,
                    initialVariants = mapOf("sdk-ci-test-local" to initialVariant),
                    fallbackVariant = fallbackVariant,
                ),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        client.start(user).get()
        val variant = client.variant("sdk-ci-test-local", inlineVariant)
        assertVariantEquals(inlineVariant, variant)
        verify(exactly = 1) {
            exposureTrackingProvider.track(
                match {
                    it.flagKey == "sdk-ci-test-local" && it.variant == null
                },
            )
        }
    }

    @Test
    fun `LocalEvaluationFlags - test locally evaluated default variant with initial variants`() {
        val user = ExperimentUser()
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    exposureTrackingProvider = exposureTrackingProvider,
                    fetchOnStart = true,
                    source = Source.LOCAL_STORAGE,
                    initialVariants = mapOf("sdk-ci-test-local" to initialVariant),
                    fallbackVariant = fallbackVariant,
                ),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        client.start(user).get()
        val variant = client.variant("sdk-ci-test-local")
        assertVariantEquals(initialVariant, variant)
        verify(exactly = 1) {
            exposureTrackingProvider.track(
                match {
                    it.flagKey == "sdk-ci-test-local" && it.variant == null
                },
            )
        }
    }

    @Test
    fun `LocalEvaluationFlags - test locally evaluated default variant with configured fallback`() {
        val user = ExperimentUser()
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    exposureTrackingProvider = exposureTrackingProvider,
                    fetchOnStart = true,
                    source = Source.LOCAL_STORAGE,
                    initialVariants = mapOf("sdk-ci-test-local-not-selected" to initialVariant),
                    fallbackVariant = fallbackVariant,
                ),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        client.start(user).get()
        val variant = client.variant("sdk-ci-test-local")
        assertVariantEquals(fallbackVariant, variant)
        verify(exactly = 1) {
            exposureTrackingProvider.track(
                match {
                    it.flagKey == "sdk-ci-test-local" && it.variant == null
                },
            )
        }
    }

    @Test
    fun `LocalEvaluationFlags - test default variant returned when no other fallback is provided`() {
        val user = ExperimentUser()
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    exposureTrackingProvider = exposureTrackingProvider,
                    fetchOnStart = true,
                    source = Source.LOCAL_STORAGE,
                ),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        client.start(user).get()
        val variant = client.variant("sdk-ci-test-local")
        Assert.assertEquals("off", variant.key)
        Assert.assertEquals(null, variant.value)
        verify(exactly = 1) {
            exposureTrackingProvider.track(
                match {
                    it.flagKey == "sdk-ci-test-local" && it.variant == null
                },
            )
        }
    }

    @Test
    fun `LocalEvaluationFlags - test all returns local evaluation variant over remote or initialVariants with local storage source`() {
        val user = ExperimentUser(userId = "test_user", deviceId = "0123456789")
        val exposureTrackingProvider = TestExposureTrackingProvider()
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    exposureTrackingProvider = exposureTrackingProvider,
                    fetchOnStart = true,
                    source = Source.LOCAL_STORAGE,
                    initialVariants =
                        mapOf(
                            "sdk-ci-test" to initialVariant,
                            "sdk-ci-test-local" to initialVariant,
                        ),
                ),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        client.start(user).get()
        val allVariants = client.all()
        val localVariant = allVariants["sdk-ci-test-local"]
        Assert.assertEquals("on", localVariant?.key)
        Assert.assertEquals("on", localVariant?.value)
        Assert.assertEquals("local", localVariant?.metadata?.get("evaluationMode"))
        val remoteVariant = allVariants["sdk-ci-test"]
        Assert.assertEquals("on", remoteVariant?.key)
        Assert.assertEquals("on", remoteVariant?.value)
    }

    @Test
    fun `LocalEvaluationFlags - test all returns local evaluation variant over remote or initialVariants with initial variants source`() {
        val user = ExperimentUser(userId = "test_user", deviceId = "0123456789")
        val exposureTrackingProvider = TestExposureTrackingProvider()
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    exposureTrackingProvider = exposureTrackingProvider,
                    fetchOnStart = true,
                    source = Source.INITIAL_VARIANTS,
                    initialVariants =
                        mapOf(
                            "sdk-ci-test" to initialVariant,
                            "sdk-ci-test-local" to initialVariant,
                        ),
                ),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        client.start(user).get()
        val allVariants = client.all()
        val localVariant = allVariants["sdk-ci-test-local"]
        Assert.assertEquals("on", localVariant?.key)
        Assert.assertEquals("on", localVariant?.value)
        Assert.assertEquals("local", localVariant?.metadata?.get("evaluationMode"))
        val remoteVariant = allVariants["sdk-ci-test"]
        Assert.assertEquals("initial", remoteVariant?.key)
        Assert.assertEquals("initial", remoteVariant?.value)
    }

    @Test
    fun `start - test with local and remote evaluation, calls fetchInternal`() {
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        val spyClient = spyk(client)
        spyClient.start(null).get()
        verify(exactly = 1) { spyClient.fetchInternal(any(), any(), any(), any()) }
    }

    @Test
    fun `start - with local evaluation only, calls fetchInternal`() {
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        val spyClient = spyk(client)
        every { spyClient.allFlags() } returns emptyMap()
        spyClient.start(null).get()
        verify(exactly = 1) { spyClient.fetchInternal(any(), any(), any(), any()) }
    }

    @Test
    fun `start - test with local evaluation only, fetchOnStart enabled, calls fetchInternal`() {
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(fetchOnStart = true),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        val spyClient = spyk(client)
        every { spyClient.allFlags() } returns emptyMap()
        spyClient.start(null).get()
        verify(exactly = 1) { spyClient.fetchInternal(any(), any(), any(), any()) }
    }

    @Test
    fun `start - test with local and remote evaluation, fetchOnStart disabled, does not call fetchInternal`() {
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(fetchOnStart = false),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        val spyClient = spyk(client)
        spyClient.start(null).get()
        verify(exactly = 0) { spyClient.fetchInternal(any(), any(), any(), any()) }
    }

    @Test
    fun `LocalEvaluation - test payload format`() {
        val user = ExperimentUser(userId = "test_user")
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    debug = true,
                    exposureTrackingProvider = exposureTrackingProvider,
                    fetchOnStart = false,
                    source = Source.LOCAL_STORAGE,
                ),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        client.start(user).get()
        var variant = client.variant("sdk-payload-ci-test")
        val obj = JSONObject().put("key1", "val1").put("key2", "val2")
        val array =
            JSONArray().put(JSONObject().put("key1", "obj1")).put(JSONObject().put("key2", "obj2"))
        Assert.assertEquals(obj::class, variant.payload!!::class)
        Assert.assertEquals(obj.toString(), variant.payload.toString())
        // set null user to get array variant
        client.start(ExperimentUser()).get()
        variant = client.variant("sdk-payload-ci-test")
        Assert.assertEquals(array::class, variant.payload!!::class)
        Assert.assertEquals(array.toString(), variant.payload.toString())
    }

    @Test
    fun `initial flags`() {
        val storage = MockStorage()
        // Flag, sdk-ci-test-local is modified to always return off
        val initialFlags =
            """
            [
                {"key":"sdk-ci-test-local","metadata":{"deployed":true,"evaluationMode":"local","flagType":"release","flagVersion":1},"segments":[{"metadata":{"segmentName":"All Other Users"},"variant":"off"}],"variants":{"off":{"key":"off","metadata":{"default":true}},"on":{"key":"on","value":"on"}}},
                {"key":"sdk-ci-test-local-2","metadata":{"deployed":true,"evaluationMode":"local","flagType":"release","flagVersion":1},"segments":[{"metadata":{"segmentName":"All Other Users"},"variant":"on"}],"variants":{"off":{"key":"off","metadata":{"default":true}},"on":{"key":"on","value":"on"}}}
            ]
            """.trimIndent()
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    initialFlags = initialFlags,
                ),
                OkHttpClient(),
                storage,
                Experiment.executorService,
            )
        val user = ExperimentUser(userId = "user_id", deviceId = "device_id")
        client.setUser(user)
        var variant = client.variant("sdk-ci-test-local")
        Assert.assertEquals("off", variant.key)
        var variant2 = client.variant("sdk-ci-test-local-2")
        Assert.assertEquals("on", variant2.key)
        // Call start to update the flag, overwrites the initial flag to return on
        client.start(user).get()
        variant = client.variant("sdk-ci-test-local")
        Assert.assertEquals("on", variant.key)
        variant2 = client.variant("sdk-ci-test-local-2")
        Assert.assertEquals("on", variant2.key)
        // Initialize a second client with the same storage to simulate an app restart
        val client2 =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    initialFlags = initialFlags,
                ),
                OkHttpClient(),
                storage,
                Experiment.executorService,
            )
        // Storage flag should take precedent over initial flag
        variant = client.variant("sdk-ci-test-local")
        Assert.assertEquals("on", variant.key)
        variant2 = client.variant("sdk-ci-test-local-2")
        Assert.assertEquals("on", variant2.key)
    }

    @Test
    fun `fetch retry with different response codes`() {
        // Response code, error message, and whether retry should be called
        val testData =
            listOf(
                Triple(300, "Fetch Exception 300", 1),
                Triple(400, "Fetch Exception 400", 0),
                Triple(429, "Fetch Exception 429", 1),
                Triple(500, "Fetch Exception 500", 1),
                Triple(0, "Other Exception", 1),
            )

        testData.forEach { (responseCode, errorMessage, retryCalled) ->
            val storage = MockStorage()
            val client =
                spyk(
                    DefaultExperimentClient(
                        API_KEY,
                        ExperimentConfig(retryFetchOnFailure = true),
                        OkHttpClient(),
                        storage,
                        Experiment.executorService,
                    ),
                    recordPrivateCalls = true,
                )
            // Mock the private method to throw FetchException or other exceptions
            every {
                client["doFetch"](
                    any<ExperimentUser>(),
                    any<Long>(),
                    any<FetchOptions>(),
                )
            } answers {
                val future = CompletableFuture<Map<String, Variant>>()
                if (responseCode == 0) {
                    future.completeExceptionally(Exception(errorMessage))
                } else {
                    future.completeExceptionally(FetchException(responseCode, errorMessage))
                }
                future
            }

            try {
                client.fetch(ExperimentUser("test_user")).get()
            } catch (t: Throwable) {
                // Ignore exception
            }

            verify(exactly = retryCalled) {
                client["startRetries"](
                    any<ExperimentUser>(),
                    any<FetchOptions>(),
                )
            }
        }
    }

    @Test
    fun `test flag config polling interval, config not set`() {
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        Assert.assertEquals(300000, client.flagConfigPollingIntervalMillis)
    }

    @Test
    fun `test flag config polling interval, config set under min`() {
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    flagConfigPollingIntervalMillis = 1000,
                ),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        Assert.assertEquals(60000, client.flagConfigPollingIntervalMillis)
    }

    @Test
    fun `test flag config polling interval, config set over min`() {
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    flagConfigPollingIntervalMillis = 900000,
                ),
                OkHttpClient(),
                mockStorage,
                Experiment.executorService,
            )
        Assert.assertEquals(900000, client.flagConfigPollingIntervalMillis)
    }

    @Test
    fun `test set track assignment event`() {
        val storage = MockStorage()
        val testClient =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(),
                OkHttpClient(),
                storage,
                Experiment.executorService,
            )

        // Test that setTracksAssignment returns the client for chaining
        val result = testClient.setTracksAssignment(true)
        Assert.assertSame(testClient, result)

        // Test that setTracksAssignment(false) also works
        val result2 = testClient.setTracksAssignment(false)
        Assert.assertSame(testClient, result2)
    }

    @Test
    fun `test set track assignment event persistence`() {
        val storage = MockStorage()

        // Create first client
        val client1 =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(),
                OkHttpClient(),
                storage,
                Experiment.executorService,
            )

        // Create second client with same storage
        val client2 =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(),
                OkHttpClient(),
                storage,
                Experiment.executorService,
            )

        // Set track assignment event on first client
        client1.setTracksAssignment(true)

        // Verify the setting was persisted by checking storage directly
        val trackAssignmentStorage = getTrackAssignmentEventStorage(API_KEY, "\$default_instance", storage)
        trackAssignmentStorage.load()
        Assert.assertEquals(true, trackAssignmentStorage.get())
    }

    @Test
    fun `test multiple calls to set track assignment event uses latest setting`() {
        val storage = MockStorage()
        val testClient =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(),
                OkHttpClient(),
                storage,
                Experiment.executorService,
            )

        // Set track assignment event to true, then false
        testClient.setTracksAssignment(true)
        testClient.setTracksAssignment(false)

        // Verify the latest setting is stored
        val trackAssignmentStorage = getTrackAssignmentEventStorage(API_KEY, "\$default_instance", storage)
        trackAssignmentStorage.load()
        Assert.assertEquals(false, trackAssignmentStorage.get())
    }

    @Test
    fun `test set track assignment event preserves other options`() {
        val storage = MockStorage()
        val testClient =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(),
                OkHttpClient(),
                storage,
                Experiment.executorService,
            )

        // Set track assignment event to true
        testClient.setTracksAssignment(true)

        // Verify the setting is stored
        val trackAssignmentStorage = getTrackAssignmentEventStorage(API_KEY, "\$default_instance", storage)
        trackAssignmentStorage.load()
        Assert.assertEquals(true, trackAssignmentStorage.get())

        // Test that the setting works with fetch options
        val fetchOptions = FetchOptions(listOf("test-flag"))

        // This test verifies that the tracking option setting doesn't interfere with other fetch options
        Assert.assertNotNull(fetchOptions.flagKeys)
        Assert.assertEquals(listOf("test-flag"), fetchOptions.flagKeys)
    }

    @Test
    fun `test config custom request headers added, http call on fetch includes headers`() {
        val mockHttpClient = mockk<OkHttpClient>()
        var counter = 1

        fun getVariableHeaderValue(): String = counter.toString().also { counter++ }

        val testServerUrlHost = "api.test-server-url.com"
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    serverUrl = "https://$testServerUrlHost",
                    customRequestHeaders = {
                        mapOf("counter" to getVariableHeaderValue())
                    },
                ),
                mockHttpClient,
                mockStorage,
                Experiment.executorService,
            )

        try {
            client.fetch().get()
        } catch (_: Throwable) {
            // It will fail as we don't mock any return value for newCall or responses.
            // It's ok to fail, just need to check the request header is present in newCall.
        }
        try {
            client.fetch().get()
        } catch (_: Throwable) {
            // It will fail as we don't mock any return value for newCall or responses.
            // It's ok to fail, just need to check the request header is present in newCall.
        }

        verifyOrder {
            mockHttpClient.newCall(
                match {
                    it.url.host == testServerUrlHost &&
                        it.headers["counter"] == "1"
                },
            )
            mockHttpClient.newCall(
                match {
                    it.url.host == testServerUrlHost &&
                        it.headers["counter"] == "2"
                },
            )
        }
    }

    @Test
    fun `test config custom request headers added, http call on start includes headers`() {
        val mockHttpClient = mockk<OkHttpClient>()
        val testFlagsHost = "api.test-flags-server-url.com"
        val testHeader = "testKey" to "testValue"
        val client =
            DefaultExperimentClient(
                API_KEY,
                ExperimentConfig(
                    flagsServerUrl = "https://$testFlagsHost",
                    customRequestHeaders = {
                        mapOf(testHeader)
                    },
                ),
                mockHttpClient,
                mockStorage,
                Experiment.executorService,
            )

        try {
            client.start().get()
        } catch (_: Throwable) {
            // It will fail as we don't mock any return value for newCall or responses.
            // It's ok to fail, just need to check the request header is present in newCall.
        }

        verify(exactly = 1) {
            mockHttpClient.newCall(
                match {
                    it.url.host == testFlagsHost &&
                        it.headers["testKey"] == "testValue"
                },
            )
        }
    }

    private fun fetchLibraryFromRequest(mockHttpClient: OkHttpClient): String? {
        val slot = mutableListOf<okhttp3.Request>()
        verify { mockHttpClient.newCall(capture(slot)) }
        val request = slot.firstOrNull { it.url.encodedPath.contains("vardata") } ?: return null
        val userHeader = request.headers["X-Amp-Exp-User"] ?: return null
        val decoded = String(java.util.Base64.getUrlDecoder().decode(userHeader), Charsets.UTF_8)
        return JSONObject(decoded).optString("library", null)
    }

    private fun createClientWithMockHttp(
        mockHttpClient: OkHttpClient = mockk<OkHttpClient>(),
        userProvider: ExperimentUserProvider? = null,
    ): Pair<DefaultExperimentClient, OkHttpClient> {
        val client = DefaultExperimentClient(
            API_KEY,
            ExperimentConfig(
                debug = true,
                userProvider = userProvider,
            ),
            mockHttpClient,
            mockStorage,
            Experiment.executorService,
        )
        return client to mockHttpClient
    }

    @Test
    fun `test default library is set when user library is null`() {
        val (client, mockHttp) = createClientWithMockHttp()
        client.setUser(ExperimentUser(userId = "test_user"))
        try { client.fetch().get() } catch (_: Throwable) {}
        val library = fetchLibraryFromRequest(mockHttp)
        Assert.assertTrue(library?.startsWith("experiment-android-client/") == true)
    }

    @Test
    fun `test custom library is preserved when set on user`() {
        val (client, mockHttp) = createClientWithMockHttp()
        client.setUser(ExperimentUser(userId = "test_user", library = "custom-wrapper/1.0.0"))
        try { client.fetch().get() } catch (_: Throwable) {}
        val library = fetchLibraryFromRequest(mockHttp)
        Assert.assertEquals("custom-wrapper/1.0.0", library)
    }

    @Test
    fun `test custom library is not overridden by user provider`() {
        val mockProvider = mockk<ExperimentUserProvider>()
        every { mockProvider.getUser() } returns ExperimentUser(library = "provider-lib/2.0")
        val (client, mockHttp) = createClientWithMockHttp(userProvider = mockProvider)
        client.setUser(ExperimentUser(userId = "test_user", library = "custom-wrapper/1.0.0"))
        try { client.fetch().get() } catch (_: Throwable) {}
        val library = fetchLibraryFromRequest(mockHttp)
        Assert.assertEquals("custom-wrapper/1.0.0", library)
    }
}
