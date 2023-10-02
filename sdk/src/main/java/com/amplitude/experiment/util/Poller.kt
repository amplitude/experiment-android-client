package com.amplitude.experiment.util

import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Poller(
    private val action: () -> Unit,
    private val ms: Long
) {
    private var job: Job? = null
    private val scope = MainScope()

    internal fun start(): Unit {
        job = scope.launch {
            while (true) {
                action()
                delay(ms)
            }
        }
    }

    internal fun stop() {
        job?.cancel()
        job = null
    }

}
