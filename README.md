# Gateway AI Client

[![Maven Central](https://img.shields.io/maven-central/v/io.github.brahyam/gateway-client?color=blue&label=Download)](https://central.sonatype.com/namespace/com.aallam.openai)
[![License](https://img.shields.io/github/license/brahyam/gateway-kmp?color=yellow)](LICENSE.md)
[![Documentation](https://img.shields.io/badge/docs-api-a97bff.svg?logo=kotlin)](https://docs.meetgateway.com/)

Android and iOS client for [Gateway secure AI API](https://docs.meetgateway.com/)

[Jump to the iOS setup](#-ios--swift-package-manager-setup)

## 📦 Android / KMP Setup

1. Install Gateway AI Client by adding the following dependency to your `build.gradle` file:

```groovy
repositories {
    mavenCentral()
}

dependencies {
   implementation "io.github.brahyam:gateway-client:0.1.0"
}
```

2. Configure the client early in your application startup:

```kotlin
import android.app.Application
import io.github.brahyam.gateway.client.Gateway
import io.github.brahyam.gateway.client.GatewayConfig

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val config = GatewayConfig(googleCloudProjectNumber = 1234567890) // your Google Cloud project number
        Gateway.configure(config)
    }
} 
```

3. Create an instance of a service eg. OpenAI service:

For development use the unprotected service (requests go straight to the OpenAI API):

```kotlin
val openAiService = Gateway.unprotectedOpenAIService(
    apiKey = "your-openai-api-key", // get it from OpenAI dashboard
)
```

For production use the protected service (requests go through the Gateway and are protected with
device attestation, certificate pinning, api key protection, ip rate limiting):

```kotlin
val openAiService = Gateway.protectedOpenAIService(
    partialKey = "your-partial-key", // get it from Gateway dashboard
    serviceUrl = "your service url" // get it from Gateway dashboard
)
```

4. Use your `openAiService` to make API requests. [Learn more](guides/GettingStarted.md).

```kotlin
// suspending function
val response = openAiService.chatCompletion(
    request = ChatCompletionRequest(
        model = ModelId("gpt-4o-mini"),
        messages = listOf(ChatMessage(role = Role.User, content = "Hello, how are you?"))
    )
)
println(response.choices[0].message.content)
```

## 📦 iOS / Swift Package Manager Setup

1. Install Gateway AI Client by adding the following dependency to your `Package.swift` file:

```swift
dependencies: [
    .package(url: "https://github.com/brahyam/gateway-kmp.git", from: "0.1.0")
]
```

2. Configure the client early in your application startup (in AppDelegate or SceneDelegate):

```swift
import Gateway

@main
struct MyApp: App {
    init() {
        Gateway.configure()
    }
}
```

or

```swift
import Gateway

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        Gateway.configure()
        return true
    }
}
```

3. Create an instance of a service eg. OpenAI service:

For development use the unprotected service (requests go straight to the OpenAI API):

```swift
import Gateway

let openAiService = Gateway.unprotectedOpenAIService(
    apiKey = "your-openai-api-key", // get it from OpenAI dashboard
)

For production use the protected service (requests go through the Gateway and are protected with device attestation, certificate pinning, api key protection, ip rate limiting):

```swift
let openAiService = Gateway.protectedOpenAIService(
    partialKey = "your-partial-key", // get it from Gateway dashboard
    serviceUrl = "your service url" // get it from Gateway dashboard
)
```

4. Use your `openAiService` to make API requests. [Learn more](guides/GettingStarted.md).

```swift
// async function
let response = openAiService.chatCompletion(
    request = ChatCompletionRequest(
        model = ModelId("gpt-4o-mini"),
        messages = listOf(ChatMessage(role = Role.User, content = "Hello, how are you?"))
    )
)
print(response.choices[0].message.content)
```

### Supported features

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

Sample apps are available under `sample`, please check the [README](sample/README.md) for running instructions.

## 🔒 ProGuard / R8

The specific rules are [already bundled](openai-core/src/jvmMain/resources/META-INF/proguard/openai.pro) into the Jar which can be interpreted by R8 automatically.

## 📸 Snapshots

[![Snapshot](https://img.shields.io/badge/dynamic/xml?url=https://oss.sonatype.org/service/local/repositories/snapshots/content/io/github/brahyam/openai-client/maven-metadata.xml&label=snapshot&color=red&query=.//versioning/latest)](https://oss.sonatype.org/content/repositories/snapshots/io/github/brahyam/openai-client/)

<details>
 <summary>Learn how to import snapshot version</summary>

To import snapshot versions into your project, add the following code snippet to your gradle file:

```groovy
repositories {
   //...
   maven { url 'https://oss.sonatype.org/content/repositories/snapshots/' }
}
```

</details>

## 🛠️ Troubleshooting

For common issues and their solutions, check the [Troubleshooting Guide](TROUBLESHOOTING.md).

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

### iOS (Swift Package Manager)

Add the Gateway package to your iOS project:

```swift
// In Xcode: File → Add Package Dependencies
// URL: https://github.com/brahyam/gateway-kmp.git

import Gateway

// Use the Gateway library
let gateway = Gateway()
```

The Swift Package is hosted directly in the root directory of this repository, making it easy to
integrate.

### Android

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
   implementation("io.github.brahyam:gateway-client:0.1.0")
}
```

### JVM

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
   implementation("io.github.brahyam:gateway-client:0.1.0")
}
```

## Building

### Prerequisites

- JDK 11+
- Gradle 8.0+
- Xcode 12.0+ (for iOS builds)
- Android SDK (for Android builds)

### Build Commands

```bash
# Build all platforms
./gradlew build

# Build XCFramework for iOS
./gradlew :gateway-client:assembleGatewayXCFramework

# Build and setup Swift Package Manager (recommended)
./scripts/build-swift-package.sh
```

## Publishing

### Automated Release

1. Update version in `gradle.properties`
2. Create and push a tag:
   ```bash
   git tag v0.1.1
   git push origin v0.1.1
   ```
3. GitHub Actions will automatically:
   - Build the XCFramework
   - Create a GitHub release
   - Update Package.swift in root directory

### Manual Setup

```bash
# Build and setup Swift Package Manager
./scripts/build-swift-package.sh
```

## Documentation

- [API Documentation](docs/)
- [Sample Projects](sample/)

## Repository Structure

```
gateway-kmp/
├── Package.swift              # Swift Package manifest (root directory)
├── gateway-client/            # Main Kotlin Multiplatform module
├── .github/workflows/         # Automated release workflows
├── scripts/                   # Build and setup scripts
└── docs/                      # Documentation
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
