package com.enigmacast.companion.data.live

import com.enigmacast.domain.xtream.XtreamAccount
import com.enigmacast.domain.xtream.XtreamCategory
import com.enigmacast.domain.xtream.XtreamEpisode
import com.enigmacast.domain.xtream.XtreamEpg
import com.enigmacast.domain.xtream.XtreamItem
import com.enigmacast.domain.xtream.XtreamJson
import com.enigmacast.domain.xtream.XtreamSection
import com.enigmacast.domain.xtream.XtreamUrls
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/** Minimale Xtream Codes-client: inloggen, live-categorieën en live-streams. */
class XtreamClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /** Drop keep-alive sockets after a Wi-Fi/mobile switch; the next call uses the new network. */
    fun evictConnections() {
        client.connectionPool.evictAll()
    }

    suspend fun login(server: String, username: String, password: String): Result<XtreamAccount> =
        withContext(Dispatchers.IO) {
            runCatching {
                val account = account(server, username, password)
                val categories = categories(account, XtreamSection.LIVE).getOrThrow()
                if (categories.isEmpty()) error("Xtream-login geweigerd of geen categorieën")
                account
            }
        }

    suspend fun categories(account: XtreamAccount, section: XtreamSection): Result<List<XtreamCategory>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val action = when (section) {
                    XtreamSection.LIVE -> "get_live_categories"
                    XtreamSection.MOVIES -> "get_vod_categories"
                    XtreamSection.SERIES -> "get_series_categories"
                }
                XtreamJson.categories(get(XtreamUrls.api(account, action)))
            }
        }

    suspend fun items(
        account: XtreamAccount,
        section: XtreamSection,
        categoryId: String?,
    ): Result<List<XtreamItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val action = when (section) {
                XtreamSection.LIVE -> "get_live_streams"
                XtreamSection.MOVIES -> "get_vod_streams"
                XtreamSection.SERIES -> "get_series"
            }
            val params = if (categoryId.isNullOrBlank()) emptyArray() else arrayOf("category_id" to categoryId)
            val body = get(XtreamUrls.api(account, action, *params))
            XtreamJson.items(body, section)
        }
    }

    suspend fun episodes(account: XtreamAccount, seriesId: String): Result<List<XtreamEpisode>> =
        withContext(Dispatchers.IO) {
            runCatching {
                XtreamJson.episodes(get(XtreamUrls.api(account, "get_series_info", "series_id" to seriesId)))
            }
        }

    /** Juiste container-extensie voor een film; providers gebruiken niet altijd mp4. */
    suspend fun movieExtension(account: XtreamAccount, movieId: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                XtreamJson.vodExtension(
                    get(XtreamUrls.api(account, "get_vod_info", "vod_id" to movieId)),
                )
            }.getOrNull()
        }

    suspend fun epg(account: XtreamAccount, streamId: String): Result<List<XtreamEpg>> =
        withContext(Dispatchers.IO) {
            runCatching {
                XtreamJson.shortEpg(
                    get(
                        XtreamUrls.api(
                            account,
                            "get_short_epg",
                            "stream_id" to streamId,
                            "limit" to "24",
                        ),
                    ),
                )
            }
        }

    fun streamUrl(account: XtreamAccount, item: XtreamItem): String = when (item.section) {
        XtreamSection.LIVE -> XtreamUrls.live(account, item.id)
        XtreamSection.MOVIES -> XtreamUrls.movie(account, item.id, item.extension)
        XtreamSection.SERIES -> ""
    }

    fun episodeUrl(account: XtreamAccount, episode: XtreamEpisode): String =
        XtreamUrls.episode(account, episode.id, episode.extension)

    fun liveUrl(account: XtreamAccount, item: XtreamItem): String = XtreamUrls.live(account, item.id)

    private fun account(server: String, username: String, password: String): XtreamAccount {
        val trimmed = server.trim().trimEnd('/')
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
        return XtreamAccount(
            name = withScheme.substringAfter("://").substringBefore('/'),
            server = withScheme,
            username = username.trim(),
            password = password,
        )
    }

    private fun get(url: String): String {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Xtream gaf HTTP ${response.code}")
            return response.body?.string().orEmpty()
        }
    }
}
