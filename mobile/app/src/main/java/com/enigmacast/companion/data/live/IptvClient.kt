package com.enigmacast.companion.data.live

import com.enigmacast.domain.settingsimport.M3uEntry
import com.enigmacast.domain.settingsimport.M3uParser
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/** Leest een M3U-playlist van een URL en parseert hem met dezelfde parser als de TV-app. */
class IptvClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun load(url: String): Result<List<M3uEntry>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Playlist gaf HTTP ${response.code}")
                val text = response.body?.string().orEmpty()
                if (!text.contains("#EXTM3U", ignoreCase = true)) error("Dit is geen M3U-playlist")
                M3uParser.parse(text)
            }
        }
    }

    fun headers(entry: M3uEntry): Map<String, String> = buildMap {
        entry.userAgent?.let { put("User-Agent", it) }
        entry.referrer?.let { put("Referer", it) }
    }
}
