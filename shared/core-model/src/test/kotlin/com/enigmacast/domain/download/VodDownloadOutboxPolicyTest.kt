package com.enigmacast.domain.download

import com.enigmacast.data.db.IptvResumeRow
import com.enigmacast.data.db.SyncOutboxRow
import com.enigmacast.data.xtream.IptvLibrary
import com.enigmacast.domain.sync.LibraryMerge
import com.enigmacast.domain.sync.LibraryMergeResult
import com.enigmacast.domain.sync.LibrarySync
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Offline plays of a download use the same IptvLibrary key as streamed VOD, so resume
 * goes into the existing outbox and merges with the same rules when the sync path
 * (paired TV, then the sync server) is reachable again.
 */
class VodDownloadOutboxPolicyTest {
    @Test
    fun downloadAndStreamShareOneLibraryKey() {
        val download = sample(source = "acc", kind = "e", streamId = "42")
        assertEquals("acc/e/42", download.libraryKey)
        assertEquals(IptvLibrary.key("acc", "e", "42"), download.libraryKey)
        assertEquals(IptvLibrary.key("acc", "m", "9"), sample(kind = "m", streamId = "9").libraryKey)
    }

    @Test
    fun progressPayloadUsesTheSameKey() {
        val download = sample(kind = "e", streamId = "42")
        val parts = LibrarySync.parts(download.libraryKey)!!
        val row = IptvResumeRow(
            source = parts.first, kind = parts.second, id = parts.third,
            positionMs = 80_000, durationMs = 100_000, updatedMs = 2_000L,
            title = "Film", poster = null, extension = "mp4", url = download.url,
            categoryId = "", seriesId = "7", seriesTitle = "Show", season = 1, episode = 2,
            profileId = "p",
        )
        val payload = LibrarySync.progressPayload(row)
        assertEquals(Triple("acc", "e", "42"), LibrarySync.keyOfChange("", payload))
        val parsed = LibrarySync.progressFrom("p", parts, payload, 2_000L, null)
        assertEquals(80_000L, parsed.positionMs)
        assertEquals("7", parsed.seriesId)
    }

    @Test
    fun anOlderIncomingSaveDoesNotReplaceANewerOne() {
        assertEquals(LibraryMergeResult.UPSERT, LibraryMerge.decide(1_000, null, 2_000, "tv", "upsert"))
        assertEquals(LibraryMergeResult.IGNORE, LibraryMerge.decide(3_000, null, 2_000, "tv", "upsert"))
    }

    @Test
    fun outboxProgressKeepsTheNewestTimestamp() {
        fun progress(pos: Long, at: Long, change: String) = SyncOutboxRow(
            change, "p", LibrarySync.PROGRESS, "upsert",
            LibrarySync.progressPayload(
                IptvResumeRow(
                    "acc", "m", "9", pos, 100_000, at, "t", null, "mp4",
                    "http://example/movie.mp4", "", null, null, 0, 0, "p",
                ),
            ),
            at, "",
        )
        val batch = LibrarySync.batch(listOf(progress(30_000, 1_000, "c1"), progress(80_000, 2_000, "c2")))
        assertEquals(1, batch.send.size)
        assertEquals("c2", batch.send.single().changeId)
        assertEquals(setOf("c1", "c2"), batch.settled.toSet())
        assertTrue(LibrarySync.keyOf(batch.send.single().payload) == "acc/m/9")
    }

    private fun sample(source: String = "acc", kind: String = "m", streamId: String = "1") = VodDownload(
        id = "d1",
        profileId = "p",
        source = source,
        kind = kind,
        streamId = streamId,
        title = "Film",
        url = "http://example/movie.mp4",
        extension = "mp4",
        userAgent = "EnigmaCast",
        httpHeaders = "",
        volumeIndex = 0,
        fileName = "vod_${kind}_$streamId.mp4",
        addedMs = 1,
        updatedMs = 1,
    )
}
