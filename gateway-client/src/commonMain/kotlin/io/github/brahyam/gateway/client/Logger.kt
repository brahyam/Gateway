package io.github.brahyam.gateway.client

public enum class LogLevel {
    VERBOSE, DEBUG, INFO, WARN, ERROR
}

public interface Logger {
    public fun log(message: String, level: LogLevel = LogLevel.INFO)
    public fun verbose(message: String): Unit = log(message, LogLevel.VERBOSE)
    public fun debug(message: String): Unit = log(message, LogLevel.DEBUG)
    public fun info(message: String): Unit = log(message, LogLevel.INFO)
    public fun warn(message: String): Unit = log(message, LogLevel.WARN)
    public fun error(message: String): Unit = log(message, LogLevel.ERROR)
}

internal class PrintlnLogger(private val minLevel: LogLevel = LogLevel.INFO) : Logger {
    override fun log(message: String, level: LogLevel) {
        if (level.ordinal >= minLevel.ordinal) {
            println("[Gateway][${level.name}] $message")
        }
    }

    override fun verbose(message: String) = log(message, LogLevel.VERBOSE)
    override fun debug(message: String) = log(message, LogLevel.DEBUG)
    override fun info(message: String) = log(message, LogLevel.INFO)
    override fun warn(message: String) = log(message, LogLevel.WARN)
    override fun error(message: String) = log(message, LogLevel.ERROR)
}
