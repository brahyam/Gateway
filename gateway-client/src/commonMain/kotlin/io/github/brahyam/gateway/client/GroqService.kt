package io.github.brahyam.gateway.client

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.ProxyConfig
import com.aallam.openai.client.RetryStrategy
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import kotlin.time.Duration.Companion.seconds

/**
 * Interface for Groq service implementations.
 */
public interface GroqService : OpenAI

/**
 * Groq service provider configuration
 */
public object GroqProvider {
    public val CONFIG: ServiceProviderConfig = ServiceProviderConfig(
        name = "Groq",
        baseUrl = "api.groq.com",
        openAiCompatiblePath = "/openai/v1/"
    )
}

/**
 * Unprotected Groq service implementation for BYOK (Bring Your Own Key) use cases.
 */
internal class GatewayDirectGroqService(
    openAiConfig: OpenAIConfig,
) : BaseDirectService(openAiConfig, GroqProvider.CONFIG), GroqService

/**
 * Protected Groq service implementation that uses Gateway's attestation and protection mechanisms.
 */
internal class GatewayGroqService(
    openAiConfig: OpenAIConfig,
    gatewayImpl: GatewayImpl,
) : BaseProtectedService(openAiConfig, gatewayImpl, GroqProvider.CONFIG), GroqService

/**
 * Get an unprotected Groq service instance for BYOK (Bring Your Own Key) use cases.
 * This should only be used in development or when you want your users to add their own Groq API key.
 *
 * @param apiKey Your Groq API key
 * @param logging client logging configuration
 * @param timeout http client timeout
 * @param organization organization ID
 * @param headers extra http headers
 * @param proxy HTTP proxy configuration
 * @param retry rate limit retry configuration
 * @param engine explicit ktor engine for http requests
 * @param httpClientConfig additional custom client configuration
 * @return A Groq service instance
 */
public fun Gateway.createDirectGroqService(
    apiKey: String,
    logging: LoggingConfig = LoggingConfig(),
    timeout: Timeout = Timeout(socket = 30.seconds),
    organization: String? = null,
    headers: Map<String, String> = emptyMap(),
    proxy: ProxyConfig? = null,
    retry: RetryStrategy = RetryStrategy(),
    engine: HttpClientEngine? = null,
    httpClientConfig: HttpClientConfig<*>.() -> Unit = {},
): GroqService {
    return createDirectService(
        apiKey = apiKey,
        providerConfig = GroqProvider.CONFIG,
        logging = logging,
        timeout = timeout,
        organization = organization,
        headers = headers,
        proxy = proxy,
        retry = retry,
        engine = engine,
        httpClientConfig = httpClientConfig
    ) { config ->
        GatewayDirectGroqService(config)
    }
}

/**
 * Get a protected Groq service instance for production use cases.
 * This uses the Gateway's attestation and protection mechanisms.
 *
 * @param partialKey Partial key from your Gateway developer dashboard
 * @param serviceURL Service URL from your Gateway developer dashboard
 * @param logging Client logging configuration
 * @param timeout HTTP client timeout
 * @param organization organization ID
 * @param headers Extra HTTP headers
 * @param proxy HTTP proxy configuration
 * @param retry Rate limit retry configuration
 * @param enableCertPinning Whether to enable certificate pinning
 * @return A protected Groq service instance
 *
 */
public fun Gateway.createGroqService(
    partialKey: String,
    serviceURL: String,
    logging: LoggingConfig = LoggingConfig(),
    timeout: Timeout = Timeout(socket = 30.seconds),
    organization: String? = null,
    headers: Map<String, String> = emptyMap(),
    proxy: ProxyConfig? = null,
    retry: RetryStrategy = RetryStrategy(),
    enableCertPinning: Boolean = true,
): GroqService {
    return createProtectedService(
        partialKey = partialKey,
        serviceURL = serviceURL,
        providerConfig = GroqProvider.CONFIG,
        logging = logging,
        timeout = timeout,
        organization = organization,
        headers = headers,
        proxy = proxy,
        retry = retry,
        enableCertPinning = enableCertPinning
    ) { config, gatewayImpl ->
        GatewayGroqService(config, gatewayImpl)
    }
} 