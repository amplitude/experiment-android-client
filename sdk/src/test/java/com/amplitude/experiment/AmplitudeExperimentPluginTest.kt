package com.amplitude.experiment

import com.amplitude.common.Logger
import com.amplitude.core.AmplitudeContext
import com.amplitude.core.AnalyticsClient
import com.amplitude.core.AnalyticsIdentity
import com.amplitude.core.RestrictedAmplitudeFeature
import com.amplitude.core.diagnostics.DiagnosticsClient
import com.amplitude.core.remoteconfig.RemoteConfigClient
import com.amplitude.experiment.util.AmpLogger
import com.amplitude.experiment.util.SystemLoggerProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import com.amplitude.core.ServerZone as CoreServerZone

private const val API_KEY = "client-DvWljIjiiuqLbyjqdvBaLFfEBrAvGuA3"

@OptIn(RestrictedAmplitudeFeature::class)
class AmplitudeExperimentPluginTest {
    init {
        AmpLogger.loggerProvider = SystemLoggerProvider(true)
    }

    private val applicationContext = mockk<android.content.Context>(relaxed = true)
    private val analyticsClient = mockk<AnalyticsClient>(relaxed = true)
    private val remoteConfigClient = mockk<RemoteConfigClient>(relaxed = true)
    private val logger = mockk<Logger>(relaxed = true)
    private val diagnosticsClient = mockk<DiagnosticsClient>(relaxed = true)

    private val identity =
        object : AnalyticsIdentity {
            override val userId: String? = "user-1"
            override val deviceId: String? = "device-1"
            override val userProperties: Map<String, Any?> = mapOf("plan" to "pro")
        }

    @Before
    fun setUp() {
        every { applicationContext.applicationContext } returns applicationContext
        every { applicationContext.getSharedPreferences(any(), any()) } returns mockk(relaxed = true)
        every { analyticsClient.identity } returns identity
        every { analyticsClient.sessionId } returns 42L
        every { analyticsClient.optOut } returns false
    }

    @Test
    fun `exposure tracking routes through analytics client track`() {
        val provider = AnalyticsClientExposureTrackingProvider { analyticsClient }
        val exposure =
            Exposure(
                flagKey = "flag-key",
                variant = "treatment",
                experimentKey = "exp-key",
                metadata = mapOf("source" to "remote"),
            )

        provider.track(exposure)

        verify {
            analyticsClient.track(
                "\$exposure",
                mapOf(
                    "flag_key" to "flag-key",
                    "variant" to "treatment",
                    "experiment_key" to "exp-key",
                    "metadata" to mapOf("source" to "remote"),
                ),
            )
        }
    }

    @Test
    fun `setup starts experiment client without remote config`() {
        val plugin =
            AmplitudeExperimentPlugin(
                applicationContext,
                ExperimentConfig(
                    debug = true,
                    fetchOnStart = false,
                    pollOnStart = false,
                ),
            )

        plugin.setup(analyticsClient, createContext())

        Assert.assertNotNull(plugin.experimentClient?.getUser())
        Assert.assertEquals("user-1", plugin.experimentClient?.getUser()?.userId)
        Assert.assertEquals("device-1", plugin.experimentClient?.getUser()?.deviceId)
        verify(exactly = 0) { remoteConfigClient.subscribe(any(), any(), any()) }
    }

    @Test
    fun `setup does not start when opted out`() {
        every { analyticsClient.optOut } returns true
        val plugin =
            AmplitudeExperimentPlugin(
                applicationContext,
                ExperimentConfig(
                    debug = true,
                    fetchOnStart = false,
                    pollOnStart = false,
                ),
            )

        plugin.setup(analyticsClient, createContext())

        Assert.assertNotNull(plugin.experimentClient)
        Assert.assertNull(plugin.experimentClient?.getUser())
    }

    @Test
    fun `onIdentityChanged rebuilds user and triggers fetch`() {
        val plugin =
            AmplitudeExperimentPlugin(
                applicationContext,
                ExperimentConfig(
                    debug = true,
                    automaticFetchOnAmplitudeIdentityChange = true,
                    fetchOnStart = false,
                    pollOnStart = false,
                ),
            )

        plugin.setup(analyticsClient, createContext())

        val spyClient = spyk(plugin.experimentClient as DefaultExperimentClient)
        setExperimentClient(plugin, spyClient)

        val newIdentity =
            object : AnalyticsIdentity {
                override val userId: String? = "user-2"
                override val deviceId: String? = "device-2"
                override val userProperties: Map<String, Any?> = mapOf("plan" to "enterprise")
            }

        plugin.onIdentityChanged(newIdentity)

        Assert.assertEquals("user-2", spyClient.getUser()?.userId)
        Assert.assertEquals("device-2", spyClient.getUser()?.deviceId)
        verify { spyClient.fetch(any()) }
    }

    @Test
    fun `onIdentityChanged does not fetch while opted out`() {
        val plugin =
            AmplitudeExperimentPlugin(
                applicationContext,
                ExperimentConfig(
                    debug = true,
                    automaticFetchOnAmplitudeIdentityChange = true,
                    fetchOnStart = false,
                    pollOnStart = false,
                ),
            )

        plugin.setup(analyticsClient, createContext())
        val spyClient = spyk(plugin.experimentClient as DefaultExperimentClient)
        setExperimentClient(plugin, spyClient)

        plugin.onOptOutChanged(true)
        plugin.onIdentityChanged(identity)

        verify(exactly = 0) { spyClient.fetch(any()) }
    }

    @Test
    fun `onSessionIdChanged updates session id on the user`() {
        val plugin =
            AmplitudeExperimentPlugin(
                applicationContext,
                ExperimentConfig(
                    debug = true,
                    fetchOnStart = false,
                    pollOnStart = false,
                ),
            )

        plugin.setup(analyticsClient, createContext())

        plugin.onSessionIdChanged(99L)

        val user = plugin.experimentClient?.getUser()
        Assert.assertEquals(99L, user?.userProperties?.get(AnalyticsClientUserProvider.SESSION_ID_USER_PROPERTY))
    }

    @Test
    fun `onOptOutChanged resumes start after opting back in`() {
        val plugin =
            AmplitudeExperimentPlugin(
                applicationContext,
                ExperimentConfig(
                    debug = true,
                    fetchOnStart = false,
                    pollOnStart = false,
                ),
            )

        plugin.setup(analyticsClient, createContext())
        val spyClient = spyk(plugin.experimentClient as DefaultExperimentClient)
        setExperimentClient(plugin, spyClient)

        plugin.onOptOutChanged(true)
        verify { spyClient.stop() }

        plugin.onOptOutChanged(false)
        verify { spyClient.start(any()) }
    }

    @Test
    fun `onReset clears cached variants and rebuilds user`() {
        val plugin =
            AmplitudeExperimentPlugin(
                applicationContext,
                ExperimentConfig(
                    debug = true,
                    fetchOnStart = false,
                    pollOnStart = false,
                ),
            )

        plugin.setup(analyticsClient, createContext())

        val spyClient = spyk(plugin.experimentClient as DefaultExperimentClient)
        setExperimentClient(plugin, spyClient)

        plugin.onReset()

        verify { spyClient.clear() }
        verify { spyClient.setUser(match { it.userId == "user-1" && it.deviceId == "device-1" }) }
    }

    @Test
    fun `teardown stops and clears the experiment client`() {
        val plugin =
            AmplitudeExperimentPlugin(
                applicationContext,
                ExperimentConfig(
                    debug = true,
                    fetchOnStart = false,
                    pollOnStart = false,
                ),
            )

        plugin.setup(analyticsClient, createContext())
        Assert.assertNotNull(plugin.experimentClient)

        plugin.teardown()

        Assert.assertNull(plugin.experimentClient)
    }

    @Test
    fun `plugin exposes stable name`() {
        val plugin = AmplitudeExperimentPlugin(applicationContext)
        Assert.assertEquals(AmplitudeExperimentPlugin.PLUGIN_NAME, plugin.name)
    }

    @Test
    fun `plugin name includes deployment key`() {
        val plugin =
            AmplitudeExperimentPlugin(
                context = applicationContext,
                deploymentKey = "deployment-key",
            )

        Assert.assertEquals("${AmplitudeExperimentPlugin.PLUGIN_NAME}_deployment-key", plugin.name)
    }

    @Test
    fun `plugin falls back to provided context when application context is unavailable`() {
        val context = mockk<android.content.Context>()
        every { context.applicationContext } returns null

        val plugin = AmplitudeExperimentPlugin(context)

        Assert.assertEquals(AmplitudeExperimentPlugin.PLUGIN_NAME, plugin.name)
    }

    @Test
    fun `exposure tracking ignores disconnected analytics client`() {
        var client: AnalyticsClient? = analyticsClient
        val provider = AnalyticsClientExposureTrackingProvider { client }
        client = null

        provider.track(
            Exposure(
                flagKey = "flag-key",
                variant = "treatment",
                experimentKey = null,
            ),
        )

        verify(exactly = 0) { analyticsClient.track(any(), any()) }
    }

    private fun createContext(): AmplitudeContext {
        return AmplitudeContext(
            apiKey = API_KEY,
            instanceName = ExperimentConfig.Defaults.INSTANCE_NAME,
            serverZone = CoreServerZone.US,
            logger = logger,
            remoteConfigClient = remoteConfigClient,
            diagnosticsClient = diagnosticsClient,
        )
    }

    private fun setExperimentClient(
        plugin: AmplitudeExperimentPlugin,
        client: ExperimentClient,
    ) {
        val field = AmplitudeExperimentPlugin::class.java.getDeclaredField("experimentClient")
        field.isAccessible = true
        field.set(plugin, client)
    }
}
