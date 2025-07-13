package io.github.brahyam.gateway.kmpsample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.core.Role
import com.aallam.openai.api.model.ModelId
import gateway_kmp.sample.kmp.composeApp.BuildConfig
import io.github.brahyam.gateway.client.Gateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        data class ChatMessageUi(val text: String, val isUser: Boolean)

        var messages by remember { mutableStateOf(listOf<ChatMessageUi>()) }
        var input by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        val coroutineScope = remember { CoroutineScope(Dispatchers.Main) }
        val snackbarHostState = remember { SnackbarHostState() }

        // Initialize OpenAIService once
        val openAIService = remember {
            Gateway.configure(
                googleCloudProjectNumber = BuildConfig.GOOGLE_CLOUD_PROJECT_NUMBER_STRING.toLong()
            )
            // Use to route requests through the Gateway service with protection against abuse
            Gateway.createOpenAIService(
                serviceURL = BuildConfig.GATEWAY_SERVICE_URL,
                partialKey = BuildConfig.GATEWAY_PARTIAL_KEY,
            )

            // Use this to directly access OpenAI API (DONT USE IN PRODUCTION)
//            Gateway.createDirectOpenAIService(apiKey = BuildConfig.OPENAI_API_KEY)
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
                                Text(
                                    msg.text,
                                    modifier = Modifier.padding(12.dp),
                                    color = if (msg.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                    if (isLoading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loading...", color = MaterialTheme.colorScheme.primary)
                    }
                    // Error is now shown via Snackbar
                }
                Spacer(modifier = Modifier.height(8.dp))
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
                                messages = messages + ChatMessageUi(userInput, true)
                                input = ""
                                isLoading = true
                                error = null
                                coroutineScope.launch {
                                    try {
                                        val openAiMessages = messages.map {
                                            ChatMessage(
                                                role = if (it.isUser) Role.User else Role.Assistant,
                                                content = it.text
                                            )
                                        } + ChatMessage(role = Role.User, content = userInput)
                                        val request = ChatCompletionRequest(
                                            model = ModelId("gpt-4o-mini"),
                                            n = 1,
                                            messages = openAiMessages
                                        )
                                        val response =
                                            openAIService.chatCompletion(request).choices.first().message.content!!
                                        messages = messages + ChatMessageUi(response, false)
                                    } catch (e: Exception) {
                                        error = e.message
                                        snackbarHostState.showSnackbar("Error: ${e.message}")
                                    } finally {
                                        isLoading = false
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