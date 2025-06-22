package io.github.brahyam.gateway.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.DeviceCheck.DCDevice
import platform.Foundation.base64Encoding
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal actual fun createGatewayImpl(config: GatewayConfig): GatewayImpl {
    return IOSGatewayImpl(config)
}

internal class IOSGatewayImpl(
    private val config: GatewayConfig,
) : GatewayImpl {
    override suspend fun warmUpAttestation() {
        // iOS DCDevice doesn't require warm-up like Android's Integrity API
        // The device token is available immediately
    }

    override suspend fun getIntegrityToken(): String {
        println("Requesting iOS Device Check Token...")
        return withContext(Dispatchers.Default) {
            suspendCancellableCoroutine { continuation ->
                DCDevice.currentDevice().generateTokenWithCompletionHandler { token, error ->
                    if (error != null) {
                        val nsError = error
                        continuation.resumeWithException(
                            RuntimeException(
                                "Failed to generate iOS Device Check token: ${nsError.localizedDescription}",
                                Exception(nsError.localizedDescription)
                            )
                        )
                    } else if (token != null) {
                        println("Successfully generated iOS Device Check Token.")
                        // Convert the token data to base64 string
                        val tokenString = token.base64Encoding()
                        continuation.resume(tokenString)
                    } else {
                        continuation.resumeWithException(
                            RuntimeException("iOS Device Check token generation returned null")
                        )
                    }
                }
            }
        }
    }
}