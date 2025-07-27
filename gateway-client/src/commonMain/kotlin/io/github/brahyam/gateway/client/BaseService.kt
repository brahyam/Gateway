package io.github.brahyam.gateway.client

import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin

/**
 * Configuration for AI service providers
 */
public data class ServiceProviderConfig(
    val name: String,
    val proxyDomain: String,
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
            val integrityToken = gatewayImpl.getIntegrityToken()
            request.headers.append("gateway-integrity", integrityToken)
            request.headers.append("gateway-sdk-version", Gateway.VERSION)
            request.headers.append("gateway-device-type", getDeviceType())
            request.headers.append("gateway-provider", providerConfig.name)
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