package io.github.brahyam.gateway.client

import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder

/**
 * Configuration for AI service providers
 */
public data class ServiceProviderConfig(
    val name: String,
    val baseUrl: String,
    val openAiCompatiblePath: String = "/v1/",
)

/**
 * Base class for unprotected AI service implementations
 */
internal abstract class BaseDirectService(
    protected val openAiConfig: OpenAIConfig,
    protected val providerConfig: ServiceProviderConfig,
) : OpenAI by OpenAI(config = openAiConfig)

/**
 * Base class for protected AI service implementations with Gateway attestation
 */
internal abstract class BaseProtectedService(
    protected val openAiConfig: OpenAIConfig,
    protected val gatewayImpl: GatewayImpl,
    protected val providerConfig: ServiceProviderConfig,
) : OpenAI by OpenAI(
    config = openAiConfig,
    httpClientApplicator = {
        plugin(HttpSend).intercept { request ->
            request.addGatewayHeaders(gatewayImpl, providerConfig)
            execute(request)
        }
    }
)

// Add expect function for device type
internal expect fun getDeviceType(): String

/**
 * Adds Gateway-specific headers to the HTTP request
 */
internal suspend fun HttpRequestBuilder.addGatewayHeaders(
    gatewayImpl: GatewayImpl,
    providerConfig: ServiceProviderConfig,
    token: String? = null,
) {
    val integrityToken = gatewayImpl.getIntegrityToken()
    headers.append("gateway-integrity", integrityToken)
    headers.append("gateway-sdk-version", Gateway.VERSION)
    headers.append("gateway-device-type", getDeviceType())
    headers.append("gateway-provider", providerConfig.name)

    // Add authorization header if token is provided
    token?.let { headers.append("Authorization", "Bearer $it") }

    // Add anonymous id header if enabled
    try {
        val anonId = gatewayImpl.getAnonymousId()
        headers.append("gateway-anonymous-id", anonId)
    } catch (_: NotImplementedError) {
    } catch (_: IllegalStateException) {
    }
} 