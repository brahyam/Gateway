package io.github.brahyam.gateway.sample.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.core.Role.Companion.Assistant
import com.aallam.openai.api.core.Role.Companion.User
import com.aallam.openai.api.model.ModelId
import io.github.brahyam.gateway.client.Gateway
import io.github.brahyam.gateway.client.OpenAIService
import io.github.brahyam.gateway.sample.BuildConfig
import io.github.brahyam.gateway.sample.ui.theme.SampleTheme
import kotlinx.coroutines.launch

data class Message(
    val text: String,
    val isFromUser: Boolean,
)

class MainActivity : ComponentActivity() {
    private lateinit var openAIService: OpenAIService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Use this for BYOK (Bring Your Own Key) or during development
        openAIService = Gateway.createDirectOpenAIService(
            apiKey = BuildConfig.OPENAI_API_KEY // Replace with your OpenAI API key
        )

        // Use this for production use cases with Gateway protection
//        openAIService = Gateway.createOpenAIService(
//            partialKey = BuildConfig.GATEWAY_PARTIAL_KEY,
//            serviceURL = BuildConfig.GATEWAY_SERVICE_URL
//        )

        enableEdgeToEdge()
        setContent {
            SampleTheme {
                ChatScreen(openAIService)
            }
        }
    }
}

@Composable
fun ChatScreen(openAIService: OpenAIService) {
    var messages by remember { mutableStateOf(listOf<Message>()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomAppBar {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp),
                    placeholder = { Text("Type a message...") },
                    singleLine = true,
                    enabled = !isLoading
                )
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isLoading) {
                            val userMessage = Message(inputText, true)
                            messages = messages + userMessage
                            val currentInput = inputText
                            inputText = ""
                            isLoading = true

                            scope.launch {
                                try {
                                    val openAiMessages = messages.map {
                                        ChatMessage(
                                            role = if (it.isFromUser) User else Assistant,
                                            content = it.text
                                        )
                                    } + ChatMessage(role = User, content = currentInput)
                                    val request = ChatCompletionRequest(
                                        model = ModelId("gpt-4o-mini"),
                                        n = 1, // Number of responses to generate
                                        messages = openAiMessages
                                    )
                                    val response =
                                        openAIService.chatCompletion(request).choices.first().message.content!!
                                    messages = messages + Message(response, false)
                                    isLoading = false
                                } catch (e: Exception) {
                                    messages = messages + Message("Error: ${e.message}", false)
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    Icon(Icons.AutoMirrored.Default.Send, contentDescription = "Send")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message)
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (message.isFromUser) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondary,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    SampleTheme {
        ChatScreen(Gateway.createDirectOpenAIService(12345.toLong().toString()))
    }
}