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
 * Interface for Custom service implementations.
 */
public interface CustomService : OpenAI

/**
 * Unprotected Custom service implementation for BYOK (Bring Your Own Key) use cases.
 */
internal class GatewayDirectCustomService(
    openAiConfig: OpenAIConfig,
) : BaseDirectService(openAiConfig, ServiceProviderConfig("Custom", "")), CustomService

/**
 * Protected Custom service implementation that uses Gateway's attestation and protection mechanisms.
 */
internal class GatewayCustomService(
    openAiConfig: OpenAIConfig,
    gatewayImpl: GatewayImpl,
) : BaseProtectedService(openAiConfig, gatewayImpl, ServiceProviderConfig("Custom", "")),
    CustomService

/**
 * Get an unprotected Custom service instance for BYOK (Bring Your Own Key) use cases.
 * This should only be used in development or when you want your users to add their own API key.
 *
 * @param apiKey Your API key
 * @param proxyDomain The proxy domain to use
 * @param logging client logging configuration
 * @param timeout http client timeout
 * @param organization organization ID
 * @param headers extra http headers
 * @param proxy HTTP proxy configuration
 * @param retry rate limit retry configuration
 * @param engine explicit ktor engine for http requests
 * @param httpClientConfig additional custom client configuration
 * @return A Custom service instance
 */
public fun Gateway.createDirectCustomService(
    apiKey: String,
    proxyDomain: String,
    logging: LoggingConfig = LoggingConfig(),
    timeout: Timeout = Timeout(socket = 30.seconds),
    organization: String? = null,
    headers: Map<String, String> = emptyMap(),
    proxy: ProxyConfig? = null,
    retry: RetryStrategy = RetryStrategy(),
    engine: HttpClientEngine? = null,
    httpClientConfig: HttpClientConfig<*>.() -> Unit = {},
): CustomService {
    val customProvider = ServiceProviderConfig(
        name = "Custom",
        baseUrl = proxyDomain,
        openAiCompatiblePath = ""
    )
    return createDirectService(
        apiKey = apiKey,
        providerConfig = customProvider,
        logging = logging,
        timeout = timeout,
        organization = organization,
        headers = headers,
        proxy = proxy,
        retry = retry,
        engine = engine,
        httpClientConfig = httpClientConfig
    ) { config ->
        GatewayDirectCustomService(config)
    }
}

/**
 * Get a protected Custom service instance for production use cases.
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
 * @return A protected Custom service instance
 *
 */
public fun Gateway.createCustomService(
    partialKey: String,
    serviceURL: String,
    logging: LoggingConfig = LoggingConfig(),
    timeout: Timeout = Timeout(socket = 30.seconds),
    organization: String? = null,
    headers: Map<String, String> = emptyMap(),
    proxy: ProxyConfig? = null,
    retry: RetryStrategy = RetryStrategy(),
    enableCertPinning: Boolean = true,
): CustomService {
    val customProvider = ServiceProviderConfig(
        name = "Custom",
        baseUrl = serviceURL,
        openAiCompatiblePath = ""
    )
    return createProtectedService(
        partialKey = partialKey,
        serviceURL = serviceURL,
        providerConfig = customProvider,
        logging = logging,
        timeout = timeout,
        organization = organization,
        headers = headers,
        proxy = proxy,
        retry = retry,
        enableCertPinning = enableCertPinning
    ) { config, gatewayImpl ->
        GatewayCustomService(config, gatewayImpl)
    }
} 