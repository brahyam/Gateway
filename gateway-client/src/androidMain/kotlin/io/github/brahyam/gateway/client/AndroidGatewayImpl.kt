package io.github.brahyam.gateway.client

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@SuppressLint("PrivateApi")
internal actual fun createGatewayImpl(config: GatewayConfig): GatewayImpl {
    val application = Class.forName("android.app.ActivityThread")
        .getMethod("currentApplication")
        .invoke(null) as Application
    return AndroidGatewayImpl(config, application.applicationContext)
}

internal class AndroidGatewayImpl(
    private val config: GatewayConfig,
    private val applicationContext: Context,
) : GatewayImpl {
    private var integrityTokenProvider: StandardIntegrityTokenProvider? = null
    private val prefs: SharedPreferences by lazy {
        applicationContext.getSharedPreferences("gateway_prefs", Context.MODE_PRIVATE)
    }
    private val ANON_ID_KEY = "gateway_anon_id"
    private var cachedAnonId: String? = null

    override suspend fun warmUpAttestation() {
        if (config.enableAnonymousId) {
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
        require(config.googleCloudProjectNumber != null) {
            "GatewayConfig must contain Google Cloud Project Number for Android Projects."
        }
        withContext(Dispatchers.IO) {
            val integrityManager = IntegrityManagerFactory.createStandard(applicationContext)
            val request = PrepareIntegrityTokenRequest.builder()
                .setCloudProjectNumber(config.googleCloudProjectNumber)
                .build()

            integrityTokenProvider = suspendCancellableCoroutine { continuation ->
                integrityManager.prepareIntegrityToken(request)
                    .addOnSuccessListener { provider ->
                        println("Prepared Integrity Token Provider successfully.")
                        continuation.resume(provider)
                    }
                    .addOnFailureListener { exception ->
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
        println("Requesting Integrity Token...")
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                tokenProvider.request(StandardIntegrityTokenRequest.builder().build())
                    .addOnSuccessListener { response ->
                        println("Successfully requested Integrity Token.")
                        continuation.resume(response.token())
                    }
                    .addOnFailureListener { exception ->
                        continuation.resumeWithException(
                            RuntimeException("Failed to request Integrity Token", exception)
                        )
                    }
            }
        }
    }

    override suspend fun getAnonymousId(): String {
        if (!config.enableAnonymousId) throw IllegalStateException("Anonymous ID not enabled in config")
        return cachedAnonId ?: prefs.getString(ANON_ID_KEY, null)
        ?: throw IllegalStateException("Anonymous ID not initialized. Call warmUpAttestation() first.")
    }
}

internal actual fun getDeviceType(): String = "android"