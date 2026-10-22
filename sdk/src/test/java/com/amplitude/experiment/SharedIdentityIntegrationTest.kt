package com.amplitude.experiment

import android.content.Context
import com.amplitude.core.Amplitude
import com.amplitude.core.Configuration
import com.amplitude.core.State
import com.amplitude.core.events.Identify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64
import java.util.concurrent.TimeUnit

internal class SharedIdentityIntegrationTest {
    @Test
    fun `real Analytics identity updates fetch once with set unset and clear properties`() {
        val server = MockWebServer()
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse =
                    MockResponse().setBody(if (request.path?.startsWith("/sdk/v2/flags") == true) "[]" else "{}")
            }
        server.start()
        val applicationContext = mockk<Context>(relaxed = true)
        every { applicationContext.applicationContext } returns applicationContext
        every { applicationContext.getSharedPreferences(any(), any()) } returns mockk(relaxed = true)

        val analytics =
            object : Amplitude(
                Configuration(
                    apiKey = "integration-test-key",
                    offline = true,
                    deviceId = "device-1",
                    enableDiagnostics = false,
                ),
                State(),
            ) {
                override val sessionId: Long = 42L
            }
        val plugin =
            AmplitudeExperimentPlugin(
                applicationContext,
                ExperimentConfig(
                    serverUrl = server.url("/").toString(),
                    flagsServerUrl = server.url("/").toString(),
                    automaticFetchOnAmplitudeIdentityChange = true,
                    fetchOnStart = false,
                    pollOnStart = false,
                ),
            )

        try {
            assertTrue(runBlocking { analytics.isBuilt.await() })
            analytics.add(plugin)
            assertEquals("/sdk/v2/flags", requireNotNull(server.takeRequest(5, TimeUnit.SECONDS)).requestUrl?.encodedPath)

            analytics.identify(Identify().set("plan", "paid"))
            val first = fetchProperties(server)
            analytics.identify(Identify().unset("plan"))
            val second = fetchProperties(server)
            analytics.identify(Identify().set("tier", "gold"))
            val third = fetchProperties(server)
            analytics.identify(Identify().clearAll())
            val fourth = fetchProperties(server)

            assertEquals(5, server.requestCount)
            assertEquals("paid", first.getString("plan"))
            assertFalse(second.has("plan"))
            assertEquals("gold", third.getString("tier"))
            assertFalse(fourth.has("tier"))
            assertEquals(42L, fourth.getLong("session_id"))
        } finally {
            plugin.teardown()
            analytics.amplitudeScope.cancel()
            server.shutdown()
        }
    }

    private fun fetchProperties(server: MockWebServer): JSONObject {
        val request = requireNotNull(server.takeRequest(5, TimeUnit.SECONDS))
        assertEquals("/sdk/v2/vardata", request.requestUrl?.encodedPath)
        val user = String(Base64.getUrlDecoder().decode(requireNotNull(request.getHeader("X-Amp-Exp-User"))))
        return JSONObject(user).getJSONObject("user_properties")
    }
}
