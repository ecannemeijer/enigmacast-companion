package com.enigmacast.companion.data.download

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.enigmacast.domain.download.VodDownload
import com.enigmacast.domain.download.VodDownloadFiles
import com.enigmacast.domain.download.VodDownloadState
import com.enigmacast.domain.xtream.IptvHeaders
import com.enigmacast.domain.xtream.XtreamAccount
import com.enigmacast.domain.xtream.XtreamEpisode
import com.enigmacast.domain.xtream.XtreamItem
import com.enigmacast.domain.xtream.XtreamUrls
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VodDownloadManager(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val store = VodDownloadStore(context)
    val items: StateFlow<List<VodDownload>> = store.downloads
    val byKey: StateFlow<Map<String, VodDownload>> = store.downloads
        .map { list -> list.associateBy { it.libraryKey } }
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    fun of(source: String, kind: String, id: String): VodDownload? = byKey.value["$source/$kind/$id"]

    fun fileUri(item: VodDownload): String? {
        val file = store.fileFor(item)
        return if (item.state == VodDownloadState.DONE && file.exists()) Uri.fromFile(file).toString() else null
    }

    /** Null when the title can be queued; otherwise a string resource reason. */
    fun rejectReason(url: String, extension: String): VodReject? {
        if (VodDownloader.isPlaylist(null, url) || isPlaylistExtension(extension, url)) {
            return VodReject.NOT_A_FILE
        }
        val ext = extensionOf(extension, url)
        if (ext.isNotBlank() && ext !in DIRECT_EXTENSIONS) {
            return VodReject.NOT_A_FILE
        }
        return null
    }

    fun enqueueMovie(account: XtreamAccount, item: XtreamItem): VodReject? {
        val url = item.url.ifBlank { XtreamUrls.movie(account, item.id, item.extension) }
        val reject = rejectReason(url, item.extension)
        if (reject != null) return reject
        scope.launch {
            enqueue(
                source = account.key,
                kind = "m",
                streamId = item.id,
                title = item.name,
                poster = item.icon.takeIf { it.isNotBlank() },
                url = url,
                extension = item.extension,
            )
        }
        return null
    }

    fun enqueueEpisode(account: XtreamAccount, show: XtreamItem, episode: XtreamEpisode, episodeUrl: String = ""): VodReject? {
        val url = episodeUrl.ifBlank { XtreamUrls.episode(account, episode.id, episode.extension) }
        val reject = rejectReason(url, episode.extension)
        if (reject != null) return reject
        scope.launch {
            enqueue(
                source = account.key,
                kind = "e",
                streamId = episode.id,
                title = episode.title.ifBlank { show.name } + if (episode.season > 0) " · S${episode.season}E${episode.episode}" else "",
                seriesId = show.id,
                seriesTitle = show.name,
                season = episode.season,
                episode = episode.episode,
                poster = episode.cover.ifBlank { show.icon }.takeIf { it.isNotBlank() },
                url = url,
                extension = episode.extension,
            )
        }
        return null
    }

    fun enqueueSeason(
        account: XtreamAccount,
        show: XtreamItem,
        season: Int,
        episodes: List<XtreamEpisode>,
        urlOf: (XtreamEpisode) -> String,
    ): Int {
        var added = 0
        episodes.filter { it.season == season }.forEach { episode ->
            if (enqueueEpisode(account, show, episode, urlOf(episode)) == null) added += 1
        }
        return added
    }

    fun pause(id: String) { scope.launch { store.pause(id); wake() } }
    fun resume(id: String) { scope.launch { store.resume(id); wake() } }
    fun cancel(id: String) { scope.launch { store.cancel(id); wake() } }
    fun delete(id: String) { scope.launch { store.delete(id) } }

    fun wake() {
        runCatching { context.startForegroundService(Intent(context, VodDownloadService::class.java)) }
    }

    private fun enqueue(
        source: String,
        kind: String,
        streamId: String,
        title: String,
        seriesId: String? = null,
        seriesTitle: String? = null,
        season: Int = 0,
        episode: Int = 0,
        poster: String?,
        url: String,
        extension: String,
    ) {
        val now = System.currentTimeMillis()
        val job = VodDownload(
            id = "",
            profileId = "",
            source = source,
            kind = kind,
            streamId = streamId,
            title = title,
            seriesId = seriesId,
            seriesTitle = seriesTitle,
            season = season,
            episode = episode,
            poster = poster,
            url = url,
            extension = extension,
            userAgent = IptvHeaders.DEFAULT_USER_AGENT,
            httpHeaders = "",
            volumeIndex = 0,
            fileName = VodDownloadFiles.safeName(kind, streamId, extension),
            addedMs = now,
            updatedMs = now,
        )
        val created = store.enqueue(job)
        if (created.state == VodDownloadState.QUEUED || created.state == VodDownloadState.RUNNING) wake()
    }

    companion object {
        val DIRECT_EXTENSIONS = setOf("mp4", "mkv")

        fun extensionOf(extension: String, url: String): String =
            extension.replace(Regex("[^A-Za-z0-9]"), "").lowercase().ifBlank {
                url.substringBefore('?').substringAfterLast('.', "").lowercase()
            }

        fun isPlaylistExtension(extension: String, url: String): Boolean =
            extensionOf(extension, url) in setOf("m3u8", "m3u", "mpd")
    }
}

enum class VodReject { NOT_A_FILE }
