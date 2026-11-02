package com.enigmacast.companion.data.live

import com.enigmacast.companion.protocol.FeaturesPayload
import com.enigmacast.companion.protocol.ProtocolJson
import com.enigmacast.companion.protocol.SearchStatePayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchPayloadTest {
    @Test
    fun decodesTheTvSearchState() {
        val json = """{"query":"dune","scope":"all","torrents":true,"sections":[{"id":"films","items":[{"key":"i:1","title":"Dune","subtitle":"2021","image":""}]}]}"""
        val payload = ProtocolJson.json.decodeFromString(SearchStatePayload.serializer(), json)
        assertEquals("films", payload.sections.single().id)
        assertEquals("i:1", payload.sections.single().items.single().key)
        assertTrue(payload.torrents)
    }

    @Test
    fun featuresDefaultToNoTorrents() {
        assertEquals(false, ProtocolJson.json.decodeFromString(FeaturesPayload.serializer(), "{}").torrents)
    }
}
