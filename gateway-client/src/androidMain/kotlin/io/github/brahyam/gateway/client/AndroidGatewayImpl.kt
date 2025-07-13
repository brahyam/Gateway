package io.github.brahyam.gateway.client

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityException
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.pow
import kotlin.random.Random

@SuppressLint("PrivateApi")
internal actual fun createGatewayImpl(
    googleCloudProjectNumber: Long?,
    enableAnonymousId: Boolean,
): GatewayImpl {
    return try {
        val application = Class.forName("android.app.ActivityThread")
            .getMethod("currentApplication")
            .invoke(null) as Application
        AndroidGatewayImpl(
            googleCloudProjectNumber,
            enableAnonymousId,
            application.applicationContext
        )
    } catch (e: Exception) {
        Gateway.logger.error("Failed to create AndroidGatewayImpl: ${e.message}")
        // Return a fallback implementation that doesn't crash
        FallbackGatewayImpl(enableAnonymousId)
    }
}

internal class AndroidGatewayImpl(
    private val googleCloudProjectNumber: Long?,
    private val enableAnonymousId: Boolean,
    private val applicationContext: Context,
) : GatewayImpl {
    private var integrityTokenProvider: StandardIntegrityTokenProvider? = null
    private val prefs: SharedPreferences by lazy {
        applicationContext.getSharedPreferences("gateway_prefs", Context.MODE_PRIVATE)
    }
    private val ANON_ID_KEY = "gateway_anon_id"
    private var cachedAnonId: String? = null

    // Retry configuration
    private val maxRetries = 3
    private val baseDelayMs = 1000L
    private val maxDelayMs = 8000L
    private val operationTimeoutMs = 30000L // 30 seconds

    override suspend fun warmUpAttestation() {
        try {
            if (enableAnonymousId) {
                initializeAnonymousId()
            }
            if (integrityTokenProvider != null) return

            if (googleCloudProjectNumber == null) {
                Gateway.logger.warn("Google Cloud Project Number not provided. Integrity attestation will be unavailable.")
                return
            }

            integrityTokenProvider = prepareTokenProviderWithRetry()
        } catch (e: Exception) {
            Gateway.logger.error("Failed to warm up attestation: ${e.message}")
            // Don't throw - allow the app to continue without attestation
        }
    }

    private fun initializeAnonymousId() {
        try {
            var anonId = prefs.getString(ANON_ID_KEY, null)
            if (anonId == null) {
                anonId = UUID.randomUUID().toString()
                with(prefs.edit()) {
                    putString(ANON_ID_KEY, anonId)
                    apply()
                }
            }
            cachedAnonId = anonId
        } catch (e: Exception) {
            Gateway.logger.error("Failed to initialize anonymous ID: ${e.message}")
            // Generate a temporary ID for this session
            cachedAnonId = UUID.randomUUID().toString()
        }
    }

    private suspend fun prepareTokenProviderWithRetry(): StandardIntegrityTokenProvider? {
        return try {
            withContext(Dispatchers.IO) {
                var lastException: Exception? = null

                run breaking@{
                    repeat(maxRetries) { attempt ->
                        try {
                            Gateway.logger.debug("Preparing Integrity Token Provider (attempt ${attempt + 1}/$maxRetries)...")

                            val result = withTimeoutOrNull(operationTimeoutMs) {
                                val integrityManager =
                                    IntegrityManagerFactory.createStandard(applicationContext)
                                val request = PrepareIntegrityTokenRequest.builder()
                                    .setCloudProjectNumber(googleCloudProjectNumber!!)
                                    .build()

                                suspendCancellableCoroutine<StandardIntegrityTokenProvider> { continuation ->
                                    integrityManager.prepareIntegrityToken(request)
                                        .addOnSuccessListener { provider ->
                                            Gateway.logger.info("Prepared Integrity Token Provider successfully on attempt ${attempt + 1}.")
                                            continuation.resume(provider)
                                        }
                                        .addOnFailureListener { exception ->
                                            continuation.resumeWithException(exception)
                                        }
                                }
                            }

                            if (result != null) {
                                return@withContext result
                            } else {
                                throw RuntimeException("Timeout preparing Integrity Token Provider")
                            }
                        } catch (exception: Exception) {
                            lastException = exception
                            Gateway.logger.warn("Failed to prepare Integrity Token Provider on attempt ${attempt + 1}: ${exception.message}")

                            // Check if this is a retryable error
                            if (!isRetryableError(exception) || attempt == maxRetries - 1) {
                                return@breaking
                            }

                            // Calculate delay with exponential backoff and jitter
                            val delay = calculateBackoffDelay(attempt)
                            Gateway.logger.debug("Retrying in ${delay}ms...")
                            delay(delay)
                        }
                    }
                }

                Gateway.logger.error("Failed to prepare Integrity Token Provider after $maxRetries attempts: ${lastException?.message}")
                null // Return null instead of throwing
            }
        } catch (e: Exception) {
            Gateway.logger.error("Unexpected error preparing Integrity Token Provider: ${e.message}")
            null
        }
    }

    override suspend fun getIntegrityToken(): String {
        return try {
            val tokenProvider = integrityTokenProvider
            if (tokenProvider == null) {
                Gateway.logger.warn("Integrity Token Provider not available. Returning empty token.")
                return ""
            }

            requestIntegrityTokenWithRetry(tokenProvider) ?: ""
        } catch (e: Exception) {
            Gateway.logger.error("Failed to get integrity token: ${e.message}")
            "" // Return empty string instead of throwing
        }
    }

    private suspend fun requestIntegrityTokenWithRetry(tokenProvider: StandardIntegrityTokenProvider): String? {
        return try {
            withContext(Dispatchers.IO) {
                var lastException: Exception? = null

                run breaking@{
                    repeat(maxRetries) { attempt ->
                        try {
                            Gateway.logger.debug("Requesting Integrity Token (attempt ${attempt + 1}/$maxRetries)...")

                            val result = withTimeoutOrNull(operationTimeoutMs) {
                                suspendCancellableCoroutine<String> { continuation ->
                                    tokenProvider.request(
                                        StandardIntegrityTokenRequest.builder().build()
                                    )
                                        .addOnSuccessListener { response ->
                                            Gateway.logger.info("Successfully requested Integrity Token on attempt ${attempt + 1}.")
                                            continuation.resume(response.token())
                                        }
                                        .addOnFailureListener { exception ->
                                            continuation.resumeWithException(exception)
                                        }
                                }
                            }

                            if (result != null) {
                                return@withContext result
                            } else {
                                throw RuntimeException("Timeout requesting Integrity Token")
                            }
                        } catch (exception: Exception) {
                            lastException = exception
                            Gateway.logger.warn("Failed to request Integrity Token on attempt ${attempt + 1}: ${exception.message}")

                            // Handle specific error code -19 (provider invalid) by re-initializing
                            if (exception is StandardIntegrityException && exception.errorCode == -19) {
                                Gateway.logger.warn("Integrity Token Provider invalid, re-initializing...")
                                try {
                                    integrityTokenProvider = prepareTokenProviderWithRetry()
                                    // Continue with the retry loop using the new provider
                                    return@repeat
                                } catch (reinitException: Exception) {
                                    Gateway.logger.error("Failed to re-initialize Integrity Token Provider: ${reinitException.message}")
                                    return@breaking
                                }
                            }

                            // Check if this is a retryable error
                            if (!isRetryableError(exception) || attempt == maxRetries - 1) {
                                return@breaking
                            }

                            // Calculate delay with exponential backoff and jitter
                            val delay = calculateBackoffDelay(attempt)
                            Gateway.logger.debug("Retrying in ${delay}ms...")
                            delay(delay)
                        }
                    }
                }

                Gateway.logger.error("Failed to request Integrity Token after $maxRetries attempts: ${lastException?.message}")
                null // Return null instead of throwing
            }
        } catch (e: Exception) {
            Gateway.logger.error("Unexpected error requesting Integrity Token: ${e.message}")
            null
        }
    }

    private fun isRetryableError(exception: Exception): Boolean {
        return when (exception) {
            is StandardIntegrityException -> {
                when (exception.errorCode) {
                    -100, // INTERNAL_ERROR - transient, should retry
                    -19,  // INTEGRITY_TOKEN_PROVIDER_INVALID - can retry after re-init
                    -8,   // NETWORK_ERROR - transient, should retry
                    -9,   // PLAY_STORE_NOT_FOUND - might be transient
                    -10,   // PLAY_STORE_VERSION_OUTDATED - might be transient
                        -> true
                    else -> false
                }
            }
            is RuntimeException -> {
                // Timeout errors are retryable
                exception.message?.contains("Timeout", ignoreCase = true) == true
            }
            else -> false
        }
    }

    private fun calculateBackoffDelay(attempt: Int): Long {
        return try {
            // Exponential backoff: baseDelay * 2^attempt with jitter
            val exponentialDelay = (baseDelayMs * 2.0.pow(attempt)).toLong()
            val cappedDelay = minOf(exponentialDelay, maxDelayMs)

            // Add jitter (±25% of the delay)
            val jitter = (cappedDelay * 0.25 * (Random.nextDouble() - 0.5)).toLong()
            cappedDelay + jitter
        } catch (e: Exception) {
            Gateway.logger.error("Error calculating backoff delay: ${e.message}")
            baseDelayMs // Return base delay as fallback
        }
    }

    override suspend fun getAnonymousId(): String {
        return try {
            if (!enableAnonymousId) {
                Gateway.logger.warn("Anonymous ID not enabled in config")
                return ""
            }

            cachedAnonId ?: prefs.getString(ANON_ID_KEY, null) ?: run {
                Gateway.logger.warn("Anonymous ID not initialized. Generating temporary ID.")
                UUID.randomUUID().toString()
            }
        } catch (e: Exception) {
            Gateway.logger.error("Failed to get anonymous ID: ${e.message}")
            UUID.randomUUID().toString() // Return temporary ID instead of throwing
        }
    }
}

/**
 * Fallback implementation that provides basic functionality without crashing
 */
internal class FallbackGatewayImpl(
    private val enableAnonymousId: Boolean,
) : GatewayImpl {
    private var fallbackAnonId: String? = null

    override suspend fun warmUpAttestation() {
        try {
            if (enableAnonymousId) {
                fallbackAnonId = UUID.randomUUID().toString()
            }
            Gateway.logger.warn("Using fallback implementation - attestation features unavailable")
        } catch (e: Exception) {
            Gateway.logger.error("Error in fallback warm up: ${e.message}")
        }
    }

    override suspend fun getIntegrityToken(): String {
        Gateway.logger.warn("Integrity token unavailable in fallback mode")
        return ""
    }

    override suspend fun getAnonymousId(): String {
        return try {
            if (!enableAnonymousId) {
                Gateway.logger.warn("Anonymous ID not enabled in config")
                return ""
            }
            fallbackAnonId ?: UUID.randomUUID().toString().also { fallbackAnonId = it }
        } catch (e: Exception) {
            Gateway.logger.error("Error getting anonymous ID in fallback mode: ${e.message}")
            UUID.randomUUID().toString()
        }
    }
}

internal actual fun getDeviceType(): String = "android"