package io.github.brahyam.gateway.client

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.logging.Logger
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
    public var logger: Logger = Logger.Simple
    private var isConfigured: Boolean = false

    internal fun log(message: String) {
        when (logger) {
            Logger.Simple -> println("[Gateway] $message")
            Logger.Default -> println("[Gateway] $message")
            Logger.Empty -> {} // No logging
        }
    }

    /**
     * Configure the Gateway client. This must be called before using any other Gateway functionality.
     *
     * @param googleCloudProjectNumber The Google Cloud Project Number (Android only, nullable for iOS)
     * @param enableAnonymousId Whether to enable anonymous ID support
     * @param logger Optional logger implementation. If null, uses Logger.Simple
     */
    public fun configure(
        googleCloudProjectNumber: Long,
        enableAnonymousId: Boolean = false,
        logger: Logger? = null,
    ) {
        try {
            this.logger = logger ?: Logger.Simple
            this.instance = createGatewayImpl(googleCloudProjectNumber, enableAnonymousId)
            isConfigured = true

            // Warm up attestation in background - don't let failures crash the app
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    instance?.warmUpAttestation()
                } catch (e: Exception) {
                    log("Background attestation warm-up failed: ${e.message}")
                }
            }

            log("Gateway configured successfully")
        } catch (e: Exception) {
            log("Failed to configure Gateway: ${e.message}")
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
                log("Empty API key provided to create${providerConfig.name}Service")
                throw IllegalArgumentException("API key cannot be empty")
            }

            val openAIConfig = OpenAIConfig(
                token = apiKey,
                host = OpenAIHost("https://${providerConfig.baseUrl}${providerConfig.openAiCompatiblePath}"),
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
            log("Direct ${providerConfig.name} service created successfully")
            service
        } catch (e: Exception) {
            log("Failed to create direct ${providerConfig.name} service: ${e.message}")
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
                log("Empty partial key provided to create${providerConfig.name}Service")
                throw IllegalArgumentException("Partial key cannot be empty")
            }

            if (serviceURL.isBlank()) {
                log("Empty service URL provided to create${providerConfig.name}Service")
                throw IllegalArgumentException("Service URL cannot be empty")
            }

            val currentInstance = instance
            if (currentInstance == null) {
                log("Gateway instance is null - configuration may have failed")
                throw IllegalStateException("Gateway instance unavailable")
            }

            val normalizedServiceURL =
                if (serviceURL.endsWith("/")) serviceURL.dropLast(1) else serviceURL
            val openAIConfig = OpenAIConfig(
                token = partialKey,
                host = OpenAIHost("${normalizedServiceURL}${providerConfig.openAiCompatiblePath}"),
                engine = if (enableCertPinning) {
                    try {
                        createPinnedEngine()
                    } catch (e: Exception) {
                        log("Failed to create pinned engine: ${e.message}. Falling back to default engine.")
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
            log("Protected ${providerConfig.name} service created successfully")
            service
        } catch (e: Exception) {
            log("Failed to create protected ${providerConfig.name} service: ${e.message}")
            throw GatewayException("Failed to create protected ${providerConfig.name} service", e)
        }
    }

    private fun checkConfigured() {
        if (!isConfigured) {
            val message = "Gateway must be configured before use. Call Gateway.configure() first."
            log(message)
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
            log("Gateway reset successfully")
        } catch (e: Exception) {
            log("Error resetting Gateway: ${e.message}")
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
