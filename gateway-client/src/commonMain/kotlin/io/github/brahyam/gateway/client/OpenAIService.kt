package io.github.brahyam.gateway.client

import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin

/**
 * Interface for OpenAI service implementations.
 */
public interface OpenAIService : OpenAI

/**
 * Unprotected OpenAI service implementation for BYOK (Bring Your Own Key) use cases.
 */
internal class UnprotectedOpenAIService(
    private val apiKey: String,
    private val httpClientConfig: HttpClientConfig<*>.() -> Unit = {},
) : OpenAIService, OpenAI by OpenAI(
    token = apiKey,
    httpClientConfig = httpClientConfig
)

/**
 * Protected OpenAI service implementation that uses Gateway's attestation and protection mechanisms.
 */
internal class ProtectedOpenAIService(
    private val partialKey: String,
    private val serviceURL: String,
    private val gatewayImpl: GatewayImpl,
) : OpenAIService, OpenAI by OpenAI(
    config = OpenAIConfig(
        token = partialKey,
        host = OpenAIHost(serviceURL),
        engine = createPinnedEngine()
    ),
    httpClientApplicator = {
        plugin(HttpSend).intercept { request ->
            val integrityToken = gatewayImpl.getIntegrityToken()
            request.headers.append("gateway-integrity", integrityToken)
            execute(request)
        }
    }
)
