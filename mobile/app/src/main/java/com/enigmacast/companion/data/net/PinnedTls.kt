package com.enigmacast.companion.data.net

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import okhttp3.OkHttpClient

/**
 * Vertrouwt precies één certificaat: de SPKI-pin van de TV. Zonderpin (handmatig adres)
 * wordt de pin bij de eerste handshake vastgelegd (TOFU) en daarna vastgehouden.
 */
class PinnedTls(expectedPin: String?, private val host: String) {

    private val expected = expectedPin?.takeIf { it.isNotBlank() }

    @Volatile
    var observedPin: String? = null
        private set

    private val trust = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            val certificate = chain?.firstOrNull() ?: throw CertificateException("Geen certificaat")
            val pin = pinOf(certificate)
            observedPin = pin
            if (expected != null && expected != pin) {
                throw CertificateException("Certificaatpin klopt niet")
            }
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    /** [readTimeoutSeconds] 0 = geen timeout (WebSocket), anders de HTTP-timeout. */
    fun client(readTimeoutSeconds: Long = 0): OkHttpClient {
        val context = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trust), SecureRandom()) }
        return OkHttpClient.Builder()
            .sslSocketFactory(context.socketFactory, trust)
            .hostnameVerifier { name, _ -> name == host }
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private fun pinOf(certificate: X509Certificate): String =
        Base64.encodeToString(
            MessageDigest.getInstance("SHA-256").digest(certificate.publicKey.encoded),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
}
