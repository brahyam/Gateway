package com.aallam.openai.client.internal.api

import com.aallam.openai.api.core.RequestOptions
import com.aallam.openai.api.image.GeminiImageGeneration
import com.aallam.openai.api.image.GeminiImageResponse
import com.aallam.openai.client.GeminiImages
import com.aallam.openai.client.internal.extension.requestOptions
import com.aallam.openai.client.internal.http.HttpRequester
import com.aallam.openai.client.internal.http.perform
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Implementation of GeminiImages interface.
 * This handles Gemini's specific API format and query-based authentication.
 */
internal class GeminiImagesApi(
    private val requester: HttpRequester,
    private val apiKey: String,
) : GeminiImages {

    override suspend fun generateImages(
        generation: GeminiImageGeneration,
        requestOptions: RequestOptions?,
    ): GeminiImageResponse {
        return requester.perform<GeminiImageResponse> { httpClient ->
            httpClient.post {
                url(path = GeminiApiPath.imageGeneration(generation.model))
                parameter("key", apiKey) // Gemini uses query parameter for auth
                setBody(generation) // Send the GeminiImageGeneration directly
                contentType(ContentType.Application.Json)
                requestOptions(requestOptions)
            }
        }
    }
}

/**
 * API paths specific to Gemini endpoints.
 */
internal object GeminiApiPath {
    /**
     * Generate the image generation endpoint path for a specific model.
     */
    fun imageGeneration(model: String): String = "/v1beta/models/$model:generateContent"
}