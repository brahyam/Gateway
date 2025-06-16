package io.github.brahyam.gateway.client

import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import io.ktor.client.HttpClientConfig

/**
 * Interface for OpenAI service implementations.
 */
public interface OpenAIService : OpenAI

/**
 * Unprotected OpenAI service implementation for BYOK (Bring Your Own Key) use cases.
 */
internal class UnprotectedOpenAIService(
    private val apiKey: String,
    private val httpClientConfig: HttpClientConfig<*>.() -> Unit = {},
) : OpenAIService, OpenAI by OpenAI(
    token = apiKey,
    httpClientConfig = httpClientConfig
)

/**
 * Protected OpenAI service implementation that uses Gateway's attestation and protection mechanisms.
 */
internal class ProtectedOpenAIService(
    private val partialKey: String,
    private val serviceURL: String,
) : OpenAIService, OpenAI by OpenAI(
    token = partialKey,
    host = OpenAIHost(serviceURL)
)