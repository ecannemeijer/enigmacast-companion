package com.enigmacast.companion.data.net

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec

/**
 * De identiteit van deze telefoon: een ECDSA P-256-sleutel in de Android Keystore.
 * De publieke sleutel gaat naar de TV bij het koppelen; daarna ondertekent de telefoon
 * elk request. De private sleutel is niet exporteerbaar.
 */
class DeviceIdentity {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun publicKeyBase64(): String {
        ensure()
        return Base64.encodeToString(publicKey().encoded, Base64.NO_WRAP)
    }

    fun sign(data: ByteArray): ByteArray {
        ensure()
        return Signature.getInstance("SHA256withECDSA").run {
            initSign(privateKey())
            update(data)
            sign()
        }
    }

    @Synchronized
    private fun ensure() {
        if (keyStore.containsAlias(ALIAS)) return
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        generator.initialize(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build(),
        )
        generator.generateKeyPair()
    }

    private fun publicKey(): PublicKey = keyStore.getCertificate(ALIAS).publicKey

    private fun privateKey(): PrivateKey = keyStore.getKey(ALIAS, null) as PrivateKey

    private companion object {
        const val ALIAS = "enigmacast_companion_device"
    }
}
