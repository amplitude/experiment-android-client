package com.amplitude.experiment

import com.amplitude.experiment.analytics.ExperimentAnalyticsEvent
import com.amplitude.experiment.analytics.ExperimentAnalyticsProvider
import com.amplitude.experiment.util.Logger
import com.amplitude.experiment.util.MockStorage
import com.amplitude.experiment.util.SystemLogger
import com.amplitude.experiment.util.TestExposureTrackingProvider
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ExecutionException

private const val API_KEY = "client-DvWljIjiiuqLbyjqdvBaLFfEBrAvGuA3"
private const val SERVER_API_KEY = "server-qz35UwzJ5akieoAdIgzM4m9MIiOLXLoz"

private const val KEY = "sdk-ci-test"
private const val INITIAL_KEY = "initial-key"

class ExperimentClientTest {

    init {
        Logger.implementation = SystemLogger(true)
    }

    private var mockStorage = MockStorage()
    private val testUser = ExperimentUser(userId = "test_user")

    private val serverVariant = Variant(key = "on", value = "on", payload = "payload")
    private val fallbackVariant = Variant(key = "fallback", value = "fallback")
    private val initialVariant = Variant(key = "initial", value = "initial")
    private val inlineVariant = Variant(key = "inline", value = "inline")

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

    @Before
    fun init() {
        mockStorage = MockStorage()
    }

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
                Assert.assertEquals(event.properties["source"], "fallback-config")
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
                    initialVariants[INITIAL_KEY]
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

    @Test
    fun `ServerZone - test no config uses defaults`() {
        val client = DefaultExperimentClient(
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
        val client = DefaultExperimentClient(
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
        val client = DefaultExperimentClient(
            API_KEY,
            ExperimentConfig(
                serverZone = ServerZone.US,
                serverUrl = "https://experiment.company.com",
                flagsServerUrl = "https://flags.company.com"
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
        val client = DefaultExperimentClient(
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
        val client = DefaultExperimentClient(
            API_KEY,
            ExperimentConfig(
                serverZone = ServerZone.EU,
                serverUrl = "https://experiment.company.com",
                flagsServerUrl = "https://flags.company.com"
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
        val client = DefaultExperimentClient(
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
        val client = DefaultExperimentClient(
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
        val client = DefaultExperimentClient(
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
        val client = DefaultExperimentClient(
            API_KEY,
            ExperimentConfig(
                exposureTrackingProvider = exposureTrackingProvider,
                fetchOnStart = true,
                source = Source.LOCAL_STORAGE
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
                }
            )
        }
    }

    @Test
    fun `LocalStorage - test variant accessed from inline fallback before initial variants secondary`() {
        val user = ExperimentUser()
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client = DefaultExperimentClient(
            API_KEY,
            ExperimentConfig(
                exposureTrackingProvider = exposureTrackingProvider,
                fetchOnStart = true,
                source = Source.LOCAL_STORAGE,
                initialVariants = mapOf("sdk-ci-test" to initialVariant),
                fallbackVariant = fallbackVariant
            ),
            OkHttpClient(),
            mockStorage,
            Experiment.executorService,
        )
        client.start(user).get()
        val variant = client.variant("sdk-ci-test", inlineVariant)
        Assert.assertEquals(inlineVariant, variant)
        verify(exactly = 1) {
            exposureTrackingProvider.track(
                match {
                    it.flagKey == "sdk-ci-test" && it.variant == null
                }
            )
        }
    }

    @Test
    fun `LocalStorage - test variant accessed from initial variants when no explicit fallback provided`() {
        val user = ExperimentUser()
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client = DefaultExperimentClient(
            API_KEY,
            ExperimentConfig(
                exposureTrackingProvider = exposureTrackingProvider,
                fetchOnStart = true,
                source = Source.LOCAL_STORAGE,
                initialVariants = mapOf("sdk-ci-test" to initialVariant),
                fallbackVariant = fallbackVariant
            ),
            OkHttpClient(),
            mockStorage,
            Experiment.executorService,
        )
        client.start(user).get()
        val variant = client.variant("sdk-ci-test")
        Assert.assertEquals(initialVariant, variant)
        verify(exactly = 1) {
            exposureTrackingProvider.track(
                match {
                    it.flagKey == "sdk-ci-test" && it.variant == null
                }
            )
        }
    }

    @Test
    fun `LocalStorage - test variant accessed from configured fallback when no initial variants or explicit fallback provided`() {
        val user = ExperimentUser()
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client = DefaultExperimentClient(
            API_KEY,
            ExperimentConfig(
                exposureTrackingProvider = exposureTrackingProvider,
                fetchOnStart = true,
                source = Source.LOCAL_STORAGE,
                initialVariants = mapOf("sdk-ci-test-not-selected" to initialVariant),
                fallbackVariant = fallbackVariant
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
                }
            )
        }
    }

    @Test
    fun `LocalStorage - test default variant returned when no other fallback is provided`() {
        val user = ExperimentUser()
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client = DefaultExperimentClient(
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
                }
            )
        }
    }

    @Test
    fun `InitialVariants - test variant accessed from initial variants primary`() {
        val user = ExperimentUser(userId = "test_user")
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client = DefaultExperimentClient(
            API_KEY,
            ExperimentConfig(
                exposureTrackingProvider = exposureTrackingProvider,
                fetchOnStart = true,
                source = Source.INITIAL_VARIANTS,
                initialVariants = mapOf("sdk-ci-test" to initialVariant),
                fallbackVariant = fallbackVariant
            ),
            OkHttpClient(),
            mockStorage,
            Experiment.executorService,
        )
        client.start(user).get()
        val variant = client.variant("sdk-ci-test")
        Assert.assertEquals(initialVariant, variant)
        verify(exactly = 1) {
            exposureTrackingProvider.track(
                match {
                    it.flagKey == "sdk-ci-test" && it.variant == "initial"
                }
            )
        }
    }

    @Test
    fun `InitialVariants - test variant accessed from local storage secondary`() {
        val user = ExperimentUser(userId = "test_user")
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client = DefaultExperimentClient(
            API_KEY,
            ExperimentConfig(
                exposureTrackingProvider = exposureTrackingProvider,
                fetchOnStart = true,
                source = Source.INITIAL_VARIANTS,
                initialVariants = mapOf("sdk-ci-test-not-selected" to initialVariant),
                fallbackVariant = fallbackVariant
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
                }
            )
        }
    }

    @Test
    fun `InitialVariants - test variant accessed from inline fallback`() {
        val user = ExperimentUser()
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client = DefaultExperimentClient(
            API_KEY,
            ExperimentConfig(
                exposureTrackingProvider = exposureTrackingProvider,
                fetchOnStart = true,
                source = Source.INITIAL_VARIANTS,
                initialVariants = mapOf("sdk-ci-test-not-selected" to initialVariant),
                fallbackVariant = fallbackVariant
            ),
            OkHttpClient(),
            mockStorage,
            Experiment.executorService,
        )
        client.start(user).get()
        val variant = client.variant("sdk-ci-test", inlineVariant)
        Assert.assertEquals(inlineVariant, variant)
        verify(exactly = 1) {
            exposureTrackingProvider.track(
                match {
                    it.flagKey == "sdk-ci-test" && it.variant == null
                }
            )
        }
    }

    @Test
    fun `InitialVariants - test variant accessed from configured fallback when no initial variants or explicit fallback provided`() {
        val user = ExperimentUser()
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client = DefaultExperimentClient(
            API_KEY,
            ExperimentConfig(
                exposureTrackingProvider = exposureTrackingProvider,
                fetchOnStart = true,
                source = Source.INITIAL_VARIANTS,
                initialVariants = mapOf("sdk-ci-test-not-selected" to initialVariant),
                fallbackVariant = fallbackVariant
            ),
            OkHttpClient(),
            mockStorage,
            Experiment.executorService,
        )
        client.start(user).get()
        val variant = client.variant("sdk-ci-test")
        Assert.assertEquals(fallbackVariant, variant)
        verify(exactly = 1) {
            exposureTrackingProvider.track(
                match {
                    it.flagKey == "sdk-ci-test" && it.variant == null
                }
            )
        }
    }

    @Test
    fun `InitialVariants - default variant returned when no other fallback is provided`() {
        val user = ExperimentUser()
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client = DefaultExperimentClient(
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
        Assert.assertEquals(Variant(key = "off", metadata = mapOf("default" to true)), variant)
        verify(exactly = 1) {
            exposureTrackingProvider.track(
                match {
                    it.flagKey == "sdk-ci-test" && it.variant == null
                }
            )
        }
    }

    @Test
    fun `LocalEvaluationFlags - test returns locally evaluated variant over remote and all other fallbacks`() {
        val user = ExperimentUser(deviceId = "0123456789")
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client = DefaultExperimentClient(
            API_KEY,
            ExperimentConfig(
                exposureTrackingProvider = exposureTrackingProvider,
                fetchOnStart = true,
                source = Source.LOCAL_STORAGE,
                initialVariants = mapOf("sdk-ci-test-local" to initialVariant),
                fallbackVariant = fallbackVariant
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
                }
            )
        }
    }

    @Test
    fun `LocalEvaluationFlags - test locally evaluated default variant with inline fallback`() {
        val user = ExperimentUser()
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client = DefaultExperimentClient(
            API_KEY,
            ExperimentConfig(
                exposureTrackingProvider = exposureTrackingProvider,
                fetchOnStart = true,
                source = Source.LOCAL_STORAGE,
                initialVariants = mapOf("sdk-ci-test-local" to initialVariant),
                fallbackVariant = fallbackVariant
            ),
            OkHttpClient(),
            mockStorage,
            Experiment.executorService,
        )
        client.start(user).get()
        val variant = client.variant("sdk-ci-test-local", inlineVariant)
        Assert.assertEquals(inlineVariant, variant)
        verify(exactly = 1) {
            exposureTrackingProvider.track(
                match {
                    it.flagKey == "sdk-ci-test-local" && it.variant == null
                }
            )
        }
    }

    @Test
    fun `LocalEvaluationFlags - test locally evaluated default variant with initial variants`() {
        val user = ExperimentUser()
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client = DefaultExperimentClient(
            API_KEY,
            ExperimentConfig(
                exposureTrackingProvider = exposureTrackingProvider,
                fetchOnStart = true,
                source = Source.LOCAL_STORAGE,
                initialVariants = mapOf("sdk-ci-test-local" to initialVariant),
                fallbackVariant = fallbackVariant
            ),
            OkHttpClient(),
            mockStorage,
            Experiment.executorService,
        )
        client.start(user).get()
        val variant = client.variant("sdk-ci-test-local")
        Assert.assertEquals(initialVariant, variant)
        verify(exactly = 1) {
            exposureTrackingProvider.track(
                match {
                    it.flagKey == "sdk-ci-test-local" && it.variant == null
                }
            )
        }
    }

    @Test
    fun `LocalEvaluationFlags - test locally evaluated default variant with configured fallback`() {
        val user = ExperimentUser()
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client = DefaultExperimentClient(
            API_KEY,
            ExperimentConfig(
                exposureTrackingProvider = exposureTrackingProvider,
                fetchOnStart = true,
                source = Source.LOCAL_STORAGE,
                initialVariants = mapOf("sdk-ci-test-local-not-selected" to initialVariant),
                fallbackVariant = fallbackVariant
            ),
            OkHttpClient(),
            mockStorage,
            Experiment.executorService,
        )
        client.start(user).get()
        val variant = client.variant("sdk-ci-test-local")
        Assert.assertEquals(fallbackVariant, variant)
        verify(exactly = 1) {
            exposureTrackingProvider.track(
                match {
                    it.flagKey == "sdk-ci-test-local" && it.variant == null
                }
            )
        }
    }

    @Test
    fun `LocalEvaluationFlags - test default variant returned when no other fallback is provided`() {
        val user = ExperimentUser()
        val exposureTrackingProvider = mockk<TestExposureTrackingProvider>()
        every { exposureTrackingProvider.track(any()) } just Runs
        val client = DefaultExperimentClient(
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
                }
            )
        }
    }

    @Test
    fun `LocalEvaluationFlags - test all returns local evaluation variant over remote or initialVariants with local storage source`() {
        val user = ExperimentUser(userId = "test_user", deviceId = "0123456789")
        val exposureTrackingProvider = TestExposureTrackingProvider()
        val client = DefaultExperimentClient(
            API_KEY,
            ExperimentConfig(
                exposureTrackingProvider = exposureTrackingProvider,
                fetchOnStart = true,
                source = Source.LOCAL_STORAGE,
                initialVariants = mapOf("sdk-ci-test" to initialVariant, "sdk-ci-test-local" to initialVariant)
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
        val client = DefaultExperimentClient(
            API_KEY,
            ExperimentConfig(
                exposureTrackingProvider = exposureTrackingProvider,
                fetchOnStart = true,
                source = Source.INITIAL_VARIANTS,
                initialVariants = mapOf("sdk-ci-test" to initialVariant, "sdk-ci-test-local" to initialVariant)
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
        val client = DefaultExperimentClient(
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
    fun `start - with local evaluation only, does not call fetchInternal`() {
        val client = DefaultExperimentClient(
            API_KEY,
            ExperimentConfig(),
            OkHttpClient(),
            mockStorage,
            Experiment.executorService,
        )
        val spyClient = spyk(client)
        every { spyClient.allFlags() } returns emptyMap()
        spyClient.start(null).get()
        verify(exactly = 0) { spyClient.fetchInternal(any(), any(), any(), any()) }
    }

    @Test
    fun `start - test with local evaluation only, fetchOnStart enabled, calls fetchInternal`() {
        val client = DefaultExperimentClient(
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
        val client = DefaultExperimentClient(
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
}
