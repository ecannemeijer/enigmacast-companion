package com.enigmacast.companion.data.library

import android.content.Context
import com.enigmacast.companion.data.SecureStore
import com.enigmacast.companion.protocol.LibraryAction
import com.enigmacast.companion.protocol.LibraryCommand
import com.enigmacast.companion.protocol.LibraryEntry
import com.enigmacast.data.db.IptvResumeRow
import com.enigmacast.data.db.SyncOutboxRow
import com.enigmacast.domain.sync.LibrarySync
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/**
 * Resume and watched progress saved while the paired TV or the sync server is unreachable.
 * Payloads use the same [LibrarySync] keys as the TV IptvLibrary (source/kind/id).
 */
class LibraryOutbox(context: Context) {
    private val secure = SecureStore(context.applicationContext)

    @Synchronized
    fun enqueueProgress(
        profileId: String,
        source: String,
        kind: String,
        id: String,
        title: String,
        url: String,
        positionMs: Long,
        durationMs: Long,
        extension: String = "",
        seriesId: String? = null,
        seriesTitle: String? = null,
        season: Int = 0,
        episode: Int = 0,
        favorite: Boolean = false,
    ) {
        if (source.isBlank() || kind.isBlank() || id.isBlank()) return
        val now = System.currentTimeMillis()
        val row = IptvResumeRow(
            source = source,
            kind = kind,
            id = id,
            positionMs = positionMs,
            durationMs = durationMs,
            updatedMs = now,
            title = title,
            poster = null,
            extension = extension,
            url = url,
            categoryId = "",
            seriesId = seriesId,
            seriesTitle = seriesTitle,
            season = season,
            episode = episode,
            profileId = profileId,
        )
        val item = SyncOutboxRow(
            changeId = UUID.randomUUID().toString(),
            profileId = profileId,
            entity = LibrarySync.PROGRESS,
            op = "upsert",
            payload = LibrarySync.progressPayload(row),
            updatedMs = now,
            deviceId = "companion",
        )
        save(load() + item)
    }

    @Synchronized
    fun pending(): List<SyncOutboxRow> = load()

    /** Newest change per record, ready to send; also the change ids to drop after a successful flush. */
    @Synchronized
    fun batch(): LibrarySync.Batch = LibrarySync.batch(load())

    @Synchronized
    fun settle(changeIds: Collection<String>) {
        if (changeIds.isEmpty()) return
        val drop = changeIds.toSet()
        save(load().filterNot { it.changeId in drop })
    }

    fun commandFor(row: SyncOutboxRow, favorite: Boolean = false): LibraryCommand? {
        val key = LibrarySync.keyOfChange("", row.payload) ?: return null
        val parsed = LibrarySync.progressFrom(row.profileId, key, row.payload, row.updatedMs, null)
        return LibraryCommand(
            action = LibraryAction.PROGRESS,
            entry = LibraryEntry(
                title = parsed.title,
                url = parsed.url,
                source = parsed.source,
                kind = parsed.kind,
                id = parsed.id,
                positionMs = parsed.positionMs,
                durationMs = parsed.durationMs,
                favorite = favorite,
                updatedMs = parsed.updatedMs,
                extension = parsed.extension,
            ),
        )
    }

    private fun load(): List<SyncOutboxRow> {
        val raw = secure.get(KEY) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.optJSONObject(i) ?: continue
                    val changeId = o.optString("changeId").takeIf { it.isNotBlank() } ?: continue
                    add(
                        SyncOutboxRow(
                            changeId = changeId,
                            profileId = o.optString("profileId"),
                            entity = o.optString("entity", LibrarySync.PROGRESS),
                            op = o.optString("op", "upsert"),
                            payload = o.optString("payload"),
                            updatedMs = o.optLong("updatedMs"),
                            deviceId = o.optString("deviceId"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun save(rows: List<SyncOutboxRow>) {
        val array = JSONArray()
        rows.forEach { row ->
            array.put(
                JSONObject()
                    .put("changeId", row.changeId)
                    .put("profileId", row.profileId)
                    .put("entity", row.entity)
                    .put("op", row.op)
                    .put("payload", row.payload)
                    .put("updatedMs", row.updatedMs)
                    .put("deviceId", row.deviceId),
            )
        }
        if (rows.isEmpty()) secure.remove(KEY) else secure.put(KEY, array.toString())
    }

    private companion object {
        const val KEY = "library_outbox"
    }
}
