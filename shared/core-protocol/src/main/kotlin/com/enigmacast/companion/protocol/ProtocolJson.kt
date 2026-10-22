package com.enigmacast.companion.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Serialisatie-instellingen van het protocol.
 *
 * - onbekende velden negeren: een oudere app blijft werken met een nieuwere TV;
 * - `encodeDefaults = true`: het contract toont altijd de volledige vorm;
 * - `explicitNulls = false`: afwezige optionele velden worden niet als `null` verstuurd.
 */
object ProtocolJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        isLenient = false
    }

    fun decodeEnvelope(text: String): Envelope = json.decodeFromString(Envelope.serializer(), text)

    fun encodeEnvelope(envelope: Envelope): String = json.encodeToString(Envelope.serializer(), envelope)

    inline fun <reified T> decodePayload(envelope: Envelope): T {
        val payload = envelope.payload ?: error("${envelope.type} heeft geen payload")
        return json.decodeFromJsonElement<T>(payload)
    }

    inline fun <reified T> envelope(
        type: String,
        payload: T,
        id: String? = null,
        seq: Long? = null,
        ts: Long? = null,
    ): Envelope = Envelope(
        v = Protocol.VERSION,
        id = id,
        seq = seq,
        type = type,
        ts = ts,
        payload = json.encodeToJsonElement(payload),
    )

    fun envelopeOf(type: String, payload: JsonElement?, id: String? = null, seq: Long? = null, ts: Long? = null): Envelope =
        Envelope(v = Protocol.VERSION, id = id, seq = seq, type = type, ts = ts, payload = payload)
}
