package com.enigmacast.companion.data.net

import android.os.Build
import com.enigmacast.companion.data.PairedTv
import com.enigmacast.companion.protocol.ProtocolJson
import java.util.UUID
import javax.net.ssl.SSLException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

sealed interface PairingResult {
    data class Success(val tv: PairedTv) : PairingResult
    data class Failure(val message: String) : PairingResult
}

/** Eenmalige HTTPS-koppeling: code + publieke sleutel erheen, token + deviceId terug. */
class PairingClient(private val identity: DeviceIdentity) {

    suspend fun pair(tv: PairedTv, code: String): PairingResult = withContext(Dispatchers.IO) {
        val tls = PinnedTls(tv.pin, tv.host)
        val client = tls.client(readTimeoutSeconds = 15)
        try {
            val body = buildJsonObject {
                put("code", code)
                put("name", Build.MODEL?.takeIf { it.isNotBlank() } ?: "Android")
                put("platform", "android")
                put("publicKey", identity.publicKeyBase64())
                put("nonce", UUID.randomUUID().toString())
            }.toString()
            val request = Request.Builder()
                .url("https://${tv.host}:${tv.port}/api/v1/pair")
                .post(body.toRequestBody(JSON))
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@use PairingResult.Failure(messageFor(response.code))
                }
                val json = runCatching { ProtocolJson.json.parseToJsonElement(text).jsonObject }.getOrNull()
                    ?: return@use PairingResult.Failure("Onleesbaar antwoord van de TV")
                val token = json["token"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val deviceId = json["deviceId"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val name = json["tv"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
                if (token.isBlank() || deviceId.isBlank()) {
                    return@use PairingResult.Failure("De TV gaf geen token terug")
                }
                PairingResult.Success(
                    tv.copy(
                        name = name?.takeIf { it.isNotBlank() } ?: tv.name,
                        token = token,
                        deviceId = deviceId,
                        pin = tls.observedPin ?: tv.pin,
                        real = true,
                        online = true,
                    ),
                )
            }
        } catch (error: Exception) {
            PairingResult.Failure(describe(error))
        } finally {
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
        }
    }

    private fun messageFor(code: Int): String = when (code) {
        400, 401, 403 -> "Code ongeldig of verlopen"
        409 -> "Deze telefoon is al gekoppeld"
        429 -> "Te veel pogingen. Probeer het over een minuut opnieuw."
        else -> "De TV gaf foutcode $code"
    }

    private fun describe(error: Exception): String = when {
        error is SSLException -> "Certificaatpin klopt niet"
        error is java.net.ConnectException || error is java.net.SocketTimeoutException ->
            "TV niet bereikbaar op dit adres"
        error is java.net.UnknownHostException -> "Adres niet gevonden"
        else -> error.message ?: "Koppelen mislukt"
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
