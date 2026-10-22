package com.enigmacast.companion.data

import com.enigmacast.companion.protocol.ChannelInfo
import com.enigmacast.companion.protocol.NowInfo
import com.enigmacast.companion.protocol.PlayerInfo
import com.enigmacast.companion.protocol.ProfileInfo
import com.enigmacast.companion.protocol.TunerInfo

/** Hoe de telefoon de TV bestuurt: de app zelf, of de Enigma2-ontvanger eronder. */
enum class ControlMode { APP, RECEIVER }

enum class LinkState { IDLE, CONNECTING, READY, OFFLINE, ERROR }

data class LinkStatus(
    val state: LinkState = LinkState.IDLE,
    val tvName: String? = null,
    val latencyMs: Long? = null,
    val message: String? = null,
)

/** Een gekoppelde TV. [real] = de echte brug; anders de demo/mock. */
data class PairedTv(
    val id: String,
    val name: String,
    val host: String,
    val port: Int = 8750,
    val online: Boolean = true,
    val pin: String = "",
    val token: String = "",
    val deviceId: String = "",
    val real: Boolean = false,
)

/** Wat mDNS oplevert voordat er gekoppeld is. */
data class DiscoveredTv(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val pin: String = "",
    val appVersion: String = "",
) {
    fun toPairedTv(): PairedTv = PairedTv(
        id = id,
        name = name,
        host = host,
        port = port,
        online = true,
        pin = pin,
        real = true,
    )
}

/** Alles wat de UI van de TV toont. Komt uit `hello` en `state.*`; nooit uit lokale gokken. */
data class TvSnapshot(
    val tvName: String = "",
    val appVersion: String = "",
    val api: String = "",
    val capabilities: List<String> = emptyList(),
    val profiles: List<ProfileInfo> = emptyList(),
    val activeProfileId: String = "",
    val receiverName: String = "",
    val receiverOnline: Boolean = false,
    val receiverMode: String = "own_tuner",
    val settingsRevision: Long = 0,
    val source: String = "idle",
    val channel: ChannelInfo? = null,
    val now: NowInfo? = null,
    val nextTitle: String? = null,
    val nextStartMs: Long? = null,
    val player: PlayerInfo = PlayerInfo(),
    val tuner: TunerInfo = TunerInfo(),
    val route: String = "home",
    val screenApp: String = "",
    val screenSection: String? = null,
    val lastKey: String? = null,
    val volume: Int = 30,
    val muted: Boolean = false,
    val focusLabel: String? = null,
) {
    val activeProfile: ProfileInfo? get() = profiles.firstOrNull { it.id == activeProfileId }
    val isPlaying: Boolean get() = player.playing && !player.paused
}
