package com.enigmacast.companion.data.net

import org.junit.Assert.assertEquals
import org.junit.Test

class NsdTxtTest {

    @Test
    fun parsesThePublishedFields() {
        val data = NsdTxt.parse(
            mapOf(
                "id" to "ec-3e5c85280607055c".toByteArray(),
                "name" to "Woonkamer TV".toByteArray(),
                "fp" to "7Wxzmy9aQYfUHhzroloTmeNPKk4v1GwyZcGzEPluNOs".toByteArray(),
                "app" to "1.0.0-beta.72".toByteArray(),
            ),
        )
        assertEquals("ec-3e5c85280607055c", data.id)
        assertEquals("Woonkamer TV", data.name)
        assertEquals("7Wxzmy9aQYfUHhzroloTmeNPKk4v1GwyZcGzEPluNOs", data.pin)
        assertEquals("1.0.0-beta.72", data.app)
    }

    @Test
    fun missingFieldsStayBlank() {
        val data = NsdTxt.parse(emptyMap())
        assertEquals("", data.id)
        assertEquals("", data.name)
        assertEquals("", data.pin)
        assertEquals("", data.app)
    }

    @Test
    fun pinFallsBackToPinKey() {
        val data = NsdTxt.parse(mapOf("pin" to "abc".toByteArray()))
        assertEquals("abc", data.pin)
    }
}
