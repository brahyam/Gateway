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
 * Interface for AI/ML API service implementations.
 */
public interface AimlApiService : OpenAI

/**
 * AI/ML API service provider configuration
 */
public object AimlApiProvider {
    public val CONFIG: ServiceProviderConfig = ServiceProviderConfig(
        name = "AI/ML API",
        proxyDomain = "api.aimlapi.com/v1"
    )
}

/**
 * Unprotected AI/ML API service implementation for BYOK (Bring Your Own Key) use cases.
 */
internal class GatewayDirectAimlApiService(
    openAiConfig: OpenAIConfig,
) : BaseDirectService(openAiConfig, AimlApiProvider.CONFIG), AimlApiService

/**
 * Protected AI/ML API service implementation that uses Gateway's attestation and protection mechanisms.
 */
internal class GatewayAimlApiService(
    openAiConfig: OpenAIConfig,
    gatewayImpl: GatewayImpl,
) : BaseProtectedService(openAiConfig, gatewayImpl, AimlApiProvider.CONFIG), AimlApiService

/**
 * Get an unprotected AI/ML API service instance for BYOK (Bring Your Own Key) use cases.
 * This should only be used in development or when you want your users to add their own AI/ML API key.
 *
 * @param apiKey Your AI/ML API key
 * @param logging client logging configuration
 * @param timeout http client timeout
 * @param organization organization ID
 * @param headers extra http headers
 * @param proxy HTTP proxy configuration
 * @param retry rate limit retry configuration
 * @param engine explicit ktor engine for http requests
 * @param httpClientConfig additional custom client configuration
 * @return An AI/ML API service instance
 */
public fun Gateway.createDirectAimlApiService(
    apiKey: String,
    logging: LoggingConfig = LoggingConfig(),
    timeout: Timeout = Timeout(socket = 30.seconds),
    organization: String? = null,
    headers: Map<String, String> = emptyMap(),
    proxy: ProxyConfig? = null,
    retry: RetryStrategy = RetryStrategy(),
    engine: HttpClientEngine? = null,
    httpClientConfig: HttpClientConfig<*>.() -> Unit = {},
): AimlApiService {
    return createDirectService(
        apiKey = apiKey,
        providerConfig = AimlApiProvider.CONFIG,
        logging = logging,
        timeout = timeout,
        organization = organization,
        headers = headers,
        proxy = proxy,
        retry = retry,
        engine = engine,
        httpClientConfig = httpClientConfig
    ) { config ->
        GatewayDirectAimlApiService(config)
    }
}

/**
 * Get a protected AI/ML API service instance for production use cases.
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
 * @return A protected AI/ML API service instance
 *
 */
public fun Gateway.createAimlApiService(
    partialKey: String,
    serviceURL: String,
    logging: LoggingConfig = LoggingConfig(),
    timeout: Timeout = Timeout(socket = 30.seconds),
    organization: String? = null,
    headers: Map<String, String> = emptyMap(),
    proxy: ProxyConfig? = null,
    retry: RetryStrategy = RetryStrategy(),
    enableCertPinning: Boolean = true,
): AimlApiService {
    return createProtectedService(
        partialKey = partialKey,
        serviceURL = serviceURL,
        providerConfig = AimlApiProvider.CONFIG,
        logging = logging,
        timeout = timeout,
        organization = organization,
        headers = headers,
        proxy = proxy,
        retry = retry,
        enableCertPinning = enableCertPinning
    ) { config, gatewayImpl ->
        GatewayAimlApiService(config, gatewayImpl)
    }
} 