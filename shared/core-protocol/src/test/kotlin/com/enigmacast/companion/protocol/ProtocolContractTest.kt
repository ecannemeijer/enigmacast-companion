package com.enigmacast.companion.protocol

import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contracttest: de gouden JSON-bestanden in `protocol/v1` moeten door de DTO's worden
 * gelezen en zonder verlies opnieuw geschreven. Zo kan het contract niet stilzwijgend
 * afwijken van de code die de TV-brug en de telefoon gebruiken.
 */
class ProtocolContractTest {

    private val fixtures: File by lazy {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            val candidate = File(dir, "protocol/v1")
            if (candidate.isDirectory) return@lazy candidate
            dir = dir.parentFile
        }
        error("protocol/v1 niet gevonden vanaf ${System.getProperty("user.dir")}")
    }

    private fun envelopeFiles(): List<File> =
        fixtures.listFiles { file -> file.isFile && file.extension == "json" && !file.name.startsWith("pair-") }
            ?.sortedBy { it.name }
            .orEmpty()

    @Test
    fun everyEnvelopeRoundTripsWithoutLosingFields() {
        val files = envelopeFiles()
        assertTrue("geen protocol-fixtures gevonden", files.isNotEmpty())
        for (file in files) {
            val text = file.readText()
            val original = ProtocolJson.json.parseToJsonElement(text) as JsonObject
            val envelope = ProtocolJson.decodeEnvelope(text)

            assertEquals("${file.name}: verkeerde protocolversie", Protocol.VERSION, envelope.v)
            assertEquals(
                "${file.name}: type komt niet overeen",
                original["type"]?.jsonPrimitive?.content,
                envelope.type,
            )

            val reEncoded = ProtocolJson.json.parseToJsonElement(ProtocolJson.encodeEnvelope(envelope)) as JsonObject
            for ((key, value) in original) {
                assertJsonEquals("${file.name}: veld '$key' verandert", value, reEncoded[key])
            }
        }
    }

    @Test
    fun typedPayloadsMatchTheDocumentedShape() {
        val hello = ProtocolJson.decodePayload<HelloPayload>(read("hello.json"))
        assertEquals("Woonkamer TV", hello.tv.name)
        assertTrue(hello.capabilities.contains(Capability.KEY))
        assertEquals("home", hello.screen?.section)
        assertEquals(42L, hello.settingsRevision)

        val key = ProtocolJson.decodePayload<KeyCommand>(read("cmd-key.json"))
        assertEquals(RemoteKey.DPAD_DOWN, key.key)
        assertEquals(KeyPhase.DOWN, key.phase)

        val zap = ProtocolJson.decodePayload<ZapCommand>(
            ProtocolJson.envelope(MessageType.CMD_ZAP, ZapCommand(serviceRef = "1:0:19:4B6F:2E:19:C00000:0:0:0:")),
        )
        assertEquals("1:0:19:4B6F:2E:19:C00000:0:0:0:", zap.serviceRef)

        val state = ProtocolJson.decodePayload<StatePlayerPayload>(read("state-player.json"))
        assertEquals("enigma2", state.source)
        assertEquals("NPO 1 HD", state.channel?.name)
        assertEquals(1, state.tuner.used)

        val settings = ProtocolJson.decodePayload<SettingsCommand>(read("settings-patch.json"))
        assertEquals(42L, settings.baseRevision)
        assertEquals(2, settings.ops.size)
        assertEquals("GLASS_EARTH", settings.ops.first().value?.jsonPrimitive?.content)

        val form = ProtocolJson.decodePayload<FormDescriptor>(read("form.json"))
        assertEquals("receiver", form.formId)
        assertEquals(FieldType.PASSWORD, form.fields.last().type)
        assertTrue(form.fields.last().sensitive)

        val catalog = ProtocolJson.decodePayload<SettingsCatalogPayload>(read("state-settings.json"))
        assertEquals(3L, catalog.revision)
        assertEquals("BOOL", catalog.entries.first().type)
        assertEquals("Weergave", catalog.entries.first().category)
        assertEquals("GLASS_EARTH", catalog.entries[1].value?.jsonPrimitive?.content)
        assertEquals(100.0, catalog.entries[2].max)

        val error = ProtocolJson.decodePayload<ProtocolError>(read("error.json"))
        assertEquals(ErrorCode.AUTH_REPLAY, error.code)
    }

    @Test
    fun formCommandsCarryLiveTextAndAClosedFlag() {
        val command = FormSubmitCommand(
            formId = "receiver",
            revision = 7,
            values = mapOf("value" to "Woonkamer"),
            action = "TEXT",
        )
        val commandJson = ProtocolJson.json.encodeToString(FormSubmitCommand.serializer(), command)
        assertTrue(commandJson.contains("\"action\":\"TEXT\""))
        assertTrue(commandJson.contains("\"value\":\"Woonkamer\""))

        val closed = FormDescriptor(
            formId = "receiver",
            title = "Ontvanger",
            revision = 8,
            fields = emptyList(),
            closed = true,
        )
        val closedJson = ProtocolJson.json.encodeToString(FormDescriptor.serializer(), closed)
        assertTrue(closedJson.contains("\"closed\":true"))
    }

    @Test
    fun pairingBodiesMatchTheDocumentedShape() {
        val request = ProtocolJson.json.decodeFromString(
            PairRequest.serializer(),
            File(fixtures, "pair-request.json").readText(),
        )
        assertEquals("483920", request.code)
        assertTrue(request.publicKey.startsWith("MFkw"))

        val response = ProtocolJson.json.decodeFromString(
            PairResponse.serializer(),
            File(fixtures, "pair-response.json").readText(),
        )
        assertEquals("member", response.role)
        assertEquals("Woonkamer TV", response.tv.name)
    }

    @Test
    fun unknownFieldsAreIgnored() {
        val text = """{"v":1,"type":"hello","payload":{"tv":{"id":"x","name":"n","app":"a"},"futureField":1},"extra":true}"""
        val hello = ProtocolJson.decodePayload<HelloPayload>(ProtocolJson.decodeEnvelope(text))
        assertEquals("n", hello.tv.name)
    }

    @Test
    fun signingStringIsStable() {
        // Vaste vector: beide kanten moeten exact dezelfde bytes ondertekenen.
        val canonical = AuthSignature.canonical(
            timestamp = "1761100000123",
            nonce = "5b0e2f5e-6a1f-4f0e-9b1a-2f6f0f2a9c11",
            method = "get",
            pathWithQuery = "/api/v1/state?since=41",
            body = "{}".toByteArray(),
        )
        val head = "1761100000123\n5b0e2f5e-6a1f-4f0e-9b1a-2f6f0f2a9c11\nGET\n/api/v1/state?since=41\n"
            .toByteArray(Charsets.UTF_8)
        assertEquals(head.size + 32, canonical.size)
        assertEquals(
            "44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a",
            AuthSignature.bodyHashHex("{}".toByteArray()),
        )
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            AuthSignature.bodyHashHex(ByteArray(0)),
        )
    }

    @Test
    fun pathWithQueryUsesTheSameEncodingAsTheServer() {
        assertEquals(
            "/api/v1/sync/pull?profileId=a%20b&cursor=7",
            AuthSignature.pathWithQuery(
                "/api/v1/sync/pull",
                listOf("profileId" to "a b", "cursor" to "7"),
            ),
        )
    }

    private fun read(name: String): Envelope = ProtocolJson.decodeEnvelope(File(fixtures, name).readText())

    private fun assertJsonEquals(message: String, expected: JsonElement?, actual: JsonElement?) {
        assertNotNull("$message: ontbreekt", actual)
        assertEquals(message, expected, actual)
    }
}
