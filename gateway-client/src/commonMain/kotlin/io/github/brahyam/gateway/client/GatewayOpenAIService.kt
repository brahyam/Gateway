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
internal class GatewayDirectOpenAIService(
    private val openAiConfig: OpenAIConfig,
) : OpenAIService, OpenAI by OpenAI(
    config = openAiConfig
)

/**
 * Protected OpenAI service implementation that uses Gateway's attestation and protection mechanisms.
 */
internal class GatewayOpenAIService(
    private val openAiConfig: OpenAIConfig,
    private val gatewayImpl: GatewayImpl,
) : OpenAIService, OpenAI by OpenAI(
    config = openAiConfig,
    httpClientApplicator = {
        plugin(HttpSend).intercept { request ->
            val integrityToken = gatewayImpl.getIntegrityToken()
            request.headers.append("gateway-integrity", integrityToken)
            request.headers.append("gateway-sdk-version", Gateway.VERSION)
            request.headers.append("gateway-device-type", getDeviceType())
            // Add anonymous id header if enabled
            try {
                val anonId = gatewayImpl.getAnonymousId()
                request.headers.append("gateway-anonymous-id", anonId)
            } catch (_: NotImplementedError) {
            } catch (_: IllegalStateException) {
            }
            execute(request)
        }
    }
)

// Add expect function for device type
internal expect fun getDeviceType(): String
