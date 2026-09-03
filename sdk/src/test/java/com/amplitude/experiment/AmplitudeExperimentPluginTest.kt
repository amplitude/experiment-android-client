package com.amplitude.experiment

import com.amplitude.analytics.connector.AnalyticsConnector
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
    fun `user provider falls back to connector user properties when identity map is empty`() {
        val instanceName = "connector-props-instance"
        val emptyIdentity =
            object : AnalyticsIdentity {
                override val userId: String? = "user-1"
                override val deviceId: String? = "device-1"
            }
        every { analyticsClient.identity } returns emptyIdentity

        AnalyticsConnector.getInstance(instanceName).identityStore
            .editIdentity()
            .setUserId("user-1")
            .setDeviceId("device-1")
            .setUserProperties(mapOf("plan" to "enterprise"))
            .commit()

        val provider =
            AnalyticsClientUserProvider(
                applicationContext,
                { analyticsClient },
                instanceName,
            )

        val user = provider.getUser()

        Assert.assertEquals("enterprise", user.userProperties?.get("plan"))
    }

    @Test
    fun `user provider prefers identity user properties over connector`() {
        val instanceName = "identity-props-instance"
        AnalyticsConnector.getInstance(instanceName).identityStore
            .editIdentity()
            .setUserProperties(mapOf("plan" to "connector"))
            .commit()

        val provider =
            AnalyticsClientUserProvider(
                applicationContext,
                { analyticsClient },
                instanceName,
            )

        val user = provider.getUser()

        Assert.assertEquals("pro", user.userProperties?.get("plan"))
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
    fun `automatic fetch registers connector identity listener for property updates`() {
        val instanceName = "auto-fetch-instance"
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

        plugin.setup(analyticsClient, createContext(instanceName = instanceName))
        val spyClient = spyk(plugin.experimentClient as DefaultExperimentClient)
        setExperimentClient(plugin, spyClient)

        AnalyticsConnector.getInstance(instanceName).identityStore
            .editIdentity()
            .setUserProperties(mapOf("plan" to "enterprise"))
            .commit()

        verify(timeout = 1_000) { spyClient.setUser(any()) }
        verify(timeout = 1_000) { spyClient.fetch(any()) }
    }

    @Test
    fun `connector property update does not fetch before experiment starts`() {
        val instanceName = "connector-before-start-instance"
        every { analyticsClient.sessionId } returns -1L
        every { analyticsClient.identity } returns
            object : AnalyticsIdentity {
                override val userId: String? = "user-1"
                override val deviceId: String? = "device-1"
            }
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

        plugin.setup(analyticsClient, createContext(instanceName = instanceName))
        val spyClient = spyk(plugin.experimentClient as DefaultExperimentClient)
        setExperimentClient(plugin, spyClient)

        AnalyticsConnector.getInstance(instanceName).identityStore
            .editIdentity()
            .setUserProperties(mapOf("plan" to "enterprise"))
            .commit()

        verify(exactly = 0) { spyClient.fetch(any()) }

        every { analyticsClient.sessionId } returns 42L
        plugin.onSessionIdChanged(42L)

        Assert.assertEquals("enterprise", spyClient.getUser()?.userProperties?.get("plan"))
        verify { spyClient.start(any()) }
    }

    @Test
    fun `connector property updates while opted out do not refetch on opt-in`() {
        val instanceName = "connector-opt-out-snapshot-instance"
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

        plugin.setup(analyticsClient, createContext(instanceName = instanceName))
        val spyClient = spyk(plugin.experimentClient as DefaultExperimentClient)
        setExperimentClient(plugin, spyClient)

        plugin.onOptOutChanged(true)
        AnalyticsConnector.getInstance(instanceName).identityStore
            .editIdentity()
            .setUserProperties(mapOf("plan" to "enterprise"))
            .commit()
        verify(exactly = 0) { spyClient.fetch(any()) }

        plugin.onOptOutChanged(false)
        verify { spyClient.start(any()) }

        AnalyticsConnector.getInstance(instanceName).identityStore
            .editIdentity()
            .setUserProperties(mapOf("plan" to "enterprise"))
            .commit()
        verify(exactly = 0) { spyClient.fetch(any()) }
    }

    @Test
    fun `teardown removes connector identity listener`() {
        val instanceName = "teardown-listener-instance"
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

        plugin.setup(analyticsClient, createContext(instanceName = instanceName))
        Assert.assertNotNull(getPrivateField(plugin, "connectorIdentityListener"))

        plugin.teardown()

        Assert.assertNull(plugin.experimentClient)
        Assert.assertNull(getPrivateField(plugin, "connectorIdentityListener"))
    }

    @Test
    fun `setup starts experiment client when host identity is ready`() {
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
    fun `setup does not start when host session is uninitialized`() {
        every { analyticsClient.sessionId } returns -1L
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
    fun `setup does not start when host device id is missing`() {
        every { analyticsClient.identity } returns
            object : AnalyticsIdentity {
                override val userId: String? = "user-1"
                override val deviceId: String? = null
            }
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
    fun `first start happens after identity and session become ready`() {
        every { analyticsClient.sessionId } returns -1L
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
        Assert.assertNull(plugin.experimentClient?.getUser())

        val spyClient = spyk(plugin.experimentClient as DefaultExperimentClient)
        setExperimentClient(plugin, spyClient)
        every { analyticsClient.sessionId } returns 42L

        plugin.onSessionIdChanged(42L)

        verify { spyClient.start(any()) }
        Assert.assertEquals("user-1", spyClient.getUser()?.userId)
        Assert.assertEquals("device-1", spyClient.getUser()?.deviceId)
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
    fun `onIdentityChanged does not fetch when automatic fetch is off`() {
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

        plugin.onIdentityChanged(
            object : AnalyticsIdentity {
                override val userId: String? = "user-2"
                override val deviceId: String? = "device-2"
            },
        )

        Assert.assertEquals("user-2", spyClient.getUser()?.userId)
        verify(exactly = 0) { spyClient.fetch(any()) }
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
    fun `connector identity listener does not fetch on user or device id change`() {
        val instanceName = "no-double-fetch-instance"
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

        plugin.setup(analyticsClient, createContext(instanceName = instanceName))
        val spyClient = spyk(plugin.experimentClient as DefaultExperimentClient)
        setExperimentClient(plugin, spyClient)

        AnalyticsConnector.getInstance(instanceName).identityStore
            .editIdentity()
            .setUserId("user-2")
            .setDeviceId("device-2")
            .commit()

        verify(exactly = 0) { spyClient.fetch(any()) }
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
    fun `onReset clears cached variants without fetch when automatic fetch is off`() {
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
        verify(exactly = 0) { spyClient.fetch(any()) }
    }

    @Test
    fun `onReset does not duplicate automatic identity fetch`() {
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

        plugin.onIdentityChanged(identity)
        plugin.onReset()

        verify { spyClient.clear() }
        verify(exactly = 1) { spyClient.fetch(any()) }
    }

    @Test
    fun `onReset does not fetch while opted out`() {
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
        plugin.onReset()

        verify { spyClient.clear() }
        verify(exactly = 0) { spyClient.fetch(any()) }
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

        plugin.teardown()
        Assert.assertNull(plugin.experimentClient)
    }

    @Test
    fun `plugins with the same instance own separate clients`() {
        val context = createContext(instanceName = "separate-plugin-clients")
        val firstPlugin =
            AmplitudeExperimentPlugin(
                applicationContext,
                ExperimentConfig(
                    debug = true,
                    fetchOnStart = false,
                    pollOnStart = false,
                ),
            )
        val secondPlugin =
            AmplitudeExperimentPlugin(
                applicationContext,
                ExperimentConfig(
                    debug = true,
                    fetchOnStart = false,
                    pollOnStart = false,
                ),
            )

        firstPlugin.setup(analyticsClient, context)
        secondPlugin.setup(analyticsClient, context)

        val firstClient = firstPlugin.experimentClient
        val secondClient = secondPlugin.experimentClient
        Assert.assertNotNull(firstClient)
        Assert.assertNotNull(secondClient)
        Assert.assertNotSame(firstClient, secondClient)

        firstPlugin.teardown()

        Assert.assertNull(firstPlugin.experimentClient)
        Assert.assertSame(secondClient, secondPlugin.experimentClient)
        Assert.assertEquals("user-1", secondPlugin.experimentClient?.getUser()?.userId)
    }

    @Test
    fun `plugin exposes stable name`() {
        val plugin = AmplitudeExperimentPlugin(applicationContext)
        Assert.assertEquals(AmplitudeExperimentPlugin.PLUGIN_NAME, plugin.name)
    }

    @Test
    fun `execute is a pass-through`() {
        val plugin = AmplitudeExperimentPlugin(applicationContext)
        val event =
            object : com.amplitude.core.events.AnalyticsEvent {
                override var userId: String? = "u"
                override var deviceId: String? = "d"
                override var timestamp: Long? = 1L
                override var sessionId: Long? = 2L
                override var eventType: String = "test"
                override var eventProperties: MutableMap<String, Any?>? = null
            }
        Assert.assertSame(event, plugin.execute(event))
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

    private fun createContext(instanceName: String = ExperimentConfig.Defaults.INSTANCE_NAME): AmplitudeContext {
        return AmplitudeContext(
            apiKey = API_KEY,
            instanceName = instanceName,
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

    private fun getPrivateField(
        plugin: AmplitudeExperimentPlugin,
        name: String,
    ): Any? {
        val field = AmplitudeExperimentPlugin::class.java.getDeclaredField(name)
        field.isAccessible = true
        return field.get(plugin)
    }
}
