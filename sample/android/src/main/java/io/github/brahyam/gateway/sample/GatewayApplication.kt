package io.github.brahyam.gateway.sample

import android.app.Application
import io.github.brahyam.gateway.client.Gateway
import io.github.brahyam.gateway.client.GatewayConfig

class GatewayApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Gateway.configure(GatewayConfig(BuildConfig.GOOGLE_CLOUD_PROJECT_NUMBER))
    }
} 