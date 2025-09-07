package com.aallam.openai.client.internal

import com.aallam.openai.client.Gemini
import com.aallam.openai.client.GeminiImages
import com.aallam.openai.client.internal.api.GeminiImagesApi
import com.aallam.openai.client.internal.http.HttpRequester

/**
 * Gemini-specific API implementation that provides access to Gemini's native endpoints.
 * This differs from the standard OpenAI interface in that it uses query-based authentication
 * and Gemini's specific request/response formats.
 */
internal class GeminiApi(
    private val requester: HttpRequester,
    private val apiKey: String,
) : Gemini, GeminiImages by GeminiImagesApi(requester, apiKey), AutoCloseable by requester