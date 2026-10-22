package com.amplitude.experiment

import android.app.Application
import android.content.Context
import com.amplitude.experiment.util.MockStorage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class CallFactoryTest {
    @Test
    fun `default clients use the same shared http client`() {
        val context = mockk<Context>(relaxed = true)
        every { context.getSharedPreferences(any(), any()) } returns mockk(relaxed = true)
        val config = ExperimentConfig.builder().build()

        val first = Experiment.createClient(context, "first", config)
        val second = Experiment.createClient(context, "second", config)
        val shared = field(first, "httpClient")

        assertSame(shared, field(second, "httpClient"))
        assertSame(shared, field(first, "callFactory"))
        assertSame(shared, field(second, "callFactory"))
        assertSame(shared, field(field(first, "flagApi")!!, "callFactory"))
    }

    @Test
    fun `custom factory receives variant and flag requests`() {
        val factory = RecordingCallFactory()
        val defaultClient = mockk<OkHttpClient>()
        val executor = ScheduledThreadPoolExecutor(4)
        try {
            val config =
                ExperimentConfig
                    .builder()
                    .serverUrl("https://variants.example/")
                    .flagsServerUrl("https://flags.example/")
                    .retryFetchOnFailure(false)
                    .fetchOnStart(false)
                    .pollOnStart(false)
                    .callFactory(factory)
                    .build()
            val client = DefaultExperimentClient("key", config, defaultClient, MockStorage(), executor)

            expectFailure(client.fetch(ExperimentUser(userId = "user")))
            expectFailure(client.start())

            assertEquals(2, factory.requests.size)
            assertEquals("variants.example", factory.requests[0].url.host)
            assertEquals("/sdk/v2/vardata", factory.requests[0].url.encodedPath)
            assertEquals("flags.example", factory.requests[1].url.host)
            assertEquals("/sdk/v2/flags", factory.requests[1].url.encodedPath)
            verify(exactly = 0) { defaultClient.newCall(any()) }
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `call factory survives analytics initialization config copy`() {
        val application = mockk<Application>(relaxed = true)
        every { application.getSharedPreferences(any(), any()) } returns mockk(relaxed = true)
        val factory = RecordingCallFactory()
        val config =
            ExperimentConfig
                .builder()
                .instanceName("call-factory-${UUID.randomUUID()}")
                .fetchOnStart(false)
                .pollOnStart(false)
                .userProvider(mockk())
                .callFactory(factory)
                .build()

        assertSame(factory, config.copyToBuilder().build().callFactory)
        val client = Experiment.initializeWithAmplitudeAnalytics(application, "key", config)
        assertSame(factory, field(client, "callFactory"))
    }

    private fun field(
        owner: Any,
        name: String,
    ): Any? = owner.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(owner)

    private fun expectFailure(future: Future<*>) {
        try {
            future.get(2, TimeUnit.SECONDS)
            fail("Expected the recording factory to reject the request")
        } catch (_: ExecutionException) {
            // The factory deliberately rejects calls after recording the request.
        }
    }

    private class RecordingCallFactory : Call.Factory {
        val requests = CopyOnWriteArrayList<Request>()

        override fun newCall(request: Request): Call {
            requests += request
            throw IOException("Recorded request without making a network call")
        }
    }
}
