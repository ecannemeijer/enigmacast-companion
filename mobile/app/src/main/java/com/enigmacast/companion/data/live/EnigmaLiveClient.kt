package com.enigmacast.companion.data.live

import com.enigmacast.data.openwebif.OpenWebifEndpoints
import com.enigmacast.data.openwebif.OpenWebifJson
import com.enigmacast.domain.model.BouquetEntry
import com.enigmacast.domain.model.ReceiverConfig
import com.enigmacast.domain.tuner.NowEvent
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Leest de zenderlijst en EPG rechtstreeks van de Enigma2-ontvanger met dezelfde
 * OpenWebif-paden als de TV-app. De telefoon speelt de stream zelf af.
 */
class EnigmaLiveClient {

    private val endpoints = OpenWebifEndpoints()

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val insecureClient by lazy {
        val trust = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        val context = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trust), SecureRandom()) }
        OkHttpClient.Builder()
            .sslSocketFactory(context.socketFactory, trust)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    suspend fun bouquets(config: ReceiverConfig): Result<List<BouquetEntry>> =
        get(config, endpoints.bouquets(config)).mapCatching { OpenWebifJson.parseBouquets(it) }

    suspend fun services(config: ReceiverConfig, bouquet: BouquetEntry): Result<List<BouquetEntry>> =
        get(config, endpoints.services(config, bouquet.reference)).mapCatching { OpenWebifJson.parseServices(it) }

    suspend fun now(config: ReceiverConfig, bouquet: BouquetEntry): Result<List<NowEvent>> =
        get(config, endpoints.epgNow(config, bouquet.reference)).mapCatching { OpenWebifJson.parseNowEvents(it) }

    fun streamUrl(config: ReceiverConfig, serviceReference: String): String =
        endpoints.stream(config, serviceReference)

    fun streamHeaders(config: ReceiverConfig): Map<String, String> =
        endpoints.streamAuthorization(config)?.let { mapOf("Authorization" to it) }.orEmpty()

    private suspend fun get(config: ReceiverConfig, url: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).apply {
                endpoints.streamAuthorization(config)?.let { header("Authorization", it) }
            }.build()
            val selected = if (config.useHttps && config.allowInsecureCertificates) insecureClient else client
            selected.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Ontvanger gaf HTTP ${response.code}")
                response.body?.string().orEmpty()
            }
        }
    }
}
