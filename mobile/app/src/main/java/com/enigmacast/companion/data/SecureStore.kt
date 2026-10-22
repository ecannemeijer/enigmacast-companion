package com.enigmacast.companion.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Kleine AES-GCM-store boven op de Android Keystore voor het koppeltoken. Net als de
 * TV-kant (`SecretStore`): de sleutel verlaat de Keystore nooit.
 */
class SecureStore(context: Context) {

    private val prefs = context.getSharedPreferences("companion_secure", Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun put(name: String, value: String) {
        if (value.isBlank()) {
            remove(name)
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val blob = ByteArray(1 + iv.size + encrypted.size)
        blob[0] = iv.size.toByte()
        System.arraycopy(iv, 0, blob, 1, iv.size)
        System.arraycopy(encrypted, 0, blob, 1 + iv.size, encrypted.size)
        prefs.edit().putString(name, Base64.encodeToString(blob, Base64.NO_WRAP)).apply()
    }

    fun get(name: String): String? {
        val encoded = prefs.getString(name, null) ?: return null
        return runCatching {
            val blob = Base64.decode(encoded, Base64.DEFAULT)
            val ivSize = blob[0].toInt()
            val iv = blob.copyOfRange(1, 1 + ivSize)
            val cipherText = blob.copyOfRange(1 + ivSize, blob.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        }.getOrNull()
    }

    fun remove(name: String) {
        prefs.edit().remove(name).apply()
    }

    private fun key(): SecretKey {
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ALIAS = "enigmacast_companion_secrets"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
