package io.github.brahyam.gateway.client

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
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
     * Generic function to create unprotected AI service instances
     */
    internal inline fun <T : OpenAI> createDirectService(
        apiKey: String,
        providerConfig: ServiceProviderConfig,
        logging: LoggingConfig = LoggingConfig(),
        timeout: Timeout = Timeout(socket = 30.seconds),
        organization: String? = null,
        headers: Map<String, String> = emptyMap(),
        proxy: ProxyConfig? = null,
        retry: RetryStrategy = RetryStrategy(),
        engine: HttpClientEngine? = null,
        noinline httpClientConfig: HttpClientConfig<*>.() -> Unit = {},
        serviceFactory: (OpenAIConfig) -> T,
    ): T {
        return try {
            if (apiKey.isBlank()) {
                logger.error("Empty API key provided to create${providerConfig.name}Service")
                throw IllegalArgumentException("API key cannot be empty")
            }

            val openAIConfig = OpenAIConfig(
                token = apiKey,
                host = OpenAIHost("https://${providerConfig.proxyDomain}/"),
                logging = logging,
                timeout = timeout,
                organization = organization,
                headers = headers,
                proxy = proxy,
                retry = retry,
                engine = engine,
                httpClientConfig = httpClientConfig
            )

            val service = serviceFactory(openAIConfig)
            logger.info("Direct ${providerConfig.name} service created successfully")
            service
        } catch (e: Exception) {
            logger.error("Failed to create direct ${providerConfig.name} service: ${e.message}")
            throw GatewayException("Failed to create direct ${providerConfig.name} service", e)
        }
    }

    /**
     * Generic function to create protected AI service instances
     */
    internal inline fun <T : OpenAI> createProtectedService(
        partialKey: String,
        serviceURL: String,
        providerConfig: ServiceProviderConfig,
        logging: LoggingConfig = LoggingConfig(),
        timeout: Timeout = Timeout(socket = 30.seconds),
        organization: String? = null,
        headers: Map<String, String> = emptyMap(),
        proxy: ProxyConfig? = null,
        retry: RetryStrategy = RetryStrategy(),
        enableCertPinning: Boolean = true,
        serviceFactory: (OpenAIConfig, GatewayImpl) -> T,
    ): T {
        return try {
            checkConfigured()

            if (partialKey.isBlank()) {
                logger.error("Empty partial key provided to create${providerConfig.name}Service")
                throw IllegalArgumentException("Partial key cannot be empty")
            }

            if (serviceURL.isBlank()) {
                logger.error("Empty service URL provided to create${providerConfig.name}Service")
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

            val service = serviceFactory(openAIConfig, currentInstance)
            logger.info("Protected ${providerConfig.name} service created successfully")
            service
        } catch (e: Exception) {
            logger.error("Failed to create protected ${providerConfig.name} service: ${e.message}")
            throw GatewayException("Failed to create protected ${providerConfig.name} service", e)
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
