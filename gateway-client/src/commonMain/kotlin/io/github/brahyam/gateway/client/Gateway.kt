package io.github.brahyam.gateway.client

import io.ktor.client.HttpClientConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
     * @param unprotectedAPIKey Your OpenAI API key
     * @return An OpenAI service instance
     */
    public fun unprotectedOpenAIService(
        unprotectedAPIKey: String, httpClientConfig: HttpClientConfig<*>.() -> Unit = {},
    ): OpenAIService {
        checkConfigured()
        return UnprotectedOpenAIService(unprotectedAPIKey, httpClientConfig)
    }

    /**
     * Get a protected OpenAI service instance for production use cases.
     * This uses the Gateway's attestation and protection mechanisms.
     *
     * @param partialKey Partial key from your Gateway developer dashboard
     * @param serviceURL Service URL from your Gateway developer dashboard
     * @return A protected OpenAI service instance
     */
    public fun protectedOpenAIService(partialKey: String, serviceURL: String): OpenAIService {
        checkConfigured()
        return ProtectedOpenAIService(partialKey, serviceURL, instance!!)
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
