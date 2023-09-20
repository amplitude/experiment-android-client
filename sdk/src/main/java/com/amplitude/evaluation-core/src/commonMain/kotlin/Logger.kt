package com.amplitude.experiment.evaluation

enum class Level {
    VERBOSE,
    DEBUG,
    INFO,
    WARN,
    ERROR
}

interface Logger {
    fun verbose(log: () -> String)
    fun debug(log: () -> String)
    fun info(log: () -> String)
    fun warn(e: Throwable? = null, log: () -> String)
    fun error(e: Throwable? = null, log: () -> String)
}

class DefaultLogger(
    private val level: Level = Level.ERROR,
    private val tag: String = "Experiment"
) : Logger {

    override fun verbose(log: () -> String) {
        if (level <= Level.VERBOSE) {
            println("VERBOSE [$tag] ${log.invoke()}")
        }
    }

    override fun debug(log: () -> String) {
        if (level <= Level.DEBUG) {
            println("DEBUG [$tag] ${log.invoke()}")
        }
    }

    override fun info(log: () -> String) {
        if (level <= Level.INFO) {
            println("INFO [$tag] ${log.invoke()}")
        }
    }

    override fun warn(e: Throwable?, log: () -> String) {
        if (level <= Level.WARN) {
            if (e == null) {
                println("WARN [$tag] ${log.invoke()}")
            } else {
                println("WARN [$tag] ${log.invoke()}\n${e.printStackTrace()}")
            }
        }
    }

    override fun error(e: Throwable?, log: () -> String) {
        if (level <= Level.ERROR) {
            if (e == null) {
                println("ERROR [$tag] ${log.invoke()}")
            } else {
                println("ERROR [$tag] ${log.invoke()}\n$e")
            }
        }
    }
}
