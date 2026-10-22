package com.enigmacast.companion.data.tmdb

import com.enigmacast.companion.BuildConfig
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

data class TmdbInfo(
    val title: String,
    val year: String = "",
    val overview: String = "",
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val rating: Double? = null,
    val series: Boolean = false,
)

/** Kleine TMDB-zoekclient voor de IPTV-films en -series. */
class TmdbClient {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun lookup(title: String, series: Boolean, yearHint: String? = null): TmdbInfo? = withContext(Dispatchers.IO) {
        val key = BuildConfig.TMDB_API_KEY.trim()
        if (key.isBlank() || title.isBlank()) return@withContext null
        val query = cleanTitle(title)
        if (query.isBlank()) return@withContext null
        val path = if (series) "search/tv" else "search/movie"
        val url = "https://api.themoviedb.org/3/$path".toHttpUrl().newBuilder()
            .addQueryParameter("api_key", key)
            .addQueryParameter("query", query)
            .addQueryParameter("language", "nl-NL")
            .addQueryParameter("include_adult", "false")
            .apply {
                if (!series && !yearHint.isNullOrBlank()) addQueryParameter("year", yearHint)
                if (series && !yearHint.isNullOrBlank()) addQueryParameter("first_air_date_year", yearHint)
            }
            .build()
        val body = runCatching {
            client.newCall(Request.Builder().url(url).header("Accept", "application/json").build())
                .execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    response.body.string()
                }
        }.getOrNull() ?: return@withContext null
        val results = runCatching {
            json.parseToJsonElement(body).jsonObject["results"]?.jsonArray
        }.getOrNull() ?: return@withContext null
        val objects = results.mapNotNull { runCatching { it.jsonObject }.getOrNull() }
        val normalized = query.lowercase()
        val best = objects.firstOrNull { result ->
            val name = result.text(if (series) "name" else "title").lowercase()
            val date = result.text(if (series) "first_air_date" else "release_date")
            name == normalized && (yearHint.isNullOrBlank() || date.take(4) == yearHint)
        } ?: objects.firstOrNull { result ->
            result.text(if (series) "name" else "title").lowercase() == normalized
        }
        best?.toInfo(series)
    }

    private fun JsonObject.text(key: String): String =
        this[key]?.jsonPrimitive?.contentOrNull.orEmpty()

    /** Provider-titels zoals "EN - The Gentleman (2025)" terug naar "The Gentleman". */
    fun cleanTitle(raw: String): String = raw
        .replace(Regex("(?i)^\\s*\\|[A-Z]{2}\\|\\s*"), "")
        .replace(Regex("(?i)^\\s*(?:[A-Z]{2,3}|VOD)\\s*[-–|:]\\s*"), "")
        .replace(Regex("(?i)\\[[^\\]]*\\]"), " ")
        .replace(Regex("(?i)\\((?:19|20)\\d{2}\\)"), " ")
        .replace(Regex("(?i)\\((?:SUBS?|SUBTITLED|MULTI|DUBBED|HD|UHD|SD|FHD|4K)\\)"), " ")
        .replace(Regex("(?i)\\b(SUBS?|SUBTITLED|MULTI|DUBBED|HD|UHD|SD|FHD|4K)\\b"), " ")
        .replace(Regex("\\s*\\(\\s*\\)"), " ")
        .replace(Regex("\\s+"), " ")
        .trim(' ', '-', '|', ':', '.', '(', ')')

    private fun JsonObject.toInfo(series: Boolean): TmdbInfo {
        fun text(key: String): String = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
        val poster = text("poster_path").let { if (it.isBlank()) "" else "https://image.tmdb.org/t/p/w500$it" }
        val backdrop = text("backdrop_path").let { if (it.isBlank()) "" else "https://image.tmdb.org/t/p/w780$it" }
        val date = text(if (series) "first_air_date" else "release_date")
        return TmdbInfo(
            title = text(if (series) "name" else "title").ifBlank { text("original_name") },
            year = date.take(4),
            overview = text("overview"),
            posterUrl = poster,
            backdropUrl = backdrop,
            rating = this["vote_average"]?.jsonPrimitive?.doubleOrNull,
            series = series,
        )
    }
}
