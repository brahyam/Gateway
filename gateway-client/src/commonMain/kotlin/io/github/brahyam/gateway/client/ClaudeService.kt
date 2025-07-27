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
 * Interface for Anthropic Claude service implementations.
 */
public interface ClaudeService : OpenAI

/**
 * Anthropic Claude service provider configuration
 */
public object ClaudeProvider {
    public val CONFIG: ServiceProviderConfig = ServiceProviderConfig(
        name = "Anthropic Claude",
        proxyDomain = "api.anthropic.com/v1"
    )
}

/**
 * Unprotected Anthropic Claude service implementation for BYOK (Bring Your Own Key) use cases.
 */
internal class GatewayDirectClaudeService(
    openAiConfig: OpenAIConfig,
) : BaseDirectService(openAiConfig, ClaudeProvider.CONFIG), ClaudeService

/**
 * Protected Anthropic Claude service implementation that uses Gateway's attestation and protection mechanisms.
 */
internal class GatewayClaudeService(
    openAiConfig: OpenAIConfig,
    gatewayImpl: GatewayImpl,
) : BaseProtectedService(openAiConfig, gatewayImpl, ClaudeProvider.CONFIG), ClaudeService

/**
 * Get an unprotected Anthropic Claude service instance for BYOK (Bring Your Own Key) use cases.
 * This should only be used in development or when you want your users to add their own Claude API key.
 *
 * @param apiKey Your Claude API key
 * @param logging client logging configuration
 * @param timeout http client timeout
 * @param organization organization ID
 * @param headers extra http headers
 * @param proxy HTTP proxy configuration
 * @param retry rate limit retry configuration
 * @param engine explicit ktor engine for http requests
 * @param httpClientConfig additional custom client configuration
 * @return A Claude service instance
 */
public fun Gateway.createDirectClaudeService(
    apiKey: String,
    logging: LoggingConfig = LoggingConfig(),
    timeout: Timeout = Timeout(socket = 30.seconds),
    organization: String? = null,
    headers: Map<String, String> = emptyMap(),
    proxy: ProxyConfig? = null,
    retry: RetryStrategy = RetryStrategy(),
    engine: HttpClientEngine? = null,
    httpClientConfig: HttpClientConfig<*>.() -> Unit = {},
): ClaudeService {
    return createDirectService(
        apiKey = apiKey,
        providerConfig = ClaudeProvider.CONFIG,
        logging = logging,
        timeout = timeout,
        organization = organization,
        headers = headers,
        proxy = proxy,
        retry = retry,
        engine = engine,
        httpClientConfig = httpClientConfig
    ) { config ->
        GatewayDirectClaudeService(config)
    }
}

/**
 * Get a protected Anthropic Claude service instance for production use cases.
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
 * @return A protected Anthropic Claude service instance
 *
 */
public fun Gateway.createClaudeService(
    partialKey: String,
    serviceURL: String,
    logging: LoggingConfig = LoggingConfig(),
    timeout: Timeout = Timeout(socket = 30.seconds),
    organization: String? = null,
    headers: Map<String, String> = emptyMap(),
    proxy: ProxyConfig? = null,
    retry: RetryStrategy = RetryStrategy(),
    enableCertPinning: Boolean = true,
): ClaudeService {
    return createProtectedService(
        partialKey = partialKey,
        serviceURL = serviceURL,
        providerConfig = ClaudeProvider.CONFIG,
        logging = logging,
        timeout = timeout,
        organization = organization,
        headers = headers,
        proxy = proxy,
        retry = retry,
        enableCertPinning = enableCertPinning
    ) { config, gatewayImpl ->
        GatewayClaudeService(config, gatewayImpl)
    }
} 