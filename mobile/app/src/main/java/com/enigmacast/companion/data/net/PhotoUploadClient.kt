package com.enigmacast.companion.data.net

import android.util.Base64
import com.enigmacast.companion.data.PairedTv
import com.enigmacast.companion.protocol.AuthHeaders
import com.enigmacast.companion.protocol.AuthSignature
import com.enigmacast.companion.protocol.ProtocolJson
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Uploadt één profielfoto als getekende `PUT /api/v1/profiles/{id}/photo`. */
class PhotoUploadClient(private val identity: DeviceIdentity) {

    suspend fun upload(tv: PairedTv, profileId: String, bytes: ByteArray): Result<Long> =
        withContext(Dispatchers.IO) {
            if (bytes.isEmpty()) return@withContext Result.failure(IOException("Lege foto"))
            if (bytes.size > MAX_BYTES) return@withContext Result.failure(IOException("Foto is te groot"))
            val tls = PinnedTls(tv.pin, tv.host)
            val client = tls.client(readTimeoutSeconds = 45)
            try {
                val path = "/api/v1/profiles/$profileId/photo"
                val timestamp = System.currentTimeMillis().toString()
                val nonce = UUID.randomUUID().toString()
                val signature = Base64.encodeToString(
                    identity.sign(AuthSignature.canonical(timestamp, nonce, "PUT", path, bytes)),
                    Base64.NO_WRAP,
                )
                val request = Request.Builder()
                    .url("https://${tv.host}:${tv.port}$path")
                    .put(bytes.toRequestBody("image/*".toMediaType()))
                    .header("Authorization", "Bearer ${tv.token}")
                    .header(AuthHeaders.DEVICE_ID, tv.deviceId)
                    .header(AuthHeaders.TIMESTAMP, timestamp)
                    .header(AuthHeaders.NONCE, nonce)
                    .header(AuthHeaders.SIGNATURE, signature)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@use Result.failure(IOException("TV gaf foutcode ${response.code}"))
                    }
                    val text = response.body?.string().orEmpty()
                    val stamp = runCatching {
                        ProtocolJson.json.parseToJsonElement(text).jsonObject["stamp"]?.jsonPrimitive?.longOrNull
                    }.getOrNull() ?: 0L
                    Result.success(stamp)
                }
            } catch (error: Exception) {
                Result.failure(error)
            } finally {
                client.dispatcher.executorService.shutdown()
                client.connectionPool.evictAll()
            }
        }

    suspend fun fetch(tv: PairedTv, profileId: String): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            val tls = PinnedTls(tv.pin, tv.host)
            val client = tls.client(readTimeoutSeconds = 20)
            try {
                val path = "/api/v1/profiles/$profileId/photo"
                client.newCall(signed(tv, "GET", path)).execute().use { response ->
                    if (!response.isSuccessful) return@use Result.failure(IOException("TV gaf foutcode ${response.code}"))
                    val bytes = response.body?.bytes() ?: ByteArray(0)
                    if (bytes.isEmpty()) Result.failure(IOException("Geen foto")) else Result.success(bytes)
                }
            } catch (error: Exception) {
                Result.failure(error)
            } finally {
                client.dispatcher.executorService.shutdown()
                client.connectionPool.evictAll()
            }
        }

    suspend fun delete(tv: PairedTv, profileId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val tls = PinnedTls(tv.pin, tv.host)
            val client = tls.client(readTimeoutSeconds = 20)
            try {
                val path = "/api/v1/profiles/$profileId/photo"
                client.newCall(signed(tv, "DELETE", path)).execute().use { response ->
                    if (!response.isSuccessful) return@use Result.failure(IOException("TV gaf foutcode ${response.code}"))
                    Result.success(Unit)
                }
            } catch (error: Exception) {
                Result.failure(error)
            } finally {
                client.dispatcher.executorService.shutdown()
                client.connectionPool.evictAll()
            }
        }

    private fun signed(tv: PairedTv, method: String, path: String): Request {
        val timestamp = System.currentTimeMillis().toString()
        val nonce = UUID.randomUUID().toString()
        val signature = Base64.encodeToString(
            identity.sign(AuthSignature.canonical(timestamp, nonce, method, path, ByteArray(0))),
            Base64.NO_WRAP,
        )
        val builder = Request.Builder()
            .url("https://${tv.host}:${tv.port}$path")
            .header("Authorization", "Bearer ${tv.token}")
            .header(AuthHeaders.DEVICE_ID, tv.deviceId)
            .header(AuthHeaders.TIMESTAMP, timestamp)
            .header(AuthHeaders.NONCE, nonce)
            .header(AuthHeaders.SIGNATURE, signature)
        return when (method) {
            "GET" -> builder.get().build()
            "DELETE" -> builder.delete().build()
            else -> error("unsupported method")
        }
    }

    private companion object {
        const val MAX_BYTES = 10 * 1024 * 1024
    }
}
