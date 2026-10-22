package com.enigmacast.tools.bridge

import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import java.util.UUID
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * End-to-end rooktest tegen de **echte** TV-brug. Draait alleen als `BRIDGE_CODE` is gezet:
 *
 * ```
 * adb forward tcp:8750 tcp:8750
 * BRIDGE_CODE=123456 BRIDGE_PIN=<pin> gradle :tools:tls-spike:test --tests '*BridgeSmokeTest*'
 * ```
 *
 * De client vertrouwt het zelfondertekende certificaat (test-only) en controleert de pin.
 */
class BridgeSmokeTest {

    private val host = System.getenv("BRIDGE_HOST") ?: "127.0.0.1"
    private val port = System.getenv("BRIDGE_PORT")?.toIntOrNull() ?: 8750
    private val code = System.getenv("BRIDGE_CODE")
    private val expectedPin = System.getenv("BRIDGE_PIN")

    @Test(timeout = 45_000)
    fun healthPairAndWebSocket() {
        assumeTrue("BRIDGE_CODE niet gezet; rooktest overgeslagen", !code.isNullOrBlank())
        val context = trustAll()

        val (healthStatus, healthBody) = http(context, "GET", "/api/v1/health", emptyMap(), ByteArray(0))
        println("health: $healthStatus $healthBody")
        assertEquals(200, healthStatus)
        assertTrue(healthBody.contains("\"ok\":true"))

        val keyPair = ecKeyPair()
        val publicKey = Base64.getEncoder().encodeToString(keyPair.public.encoded)
        val pairBody = """
            {"code":"$code","name":"JVM rooktest","platform":"android","publicKey":"$publicKey","nonce":"${UUID.randomUUID()}"}
        """.trimIndent()
        val (pairStatus, pairText) = http(
            context,
            "POST",
            "/api/v1/pair",
            mapOf("Content-Type" to "application/json"),
            pairBody.toByteArray(),
        )
        println("pair: $pairStatus $pairText")
        assertEquals(200, pairStatus)
        val token = extract(pairText, "token")
        val deviceId = extract(pairText, "deviceId")
        assertTrue(token.isNotBlank())
        assertTrue(deviceId.isNotBlank())

        val socket = socket(context)
        try {
            socket.connect(InetSocketAddress(host, port), 5_000)
            socket.soTimeout = 8_000
            socket.startHandshake()
            val certificate = socket.session.peerCertificates.first() as X509Certificate
            val pin = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(MessageDigest.getInstance("SHA-256").digest(certificate.publicKey.encoded))
            println("pin: $pin")
            if (!expectedPin.isNullOrBlank()) assertEquals(expectedPin, pin)

            val timestamp = System.currentTimeMillis().toString()
            val nonce = UUID.randomUUID().toString()
            val canonical = "$timestamp\n$nonce\nGET\n/ws\n".toByteArray() +
                MessageDigest.getInstance("SHA-256").digest(ByteArray(0))
            val signature = sign(keyPair, canonical)
            val headers = linkedMapOf(
                "Host" to "$host:$port",
                "Upgrade" to "websocket",
                "Connection" to "Upgrade",
                "Sec-WebSocket-Version" to "13",
                "Sec-WebSocket-Key" to Base64.getEncoder().encodeToString(randomBytes(16)),
                "Authorization" to "Bearer $token",
                "X-Device-Id" to deviceId,
                "X-Timestamp" to timestamp,
                "X-Nonce" to nonce,
                "X-Signature" to Base64.getEncoder().encodeToString(signature),
            )
            upgrade(socket.outputStream, headers)
            val status = readLine(socket.inputStream)
            println("ws: $status")
            assertTrue(status.contains("101"))
            while (readLine(socket.inputStream).isNotEmpty()) Unit

            val hello = readFrame(socket.inputStream)
            println("hello: ${hello.text.take(220)}")
            assertTrue(hello.text.contains("\"hello\""))
            val state = readFrame(socket.inputStream)
            println("state: ${state.text.take(220)}")
            assertTrue(state.text.contains("state.player"))

            val command = """{"v":1,"id":"t1","type":"cmd.key","payload":{"key":"DPAD_DOWN","phase":"DOWN","repeat":0,"long":false}}"""
            writeText(socket.outputStream, command)
            var ack = readFrame(socket.inputStream)
            var guard = 0
            while (!ack.text.contains("\"ack\"") && guard < 6) {
                ack = readFrame(socket.inputStream)
                guard++
            }
            println("ack: ${ack.text}")
            assertTrue(ack.text.contains("\"ack\""))
            assertTrue(ack.text.contains("\"ok\":true"))
        } finally {
            runCatching { socket.close() }
        }
    }

    private fun trustAll(): SSLContext {
        val manager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        return SSLContext.getInstance("TLS").apply { init(null, arrayOf(manager), SecureRandom()) }
    }

    private fun socket(context: SSLContext): SSLSocket =
        context.socketFactory.createSocket() as SSLSocket

    private fun http(
        context: SSLContext,
        method: String,
        path: String,
        headers: Map<String, String>,
        body: ByteArray,
    ): Pair<Int, String> {
        val socket = socket(context)
        socket.use {
            it.connect(InetSocketAddress(host, port), 5_000)
            it.soTimeout = 8_000
            it.startHandshake()
            val head = buildString {
                append("$method $path HTTP/1.1\r\n")
                append("Host: $host:$port\r\n")
                append("Connection: close\r\n")
                headers.forEach { (name, value) -> append("$name: $value\r\n") }
                append("Content-Length: ${body.size}\r\n\r\n")
            }
            it.outputStream.write(head.toByteArray())
            it.outputStream.write(body)
            it.outputStream.flush()
            val text = it.inputStream.readBytes().toString(Charsets.UTF_8)
            val status = text.lineSequence().first().split(' ')[1].toInt()
            return status to text.substringAfter("\r\n\r\n", "")
        }
    }

    private fun upgrade(output: OutputStream, headers: Map<String, String>) {
        val request = buildString {
            append("GET /ws HTTP/1.1\r\n")
            headers.forEach { (name, value) -> append("$name: $value\r\n") }
            append("\r\n")
        }
        output.write(request.toByteArray())
        output.flush()
    }

    private fun ecKeyPair(): KeyPair =
        KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()

    private fun sign(keyPair: KeyPair, data: ByteArray): ByteArray =
        Signature.getInstance("SHA256withECDSA").run {
            initSign(keyPair.private)
            update(data)
            sign()
        }

    private fun randomBytes(size: Int): ByteArray = ByteArray(size).also { SecureRandom().nextBytes(it) }

    private fun extract(json: String, name: String): String =
        Regex("\"$name\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1).orEmpty()

    private fun readLine(input: InputStream): String {
        val buffer = StringBuilder()
        while (true) {
            val value = input.read()
            if (value < 0 || value == '\n'.code) break
            if (value != '\r'.code) buffer.append(value.toChar())
        }
        return buffer.toString()
    }

    private data class Frame(val opcode: Int, val text: String)

    private fun readFrame(input: InputStream): Frame {
        val first = input.read()
        val second = input.read()
        if (first < 0 || second < 0) error("verbinding gesloten")
        var length = second and 0x7F
        if (length == 126) {
            length = (input.read() shl 8) or input.read()
        } else if (length == 127) {
            var value = 0L
            repeat(8) { value = (value shl 8) or input.read().toLong() }
            length = value.toInt()
        }
        val masked = (second and 0x80) != 0
        val mask = if (masked) ByteArray(4).also { readFully(input, it) } else null
        val payload = ByteArray(length).also { readFully(input, it) }
        if (mask != null) {
            for (index in payload.indices) {
                payload[index] = (payload[index].toInt() xor mask[index % 4].toInt()).toByte()
            }
        }
        return Frame(first and 0x0F, String(payload, Charsets.UTF_8))
    }

    private fun writeText(output: OutputStream, text: String) {
        val payload = text.toByteArray()
        output.write(0x81)
        val mask = randomBytes(4)
        when {
            payload.size < 126 -> output.write(0x80 or payload.size)
            payload.size < 65536 -> {
                output.write(0x80 or 126)
                output.write((payload.size shr 8) and 0xFF)
                output.write(payload.size and 0xFF)
            }
            else -> error("te groot")
        }
        output.write(mask)
        for (index in payload.indices) {
            output.write((payload[index].toInt() xor mask[index % 4].toInt()) and 0xFF)
        }
        output.flush()
    }

    private fun readFully(input: InputStream, target: ByteArray) {
        var offset = 0
        while (offset < target.size) {
            val read = input.read(target, offset, target.size - offset)
            if (read < 0) error("onverwacht einde")
            offset += read
        }
    }
}
