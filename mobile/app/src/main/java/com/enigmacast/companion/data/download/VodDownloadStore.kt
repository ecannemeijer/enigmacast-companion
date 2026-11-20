package com.enigmacast.companion.data.download

import android.content.Context
import com.enigmacast.companion.data.SecureStore
import com.enigmacast.domain.download.VodDownload
import com.enigmacast.domain.download.VodDownloadSpace
import com.enigmacast.domain.download.VodDownloadState
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Queue and metadata for IPTV film/episode downloads. URLs can contain credentials, so the
 * JSON lives in [SecureStore]. Files are written to the app-specific folder — no shared
 * storage permission is required on minSdk 26 / target 37.
 */
class VodDownloadStore(context: Context) {
    private val app = context.applicationContext
    private val secure = SecureStore(app)
    private val items = MutableStateFlow(load())
    val downloads: StateFlow<List<VodDownload>> = items.asStateFlow()

    fun all(): List<VodDownload> = items.value

    fun get(id: String): VodDownload? = items.value.firstOrNull { it.id == id }

    fun byLibraryKey(source: String, kind: String, streamId: String): VodDownload? =
        items.value.firstOrNull { it.source == source && it.kind == kind && it.streamId == streamId }

    fun of(source: String, kind: String, id: String): VodDownload? = byLibraryKey(source, kind, id)

    fun nextQueued(): VodDownload? =
        items.value.firstOrNull { it.state == VodDownloadState.QUEUED }
            ?: items.value.firstOrNull { it.state == VodDownloadState.RUNNING }

    @Synchronized
    fun enqueue(item: VodDownload): VodDownload {
        val existing = byLibraryKey(item.source, item.kind, item.streamId)
        if (existing != null && existing.state != VodDownloadState.CANCELED && existing.state != VodDownloadState.FAILED) {
            return existing
        }
        val now = System.currentTimeMillis()
        val created = if (existing != null) {
            existing.copy(
                url = item.url,
                userAgent = item.userAgent,
                httpHeaders = item.httpHeaders,
                volumeIndex = item.volumeIndex,
                title = item.title.ifBlank { existing.title },
                poster = item.poster ?: existing.poster,
                state = VodDownloadState.QUEUED,
                error = "",
                updatedMs = now,
            )
        } else {
            item.copy(
                id = item.id.ifBlank { UUID.randomUUID().toString() },
                addedMs = now,
                updatedMs = now,
                state = VodDownloadState.QUEUED,
            )
        }
        put(created)
        return created
    }

    @Synchronized
    fun put(item: VodDownload) {
        val current = items.value.toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index >= 0) current[index] = item else current += item
        save(current.sortedByDescending { it.addedMs })
    }

    @Synchronized
    fun setState(
        id: String,
        state: VodDownloadState,
        error: String = "",
        downloadedBytes: Long? = null,
        totalBytes: Long? = null,
    ) {
        val current = get(id) ?: return
        put(
            current.copy(
                state = state,
                error = error,
                downloadedBytes = downloadedBytes ?: current.downloadedBytes,
                totalBytes = totalBytes ?: current.totalBytes,
                updatedMs = System.currentTimeMillis(),
            ),
        )
    }

    @Synchronized
    fun pause(id: String) {
        val current = get(id) ?: return
        if (current.state == VodDownloadState.RUNNING || current.state == VodDownloadState.QUEUED) {
            setState(id, VodDownloadState.PAUSED)
        }
    }

    @Synchronized
    fun resume(id: String) {
        val current = get(id) ?: return
        if (current.state == VodDownloadState.PAUSED || current.state == VodDownloadState.FAILED) {
            setState(id, VodDownloadState.QUEUED, error = "")
        }
    }

    @Synchronized
    fun cancel(id: String) {
        val current = get(id) ?: return
        if (current.state != VodDownloadState.DONE) setState(id, VodDownloadState.CANCELED)
    }

    @Synchronized
    fun delete(id: String) {
        val current = get(id)
        current?.let { fileFor(it).delete() }
        save(items.value.filterNot { it.id == id })
    }

    fun fileFor(item: VodDownload): File {
        val dir = File(app.getExternalFilesDir(null) ?: app.filesDir, "downloads")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, item.fileName)
    }

    fun hasRoom(item: VodDownload): Boolean {
        val file = fileFor(item)
        file.parentFile?.mkdirs()
        return VodDownloadSpace.hasRoom(VodDownloadSpace.freeBytes(file), item.totalBytes, item.downloadedBytes)
    }

    private fun load(): List<VodDownload> {
        val raw = secure.get(KEY) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.optJSONObject(i) ?: continue
                    val id = o.optString("id").takeIf { it.isNotBlank() } ?: continue
                    add(
                        VodDownload(
                            id = id,
                            profileId = o.optString("profileId"),
                            source = o.optString("source"),
                            kind = o.optString("kind"),
                            streamId = o.optString("streamId"),
                            title = o.optString("title"),
                            seriesId = o.optString("seriesId").takeIf { it.isNotBlank() },
                            seriesTitle = o.optString("seriesTitle").takeIf { it.isNotBlank() },
                            season = o.optInt("season"),
                            episode = o.optInt("episode"),
                            poster = o.optString("poster").takeIf { it.isNotBlank() },
                            url = o.optString("url"),
                            extension = o.optString("extension"),
                            userAgent = o.optString("userAgent"),
                            httpHeaders = o.optString("httpHeaders"),
                            volumeIndex = o.optInt("volumeIndex"),
                            fileName = o.optString("fileName"),
                            totalBytes = o.optLong("totalBytes"),
                            downloadedBytes = o.optLong("downloadedBytes"),
                            state = runCatching { VodDownloadState.valueOf(o.optString("state")) }
                                .getOrDefault(VodDownloadState.QUEUED),
                            error = o.optString("error"),
                            addedMs = o.optLong("addedMs"),
                            updatedMs = o.optLong("updatedMs"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun save(list: List<VodDownload>) {
        val array = JSONArray()
        list.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("profileId", item.profileId)
                    .put("source", item.source)
                    .put("kind", item.kind)
                    .put("streamId", item.streamId)
                    .put("title", item.title)
                    .put("seriesId", item.seriesId.orEmpty())
                    .put("seriesTitle", item.seriesTitle.orEmpty())
                    .put("season", item.season)
                    .put("episode", item.episode)
                    .put("poster", item.poster.orEmpty())
                    .put("url", item.url)
                    .put("extension", item.extension)
                    .put("userAgent", item.userAgent)
                    .put("httpHeaders", item.httpHeaders)
                    .put("volumeIndex", item.volumeIndex)
                    .put("fileName", item.fileName)
                    .put("totalBytes", item.totalBytes)
                    .put("downloadedBytes", item.downloadedBytes)
                    .put("state", item.state.name)
                    .put("error", item.error)
                    .put("addedMs", item.addedMs)
                    .put("updatedMs", item.updatedMs),
            )
        }
        secure.put(KEY, array.toString())
        items.value = list
    }

    private companion object {
        const val KEY = "vod_downloads"
    }
}
