package com.enigmacast.companion.data.tmdb

import org.junit.Assert.assertEquals
import org.junit.Test

class TmdbClientTest {
    private val client = TmdbClient()

    @Test
    fun `provider title is reduced to the searchable name`() {
        assertEquals("The Gentleman", client.cleanTitle("EN - The Gentleman (2025)"))
        assertEquals("Hope", client.cleanTitle("EN - Hope (SUBS) (2026)"))
        assertEquals("Rai 1", client.cleanTitle("|IT| Rai 1 HD"))
    }
}
