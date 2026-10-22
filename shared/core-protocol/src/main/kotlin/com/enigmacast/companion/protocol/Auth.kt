package com.enigmacast.companion.protocol

import java.security.MessageDigest

/** Headernamen van een geauthenticeerd request. */
object AuthHeaders {
    const val AUTHORIZATION = "Authorization"
    const val DEVICE_ID = "X-Device-Id"
    const val TIMESTAMP = "X-Timestamp"
    const val NONCE = "X-Nonce"
    const val SIGNATURE = "X-Signature"

    /** Alleen bij een beveiligde instellingenwijziging: de 4-cijferige Settings-PIN. */
    const val SETTINGS_PIN = "X-Settings-Pin"
}

/**
 * Canonieke vorm van een getekend request. Exact het schema dat EnigmaCast al gebruikt
 * voor zijn synchronisatieserver (`EnigmaSyncClient` / `service.py::authenticate`):
 *
 * ```
 * <timestamp>\n<nonce>\n<METHOD>\n<path?query>\n<sha256(body)>
 * ```
 *
 * De ondertekende string bevat de *ruwe* 32 bytes van de body-hash, niet de hex-weergave.
 * Beide kanten gebruiken deze functie, zodat er geen interpretatieverschil kan ontstaan.
 */
object AuthSignature {
    fun bodyHash(body: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(body)

    fun bodyHashHex(body: ByteArray): String = bodyHash(body).joinToString("") { "%02x".format(it) }

    fun canonical(
        timestamp: String,
        nonce: String,
        method: String,
        pathWithQuery: String,
        body: ByteArray,
    ): ByteArray {
        val head = "$timestamp\n$nonce\n${method.uppercase()}\n$pathWithQuery\n".toByteArray(Charsets.UTF_8)
        return head + bodyHash(body)
    }

    /** Pad plus query, zoals de server het ondertekent (query in dezelfde volgorde). */
    fun pathWithQuery(path: String, query: List<Pair<String, String>> = emptyList()): String {
        if (query.isEmpty()) return path
        val encoded = query.joinToString("&") { (key, value) -> "$key=${encode(value)}" }
        return "$path?$encoded"
    }

    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
