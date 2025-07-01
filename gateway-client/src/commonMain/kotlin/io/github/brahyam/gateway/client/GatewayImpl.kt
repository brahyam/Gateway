package io.github.brahyam.gateway.client

internal interface GatewayImpl {
    suspend fun warmUpAttestation()
    suspend fun getIntegrityToken(): String
    suspend fun getAnonymousId(): String
}

internal expect fun createGatewayImpl(config: GatewayConfig): GatewayImpl