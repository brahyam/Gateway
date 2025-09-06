package com.aallam.openai.client

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.client.internal.GeminiApi
import com.aallam.openai.client.internal.createGeminiHttpClient
import com.aallam.openai.client.internal.http.HttpTransport
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import kotlin.time.Duration.Companion.minutes

/**
 * Gemini API client interface.
 * This provides access to Gemini's native API endpoints with their specific formats.
 */
public interface Gemini : GeminiImages, AutoCloseable

/**
 * Creates an instance of [Gemini] client.
 * This client uses Gemini's native API format and query-based authentication.
 *
 * @param apiKey Gemini API key
 * @param logging client logging configuration
 * @param timeout http client timeout
 * @param headers extra http headers
 * @param host Gemini host configuration
 * @param proxy HTTP proxy configuration
 * @param retry rate limit retry configuration
 * @param httpClientConfig additional custom client configuration
 */
public fun Gemini(
    apiKey: String,
    logging: LoggingConfig = LoggingConfig(),
    timeout: Timeout = Timeout(socket = 3.minutes),
    headers: Map<String, String> = emptyMap(),
    host: GeminiHost = GeminiHost.Gemini,
    proxy: ProxyConfig? = null,
    retry: RetryStrategy = RetryStrategy(),
    httpClientConfig: HttpClientConfig<*>.() -> Unit = {},
): Gemini = Gemini(
    config = GeminiConfig(
        apiKey = apiKey,
        logging = logging,
        timeout = timeout,
        headers = headers,
        host = host,
        proxy = proxy,
        retry = retry,
        httpClientConfig = httpClientConfig,
    )
)

/**
 * Creates an instance of [Gemini] client.
 *
 * @param config Gemini client config
 */
public fun Gemini(config: GeminiConfig): Gemini {
    val httpClient = createGeminiHttpClient(config)
    val transport = HttpTransport(httpClient)
    return GeminiApi(transport, config.apiKey)
}

/**
 * Creates an instance of [Gemini] client with a custom HTTP client plugin.
 */
public fun Gemini(
    config: GeminiConfig,
    httpClientApplicator: HttpClient.() -> Unit = {},
): Gemini {
    val httpClient = createGeminiHttpClient(config)
    httpClient.httpClientApplicator()
    val transport = HttpTransport(httpClient)
    return GeminiApi(transport, config.apiKey)
}