package com.amplitude.experiment.util

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LoggerTest {
    private lateinit var mockProvider: LoggerProvider

    @Before
    fun setup() {
        mockProvider = mockk(relaxed = true)
        AmpLogger.configure(LogLevel.ERROR, null)
    }

    @After
    fun teardown() {
        AmpLogger.configure(LogLevel.ERROR, null)
        clearMocks(mockProvider)
    }

    // ========== AmpLogger Configuration Tests ==========

    @Test
    fun `test configure sets log level and provider`() {
        val configs =
            listOf(
                LogLevel.DEBUG to mockProvider,
                LogLevel.INFO to null,
                LogLevel.VERBOSE to mockProvider,
            )

        configs.forEach { (level, provider) ->
            AmpLogger.configure(level, provider)
            assertEquals(level, AmpLogger.logLevel)
            assertEquals(provider, AmpLogger.loggerProvider)
        }
    }

    @Test
    fun `test log level filtering behavior`() {
        // Define expected behavior: for each log level, which methods should be called
        val testCases =
            mapOf(
                LogLevel.DISABLE to LogCallExpectations(0, 0, 0, 0, 0),
                LogLevel.ERROR to LogCallExpectations(0, 0, 0, 0, 1),
                LogLevel.WARN to LogCallExpectations(0, 0, 0, 1, 1),
                LogLevel.INFO to LogCallExpectations(0, 0, 1, 1, 1),
                LogLevel.DEBUG to LogCallExpectations(0, 1, 1, 1, 1),
                LogLevel.VERBOSE to LogCallExpectations(1, 1, 1, 1, 1),
            )

        testCases.forEach { (level, expectations) ->
            clearMocks(mockProvider, answers = false)
            AmpLogger.configure(level, mockProvider)

            logAllLevels("test")

            verifyLogCalls("test", expectations)
        }
    }

    // ========== Null Provider Tests ==========

    @Test
    fun `test logging with null provider does not throw`() {
        val levels = listOf(LogLevel.VERBOSE, LogLevel.ERROR, LogLevel.DISABLE)

        levels.forEach { level ->
            AmpLogger.configure(level, null)

            // Should not throw exceptions
            logAllLevels("test")
        }
    }

    // ========== Message Content Tests ==========

    @Test
    fun `test log messages are preserved correctly`() {
        AmpLogger.configure(LogLevel.INFO, mockProvider)

        val messages =
            listOf(
                "simple message",
                "message with numbers: 123",
                "message with special chars: !@#$%",
                "message with unicode: 你好",
                "",
            )

        messages.forEach { msg ->
            clearMocks(mockProvider, answers = false)
            AmpLogger.info(msg)
            verify(exactly = 1) { mockProvider.info(msg) }
        }
    }

    @Test
    fun `test provider not called when logs are filtered`() {
        AmpLogger.configure(LogLevel.ERROR, mockProvider)

        // These should be filtered
        listOf(
            AmpLogger::verbose,
            AmpLogger::debug,
            AmpLogger::info,
            AmpLogger::warn,
        ).forEach { it("msg") }

        AmpLogger.error("error msg")

        verify(exactly = 1) { mockProvider.error("error msg") }
        verify(exactly = 0) { mockProvider.verbose(any()) }
        verify(exactly = 0) { mockProvider.debug(any()) }
        verify(exactly = 0) { mockProvider.info(any()) }
        verify(exactly = 0) { mockProvider.warn(any()) }
    }

    // ========== Helper Methods ==========

    private fun logAllLevels(message: String) {
        AmpLogger.verbose(message)
        AmpLogger.debug(message)
        AmpLogger.info(message)
        AmpLogger.warn(message)
        AmpLogger.error(message)
    }

    private fun verifyLogCalls(
        msg: String,
        expectations: LogCallExpectations,
    ) {
        verify(exactly = expectations.verbose) { mockProvider.verbose(msg) }
        verify(exactly = expectations.debug) { mockProvider.debug(msg) }
        verify(exactly = expectations.info) { mockProvider.info(msg) }
        verify(exactly = expectations.warn) { mockProvider.warn(msg) }
        verify(exactly = expectations.error) { mockProvider.error(msg) }
    }

    private data class LogCallExpectations(
        val verbose: Int,
        val debug: Int,
        val info: Int,
        val warn: Int,
        val error: Int,
    )
}
