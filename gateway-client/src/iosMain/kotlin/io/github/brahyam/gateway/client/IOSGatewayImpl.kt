package io.github.brahyam.gateway.client

internal actual fun createGatewayImpl(config: GatewayConfig): GatewayImpl {
    return IOSGatewayImpl(config)
}

internal class IOSGatewayImpl(
    private val config: GatewayConfig,
) : GatewayImpl {
    override suspend fun warmUpAttestation() {
        // No-op for non-Android platforms
    }
}