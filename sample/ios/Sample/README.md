# Gateway AI Client iOS Sample App

This sample app demonstrates how to use the Gateway AI Client on iOS to access OpenAI services via Gateway protection, using Swift and SwiftUI.

## 🚀 What does this app show?
- How to configure the Gateway SDK in your SwiftUI `@main` App struct
- How to use the Gateway-protected OpenAI service
- How to make a chat request and display the response in a SwiftUI interface

## 🛠 Setup Instructions

1. **Open the sample app in Xcode**
   - Open `Sample.xcodeproj` in Xcode.

2. **Add your API keys and configuration:**
   - In `ContentView.swift`, set:
     - `partialKey: "YOUR_GATEWAY_PARTIAL_KEY"` – Get from the Gateway dashboard
     - `serviceURL: "YOUR_GATEWAY_SERVICE_URL"` – Get from the Gateway dashboard

3. **Configure Gateway in your App struct:**
   - See `SampleApp.swift` for an example:
     ```swift
     import Gateway
     @main
     struct SampleApp: App {
         init() {
             // Initialize the Gateway SDK for iOS
             Gateway_.shared.configureIOS()
         }
         var body: some Scene {
             WindowGroup {
                 NavigationView {
                     ContentView()
                 }
             }
         }
     }
     ```

4. **Create and use the OpenAI service in your view:**
   - See `ContentView.swift` for usage:
     ```swift
     let openAiService = Gateway_.shared.createOpenAIService(
         partialKey: "YOUR_GATEWAY_PARTIAL_KEY",
         serviceURL: "YOUR_GATEWAY_SERVICE_URL"
     )
     ```
   - To send a chat message:
     ```swift
     let chatMessages = messages.map { msg in
         Gateway.Openai_coreChatMessage(role: msg.isFromUser ? "user" : "assistant", content: msg.text)
     }
     let allMessages = chatMessages + [Gateway.Openai_coreChatMessage(role: "user", content: inputText)]
     let request = Gateway.Openai_coreChatCompletionRequest(model: "gpt-4o-mini", messages: allMessages, reasoningEffort: nil, temperature: nil, topP: nil, n: nil, stop: nil, store: nil, maxTokens: nil, maxCompletionTokens: nil, presencePenalty: nil, frequencyPenalty: nil, logitBias: nil, user: nil, functions: nil, functionCall: nil, responseFormat: nil, tools: nil, toolChoice: nil, seed: nil, logprobs: nil, topLogprobs: nil, instanceId: nil, streamOptions: nil)
     let response = try await openAiService.chatCompletion(request: request, requestOptions: nil)
     if let firstChoice = response.choices.first, let content = firstChoice.message.content {
         // Use the response content
         print(content)
     }
     ```

5. **Build and run the app** on a simulator or device.

## 📚 More Information
- [Main Project README](../../../README.md)
- [Gateway Documentation](https://docs.meetgateway.com/)

---

If you have questions or issues, please open an issue or discussion in the main repository. 