package io.github.brahyam.gateway.sample

import android.app.Application
import com.aallam.openai.api.logging.LogLevel
import io.github.brahyam.gateway.client.Gateway
import io.github.brahyam.gateway.client.GatewayConfig

class GatewayApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Gateway.configure(GatewayConfig(LogLevel.All))
    }
} 