package io.github.brahyam.gateway.client

import gateway_kmp.gateway_client.BuildConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient

internal actual fun createPinnedEngine(): HttpClientEngine {
    val certificatePinner = CertificatePinner.Builder()
        .add(BuildConfig.GATEWAY_HOST, *BuildConfig.GATEWAY_PINS)
        .build()

    return OkHttp.create {
        preconfigured = OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .build()
    }
}
