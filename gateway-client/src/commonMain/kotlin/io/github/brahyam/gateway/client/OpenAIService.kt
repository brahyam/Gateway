package io.github.brahyam.gateway.client

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost
import com.aallam.openai.client.ProxyConfig
import com.aallam.openai.client.RetryStrategy
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import kotlin.time.Duration.Companion.seconds

/**
 * Interface for OpenAI service implementations.
 */
public interface OpenAIService : OpenAI

/**
 * OpenAI service provider configuration
 */
public object OpenAIProvider {
    public val CONFIG: ServiceProviderConfig = ServiceProviderConfig(
        name = "OpenAI",
        proxyDomain = "api.openai.com/v1"
    )
}

/**
 * Unprotected OpenAI service implementation for BYOK (Bring Your Own Key) use cases.
 */
internal class GatewayDirectOpenAIService(
    openAiConfig: OpenAIConfig,
) : BaseDirectService(openAiConfig, OpenAIProvider.CONFIG), OpenAIService

/**
 * Protected OpenAI service implementation that uses Gateway's attestation and protection mechanisms.
 */
internal class GatewayOpenAIService(
    openAiConfig: OpenAIConfig,
    gatewayImpl: GatewayImpl,
) : BaseProtectedService(openAiConfig, gatewayImpl, OpenAIProvider.CONFIG), OpenAIService

/**
 * Get an unprotected OpenAI service instance for BYOK (Bring Your Own Key) use cases.
 * This should only be used in development or when you want your users to add their own OpenAI API key.
 *
 * @param apiKey Your OpenAI API key
 * @param logging client logging configuration
 * @param timeout http client timeout
 * @param organization OpenAI organization ID
 * @param headers extra http headers
 * @param host OpenAI host configuration
 * @param proxy HTTP proxy configuration
 * @param retry rate limit retry configuration
 * @param engine explicit ktor engine for http requests
 * @param httpClientConfig additional custom client configuration
 * @return An OpenAI service instance
 */
public fun Gateway.createDirectOpenAIService(
    apiKey: String,
    logging: LoggingConfig = LoggingConfig(),
    timeout: Timeout = Timeout(socket = 30.seconds),
    organization: String? = null,
    headers: Map<String, String> = emptyMap(),
    host: OpenAIHost = OpenAIHost.OpenAI,
    proxy: ProxyConfig? = null,
    retry: RetryStrategy = RetryStrategy(),
    engine: HttpClientEngine? = null,
    httpClientConfig: HttpClientConfig<*>.() -> Unit = {},
): OpenAIService {
    return try {
        if (apiKey.isBlank()) {
            logger.error("Empty API key provided to createDirectOpenAIService")
            throw IllegalArgumentException("API key cannot be empty")
        }

        val openAIConfig = OpenAIConfig(
            token = apiKey,
            logging = logging,
            timeout = timeout,
            organization = organization,
            headers = headers,
            host = host,
            proxy = proxy,
            retry = retry,
            engine = engine,
            httpClientConfig = httpClientConfig
        )

        val service = GatewayDirectOpenAIService(openAIConfig)
        logger.info("Direct OpenAI service created successfully")
        service
    } catch (e: Exception) {
        logger.error("Failed to create direct OpenAI service: ${e.message}")
        throw GatewayException("Failed to create direct OpenAI service", e)
    }
}

/**
 * Get a protected OpenAI service instance for production use cases.
 * This uses the Gateway's attestation and protection mechanisms.
 *
 * @param partialKey Partial key from your Gateway developer dashboard
 * @param serviceURL Service URL from your Gateway developer dashboard
 * @param logging Client logging configuration
 * @param timeout HTTP client timeout
 * @param organization OpenAI organization ID
 * @param headers Extra HTTP headers
 * @param proxy HTTP proxy configuration
 * @param retry Rate limit retry configuration
 * @param enableCertPinning Whether to enable certificate pinning
 * @return A protected OpenAI service instance
 *
 */
public fun Gateway.createOpenAIService(
    partialKey: String,
    serviceURL: String,
    logging: LoggingConfig = LoggingConfig(),
    timeout: Timeout = Timeout(socket = 30.seconds),
    organization: String? = null,
    headers: Map<String, String> = emptyMap(),
    proxy: ProxyConfig? = null,
    retry: RetryStrategy = RetryStrategy(),
    enableCertPinning: Boolean = true,
): OpenAIService {
    return createProtectedService(
        partialKey = partialKey,
        serviceURL = serviceURL,
        providerConfig = OpenAIProvider.CONFIG,
        logging = logging,
        timeout = timeout,
        organization = organization,
        headers = headers,
        proxy = proxy,
        retry = retry,
        enableCertPinning = enableCertPinning
    ) { config, gatewayImpl ->
        GatewayOpenAIService(config, gatewayImpl)
    }
} 