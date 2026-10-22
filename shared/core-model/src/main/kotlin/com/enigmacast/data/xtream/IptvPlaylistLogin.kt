package com.enigmacast.data.xtream

import com.enigmacast.domain.model.IptvPlaylistKind
import com.enigmacast.domain.xtream.XStreamityPlaylists
import com.enigmacast.domain.xtream.XtreamAccount

/**
 * De playlist die de gebruiker invoert (M3U-URL of Xtream-login).
 *
 * Overgenomen uit `data/xtream/IptvPlaylist.kt` in EnigmaCast, maar zonder de opslag
 * (`IptvPlaylistStore`): de Companion bewaart hem in zijn eigen Keystore-store en deelt
 * alleen dit model met de rest van de code.
 */
data class IptvPlaylistLogin(
    val m3uUrl: String = "",
    val server: String = "",
    val username: String = "",
    val password: String = "",
) {
    fun configured(kind: IptvPlaylistKind): Boolean = when (kind) {
        IptvPlaylistKind.M3U -> m3uUrl.startsWith("http://", true) || m3uUrl.startsWith("https://", true)
        IptvPlaylistKind.XTREAM -> xtreamAccount() != null
    }

    /** The Xtream account; a bare host gets http://. */
    fun xtreamAccount(): XtreamAccount? {
        if (server.isBlank() || username.isBlank()) return null
        val base = server.trim().let { if (it.contains("://")) it else "http://$it" }
            .trimEnd('/').substringBefore("/player_api.php").substringBefore("/get.php")
        return XStreamityPlaylists.fromUrl("$base/get.php?username=${enc(username)}&password=${enc(password)}&output=ts", "Playlist")
    }

    override fun toString(): String = "IptvPlaylistLogin(***)"

    private fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")
}
