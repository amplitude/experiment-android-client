package com.amplitude.experiment.util

import java.util.concurrent.TimeoutException

internal sealed class LockResult<T> {
    data class Success<T>(val value: T) : LockResult<T>()

    data class Error<T>(val error: Exception) : LockResult<T>()
}

internal class Lock<T> {
    private val lock = Object()
    private var result: LockResult<T>? = null

    fun wait(ms: Long = 0): LockResult<T> {
        return synchronized(lock) {
            val start = System.currentTimeMillis()
            var current = start
            while ((current - start < ms || ms == 0L) && result == null) {
                try {
                    lock.wait(ms)
                    current = System.currentTimeMillis()
                } catch (e: InterruptedException) {
                    result = LockResult.Error(e)
                }
            }
            result ?: LockResult.Error(
                TimeoutException("Lock timed out waiting $ms ms for notify."),
            )
        }
    }

    fun notify(result: LockResult<T>) {
        synchronized(lock) {
            this.result = result
            lock.notifyAll()
        }
    }
}
