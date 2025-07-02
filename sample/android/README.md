# Gateway AI Client Android Sample App

This sample app demonstrates how to use the Gateway AI Client on Android to access OpenAI services, both directly and via Gateway protection.

## 🚀 What does this app show?
- How to configure the Gateway client in your `Application` class
- How to use both direct (unprotected) and Gateway-protected OpenAI services
- How to make a simple chat request and display the response in a Compose UI

## 🛠 Setup Instructions

1. **Clone the repository and open the project in Android Studio.**

2. **Add your API keys and configuration:**
   - In your `local.properties` or as environment variables, set:
     - `OPENAI_API_KEY` – Get from [OpenAI dashboard](https://platform.openai.com/api-keys)
     - `GATEWAY_PARTIAL_KEY` and `GATEWAY_SERVICE_URL` – Get from the Gateway dashboard
     - `GOOGLE_CLOUD_PROJECT_NUMBER` – Get from [Google Cloud Console](https://console.cloud.google.com/welcome)

3. **Configure Gateway in your Application class:**
   - See `GatewayApplication.kt` for an example:
     ```kotlin
     class MyApplication : Application() {
         override fun onCreate() {
             super.onCreate()
             Gateway.configure(GatewayConfig(googleCloudProjectNumber = "YOUR_GCP_PROJECT_NUMBER"))
         }
     }
     ```

4. **Choose your OpenAI service in your Activity:**
   - For development (direct, unprotected):
     ```kotlin
     val openAIService = Gateway.createDirectOpenAIService(
         apiKey = "your-openai-api-key" // from OpenAI dashboard
     )
     ```
   - For production (Gateway-protected):
     ```kotlin
     val openAIService = Gateway.createOpenAIService(
         partialKey = "your-partial-key", // from Gateway dashboard
         serviceURL = "your-service-url"   // from Gateway dashboard
     )
     ```
   - See `MainActivity.kt` for usage details.

5. **Build and run the app** on an emulator or device.

## 📚 More Information
- [Main Project README](../../README.md)
- [Gateway Documentation](https://docs.meetgateway.com/)

---

If you have questions or issues, please open an issue or discussion in the main repository. 