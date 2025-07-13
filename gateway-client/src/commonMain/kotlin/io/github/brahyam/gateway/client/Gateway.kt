package io.github.brahyam.gateway.client

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
    private var isConfigured: Boolean = false

    /**
     * Configure the Gateway client. This must be called before using any other Gateway functionality.
     *
     * @param googleCloudProjectNumber The Google Cloud Project Number (Android only, nullable for iOS)
     * @param enableAnonymousId Whether to enable anonymous ID support
     * @param logger Optional logger implementation. If null, uses a simple println logger.
     * @param logLevel The minimum log level for logging (default: INFO)
     */
    public fun configure(
        googleCloudProjectNumber: Long,
        enableAnonymousId: Boolean = false,
        logger: Logger? = null,
        logLevel: LogLevel = LogLevel.INFO,
    ) {
        try {
            this.logger = logger ?: PrintlnLogger(logLevel)
            this.instance = createGatewayImpl(googleCloudProjectNumber, enableAnonymousId)
            isConfigured = true

            // Warm up attestation in background - don't let failures crash the app
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    instance?.warmUpAttestation()
                } catch (e: Exception) {
                    this@Gateway.logger.error("Background attestation warm-up failed: ${e.message}")
                }
            }

            this.logger.info("Gateway configured successfully")
        } catch (e: Exception) {
            this.logger.error("Failed to configure Gateway: ${e.message}")
            // Mark as configured with fallback to prevent repeated configuration attempts
            isConfigured = true
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
        return try {
            checkConfigured()

            if (partialKey.isBlank()) {
                logger.error("Empty partial key provided to createOpenAIService")
                throw IllegalArgumentException("Partial key cannot be empty")
            }

            if (serviceURL.isBlank()) {
                logger.error("Empty service URL provided to createOpenAIService")
                throw IllegalArgumentException("Service URL cannot be empty")
            }

            val currentInstance = instance
            if (currentInstance == null) {
                logger.error("Gateway instance is null - configuration may have failed")
                throw IllegalStateException("Gateway instance unavailable")
            }

            val normalizedServiceURL =
                if (serviceURL.endsWith("/")) serviceURL.dropLast(1) else serviceURL
            val openAIConfig = OpenAIConfig(
                token = partialKey,
                host = OpenAIHost("${normalizedServiceURL}/v1/"),
                engine = if (enableCertPinning) {
                    try {
                        createPinnedEngine()
                    } catch (e: Exception) {
                        logger.warn("Failed to create pinned engine: ${e.message}. Falling back to default engine.")
                        null
                    }
                } else null,
                logging = logging,
                timeout = timeout,
                organization = organization,
                headers = headers,
                proxy = proxy,
                retry = retry
            )

            val service = GatewayOpenAIService(openAIConfig, currentInstance)
            logger.info("Protected OpenAI service created successfully")
            service
        } catch (e: Exception) {
            logger.error("Failed to create protected OpenAI service: ${e.message}")
            throw GatewayException("Failed to create protected OpenAI service", e)
        }
    }

    private fun checkConfigured() {
        if (!isConfigured) {
            val message = "Gateway must be configured before use. Call Gateway.configure() first."
            logger.error(message)
            throw IllegalStateException(message)
        }
    }

    /**
     * Check if the Gateway has been configured
     */
    public fun isConfigured(): Boolean = isConfigured

    /**
     * Reset the Gateway configuration (primarily for testing)
     */
    public fun reset() {
        try {
            instance = null
            isConfigured = false
            logger.info("Gateway reset successfully")
        } catch (e: Exception) {
            logger.error("Error resetting Gateway: ${e.message}")
        }
    }
}

/**
 * Exception thrown by Gateway operations to wrap underlying errors
 */
public class GatewayException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
