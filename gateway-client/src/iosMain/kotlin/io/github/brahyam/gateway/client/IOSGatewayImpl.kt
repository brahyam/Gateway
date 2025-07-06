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

internal actual fun createGatewayImpl(
    googleCloudProjectNumber: Long?,
    enableAnonymousId: Boolean,
): GatewayImpl {
    return IOSGatewayImpl(enableAnonymousId)
}

internal class IOSGatewayImpl(
    private val enableAnonymousId: Boolean,
) : GatewayImpl {
    private var cachedAnonId: String? = null
    override suspend fun warmUpAttestation() {
        if (enableAnonymousId) {
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
        Gateway.logger.debug("Entering getIntegrityToken() - Requesting iOS Device Check Token...")

        // Simulator detection
        val device = UIDevice.currentDevice.model
        val isSimulator = device.contains("Simulator")
        val deviceNotSupported = DCDevice.currentDevice.isSupported()
        Gateway.logger.debug("Device model: ${UIDevice.currentDevice.model}")
        if (isSimulator || !deviceNotSupported) {
            Gateway.logger.warn("Running on Simulator or not supported device ($device) - returning dummy Device Check token.")
            return "DUMMY_TOKEN"
        }

        return withContext(Dispatchers.Default) {
            suspendCancellableCoroutine { continuation ->
                DCDevice.currentDevice.generateTokenWithCompletionHandler { token, error ->
                    if (error != null) {
                        val nsError = error
                        Gateway.logger.error("Error generating iOS Device Check token: ${nsError.localizedDescription}")
                        continuation.resumeWithException(
                            RuntimeException(
                                "Failed to generate iOS Device Check token: ${nsError.localizedDescription}",
                                Exception(nsError.localizedDescription)
                            )
                        )
                        Gateway.logger.error("Coroutine resumed with exception (error case)")
                    } else if (token != null) {
                        Gateway.logger.info("Successfully generated iOS Device Check Token.")
                        // Convert the token data to base64 string
                        val tokenString = token.base64Encoding()
                        continuation.resume(tokenString)
                        Gateway.logger.debug("Coroutine resumed with token (success case)")
                    } else {
                        Gateway.logger.error("iOS Device Check token generation returned null (no token, no error)")
                        continuation.resumeWithException(
                            RuntimeException("iOS Device Check token generation returned null")
                        )
                        Gateway.logger.error("Coroutine resumed with exception (null case)")
                    }
                }
            }
        }.also {
            Gateway.logger.debug("Exiting getIntegrityToken()")
        }
    }

    override suspend fun getAnonymousId(): String {
        if (!enableAnonymousId) throw IllegalStateException("Anonymous ID not enabled in config")
        return cachedAnonId ?: NSUserDefaults.standardUserDefaults.stringForKey("gateway_anon_id")
        ?: throw IllegalStateException("Anonymous ID not initialized. Call warmUpAttestation() first.")
    }
}

public fun Gateway.configureIOS() {
    this.configure(
        googleCloudProjectNumber = null,
        enableAnonymousId = false,
        logger = null
    )
}

public fun Gateway.configureIOS(enableAnonymousId: Boolean, logger: Logger?) {
    this.configure(
        googleCloudProjectNumber = null,
        enableAnonymousId = enableAnonymousId,
        logger = logger
    )
}

internal actual fun getDeviceType(): String = "ios"