package com.amplitude.experiment.util

import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.min

internal fun ScheduledExecutorService.backoff(
    config: BackoffConfig,
    function: () -> Unit,
) = Backoff(config, this).apply {
    start(function)
}

internal data class BackoffConfig(
    val attempts: Long,
    val min: Long,
    val max: Long,
    val scalar: Float,
)

internal class Backoff constructor(
    private val config: BackoffConfig,
    private val executorService: ScheduledExecutorService,
) {
    private val lock = Any()
    private var started = false
    private var cancelled = false
    private var future: Future<*>? = null

    fun start(function: () -> Unit) =
        synchronized(lock) {
            if (started) {
                return
            }
            started = true
            backoff(0, config.min, function)
        }

    fun cancel() =
        synchronized(lock) {
            if (!cancelled) {
                cancelled = true
                future?.cancel(true)
            }
        }

    private fun backoff(
        attempt: Int,
        delay: Long,
        function: () -> Unit,
    ): Unit =
        synchronized(lock) {
            future =
                executorService.schedule(
                    {
                        if (cancelled) {
                            return@schedule
                        }
                        try {
                            function.invoke()
                        } catch (e: Exception) {
                            // Retry the request function
                            val nextAttempt = attempt + 1
                            if (nextAttempt < config.attempts) {
                                val nextDelay = min(delay * config.scalar, config.max.toFloat()).toLong()
                                backoff(nextAttempt, nextDelay, function)
                            }
                        }
                    },
                    delay, TimeUnit.MILLISECONDS,
                )
        }
}
