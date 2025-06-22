package io.github.brahyam.gateway.client

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost
import com.aallam.openai.client.ProxyConfig
import com.aallam.openai.client.RetryStrategy
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * Gateway client for access AI services.
 */
public object Gateway {
    private var config: GatewayConfig? = null
    private var instance: GatewayImpl? = null

    /**
     * Configure the Gateway client. This must be called before using any other Gateway functionality.
     *
     * @param config The configuration for the Gateway client.
     */
    public fun configure(config: GatewayConfig) {
        this.config = config
        this.instance = createGatewayImpl(config)

        CoroutineScope(Dispatchers.Default).launch {
            instance?.warmUpAttestation()
        }
    }

    /**
     * Get an unprotected OpenAI service instance for BYOK (Bring Your Own Key) use cases.
     * This should only be used in development or when you want to use your own OpenAI API key.
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
    public fun unprotectedOpenAIService(
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
        checkConfigured()
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
        return UnprotectedOpenAIService(openAIConfig)
    }

    /**
     * Get a protected OpenAI service instance for production use cases.
     * This uses the Gateway's attestation and protection mechanisms.
     *
     * @param partialKey Partial key from your Gateway developer dashboard
     * @param serviceURL Service URL from your Gateway developer dashboard
     * @return A protected OpenAI service instance
     */
    public fun protectedOpenAIService(
        partialKey: String,
        serviceURL: String,
        logging: LoggingConfig = LoggingConfig(),
        timeout: Timeout = Timeout(socket = 30.seconds),
        organization: String? = null,
        headers: Map<String, String> = emptyMap(),
        proxy: ProxyConfig? = null,
        retry: RetryStrategy = RetryStrategy(),
    ): OpenAIService {
        checkConfigured()
        val openAIConfig = OpenAIConfig(
            token = partialKey,
            host = OpenAIHost(serviceURL),
            engine = createPinnedEngine(),
            logging = logging,
            timeout = timeout,
            organization = organization,
            headers = headers,
            proxy = proxy,
            retry = retry
        )
        return ProtectedOpenAIService(openAIConfig, instance!!)
    }

    private fun checkConfigured() {
        require(config != null && instance != null) {
            "Gateway must be configured before use. Call Gateway.configure() first."
        }
    }
}

/**
 * Configuration for the Gateway client.
 */
public data class GatewayConfig(
    val googleCloudProjectNumber: Long,
)
