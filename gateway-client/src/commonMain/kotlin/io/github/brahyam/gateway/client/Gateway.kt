package io.github.brahyam.gateway.client

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost
import com.aallam.openai.client.ProxyConfig
import com.aallam.openai.client.RetryStrategy
import gateway_kmp.gateway_client.BuildConfig
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
    public const val VERSION: String = BuildConfig.GATEWAY_VERSION
    private var instance: GatewayImpl? = null
    public var logger: Logger = PrintlnLogger()

    /**
     * Configure the Gateway client. This must be called before using any other Gateway functionality.
     *
     * @param googleCloudProjectNumber The Google Cloud Project Number (Android only, nullable for iOS)
     * @param enableAnonymousId Whether to enable anonymous ID support
     * @param logger Optional logger implementation. If null, uses a simple println logger.
     * @param logLevel The minimum log level for logging (default: INFO)
     */
    @DefaultArgumentInterop.Enabled
    @DefaultArgumentInterop.MaximumDefaultArgumentCount(6)
    public fun configure(
        googleCloudProjectNumber: Long? = null,
        enableAnonymousId: Boolean = false,
        logger: Logger? = null,
        logLevel: LogLevel = LogLevel.INFO,
    ) {
        this.logger = logger ?: PrintlnLogger(logLevel)
        this.instance = createGatewayImpl(googleCloudProjectNumber, enableAnonymousId)
        CoroutineScope(Dispatchers.Default).launch {
            instance?.warmUpAttestation()
        }
    }

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
    @DefaultArgumentInterop.Enabled
    @DefaultArgumentInterop.MaximumDefaultArgumentCount(12)
    public fun createDirectOpenAIService(
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
        return GatewayDirectOpenAIService(openAIConfig)
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
    @DefaultArgumentInterop.Enabled
    @DefaultArgumentInterop.MaximumDefaultArgumentCount(12)
    public fun createOpenAIService(
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
        checkConfigured()
        val openAIConfig = OpenAIConfig(
            token = partialKey,
            host = OpenAIHost("$serviceURL/v1/"),
            engine = if (enableCertPinning) createPinnedEngine() else null,
            logging = logging,
            timeout = timeout,
            organization = organization,
            headers = headers,
            proxy = proxy,
            retry = retry
        )
        return GatewayOpenAIService(openAIConfig, instance!!)
    }

    private fun checkConfigured() {
        require(instance != null) {
            "Gateway must be configured before use. Call Gateway.configure() first."
        }
    }
}
