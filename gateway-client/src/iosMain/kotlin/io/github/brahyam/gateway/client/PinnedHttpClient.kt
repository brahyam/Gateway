package io.github.brahyam.gateway.client

import gateway_kmp.gateway_client.BuildConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.certificates.CertificatePinner
import kotlinx.cinterop.UnsafeNumber

@OptIn(UnsafeNumber::class)
internal actual fun createPinnedEngine(): HttpClientEngine {
    val pinner = CertificatePinner.Builder()
        .add(BuildConfig.GATEWAY_HOST, *BuildConfig.GATEWAY_PINS)
        .build()

    val engine = Darwin.create {
        handleChallenge(pinner)
    }

    return engine
}
