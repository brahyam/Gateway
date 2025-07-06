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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@SuppressLint("PrivateApi")
internal actual fun createGatewayImpl(
    googleCloudProjectNumber: Long?,
    enableAnonymousId: Boolean,
): GatewayImpl {
    val application = Class.forName("android.app.ActivityThread")
        .getMethod("currentApplication")
        .invoke(null) as Application
    return AndroidGatewayImpl(
        googleCloudProjectNumber,
        enableAnonymousId,
        application.applicationContext
    )
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

    override suspend fun warmUpAttestation() {
        if (enableAnonymousId) {
            var anonId = prefs.getString(ANON_ID_KEY, null)
            if (anonId == null) {
                anonId = UUID.randomUUID().toString()
                with(prefs.edit()) {
                    putString(ANON_ID_KEY, anonId)
                    apply()
                }
            }
            cachedAnonId = anonId
        }
        if (integrityTokenProvider != null) return
        require(googleCloudProjectNumber != null) {
            "Google Cloud Project Number must be provided for Android Projects."
        }
        withContext(Dispatchers.IO) {
            val integrityManager = IntegrityManagerFactory.createStandard(applicationContext)
            val request = PrepareIntegrityTokenRequest.builder()
                .setCloudProjectNumber(googleCloudProjectNumber)
                .build()

            integrityTokenProvider = suspendCancellableCoroutine { continuation ->
                integrityManager.prepareIntegrityToken(request)
                    .addOnSuccessListener { provider ->
                        Gateway.logger.info("Prepared Integrity Token Provider successfully.")
                        continuation.resume(provider)
                    }
                    .addOnFailureListener { exception ->
                        Gateway.logger.error("Failed to prepare Integrity Token Provider: ${exception.message}")
                        continuation.resumeWithException(
                            RuntimeException(
                                "Failed to prepare Integrity Token Provider",
                                exception
                            )
                        )
                    }
            }
        }
    }

    override suspend fun getIntegrityToken(): String {
        val tokenProvider = integrityTokenProvider
            ?: throw IllegalStateException("Integrity Token Provider not initialized. Call warmUpAttestation() first.")
        Gateway.logger.debug("Requesting Integrity Token...")
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                tokenProvider.request(StandardIntegrityTokenRequest.builder().build())
                    .addOnSuccessListener { response ->
                        Gateway.logger.info("Successfully requested Integrity Token.")
                        continuation.resume(response.token())
                    }
                    .addOnFailureListener { exception ->
                        if (exception is StandardIntegrityException && exception.errorCode == -19) {
                            Gateway.logger.warn("Integrity Token Provider invalid, re-initializing and retrying...")
                            GlobalScope.launch(Dispatchers.IO) {
                                try {
                                    warmUpAttestation()
                                    val retryProvider = integrityTokenProvider
                                        ?: throw IllegalStateException("Integrity Token Provider not initialized after retry.")
                                    retryProvider.request(
                                        StandardIntegrityTokenRequest.builder().build()
                                    )
                                        .addOnSuccessListener { retryResponse ->
                                            Gateway.logger.info("Successfully requested Integrity Token after re-initialization.")
                                            continuation.resume(retryResponse.token())
                                        }
                                        .addOnFailureListener { retryException ->
                                            Gateway.logger.error("Failed to request Integrity Token after re-initialization: ${retryException.message}")
                                            continuation.resumeWithException(
                                                RuntimeException(
                                                    "Failed to request Integrity Token after re-initialization",
                                                    retryException
                                                )
                                            )
                                        }
                                } catch (e: Exception) {
                                    Gateway.logger.error("Failed to re-initialize Integrity Token Provider: ${e.message}")
                                    continuation.resumeWithException(
                                        RuntimeException(
                                            "Failed to re-initialize Integrity Token Provider",
                                            e
                                        )
                                    )
                                }
                            }
                        } else {
                            Gateway.logger.error("Failed to request Integrity Token: ${exception.message}")
                            continuation.resumeWithException(
                                RuntimeException("Failed to request Integrity Token", exception)
                            )
                        }
                    }
            }
        }
    }

    override suspend fun getAnonymousId(): String {
        if (!enableAnonymousId) throw IllegalStateException("Anonymous ID not enabled in config")
        return cachedAnonId ?: prefs.getString(ANON_ID_KEY, null)
        ?: throw IllegalStateException("Anonymous ID not initialized. Call warmUpAttestation() first.")
    }
}

internal actual fun getDeviceType(): String = "android"