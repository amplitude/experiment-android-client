package com.amplitude.experiment.util

import okhttp3.Call
import java.lang.NullPointerException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

internal class AsyncFuture<T>(private val call: Call? = null): Future<T> {

    @Volatile private var value: T? = null
    @Volatile private var completed = false
    @Volatile private var throwable: Throwable? = null
    private val lock = Object()

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        call?.cancel()
        return true
    }

    override fun isCancelled(): Boolean {
        return call?.isCanceled() ?: false
    }

    override fun isDone(): Boolean {
        return completed
    }

    @Throws(InterruptedException::class, ExecutionException::class)
    override fun get(): T {
        synchronized(lock) {
            while (!completed) {
                lock.wait()
            }
        }
        if (throwable != null) {
            throw ExecutionException(throwable)
        }
        return value
            ?: throw ExecutionException(NullPointerException("Future value must not be null"))
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    override fun get(timeout: Long, unit: TimeUnit): T {
        var nanosRemaining = unit.toNanos(timeout)
        val end = System.nanoTime() + nanosRemaining
        synchronized(lock) {
            while (!completed && nanosRemaining > 0) {
                TimeUnit.NANOSECONDS.timedWait(lock, nanosRemaining)
                nanosRemaining = end - System.nanoTime()
            }
        }
        if (!completed) {
            throw TimeoutException()
        }
        if (throwable != null) {
            throw ExecutionException(throwable)
        }
        return value
            ?: throw ExecutionException(NullPointerException("Future value must not be null"))
    }

    @Synchronized
    internal fun complete(value: T) {
        if (!completed) {
            this.value = value
            synchronized(lock) {
                completed = true
                lock.notifyAll()
            }
        }
    }

    @Synchronized
    internal fun completeExceptionally(ex: Throwable) {
        if (!completed) {
            throwable = ex
            synchronized(lock) {
                completed = true
                lock.notifyAll()
            }
        }
    }
}