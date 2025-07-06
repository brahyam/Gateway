package io.github.brahyam.gateway.sample

import android.app.Application
import io.github.brahyam.gateway.client.Gateway

class GatewayApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Configure Gateway before using any service (example: with Google Cloud Project Number and anonymous ID)
        Gateway.configure(
            googleCloudProjectNumber = BuildConfig.GOOGLE_CLOUD_PROJECT_NUMBER,
            enableAnonymousId = false // Set to true if you want to enable anonymous ID
        )
        println("initialized Gateway ${Gateway.VERSION}")
    }
} 