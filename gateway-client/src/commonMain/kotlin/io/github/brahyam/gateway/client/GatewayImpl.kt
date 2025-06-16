package io.github.brahyam.gateway.client

import com.aallam.openai.api.logging.LogLevel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal class GatewayImpl(
    private val config: GatewayConfig,
) {
    internal val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = when (config.logLevel) {
                LogLevel.All -> io.ktor.client.plugins.logging.LogLevel.ALL
                LogLevel.Headers -> io.ktor.client.plugins.logging.LogLevel.HEADERS
                LogLevel.Body -> io.ktor.client.plugins.logging.LogLevel.BODY
                LogLevel.Info -> io.ktor.client.plugins.logging.LogLevel.INFO
                LogLevel.None -> io.ktor.client.plugins.logging.LogLevel.NONE
            }
        }
    }
} 