package com.enigmacast.companion.data.update

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `combined TV manifest exposes companion release`() {
        val manifest = json.decodeFromString<UpdateManifest>(
            """
            {
              "versionCode": 76,
              "versionName": "1.0.0-beta.73",
              "companion": {
                "versionCode": 102,
                "versionName": "1.0.0-beta.3",
                "apkUrl": "https://example.test/companion.apk",
                "notes": "test"
              }
            }
            """.trimIndent(),
        )
        assertEquals(102, manifest.companion?.versionCode)
        assertEquals("1.0.0-beta.3", manifest.companion?.versionName)
    }

    @Test
    fun `companion-only manifest decodes as release`() {
        val release = json.decodeFromString<RemoteRelease>(
            """
            {"versionCode":102,"versionName":"1.0.0-beta.3","apkUrl":"https://example.test/companion.apk"}
            """.trimIndent(),
        )
        assertEquals(102, release.versionCode)
        assertEquals("https://example.test/companion.apk", release.apkUrl)
    }

    @Test
    fun `newer version is only newer when code increases`() {
        assertTrue(isNewerRelease(101, RemoteRelease(versionCode = 102)))
        assertFalse(isNewerRelease(102, RemoteRelease(versionCode = 102)))
        assertFalse(isNewerRelease(102, RemoteRelease(versionCode = 101)))
    }
}
