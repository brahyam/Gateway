package io.github.brahyam.gateway.kmpsample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aallam.openai.api.chat.ChatCompletionChunk
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.core.Role
import com.aallam.openai.api.image.createGeminiImageGenerationWithImages
import com.aallam.openai.api.image.getFirstImage
import com.aallam.openai.api.model.ModelId
import gateway_kmp.sample.kmp.composeApp.BuildConfig
import io.github.brahyam.gateway.client.Gateway
import io.github.brahyam.gateway.client.createGeminiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

data class ChatMessageUi(val text: String, val isUser: Boolean, val imageUrl: String? = null)

enum class AIFunction(val displayName: String) {
    NORMAL("Normal Chat"),
    STREAMING("Stream Response"),
    IMAGE("Create Image")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
@Preview
fun App() {
    MaterialTheme {

        var messages by remember { mutableStateOf(listOf<ChatMessageUi>()) }
        var input by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var selectedFunction by remember { mutableStateOf(AIFunction.NORMAL) }
        var streamingResponse by remember { mutableStateOf("") }
        var attachedImageUrl by remember { mutableStateOf<String?>(null) }
        val coroutineScope = remember { CoroutineScope(Dispatchers.Main) }
        val snackbarHostState = remember { SnackbarHostState() }

        // Initialize GeminiService once
        val geminiService = remember {
            Gateway.configure(
                googleCloudProjectNumber = BuildConfig.GOOGLE_CLOUD_PROJECT_NUMBER_STRING.toLong()
            )
//             Use to route requests through the Gateway service with protection against abuse
            Gateway.createGeminiService(
                serviceURL = BuildConfig.GATEWAY_SERVICE_URL,
                partialKey = BuildConfig.GATEWAY_PARTIAL_KEY,
            )

            // Use this to directly access Gemini API (DONT USE IN PRODUCTION)
//            Gateway.createDirectGeminiService(
//                apiKey = BuildConfig.GEMINI_API_KEY, logging = LoggingConfig(
//                    LogLevel.All
//                )
//            )
        }

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    for (msg in messages) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                color = if (msg.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (msg.text.isNotEmpty()) {
                                        Text(
                                            msg.text,
                                            color = if (msg.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    msg.imageUrl?.let { imageUrl ->
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = imageUrl,
                                                contentDescription = msg.text,
                                                modifier = Modifier
                                                    .size(200.dp)
                                                    .padding(4.dp),
                                                contentScale = ContentScale.Crop
                                            )
                                            if (!msg.isUser) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                IconButton(
                                                    onClick = {
                                                        attachedImageUrl = imageUrl
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Add,
                                                        contentDescription = "Attach image",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Show streaming response if in progress
                    if (isLoading && selectedFunction == AIFunction.STREAMING && streamingResponse.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        streamingResponse,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    } else if (isLoading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Loading...", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Filter chips for AI function selection
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AIFunction.entries.forEach { function ->
                        FilterChip(
                            onClick = { selectedFunction = function },
                            label = { Text(function.displayName) },
                            selected = selectedFunction == function
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Show attached image thumbnail
                attachedImageUrl?.let { imageUrl ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Attached image",
                                modifier = Modifier
                                    .size(60.dp)
                                    .padding(4.dp),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Image attached",
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = {
                                    attachedImageUrl = null
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Remove image",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        enabled = !isLoading
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (input.isNotBlank() && !isLoading) {
                                val userInput = input
                                val userImageUrl = attachedImageUrl
                                messages = messages + ChatMessageUi(userInput, true, userImageUrl)
                                input = ""
                                attachedImageUrl = null
                                isLoading = true
                                streamingResponse = ""

                                coroutineScope.launch {
                                    try {
                                        when (selectedFunction) {
                                            AIFunction.NORMAL -> {
                                                val openAiMessages = messages.map {
                                                    ChatMessage(
                                                        role = if (it.isUser) Role.User else Role.Assistant,
                                                        content = it.text
                                                    )
                                                } + ChatMessage(
                                                    role = Role.User,
                                                    content = userInput
                                                )
                                                val request = ChatCompletionRequest(
                                                    model = ModelId("gemini-1.5-flash"),
                                                    n = 1,
                                                    messages = openAiMessages
                                                )
                                                val response =
                                                    geminiService.chatCompletion(request).choices.first().message.content!!
                                                messages = messages + ChatMessageUi(response, false)
                                            }

                                            AIFunction.STREAMING -> {
                                                val openAiMessages = messages.map {
                                                    ChatMessage(
                                                        role = if (it.isUser) Role.User else Role.Assistant,
                                                        content = it.text
                                                    )
                                                } + ChatMessage(
                                                    role = Role.User,
                                                    content = userInput
                                                )
                                                val request = ChatCompletionRequest(
                                                    model = ModelId("gemini-1.5-flash"),
                                                    n = 1,
                                                    messages = openAiMessages
                                                )
                                                val completions: Flow<ChatCompletionChunk> =
                                                    geminiService.chatCompletions(request)
                                                var fullResponse = ""

                                                completions.collect { chunk ->
                                                    chunk.choices.firstOrNull()?.delta?.content?.let { content ->
                                                        fullResponse += content
                                                        streamingResponse = fullResponse
                                                    }
                                                }

                                                if (fullResponse.isNotEmpty()) {
                                                    messages = messages + ChatMessageUi(
                                                        fullResponse,
                                                        false
                                                    )
                                                }
                                            }

                                            AIFunction.IMAGE -> {
                                                val geminiImageGeneration =
                                                    createGeminiImageGenerationWithImages(
                                                        text = userInput,
                                                        model = "gemini-2.5-flash-image-preview",
                                                        images = userImageUrl?.let { imageUrl ->
                                                            // Remove data:image/png;base64, prefix if present
                                                            val base64Data =
                                                                if (imageUrl.startsWith("data:image/png;base64,")) {
                                                                    imageUrl.removePrefix("data:image/png;base64,")
                                                                } else {
                                                                    imageUrl
                                                                }
                                                            listOf(Pair("image/png", base64Data))
                                                        } ?: emptyList(),
                                                        responseModalities = listOf("TEXT", "IMAGE")
                                                    )
                                                val imageResponse = geminiService.generateImages(
                                                    geminiImageGeneration
                                                )
                                                val imageData = imageResponse.getFirstImage()?.data
                                                if (imageData != null) {
                                                    val base64Image =
                                                        "data:image/png;base64,$imageData"
                                                    messages = messages + ChatMessageUi(
                                                        "Generated image for: \"$userInput\"",
                                                        false,
                                                        base64Image
                                                    )
                                                } else {
                                                    snackbarHostState.showSnackbar("Failed to generate image")
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        println(e.message)
                                        snackbarHostState.showSnackbar("Error: ${e.message}")
                                    } finally {
                                        isLoading = false
                                        streamingResponse = ""
                                    }
                                }
                            }
                        },
                        enabled = !isLoading
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    }
}