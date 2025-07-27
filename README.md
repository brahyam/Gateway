# Gateway AI Client

![Maven Central Version](https://img.shields.io/maven-central/v/io.github.brahyam/gateway-client)
![GitHub License](https://img.shields.io/github/license/brahyam/Gateway)
[![Documentation](https://img.shields.io/badge/docs-api-a97bff.svg?logo=kotlin)](https://docs.meetgateway.com/)

Android and Kotlin Multiplatform (KMP) client for accessing different AI providers (OpenAI,
Claude...) directly or with API key protection through [Gateway's servers](https://meetgateway.com/)

## 🚀 Supported Service Providers

The `Gateway` client supports the following AI service providers (see [
`Gateway.kt`](gateway-client/src/commonMain/kotlin/io/github/brahyam/gateway/client/Gateway.kt)):

- **OpenAI**
- **Google Gemini**
- **Groq**
- **Mistral AI**
- **Together AI**
- **Anthropic Claude**
- **AI/ML API**
- **Custom** (any compatible OpenAI-style API)

## 📦 Android & Kotlin Multiplatform (KMP) Setup

### 1. Add the Gateway AI Client dependency

For **KMP projects**, add to your **commonMain** dependencies in your shared module's
`build.gradle.kts`:

```kotlin
commonMain.dependencies {
    implementation("io.github.brahyam:gateway-client:0.2.0")
}
```

For **Android-only projects**, add to your `build.gradle`:

```kotlin
dependencies {
   implementation("io.github.brahyam:gateway-client:0.2.0")
}
```

### 2. Configure Gateway early in your code (Only needed for Gateway-protected services)

For **KMP**, configure Gateway in your shared code (e.g., `RootComponent`, `MainViewModel`, or app
startup):

```kotlin
import io.github.brahyam.gateway.client.Gateway

class RootComponent {
    init {
        // Replace with your actual Google Cloud Project Number
        Gateway.configure(
            googleCloudProjectNumber = YOUR_GCP_PROJECT_NUMBER
        )
    }
}
```

For **Android**, configure Gateway in your `Application` class:

```kotlin
import android.app.Application
import io.github.brahyam.gateway.client.Gateway

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Gateway.configure(
            googleCloudProjectNumber = YOUR_GCP_PROJECT_NUMBER
        )
    }
}
```

### 3. Create an OpenAI Service instance

You can either call AI Services directly (not recommended for production as it exposes your API
keys),
or use the Gateway-protected service which offers:

- **Device attestation**: Ensures the request is coming from a genuine device.
- **API key protection**: Hides your real API keys.
- **Rate limiting**: Controls the number of requests based on IP/User.
- **Certificate pinning**: Ensures secure communication with Gateway servers.
- **Monitoring**: Tracks usage and performance.

**Direct (Unprotected!!) OpenAI Service**

```kotlin
val openAIService = Gateway.createDirectOpenAIService(
   apiKey = "your-openai-api-key"
)
```

**Gateway-Protected OpenAI Service**

```kotlin
val openAIService = Gateway.createOpenAIService(
    partialKey = "your-partial-key", // Get this from the Gateway dashboard
    serviceURL = "your-service-url"   // Get this from the Gateway dashboard
)
```

### 4. Make API requests

```kotlin
val response = openAIService.chatCompletion(
    request = ChatCompletionRequest(
        model = ModelId("gpt-4o-mini"),
        messages = listOf(ChatMessage(role = Role.User, content = "Hello, how are you?"))
    )
)
println(response.choices[0].message.content)
```

For a complete working example, check out the [sample/android](sample/android/)
and [sample/kmp](sample/kmp/) folders.

---

## 📚 Guides

Get started and understand more about how to use OpenAI API client for Kotlin with these guides:

- [Getting Started](guides/GettingStarted.md)
- [Chat & Function Call](guides/ChatToolCalls.md)
- [FileSource Guide](guides/FileSource.md)
- [Assistants](guides/Assistants.md)

## ⭐️ Support

Appreciate the project? Here's how you can help:

1. **Star**: Give it a star at the top right. It means a lot!
2. **Contribute**: Found an issue or have a feature idea? Submit a PR.
3. **Feedback**: Have suggestions? Open an issue or start a discussion.

## 📄 License

Gateway AI Client is an open-sourced software licensed under the [MIT license](LICENSE.md).
**This is an unofficial library, it is not affiliated with nor endorsed by Open AI**. Contributions
are welcome.

## 📝 Acknowledgments

This project starts as a fork of [OpenAI Kotlin Client](https://github.com/aallam/openai-kotlin) and
the great work of all its contributors. Thank you.
