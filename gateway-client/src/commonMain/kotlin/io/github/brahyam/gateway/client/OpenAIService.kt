package io.github.brahyam.gateway.client

import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
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
    private val openAiConfig: OpenAIConfig,
) : OpenAIService, OpenAI by OpenAI(
    config = openAiConfig
)

/**
 * Protected OpenAI service implementation that uses Gateway's attestation and protection mechanisms.
 */
internal class ProtectedOpenAIService(
    private val openAiConfig: OpenAIConfig,
    private val gatewayImpl: GatewayImpl,
) : OpenAIService, OpenAI by OpenAI(
    config = openAiConfig,
    httpClientApplicator = {
        plugin(HttpSend).intercept { request ->
            val integrityToken = gatewayImpl.getIntegrityToken()
            request.headers.append("gateway-integrity", integrityToken)
            execute(request)
        }
    }
)
