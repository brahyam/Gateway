package io.github.brahyam.gateway.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.DeviceCheck.DCDevice
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import platform.Foundation.base64Encoding
import platform.UIKit.UIDevice
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal actual fun createGatewayImpl(config: GatewayConfig): GatewayImpl {
    return IOSGatewayImpl(config)
}

internal class IOSGatewayImpl(
    private val config: GatewayConfig,
) : GatewayImpl {
    private var cachedAnonId: String? = null
    override suspend fun warmUpAttestation() {
        if (config.enableAnonymousId) {
            val defaults = NSUserDefaults.standardUserDefaults
            val key = "gateway_anon_id"
            var anonId = defaults.stringForKey(key)
            if (anonId == null) {
                anonId = NSUUID().UUIDString
                defaults.setObject(anonId, forKey = key)
            }
            cachedAnonId = anonId
        }
        // iOS DCDevice doesn't require warm-up like Android's Integrity API
        // The device token is available immediately
    }

    override suspend fun getIntegrityToken(): String {
        println("[Gateway] Entering getIntegrityToken() - Requesting iOS Device Check Token...")

        // Simulator detection
        val isSimulator = UIDevice.currentDevice.model.contains("Simulator")
        if (isSimulator) {
            println("[Gateway] Running on Simulator - returning dummy Device Check token.")
            return "SIMULATOR_DUMMY_TOKEN"
        }

        return withContext(Dispatchers.Default) {
            suspendCancellableCoroutine { continuation ->
                DCDevice.currentDevice().generateTokenWithCompletionHandler { token, error ->
                    if (error != null) {
                        val nsError = error
                        println("[Gateway] Error generating iOS Device Check token: ${nsError.localizedDescription}")
                        continuation.resumeWithException(
                            RuntimeException(
                                "Failed to generate iOS Device Check token: ${nsError.localizedDescription}",
                                Exception(nsError.localizedDescription)
                            )
                        )
                        println("[Gateway] Coroutine resumed with exception (error case)")
                    } else if (token != null) {
                        println("[Gateway] Successfully generated iOS Device Check Token.")
                        // Convert the token data to base64 string
                        val tokenString = token.base64Encoding()
                        continuation.resume(tokenString)
                        println("[Gateway] Coroutine resumed with token (success case)")
                    } else {
                        println("[Gateway] iOS Device Check token generation returned null (no token, no error)")
                        continuation.resumeWithException(
                            RuntimeException("iOS Device Check token generation returned null")
                        )
                        println("[Gateway] Coroutine resumed with exception (null case)")
                    }
                }
            }
        }.also {
            println("[Gateway] Exiting getIntegrityToken()")
        }
    }

    override suspend fun getAnonymousId(): String {
        if (!config.enableAnonymousId) throw IllegalStateException("Anonymous ID not enabled in config")
        return cachedAnonId ?: NSUserDefaults.standardUserDefaults.stringForKey("gateway_anon_id")
        ?: throw IllegalStateException("Anonymous ID not initialized. Call warmUpAttestation() first.")
    }
}

public fun Gateway.configureIOS(enableAnonymousId: Boolean = false) {
    this.configure(
        GatewayConfig(
            googleCloudProjectNumber = null,
            enableAnonymousId = enableAnonymousId
        )
    )
}

internal actual fun getDeviceType(): String = "ios"