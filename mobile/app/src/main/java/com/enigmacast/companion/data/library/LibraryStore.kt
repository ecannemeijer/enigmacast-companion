package com.enigmacast.companion.data.library

import android.content.Context
import com.enigmacast.companion.data.SecureStore
import com.enigmacast.companion.protocol.LibraryEntry
import org.json.JSONArray
import org.json.JSONObject

data class LibraryItem(
    val title: String,
    val url: String,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val source: String = "stream",
    val updatedAt: Long = 0L,
    val favorite: Boolean = false,
    val tvSource: String? = null,
    val tvKind: String? = null,
    val tvId: String? = null,
    val extension: String = "",
)

/**
 * Kijkgeschiedenis en favorieten op de telefoon. De JSON staat in [SecureStore] (AES-GCM/
 * Keystore), omdat IPTV-URL's soms inloggegevens bevatten. Bewust lokaal: de TV bewaart
 * zijn eigen geschiedenis per profiel; de Companion-brug synchroniseert profielen en
 * instellingen.
 */
class LibraryStore(context: Context) {

    private val secure = SecureStore(context)

    @Synchronized
    fun all(): List<LibraryItem> {
        val raw = secure.get(KEY_ITEMS) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.optJSONObject(i) ?: continue
                    val url = o.optString("url").takeIf { it.isNotBlank() } ?: continue
                    add(
                        LibraryItem(
                            title = o.optString("title"),
                            url = url,
                            positionMs = o.optLong("positionMs"),
                            durationMs = o.optLong("durationMs"),
                            source = o.optString("source", "stream"),
                            updatedAt = o.optLong("updatedAt"),
                            favorite = o.optBoolean("favorite"),
                            tvSource = o.optString("tvSource").takeIf { it.isNotBlank() },
                            tvKind = o.optString("tvKind").takeIf { it.isNotBlank() },
                            tvId = o.optString("tvId").takeIf { it.isNotBlank() },
                            extension = o.optString("extension"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun record(
        title: String,
        url: String,
        positionMs: Long,
        durationMs: Long,
        source: String,
        tvSource: String? = null,
        tvKind: String? = null,
        tvId: String? = null,
    ): List<LibraryItem> {
        if (url.isBlank()) return all()
        val current = all().toMutableList()
        val index = current.indexOfFirst { it.url == url }
        val previous = current.getOrNull(index)
        val item = LibraryItem(
            title = title.ifBlank { previous?.title.orEmpty() },
            url = url,
            positionMs = positionMs.coerceAtLeast(0L),
            durationMs = durationMs.coerceAtLeast(0L),
            source = source,
            updatedAt = System.currentTimeMillis(),
            favorite = previous?.favorite == true,
            tvSource = tvSource ?: previous?.tvSource,
            tvKind = tvKind ?: previous?.tvKind,
            tvId = tvId ?: previous?.tvId,
        )
        if (index >= 0) current[index] = item else current += item
        val sorted = current.sortedByDescending { it.updatedAt }.take(100)
        save(sorted)
        return sorted
    }

    @Synchronized
    fun toggleFavorite(url: String): List<LibraryItem> {
        val current = all().toMutableList()
        val index = current.indexOfFirst { it.url == url }
        if (index < 0) return current
        current[index] = current[index].copy(
            favorite = !current[index].favorite,
            updatedAt = System.currentTimeMillis(),
        )
        save(current)
        return current
    }

    @Synchronized
    fun remove(url: String): List<LibraryItem> {
        val remaining = all().filterNot { it.url == url }
        save(remaining)
        return remaining
    }

    /**
     * Voegt de TV-bibliotheek (favorieten + kijkposities) samen met de lokale lijst.
     * De TV is leidend voor de velden die hij stuurt; een lokale favoriet blijft favoriet.
     */
    @Synchronized
    fun mergeTv(entries: List<LibraryEntry>): List<LibraryItem> {
        if (entries.isEmpty()) return all()
        val current = all().toMutableList()
        entries.forEach { entry ->
            if (entry.url.isBlank()) return@forEach
            val index = current.indexOfFirst {
                it.url == entry.url ||
                    (it.tvSource == entry.source && it.tvKind == entry.kind && it.tvId == entry.id)
            }
            val previous = current.getOrNull(index)
            val merged = LibraryItem(
                title = entry.title.ifBlank { previous?.title.orEmpty() },
                url = entry.url,
                positionMs = maxOf(previous?.positionMs ?: 0L, entry.positionMs),
                durationMs = maxOf(previous?.durationMs ?: 0L, entry.durationMs),
                source = "iptv",
                updatedAt = maxOf(previous?.updatedAt ?: 0L, entry.updatedMs),
                favorite = previous?.favorite == true || entry.favorite,
                tvSource = entry.source,
                tvKind = entry.kind,
                tvId = entry.id,
                extension = entry.extension,
            )
            if (index >= 0) current[index] = merged else current += merged
        }
        val sorted = current.sortedByDescending { it.updatedAt }.take(100)
        save(sorted)
        return sorted
    }

    /**
     * De TV is leidend voor de IPTV-bibliotheek. Lokale IPTV-items (waaronder oude demo's)
     * die niet in de TV-lijst staan verdwijnen; lokale Enigma2/live-geschiedenis blijft.
     */
    @Synchronized
    fun syncFromTv(entries: List<LibraryEntry>): List<LibraryItem> {
        val tvKeys = entries.map { Triple(it.source, it.kind, it.id) }.toSet()
        val current = all().filter { item ->
            val matchesTv = item.tvSource != null &&
                Triple(item.tvSource, item.tvKind.orEmpty(), item.tvId.orEmpty()) in tvKeys
            val localMovie = item.tvKind == "m" || item.tvKind == "s"
            matchesTv || (!isDemoUrl(item.url) && (item.source != "iptv" || localMovie))
        }.toMutableList()

        entries.forEach { entry ->
            if (entry.title.isBlank() || entry.source == "torrent") return@forEach
            val url = entry.url.ifBlank {
                "iptv://${entry.source}/${entry.kind}/${entry.id}"
            }
            if (url.isBlank()) return@forEach
            val index = current.indexOfFirst {
                it.url == url ||
                    (it.tvSource == entry.source && it.tvKind == entry.kind && it.tvId == entry.id)
            }
            val previous = current.getOrNull(index)
            val merged = LibraryItem(
                title = entry.title.ifBlank { previous?.title.orEmpty() },
                url = url,
                positionMs = maxOf(previous?.positionMs ?: 0L, entry.positionMs),
                durationMs = maxOf(previous?.durationMs ?: 0L, entry.durationMs),
                source = "iptv",
                updatedAt = maxOf(previous?.updatedAt ?: 0L, entry.updatedMs),
                favorite = previous?.favorite == true || entry.favorite,
                tvSource = entry.source,
                tvKind = entry.kind,
                tvId = entry.id,
                extension = entry.extension,
            )
            if (index >= 0) current[index] = merged else current += merged
        }
        val sorted = current.sortedByDescending { it.updatedAt }.take(100)
        save(sorted)
        return sorted
    }

    @Synchronized
    fun clear(): List<LibraryItem> {
        secure.remove(KEY_ITEMS)
        return emptyList()
    }

    private fun isDemoUrl(url: String): Boolean {
        val host = runCatching { java.net.URI(url).host }.getOrNull()?.lowercase().orEmpty()
        return host == "127.0.0.1" || host == "localhost" || host == "::1" || host == "0.0.0.0"
    }

    private fun save(items: List<LibraryItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("title", item.title)
                    .put("url", item.url)
                    .put("positionMs", item.positionMs)
                    .put("durationMs", item.durationMs)
                    .put("source", item.source)
                    .put("updatedAt", item.updatedAt)
                    .put("favorite", item.favorite)
                    .put("tvSource", item.tvSource.orEmpty())
                    .put("tvKind", item.tvKind.orEmpty())
                    .put("tvId", item.tvId.orEmpty())
                    .put("extension", item.extension),
            )
        }
        secure.put(KEY_ITEMS, array.toString())
    }

    private companion object {
        const val KEY_ITEMS = "items"
    }
}
