package io.github.brahyam.gateway.client

import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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
    return try {
        IOSGatewayImpl(enableAnonymousId)
    } catch (e: Exception) {
        Gateway.log("Failed to create IOSGatewayImpl: ${e.message}")
        // Return a fallback implementation that doesn't crash
        IOSFallbackGatewayImpl(enableAnonymousId)
    }
}

internal class IOSGatewayImpl(
    private val enableAnonymousId: Boolean,
) : GatewayImpl {
    private var cachedAnonId: String? = null
    private val operationTimeoutMs = 30000L // 30 seconds
    
    override suspend fun warmUpAttestation() {
        try {
            if (enableAnonymousId) {
                initializeAnonymousId()
            }
            // iOS DCDevice doesn't require warm-up like Android's Integrity API
            // The device token is available immediately
            Gateway.log("iOS attestation warmed up successfully")
        } catch (e: Exception) {
            Gateway.log("Failed to warm up iOS attestation: ${e.message}")
            // Don't throw - allow the app to continue without attestation
        }
    }

    private fun initializeAnonymousId() {
        try {
            val defaults = NSUserDefaults.standardUserDefaults
            val key = "gateway_anon_id"
            var anonId = defaults.stringForKey(key)
            if (anonId == null) {
                anonId = NSUUID().UUIDString
                defaults.setObject(anonId, forKey = key)
            }
            cachedAnonId = anonId
        } catch (e: Exception) {
            Gateway.log("Failed to initialize anonymous ID: ${e.message}")
            // Generate a temporary ID for this session
            cachedAnonId = try {
                NSUUID().UUIDString
            } catch (uuidError: Exception) {
                Gateway.log("Failed to generate UUID: ${uuidError.message}")
                "temp-${getTimeMillis()}"
            }
        }
    }

    override suspend fun getIntegrityToken(): String {
        return try {
            Gateway.log("Entering getIntegrityToken() - Requesting iOS Device Check Token...")

            // Simulator detection with error handling
            val deviceInfo = getDeviceInfo()
            if (deviceInfo.isSimulator || !deviceInfo.isSupported) {
                Gateway.log("Running on Simulator or unsupported device (${deviceInfo.model}) - returning dummy Device Check token.")
                return "DUMMY_TOKEN"
            }

            generateDeviceCheckToken() ?: ""
        } catch (e: Exception) {
            Gateway.log("Failed to get iOS integrity token: ${e.message}")
            "" // Return empty string instead of throwing
        }
    }

    private data class DeviceInfo(
        val model: String,
        val isSimulator: Boolean,
        val isSupported: Boolean,
    )

    private fun getDeviceInfo(): DeviceInfo {
        return try {
            val device = UIDevice.currentDevice.model
            val isSimulator = device.contains("Simulator")
            val isSupported = try {
                DCDevice.currentDevice.isSupported()
            } catch (e: Exception) {
                Gateway.log("Failed to check device support: ${e.message}")
                false
            }

            Gateway.log("Device model: $device, isSimulator: $isSimulator, isSupported: $isSupported")
            DeviceInfo(device, isSimulator, isSupported)
        } catch (e: Exception) {
            Gateway.log("Failed to get device info: ${e.message}")
            DeviceInfo(
                model = "Unknown",
                isSimulator = true,
                isSupported = false
            ) // Assume simulator/unsupported on error
        }
    }

    private suspend fun generateDeviceCheckToken(): String? {
        return try {
            withContext(Dispatchers.Default) {
                withTimeoutOrNull(operationTimeoutMs) {
                    suspendCancellableCoroutine { continuation ->
                        try {
                            DCDevice.currentDevice.generateTokenWithCompletionHandler { token, error ->
                                try {
                                    if (error != null) {
                                        val nsError = error
                                        Gateway.log("Error generating iOS Device Check token: ${nsError.localizedDescription}")
                                        continuation.resumeWithException(
                                            RuntimeException(
                                                "Failed to generate iOS Device Check token: ${nsError.localizedDescription}",
                                                Exception(nsError.localizedDescription)
                                            )
                                        )
                                    } else if (token != null) {
                                        Gateway.log("Successfully generated iOS Device Check Token.")
                                        try {
                                            // Convert the token data to base64 string
                                            val tokenString = token.base64Encoding()
                                            continuation.resume(tokenString)
                                        } catch (encodingError: Exception) {
                                            Gateway.log("Failed to encode token: ${encodingError.message}")
                                            continuation.resumeWithException(
                                                RuntimeException(
                                                    "Failed to encode Device Check token",
                                                    encodingError
                                                )
                                            )
                                        }
                                    } else {
                                        Gateway.log("iOS Device Check token generation returned null (no token, no error)")
                                        continuation.resumeWithException(
                                            RuntimeException("iOS Device Check token generation returned null")
                                        )
                                    }
                                } catch (handlerError: Exception) {
                                    Gateway.log("Error in completion handler: ${handlerError.message}")
                                    continuation.resumeWithException(
                                        RuntimeException(
                                            "Error in completion handler",
                                            handlerError
                                        )
                                    )
                                }
                            }
                        } catch (generateError: Exception) {
                            Gateway.log("Failed to start token generation: ${generateError.message}")
                            continuation.resumeWithException(
                                RuntimeException("Failed to start token generation", generateError)
                            )
                        }
                    }
                } ?: run {
                    Gateway.log("Device Check token generation timed out")
                    throw RuntimeException("Device Check token generation timed out")
                }
            }
        } catch (e: Exception) {
            Gateway.log("Unexpected error generating Device Check token: ${e.message}")
            null
        }
    }

    override suspend fun getAnonymousId(): String {
        return try {
            if (!enableAnonymousId) {
                Gateway.log("Anonymous ID not enabled in config")
                return ""
            }

            cachedAnonId ?: run {
                try {
                    NSUserDefaults.standardUserDefaults.stringForKey("gateway_anon_id") ?: run {
                        Gateway.log("Anonymous ID not initialized. Generating temporary ID.")
                        NSUUID().UUIDString
                    }
                } catch (e: Exception) {
                    Gateway.log("Failed to get anonymous ID from defaults: ${e.message}")
                    try {
                        NSUUID().UUIDString
                    } catch (uuidError: Exception) {
                        Gateway.log("Failed to generate UUID: ${uuidError.message}")
                        "temp-${getTimeMillis()}"
                    }
                }
            }
        } catch (e: Exception) {
            Gateway.log("Failed to get anonymous ID: ${e.message}")
            try {
                NSUUID().UUIDString
            } catch (uuidError: Exception) {
                Gateway.log("Failed to generate fallback UUID: ${uuidError.message}")
                "temp-${getTimeMillis()}"
            }
        }
    }
}

/**
 * Fallback implementation for iOS that provides basic functionality without crashing
 */
internal class IOSFallbackGatewayImpl(
    private val enableAnonymousId: Boolean,
) : GatewayImpl {
    private var fallbackAnonId: String? = null

    override suspend fun warmUpAttestation() {
        try {
            if (enableAnonymousId) {
                fallbackAnonId = try {
                    NSUUID().UUIDString
                } catch (e: Exception) {
                    Gateway.log("Failed to generate UUID in fallback: ${e.message}")
                    "fallback-${getTimeMillis()}"
                }
            }
            Gateway.log("Using iOS fallback implementation - attestation features unavailable")
        } catch (e: Exception) {
            Gateway.log("Error in iOS fallback warm up: ${e.message}")
        }
    }

    override suspend fun getIntegrityToken(): String {
        Gateway.log("Integrity token unavailable in iOS fallback mode")
        return ""
    }

    override suspend fun getAnonymousId(): String {
        return try {
            if (!enableAnonymousId) {
                Gateway.log("Anonymous ID not enabled in config")
                return ""
            }
            fallbackAnonId ?: try {
                NSUUID().UUIDString.also { fallbackAnonId = it }
            } catch (e: Exception) {
                Gateway.log("Failed to generate UUID in fallback getAnonymousId: ${e.message}")
                "fallback-${getTimeMillis()}".also { fallbackAnonId = it }
            }
        } catch (e: Exception) {
            Gateway.log("Error getting anonymous ID in iOS fallback mode: ${e.message}")
            "fallback-${getTimeMillis()}"
        }
    }
}

internal actual fun getDeviceType(): String = "ios"