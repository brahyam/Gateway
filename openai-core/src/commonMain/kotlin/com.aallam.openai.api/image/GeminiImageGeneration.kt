package com.aallam.openai.api.image

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Represents a request for Gemini image generation.
 */
@Serializable
public data class GeminiImageGeneration(
    /**
     * The content parts that make up the generation request.
     */
    @SerialName("contents") public val contents: List<GeminiContent>,

    /**
     * Configuration that controls how the model generates content.
     */
    @SerialName("generationConfig") public val generationConfig: GeminiGenerationConfig,

    /**
     * The Gemini model to use for image generation.
     * This model name will be used in the API endpoint URL.
     */
    @kotlinx.serialization.Transient
    public val model: String = "gemini-2.5-flash-image-preview",
)

/**
 * Represents content in a Gemini request.
 */
@Serializable
public data class GeminiContent(
    /**
     * The parts that make up this content.
     * Using JsonElement to avoid polymorphic serialization issues.
     */
    @SerialName("parts") public val parts: List<JsonElement>,
)

/**
 * Represents inline data with MIME type and base64 content.
 * Kept for backward compatibility with existing response parsing.
 */
@Serializable
public data class GeminiInlineData(
    /**
     * The MIME type of the data (e.g., "image/jpeg").
     */
    @SerialName("mime_type") public val mimeType: String,

    /**
     * The base64-encoded data.
     */
    @SerialName("data") public val data: String,
)

/**
 * Configuration for Gemini content generation.
 */
@Serializable
public data class GeminiGenerationConfig(
    /**
     * The response modalities (e.g., "TEXT", "IMAGE").
     */
    @SerialName("responseModalities") public val responseModalities: List<String>,
)

/**
 * Builder for creating GeminiImageGeneration requests.
 */
public class GeminiImageGenerationBuilder {
    private val contents = mutableListOf<GeminiContent>()
    private var generationConfig: GeminiGenerationConfig? = null
    private var model: String = "gemini-2.5-flash-image-preview"

    /**
     * Add text content to the request.
     */
    public fun addText(text: String): GeminiImageGenerationBuilder = apply {
        val textPart = buildJsonObject {
            put("text", JsonPrimitive(text))
        }
        contents.add(GeminiContent(listOf(textPart)))
    }

    /**
     * Add image content to the request.
     */
    public fun addImage(mimeType: String, base64Data: String): GeminiImageGenerationBuilder =
        apply {
            val imagePart = buildJsonObject {
                put("inline_data", buildJsonObject {
                    put("mime_type", JsonPrimitive(mimeType))
                    put("data", JsonPrimitive(base64Data))
                })
            }
            contents.add(GeminiContent(listOf(imagePart)))
        }

    /**
     * Add mixed content (text and image) to the request.
     */
    public fun addMixedContent(
        text: String? = null,
        mimeType: String? = null,
        base64Data: String? = null,
    ): GeminiImageGenerationBuilder = apply {
        val parts = mutableListOf<JsonElement>()
        text?.let {
            parts.add(buildJsonObject {
                put("text", JsonPrimitive(it))
            })
        }
        if (mimeType != null && base64Data != null) {
            parts.add(buildJsonObject {
                put("inline_data", buildJsonObject {
                    put("mime_type", JsonPrimitive(mimeType))
                    put("data", JsonPrimitive(base64Data))
                })
            })
        }
        if (parts.isNotEmpty()) {
            contents.add(GeminiContent(parts))
        }
    }

    /**
     * Set the generation configuration.
     */
    public fun withConfig(responseModalities: List<String>): GeminiImageGenerationBuilder = apply {
        this.generationConfig = GeminiGenerationConfig(responseModalities)
    }

    /**
     * Set the Gemini model to use for image generation.
     */
    public fun withModel(model: String): GeminiImageGenerationBuilder = apply {
        this.model = model
    }

    /**
     * Build the GeminiImageGeneration request.
     */
    public fun build(): GeminiImageGeneration {
        require(contents.isNotEmpty()) { "At least one content must be provided" }
        val config = generationConfig ?: GeminiGenerationConfig(listOf("TEXT", "IMAGE"))
        return GeminiImageGeneration(contents, config, model)
    }
}

/**
 * Create a GeminiImageGeneration request using a builder.
 * Non-inline version for Java 11 compatibility.
 */
public fun geminiImageGeneration(block: GeminiImageGenerationBuilder.() -> Unit): GeminiImageGeneration {
    return GeminiImageGenerationBuilder().apply(block).build()
}

/**
 * Create a simple text-only GeminiImageGeneration request.
 * Java 11 compatible factory function.
 */
public fun createGeminiImageGeneration(
    text: String,
    model: String = "gemini-2.5-flash-image-preview",
    responseModalities: List<String> = listOf("TEXT", "IMAGE"),
): GeminiImageGeneration {
    val textPart = buildJsonObject {
        put("text", JsonPrimitive(text))
    }
    val content = GeminiContent(listOf(textPart))
    val config = GeminiGenerationConfig(responseModalities)
    return GeminiImageGeneration(listOf(content), config, model)
}

/**
 * Create a GeminiImageGeneration request with both text and image.
 * Java 11 compatible factory function.
 */
public fun createGeminiImageGenerationWithImage(
    text: String,
    mimeType: String,
    base64Data: String,
    model: String = "gemini-2.5-flash-image-preview",
    responseModalities: List<String> = listOf("TEXT", "IMAGE"),
): GeminiImageGeneration {
    val textPart = buildJsonObject {
        put("text", JsonPrimitive(text))
    }
    val imagePart = buildJsonObject {
        put("inline_data", buildJsonObject {
            put("mime_type", JsonPrimitive(mimeType))
            put("data", JsonPrimitive(base64Data))
        })
    }
    val content = GeminiContent(listOf(textPart, imagePart))
    val config = GeminiGenerationConfig(responseModalities)
    return GeminiImageGeneration(listOf(content), config, model)
}
