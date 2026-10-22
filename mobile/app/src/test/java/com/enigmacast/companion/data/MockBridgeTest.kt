package com.enigmacast.companion.data

import com.enigmacast.companion.protocol.AckPayload
import com.enigmacast.companion.protocol.Envelope
import com.enigmacast.companion.protocol.HelloPayload
import com.enigmacast.companion.protocol.KeyCommand
import com.enigmacast.companion.protocol.KeyPhase
import com.enigmacast.companion.protocol.MessageType
import com.enigmacast.companion.protocol.ProtocolJson
import com.enigmacast.companion.protocol.RemoteKey
import com.enigmacast.companion.protocol.StatePlayerPayload
import java.util.Collections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Bewijst dat de mock-brug hetzelfde protocol spreekt als straks de TV-brug:
 * `hello`, `state.player`, `ack` en `cmd.key`, inclusief phase en reconnect.
 */
class MockBridgeTest {

    private val bridge = MockBridge()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val seen = Collections.synchronizedList(mutableListOf<Envelope>())
    private val tv = PairedTv(id = "tv-test", name = "Test TV", host = "127.0.0.1", online = true)

    @After
    fun tearDown() {
        bridge.disconnect()
        scope.cancel()
    }

    private fun collect() {
        scope.launch { bridge.events.collect { seen += it } }
    }

    private fun sendKey(key: RemoteKey, phase: KeyPhase = KeyPhase.DOWN, repeat: Int = 0) {
        bridge.send(
            ProtocolJson.envelope(
                MessageType.CMD_KEY,
                KeyCommand(key = key, phase = phase, repeat = repeat),
                id = "test-${seen.size}",
            ),
        )
    }

    private fun channelNumber(): Int? = seen
        .filter { it.type == MessageType.STATE_PLAYER }
        .mapNotNull { runCatching { ProtocolJson.decodePayload<StatePlayerPayload>(it).channel?.number }.getOrNull() }
        .lastOrNull()

    private suspend fun await(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        withTimeout(timeoutMs) {
            while (!condition()) delay(20)
        }
    }

    @Test
    fun connectSendsHelloAndTheCurrentChannel() = runBlocking {
        collect()
        bridge.connect(tv)
        await { seen.any { it.type == MessageType.HELLO } && seen.any { it.type == MessageType.STATE_PLAYER } }

        val hello = ProtocolJson.decodePayload<HelloPayload>(seen.first { it.type == MessageType.HELLO })
        assertEquals("Test TV", hello.tv.name)
        assertTrue(hello.capabilities.contains("key"))
        assertEquals("default", hello.profile?.activeId)
        assertEquals(1, channelNumber())
    }

    @Test
    fun channelUpChangesTheChannelAndIsAcknowledged() = runBlocking {
        collect()
        bridge.connect(tv)
        await { channelNumber() == 1 }

        sendKey(RemoteKey.CHANNEL_UP)
        await { channelNumber() == 2 }
        await { seen.any { it.type == MessageType.ACK } }

        val ack = ProtocolJson.decodePayload<AckPayload>(seen.first { it.type == MessageType.ACK })
        assertTrue(ack.ok)
        assertNotNull(ack.latencyMs)
    }

    @Test
    fun aKeyReleaseDoesNotActAgain() = runBlocking {
        collect()
        bridge.connect(tv)
        await { channelNumber() == 1 }

        sendKey(RemoteKey.CHANNEL_UP, KeyPhase.UP)
        delay(300)
        assertEquals(1, channelNumber())
    }

    @Test
    fun aDroppedConnectionComesBackByItself() = runBlocking {
        collect()
        bridge.connect(tv)
        await { channelNumber() == 1 }

        bridge.simulateDrop()
        await { bridge.link.value.state == LinkState.OFFLINE }
        await(timeoutMs = 6_000) { bridge.link.value.state == LinkState.READY }
        await { seen.count { it.type == MessageType.HELLO } >= 2 }
    }

    @Test
    fun anUnknownCommandIsRejectedWithAnError() = runBlocking {
        collect()
        bridge.connect(tv)
        await { bridge.link.value.state == LinkState.READY }

        bridge.send(ProtocolJson.envelope("cmd.unknown", mapOf("x" to 1), id = "test-unknown"))
        await { seen.any { it.type == MessageType.ERROR } }
        val error = seen.first { it.type == MessageType.ERROR }
        assertEquals("test-unknown", error.id)
    }
}
