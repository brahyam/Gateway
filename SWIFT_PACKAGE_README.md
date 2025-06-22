# Gateway Swift Package

This is the Swift Package Manager distribution for the Gateway library.

## Installation

Add this package to your Xcode project:

1. In Xcode, go to **File** → **Add Package Dependencies**
2. Enter the URL: `https://github.com/brahyam/gateway-kmp.git`
3. Add the package to your target

## Usage

1. Install Gateway AI Client by adding the following dependency to your `Package.swift` file:

```swift
dependencies: [
    .package(url: "https://github.com/brahyam/gateway-kmp.git", from: "v0.1.0")
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

## Requirements

- iOS 14.0+
- Xcode 12.0+
- Swift 5.3+

## Documentation

For more information, see the main repository: https://github.com/brahyam/gateway-kmp
