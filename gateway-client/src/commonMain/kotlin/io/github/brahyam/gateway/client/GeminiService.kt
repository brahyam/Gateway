package io.github.brahyam.gateway.client

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.client.Assistants
import com.aallam.openai.client.Audio
import com.aallam.openai.client.Batch
import com.aallam.openai.client.Chat
import com.aallam.openai.client.Completions
import com.aallam.openai.client.Edits
import com.aallam.openai.client.Embeddings
import com.aallam.openai.client.Files
import com.aallam.openai.client.FineTunes
import com.aallam.openai.client.FineTuning
import com.aallam.openai.client.GeminiImages
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.Messages
import com.aallam.openai.client.Models
import com.aallam.openai.client.Moderations
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.ProxyConfig
import com.aallam.openai.client.RetryStrategy
import com.aallam.openai.client.Runs
import com.aallam.openai.client.Threads
import com.aallam.openai.client.VectorStores
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import kotlin.time.Duration.Companion.seconds

/**
 * Interface for Google Gemini service implementations.
 * This implements individual OpenAI interfaces (excluding Images) and adds Gemini-specific image generation capabilities.
 */
public interface GeminiService :
    Audio,
    Assistants,
    Batch,
    Chat,
    Completions,
    Edits,
    Embeddings,
    Files,
    FineTunes,
    FineTuning,
    Messages,
    Models,
    Moderations,
    Runs,
    Threads,
    VectorStores,
    GeminiImages

/**
 * Google Gemini service provider configuration
 */
public object GeminiProvider {
    public val CONFIG: ServiceProviderConfig = ServiceProviderConfig(
        name = "Google Gemini",
        proxyDomain = "generativelanguage.googleapis.com/v1beta/openai"
    )
}

/**
 * Unprotected Google Gemini service implementation for BYOK (Bring Your Own Key) use cases.
 */
internal class GatewayDirectGeminiService(
    openAiConfig: OpenAIConfig,
) : BaseDirectService(openAiConfig, GeminiProvider.CONFIG), GeminiService {

    private val geminiClient by lazy {
        com.aallam.openai.client.Gemini(
            apiKey = openAiConfig.token,
            logging = openAiConfig.logging,
            timeout = openAiConfig.timeout,
            headers = openAiConfig.headers,
            proxy = openAiConfig.proxy,
            retry = openAiConfig.retry,
            httpClientConfig = openAiConfig.httpClientConfig
        )
    }

    override suspend fun generateImages(
        generation: com.aallam.openai.api.image.GeminiImageGeneration,
        requestOptions: com.aallam.openai.api.core.RequestOptions?,
    ) = geminiClient.generateImages(generation, requestOptions)
}

/**
 * Protected Google Gemini service implementation that uses Gateway's attestation and protection mechanisms.
 */
internal class GatewayGeminiService(
    openAiConfig: OpenAIConfig,
    gatewayImpl: GatewayImpl,
) : BaseProtectedService(openAiConfig, gatewayImpl, GeminiProvider.CONFIG), GeminiService {

    private val geminiClient by lazy {
        com.aallam.openai.client.Gemini(
            apiKey = openAiConfig.token,
            logging = openAiConfig.logging,
            timeout = openAiConfig.timeout,
            headers = openAiConfig.headers,
            proxy = openAiConfig.proxy,
            retry = openAiConfig.retry,
            httpClientConfig = openAiConfig.httpClientConfig
        )
    }

    override suspend fun generateImages(
        generation: com.aallam.openai.api.image.GeminiImageGeneration,
        requestOptions: com.aallam.openai.api.core.RequestOptions?,
    ) = geminiClient.generateImages(generation, requestOptions)
}

/**
 * Get an unprotected Google Gemini service instance for BYOK (Bring Your Own Key) use cases.
 * This should only be used in development or when you want your users to add their own Gemini API key.
 *
 * @param apiKey Your Google Gemini API key
 * @param logging client logging configuration
 * @param timeout http client timeout
 * @param organization organization ID
 * @param headers extra http headers
 * @param proxy HTTP proxy configuration
 * @param retry rate limit retry configuration
 * @param engine explicit ktor engine for http requests
 * @param httpClientConfig additional custom client configuration
 * @return A Google Gemini service instance
 */
public fun Gateway.createDirectGeminiService(
    apiKey: String,
    logging: LoggingConfig = LoggingConfig(),
    timeout: Timeout = Timeout(socket = 30.seconds),
    organization: String? = null,
    headers: Map<String, String> = emptyMap(),
    proxy: ProxyConfig? = null,
    retry: RetryStrategy = RetryStrategy(),
    engine: HttpClientEngine? = null,
    httpClientConfig: HttpClientConfig<*>.() -> Unit = {},
): GeminiService {
    return createDirectService(
        apiKey = apiKey,
        providerConfig = GeminiProvider.CONFIG,
        logging = logging,
        timeout = timeout,
        organization = organization,
        headers = headers,
        proxy = proxy,
        retry = retry,
        engine = engine,
        httpClientConfig = httpClientConfig
    ) { config ->
        GatewayDirectGeminiService(config)
    }
}

/**
 * Get a protected Google Gemini service instance for production use cases.
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
 * @return A protected Google Gemini service instance
 *
 */
public fun Gateway.createGeminiService(
    partialKey: String,
    serviceURL: String,
    logging: LoggingConfig = LoggingConfig(),
    timeout: Timeout = Timeout(socket = 30.seconds),
    organization: String? = null,
    headers: Map<String, String> = emptyMap(),
    proxy: ProxyConfig? = null,
    retry: RetryStrategy = RetryStrategy(),
    enableCertPinning: Boolean = true,
): GeminiService {
    return createProtectedService(
        partialKey = partialKey,
        serviceURL = serviceURL,
        providerConfig = GeminiProvider.CONFIG,
        logging = logging,
        timeout = timeout,
        organization = organization,
        headers = headers,
        proxy = proxy,
        retry = retry,
        enableCertPinning = enableCertPinning
    ) { config, gatewayImpl ->
        GatewayGeminiService(config, gatewayImpl)
    }
} 