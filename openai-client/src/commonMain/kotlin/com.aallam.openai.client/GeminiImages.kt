package com.aallam.openai.client

import com.aallam.openai.api.core.RequestOptions
import com.aallam.openai.api.image.GeminiImageGeneration
import com.aallam.openai.api.image.GeminiImageResponse

/**
 * Gemini-specific image generation interface.
 * This uses Gemini's native API format which differs from OpenAI's Images interface.
 */
public interface GeminiImages {

    /**
     * Generate images using Gemini's image generation API.
     *
     * This endpoint supports both text-only prompts for image generation and
     * text + image inputs for image editing scenarios.
     *
     * @param generation The image generation request containing content parts and configuration
     * @param requestOptions Optional request options
     * @return The Gemini image response containing generated images and/or text
     */
    public suspend fun generateImages(
        generation: GeminiImageGeneration,
        requestOptions: RequestOptions? = null,
    ): GeminiImageResponse
}