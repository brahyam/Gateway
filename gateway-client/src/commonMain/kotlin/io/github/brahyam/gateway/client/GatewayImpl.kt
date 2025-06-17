package io.github.brahyam.gateway.client

internal interface GatewayImpl {
    suspend fun warmUpAttestation()
}

internal expect fun createGatewayImpl(config: GatewayConfig): GatewayImpl