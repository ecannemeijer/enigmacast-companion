package com.enigmacast.companion.data

import android.content.Context
import com.enigmacast.domain.model.ReceiverConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Lokale opslag: gewone voorkeuren in SharedPreferences, het koppeltoken versleuteld in
 * [SecureStore]. De vorm van [PairedTv] blijft gelijk aan de mock-periode.
 */
class LocalStore(context: Context) {

    private val prefs = context.getSharedPreferences("companion", Context.MODE_PRIVATE)
    private val secure = SecureStore(context)
    private val liveJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    var haptics: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTICS, value).apply()

    fun pairedTv(): PairedTv? {
        val id = prefs.getString(KEY_ID, null) ?: return null
        val name = prefs.getString(KEY_NAME, null) ?: return null
        val host = prefs.getString(KEY_HOST, null) ?: return null
        return PairedTv(
            id = id,
            name = name,
            host = host,
            port = prefs.getInt(KEY_PORT, 8750),
            online = true,
            pin = prefs.getString(KEY_PIN, "").orEmpty(),
            token = secure.get(KEY_TOKEN).orEmpty(),
            deviceId = prefs.getString(KEY_DEVICE_ID, "").orEmpty(),
            real = prefs.getBoolean(KEY_REAL, false),
        )
    }

    fun savePairedTv(tv: PairedTv) {
        prefs.edit()
            .putString(KEY_ID, tv.id)
            .putString(KEY_NAME, tv.name)
            .putString(KEY_HOST, tv.host)
            .putInt(KEY_PORT, tv.port)
            .putString(KEY_PIN, tv.pin)
            .putString(KEY_DEVICE_ID, tv.deviceId)
            .putBoolean(KEY_REAL, tv.real)
            .apply()
        secure.put(KEY_TOKEN, tv.token)
    }

    fun forgetTv() {
        prefs.edit()
            .remove(KEY_ID)
            .remove(KEY_NAME)
            .remove(KEY_HOST)
            .remove(KEY_PORT)
            .remove(KEY_PIN)
            .remove(KEY_DEVICE_ID)
            .remove(KEY_REAL)
            .apply()
        secure.remove(KEY_TOKEN)
    }

    // ------------------------------------------------------------ live-ontvanger

    fun liveReceiver(): ReceiverConfig? {
        val raw = prefs.getString(KEY_LIVE_RECEIVER, null) ?: return null
        val parsed = runCatching { liveJson.decodeFromString(ReceiverConfig.serializer(), raw) }.getOrNull()
            ?: return null
        return parsed.copy(password = secure.get(KEY_LIVE_PASSWORD).orEmpty())
    }

    fun saveLiveReceiver(config: ReceiverConfig) {
        val stripped = config.copy(password = "")
        prefs.edit()
            .putString(KEY_LIVE_RECEIVER, liveJson.encodeToString(ReceiverConfig.serializer(), stripped))
            .apply()
        secure.put(KEY_LIVE_PASSWORD, config.password)
    }

    fun clearLiveReceiver() {
        prefs.edit().remove(KEY_LIVE_RECEIVER).apply()
        secure.remove(KEY_LIVE_PASSWORD)
    }

    fun iptvUrl(): String? {
        val secureUrl = secure.get(KEY_IPTV_URL)
        if (secureUrl != null) return secureUrl
        val legacy = prefs.getString(KEY_IPTV_URL, null)?.takeIf { it.isNotBlank() } ?: return null
        // Eenmalig migreren: een M3U-URL kan een token bevatten.
        secure.put(KEY_IPTV_URL, legacy)
        prefs.edit().remove(KEY_IPTV_URL).apply()
        return legacy
    }

    fun saveIptvUrl(url: String) {
        secure.put(KEY_IPTV_URL, url)
        prefs.edit().remove(KEY_IPTV_URL).apply()
    }

    fun iptvKind(): String? = prefs.getString(KEY_IPTV_KIND, null)?.takeIf { it.isNotBlank() }

    fun saveIptvKind(kind: String) {
        prefs.edit().putString(KEY_IPTV_KIND, kind).apply()
    }

    fun xtreamServer(): String? = prefs.getString(KEY_XTREAM_SERVER, null)?.takeIf { it.isNotBlank() }

    fun xtreamUser(): String? = prefs.getString(KEY_XTREAM_USER, null)?.takeIf { it.isNotBlank() }

    fun xtreamPassword(): String? = secure.get(KEY_XTREAM_PASSWORD)

    fun saveXtream(server: String, user: String, password: String) {
        prefs.edit()
            .putString(KEY_XTREAM_SERVER, server)
            .putString(KEY_XTREAM_USER, user)
            .apply()
        secure.put(KEY_XTREAM_PASSWORD, password)
    }

    private companion object {
        const val KEY_HAPTICS = "haptics"
        const val KEY_ID = "tv.id"
        const val KEY_NAME = "tv.name"
        const val KEY_HOST = "tv.host"
        const val KEY_PORT = "tv.port"
        const val KEY_PIN = "tv.pin"
        const val KEY_DEVICE_ID = "tv.deviceId"
        const val KEY_REAL = "tv.real"
        const val KEY_TOKEN = "tv.token"
        const val KEY_LIVE_RECEIVER = "live.receiver"
        const val KEY_LIVE_PASSWORD = "live.password"
        const val KEY_IPTV_URL = "iptv.url"
        const val KEY_IPTV_KIND = "iptv.kind"
        const val KEY_XTREAM_SERVER = "xtream.server"
        const val KEY_XTREAM_USER = "xtream.user"
        const val KEY_XTREAM_PASSWORD = "xtream.password"
    }
}
