package io.github.brahyam.gateway.client

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    override suspend fun warmUpAttestation() {
        if (integrityTokenProvider != null) return

        withContext(Dispatchers.IO) {
            val integrityManager = IntegrityManagerFactory.createStandard(applicationContext)
            val request = PrepareIntegrityTokenRequest.builder()
                .setCloudProjectNumber(config.googleCloudProjectNumber)
                .build()

            integrityManager.prepareIntegrityToken(request).addOnSuccessListener {
                integrityTokenProvider = it
            }.addOnFailureListener { exception ->
                throw RuntimeException("Failed to prepare Integrity Token Provider", exception)
            }
        }
    }

    fun getIntegrityTokenProvider(): StandardIntegrityTokenProvider? {
        return integrityTokenProvider
    }
} 