package com.amplitude.experiment.util

import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

internal class Poller(
    private val executorService: ScheduledExecutorService,
    private val action: ()->Unit,
    private val ms: Long
) {
    private var future : ScheduledFuture<*>? = null

    internal fun start() {
        future = this.executorService.scheduleAtFixedRate(
            action, ms, ms, TimeUnit.MILLISECONDS
        )
    }

    internal fun stop() {
        future?.cancel(true)
        future = null
    }

}
