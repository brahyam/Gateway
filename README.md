# Gateway AI Client

[![Maven Central](https://img.shields.io/maven-central/v/io.github.brahyam/gateway-client?color=blue&label=Download)](https://central.sonatype.com/namespace/io.github.brahyam)
[![License](https://img.shields.io/github/license/brahyam/gateway-kmp?color=yellow)](LICENSE.md)
[![Documentation](https://img.shields.io/badge/docs-api-a97bff.svg?logo=kotlin)](https://docs.meetgateway.com/)

Android and iOS client for accessing different AI providers (OpenAI, Claude...) directly or with API
key protection through [Gateway's servers](https://meetgateway.com/)

## 📦 KMP (Kotlin Multiplatform) Setup

1. Add the Gateway AI Client to your **common** dependencies in your shared module's
   `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.brahyam:gateway-client:0.1.9")
}
```

2. Configure Gateway early in your **common code** (e.g., in your `RootComponent`, `MainViewModel`,
   or shared initialization logic):

```kotlin
import io.github.brahyam.gateway.client.Gateway
import io.github.brahyam.gateway.client.GatewayConfig

class RootComponent {
    init {
        // Replace with your actual Google Cloud Project Number
        Gateway.configure(
            googleCloudProjectNumber = YOUR_GCP_PROJECT_NUMBER
        )
    }
}
```

3. Create an instance of a service, e.g., OpenAI service, as shown in the Android section below. The
   usage is the same across platforms.

---

## 📦 Android Setup

1. Add the Gateway AI Client to your `build.gradle` file:

```groovy
repositories {
    mavenCentral()
}

dependencies {
   implementation "io.github.brahyam:gateway-client:0.1.9"
}
```

2. Configure Gateway early in your **Android Application** class:

```kotlin
import android.app.Application
import io.github.brahyam.gateway.client.Gateway
import io.github.brahyam.gateway.client.GatewayConfig

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Replace with your actual Google Cloud Project Number (see https://console.cloud.google.com/welcome)
        Gateway.configure(
            googleCloudProjectNumber = YOUR_GCP_PROJECT_NUMBER
        )
    }
}
```

3. Create an instance of a service, e.g., OpenAI service:

You can use either the direct (unprotected) service for development, or the Gateway-protected service for production. See the [sample app](sample/README.md) for a complete example.

**Direct (Unprotected) OpenAI Service**

Use this for development. Requests go straight to the OpenAI API.

```kotlin
import io.github.brahyam.gateway.client.Gateway
import io.github.brahyam.gateway.client.OpenAIService

// ... inside your Activity or shared code ...

val openAIService: OpenAIService = Gateway.createDirectOpenAIService(
   apiKey = "your-openai-api-key"
)
```

**Gateway-Protected OpenAI Service**

Use this for production. Requests go through Gateway and are protected with device attestation, certificate pinning, API key protection, and rate limiting.

```kotlin
val openAIService: OpenAIService = Gateway.createOpenAIService(
    partialKey = "your-partial-key", // Get this from the Gateway dashboard
    serviceURL = "your-service-url"   // Get this from the Gateway dashboard
)
```

4. Use your `openAIService` to make API requests. [Learn more](guides/GettingStarted.md).

```kotlin
val response = openAIService.chatCompletion(
    request = ChatCompletionRequest(
        model = ModelId("gpt-4o-mini"),
        messages = listOf(ChatMessage(role = Role.User, content = "Hello, how are you?"))
    )
)
println(response.choices[0].message.content)
```

For a complete working example, check out the [sample app](sample/android/README.md).

### OpenAI Service Supported features

- [Models](guides/GettingStarted.md#models)
- [Chat](guides/GettingStarted.md#chat)
- [Images](guides/GettingStarted.md#images)
- [Embeddings](guides/GettingStarted.md#embeddings)
- [Files](guides/GettingStarted.md#files)
- [Fine-tuning](guides/GettingStarted.md#fine-tuning)
- [Moderations](guides/GettingStarted.md#moderations)
- [Audio](guides/GettingStarted.md#audio)

#### Beta

- [Assistants](guides/GettingStarted.md#assistants)
- [Threads](guides/GettingStarted.md#threads)
- [Messages](guides/GettingStarted.md#messages)
- [Runs](guides/GettingStarted.md#runs)

## 📚 Guides

Get started and understand more about how to use OpenAI API client for Kotlin with these guides:

- [Getting Started](guides/GettingStarted.md)
- [Chat & Function Call](guides/ChatToolCalls.md)
- [FileSource Guide](guides/FileSource.md)
- [Assistants](guides/Assistants.md)

## ℹ️ Sample apps

Sample apps are available under `sample`.

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
