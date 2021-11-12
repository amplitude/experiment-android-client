package com.amplitude.experiment.util

sealed class LockResult<T> {
    data class Success<T>(val value: T) : LockResult<T>()
    data class Error<T>(val error: Exception) : LockResult<T>()
}

internal class Lock<T> {

    private val lock = Object()
    private var result: LockResult<T>? = null

    fun wait(): LockResult<T> {
        return synchronized(lock) {
            while (result == null) {
                try {
                    lock.wait()
                } catch (e: InterruptedException) {
                    result = LockResult.Error(e)
                }
            }
            result!!
        }
    }

    fun notify(result: LockResult<T>) {
        synchronized(lock) {
            this.result = result
            lock.notifyAll()
        }
    }
}