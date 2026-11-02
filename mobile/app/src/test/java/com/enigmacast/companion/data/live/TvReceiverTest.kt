package com.enigmacast.companion.data.live

import com.enigmacast.companion.protocol.ProtocolJson
import com.enigmacast.companion.protocol.ReceiverStatePayload
import com.enigmacast.companion.protocol.SharedReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TvReceiverTest {
    private val a = SharedReceiver(id = "a", name = "Woonkamer", host = "10.0.0.2")
    private val b = SharedReceiver(id = "b", name = "Zolder", host = "10.0.0.3", httpPort = 8080, useHttps = true, streamingPort = 8002)

    @Test
    fun picksTheActiveReceiver() {
        val config = tvReceiverConfig(ReceiverStatePayload(activeId = "b", receivers = listOf(a, b)))!!
        assertEquals("10.0.0.3", config.host)
        assertEquals(8080, config.httpPort)
        assertEquals(true, config.useHttps)
        assertEquals(8002, config.streamingPort)
    }

    @Test
    fun fallsBackToTheFirstAndNullWhenEmpty() {
        assertEquals("a", tvReceiverConfig(ReceiverStatePayload(activeId = "x", receivers = listOf(a, b)))!!.id)
        assertNull(tvReceiverConfig(ReceiverStatePayload()))
        assertNull(tvReceiverConfig(ReceiverStatePayload(receivers = listOf(SharedReceiver(id = "c")))))
    }

    @Test
    fun stampChangesWithTheReceiver() {
        val one = tvReceiverConfig(ReceiverStatePayload("a", listOf(a)))!!
        val two = tvReceiverConfig(ReceiverStatePayload("a", listOf(a.copy(host = "10.0.0.9"))))!!
        assertEquals(tvReceiverStamp(one), tvReceiverStamp(one.copy()))
        assertNotEquals(tvReceiverStamp(one), tvReceiverStamp(two))
    }

    @Test
    fun decodesTheTvPayload() {
        val json = """{"activeId":"a","receivers":[{"id":"a","name":"W","host":"h","httpPort":81,"extra":1}]}"""
        val payload = ProtocolJson.json.decodeFromString(ReceiverStatePayload.serializer(), json)
        assertEquals(81, tvReceiverConfig(payload)!!.httpPort)
    }
}
