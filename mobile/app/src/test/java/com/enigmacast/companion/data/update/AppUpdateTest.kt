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

    @Test
    fun `newest of both manifests wins and empty ones are ignored`() {
        val own = RemoteRelease(versionCode = 119)
        val tv = RemoteRelease(versionCode = 118)
        assertEquals(119, newestRelease(own, tv)?.versionCode)
        assertEquals(118, newestRelease(null, tv)?.versionCode)
        assertEquals(null, newestRelease(null, RemoteRelease()))
    }

    @Test
    fun `update check runs on start and then every six hours`() {
        assertTrue(updateCheckDue(0L, 1_000L))
        assertFalse(updateCheckDue(1_000L, 1_000L + UPDATE_CHECK_INTERVAL_MS - 1))
        assertTrue(updateCheckDue(1_000L, 1_000L + UPDATE_CHECK_INTERVAL_MS))
    }

    @Test
    fun `update urls point at GitHub releases`() {
        assertTrue(UPDATE_MANIFEST_URL.startsWith("https://github.com/ecannemeijer/enigmacast-companion/releases/latest/download/"))
        assertTrue(UPDATE_APK_URL.endsWith("/enigmacast-companion.apk"))
        assertFalse(UPDATE_FALLBACK_MANIFEST_URL.contains("velixatv"))
    }
}
