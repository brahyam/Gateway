package io.github.brahyam.gateway.client

internal actual fun createGatewayImpl(config: GatewayConfig): GatewayImpl {
    return IOSGatewayImpl(config)
}

internal class IOSGatewayImpl(
    private val config: GatewayConfig,
) : GatewayImpl {
    override suspend fun warmUpAttestation() {
        // Placeholder for iOS-specific implementation
    }

    override suspend fun getIntegrityToken(): String {
        return "" // Placeholder for iOS-specific implementation
    }
}