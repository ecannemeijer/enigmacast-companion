package com.enigmacast.companion.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Eén bericht over de WebSocket. [id] is alleen gezet bij client-commando's (voor de ack),
 * [seq] alleen bij server-events (monotoon, voor hervatten na een reconnect).
 */
@Serializable
data class Envelope(
    val v: Int = Protocol.VERSION,
    val id: String? = null,
    val seq: Long? = null,
    val type: String,
    val ts: Long? = null,
    val payload: JsonElement? = null,
)

// ---------------------------------------------------------------- commando's

@Serializable
enum class KeyPhase {
    DOWN,
    UP,
    REPEAT,
}

/** De virtuele toetsen die de telefoon kan sturen. Namen zijn contract. */
@Serializable
enum class RemoteKey {
    DPAD_UP, DPAD_DOWN, DPAD_LEFT, DPAD_RIGHT,
    OK, BACK, MENU, HOME,
    PLAY_PAUSE, STOP, FAST_FORWARD, REWIND, NEXT, PREVIOUS,
    CHANNEL_UP, CHANNEL_DOWN,
    VOLUME_UP, VOLUME_DOWN, MUTE,
    INFO, GUIDE, RED, GREEN, YELLOW, BLUE,
    DIGIT_0, DIGIT_1, DIGIT_2, DIGIT_3, DIGIT_4,
    DIGIT_5, DIGIT_6, DIGIT_7, DIGIT_8, DIGIT_9,
}

@Serializable
data class KeyCommand(
    val key: RemoteKey,
    val phase: KeyPhase,
    /** 0 voor de eerste press; telt op bij een herhaalde richting-/kanaaltoets. */
    val repeat: Int = 0,
    /** Lang ingedrukt (voor BACK, MENU, INFO, kleurtoetsen). */
    val long: Boolean = false,
)

@Serializable
enum class NavTarget {
    HOME, DASHBOARD, SETTINGS, GUIDE, SEARCH, MY_ITEMS, IPTV, PLAYER,
}

@Serializable
data class NavCommand(val target: NavTarget)

@Serializable
data class ZapCommand(
    val serviceRef: String? = null,
    val bouquetRef: String? = null,
    /** Direct zendernummer; alleen gebruikt als [serviceRef] ontbreekt. */
    val number: Int? = null,
)

@Serializable
enum class VolumeTarget { TV, RECEIVER, PHONE }

@Serializable
data class VolumeCommand(
    val target: VolumeTarget,
    /** +1 of -1; 0 betekent: alleen [mute] toepassen. */
    val delta: Int = 0,
    val mute: Boolean? = null,
)

@Serializable
enum class PlayerCommandAction { PLAY, PAUSE, STOP, SEEK, SPEED }

@Serializable
data class PlayerCommand(
    val action: PlayerCommandAction,
    val positionMs: Long? = null,
    val speed: Float? = null,
)

@Serializable
enum class PowerCommandAction { STANDBY, WAKE, REBOOT, RESTART_GUI, DEEP_STANDBY }

@Serializable
data class PowerCommand(val action: PowerCommandAction)

@Serializable
data class SyncCommand(val now: Boolean = true)

@Serializable
enum class ProfileCommandAction { SELECT, CREATE, RENAME, SET_AVATAR, DELETE }

@Serializable
data class ProfileCommand(
    val action: ProfileCommandAction,
    val uuid: String? = null,
    val name: String? = null,
    val avatar: Int? = null,
    /** Nodig als het profiel een PIN heeft. */
    val pin: String? = null,
    /** Bij RENAME/SET_AVATAR: de bestaande profielfoto verwijderen. */
    val clearPhoto: Boolean = false,
)

// ---------------------------------------------------------------- instellingen

@Serializable
enum class SettingsScope { DEVICE, PROFILE, RECEIVER }

@Serializable
data class SettingsPatchOp(
    /** Stippad in de catalogus, bijvoorbeeld `playback.hdmiPassthrough` of `osdSkin`. */
    val path: String,
    /** JSON-waarde: string, getal, boolean of null. */
    val value: JsonElement? = null,
)

@Serializable
data class SettingsCommand(
    val baseRevision: Long,
    val ops: List<SettingsPatchOp>,
    /** true bij een instelling die de speler of sessie herbouwt. */
    val confirm: Boolean = false,
    /** Alleen nodig als de TV een Settings-PIN heeft en een gevorderd pad wijzigt. */
    val pin: String? = null,
)

@Serializable
data class SettingsResultPayload(
    val revision: Long,
    val applied: List<String> = emptyList(),
    val rejected: List<RejectedSetting> = emptyList(),
    /** Paden waarvoor de TV opnieuw moet opstarten of de speler herbouwt. */
    val restart: List<String> = emptyList(),
)

@Serializable
data class RejectedSetting(val path: String, val code: String, val message: String)

@Serializable
data class SettingOption(val value: String, val label: String)

/**
 * Eén instelling in de catalogus. [type] is `BOOL`, `CHOICE`, `NUMBER` of `TEXT`.
 * [value] is de huidige waarde als JSON (boolean, string of getal).
 */
@Serializable
data class SettingEntry(
    val path: String,
    val label: String,
    val description: String = "",
    val category: String = "",
    val type: String,
    val value: JsonElement? = null,
    val options: List<SettingOption> = emptyList(),
    val min: Double? = null,
    val max: Double? = null,
    val step: Double? = null,
    /** Gevorderde instelling: de telefoon vraagt eerst om bevestiging. */
    val dangerous: Boolean = false,
    /** De TV moet opnieuw starten of de speler herbouwen. */
    val requiresRestart: Boolean = false,
)

@Serializable
data class SettingsCatalogPayload(
    val revision: Long,
    val entries: List<SettingEntry> = emptyList(),
    /** De TV heeft een Settings-PIN; gevorderde paden vragen de pincode. */
    val locked: Boolean = false,
)

// ---------------------------------------------------------------- formulieren

@Serializable
enum class FieldType { TEXT, HOST, NUMBER, PASSWORD, MULTILINE, SELECT, TOGGLE }

@Serializable
data class FormField(
    val id: String,
    val label: String,
    val type: FieldType,
    /** Nooit gezet voor [FieldType.PASSWORD] of `sensitive` velden. */
    val value: String? = null,
    val masked: Boolean = false,
    val sensitive: Boolean = false,
    val maxLength: Int = 0,
    val options: List<String> = emptyList(),
    val required: Boolean = false,
)

@Serializable
data class FormDescriptor(
    val formId: String,
    val title: String,
    val revision: Long,
    val fields: List<FormField>,
    val errors: Map<String, String> = emptyMap(),
    /** De TV heeft het formulier gesloten (focus weg, geannuleerd of verzonden). */
    val closed: Boolean = false,
)

@Serializable
data class FormSubmitCommand(
    val formId: String,
    val revision: Long,
    val values: Map<String, String>,
    /** `TEXT` = live bijwerken, `SUBMIT` = klaar, `CANCEL` = sluiten. */
    val action: String = "SUBMIT",
)

// ---------------------------------------------------------------- server-events

@Serializable
data class TvInfo(val id: String, val name: String, val app: String)

@Serializable
data class ProfileInfo(
    val id: String,
    val name: String,
    val avatar: Int = 0,
    /** Tijdstempel van de foto, 0 = gebundelde avatar. */
    val photoStamp: Long = 0,
    val hasPin: Boolean = false,
)

@Serializable
data class ProfileSummary(
    val activeId: String,
    val profiles: List<ProfileInfo> = emptyList(),
)

@Serializable
data class ReceiverSummary(
    val id: String,
    val name: String,
    val online: Boolean = false,
    val mode: String = "own_tuner",
)

@Serializable
data class ScreenState(
    /** `loading`, `setup`, `connection`, `dashboard`, `player`, `settings`, ... */
    val app: String,
    val section: String? = null,
    val dialog: String? = null,
)

@Serializable
data class HelloPayload(
    val tv: TvInfo,
    val api: String = "v1",
    val capabilities: List<String> = emptyList(),
    val screen: ScreenState? = null,
    val profile: ProfileSummary? = null,
    val receiver: ReceiverSummary? = null,
    val settingsRevision: Long = 0,
)

@Serializable
data class AckPayload(
    val ok: Boolean = true,
    val latencyMs: Long? = null,
    val error: ProtocolError? = null,
)

@Serializable
data class ProtocolError(
    val code: String,
    val message: String,
    val retryable: Boolean = false,
)

@Serializable
data class ChannelInfo(
    val ref: String,
    val name: String,
    val number: Int = 0,
    val picon: String = "",
)

@Serializable
data class NowInfo(
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val progress: Float = 0f,
    /** Het volgende programma, voor de statuskaart op de telefoon. */
    val nextTitle: String? = null,
    val nextStartMs: Long? = null,
)

@Serializable
data class PlayerInfo(
    val playing: Boolean = false,
    val paused: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val bufferedMs: Long = 0,
)

@Serializable
data class TunerInfo(
    val used: Int = 0,
    val total: Int = 0,
    val status: String = "IDLE",
)

@Serializable
data class StatePlayerPayload(
    /** `enigma2`, `iptv`, `torrent` of `idle`. */
    val source: String,
    val channel: ChannelInfo? = null,
    val now: NowInfo? = null,
    val player: PlayerInfo = PlayerInfo(),
    val tuner: TunerInfo = TunerInfo(),
    /** `home`, `remote` of `tunnel`. */
    val route: String = "home",
)

@Serializable
data class StateScreenPayload(val screen: ScreenState)

@Serializable
data class StateProfilesPayload(val profile: ProfileSummary)

/** Eén gedeelde IPTV-favoriet of -voortgang tussen TV en telefoon. */
@Serializable
data class LibraryEntry(
    val title: String,
    val url: String,
    /** Xtream-accountkey, `torrent` of een ander bron-id. */
    val source: String,
    /** `m` (film), `s` (serie) of `e` (aflevering). */
    val kind: String,
    val id: String,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val favorite: Boolean = false,
    val updatedMs: Long = 0,
    val extension: String = "",
)

@Serializable
data class LibraryStatePayload(
    val favorites: List<LibraryEntry> = emptyList(),
    val resume: List<LibraryEntry> = emptyList(),
    val recordings: List<RecordingEntry> = emptyList(),
)

/** Een opname die op de receiver staat; [url] speelt hem rechtstreeks af. */
@Serializable
data class RecordingEntry(
    val title: String,
    val channel: String = "",
    val url: String,
    val startMs: Long = 0,
    val durationMs: Long = 0,
    /** Basic-auth header when the receiver needs a login, else blank. */
    val authorization: String = "",
)

object LibraryAction {
    const val FAVORITE = "FAVORITE"
    const val PROGRESS = "PROGRESS"
}

@Serializable
data class LibraryCommand(
    val action: String,
    val entry: LibraryEntry,
)

/** Eén recente Enigma2-zender van de tv. */
@Serializable
data class RecentChannel(
    val reference: String,
    val name: String,
    val bouquet: String = "",
)

@Serializable
data class RecentStatePayload(
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverHost: String = "",
    val channels: List<RecentChannel> = emptyList(),
)

/** De telefoon meldt een zender die hij op dezelfde ontvanger bekeek. */
@Serializable
data class RecentCommand(
    val receiverHost: String,
    val reference: String,
    val name: String,
    val bouquet: String = "",
)

/** De IPTV-bron die de TV uit de sync-server heeft (of lokaal heeft opgeslagen). */
@Serializable
data class IptvSourcePayload(
    val configured: Boolean = false,
    /** `M3U` of `XTREAM`. */
    val kind: String = "",
    val m3uUrl: String = "",
    val server: String = "",
    val username: String = "",
    val password: String = "",
    val updatedMs: Long = 0,
    val fromSyncServer: Boolean = false,
)

@Serializable
data class StateSettingsPayload(val revision: Long)

@Serializable
data class ToastPayload(
    /** `info`, `warn` of `error`. */
    val level: String = "info",
    val message: String,
)

// ---------------------------------------------------------------- koppelen (HTTP)

@Serializable
data class HealthResponse(
    val ok: Boolean = true,
    val service: String = "enigmacast-companion",
    val api: String = "v1",
    val tvId: String,
    val name: String,
    val pairing: Boolean = false,
)

@Serializable
data class PairRequest(
    val code: String,
    val name: String,
    val platform: String = "android",
    /** Base64 van de SPKI van de ECDSA P-256-sleutel van de telefoon. */
    val publicKey: String,
    val nonce: String,
)

@Serializable
data class PairResponse(
    val deviceId: String,
    val token: String,
    val role: String = "member",
    val tv: TvInfo,
    val api: String = "v1",
    val capabilities: List<String> = emptyList(),
)

/** Foutcodes die beide kanten kennen. */
object ErrorCode {
    const val PAIR_CODE_INVALID = "PAIR_CODE_INVALID"
    const val PAIR_RATE_LIMITED = "PAIR_RATE_LIMITED"
    const val AUTH_MISSING = "AUTH_MISSING"
    const val AUTH_REPLAY = "AUTH_REPLAY"
    const val AUTH_SKEW = "AUTH_SKEW"
    const val AUTH_SIGNATURE = "AUTH_SIGNATURE"
    const val DEVICE_REVOKED = "DEVICE_REVOKED"
    const val APP_NOT_FOREGROUND = "APP_NOT_FOREGROUND"
    const val SETTINGS_CONFLICT = "SETTINGS_CONFLICT"
    const val SETTINGS_LOCKED = "SETTINGS_LOCKED"
    const val VALIDATION = "VALIDATION"
    const val PAYLOAD_TOO_LARGE = "PAYLOAD_TOO_LARGE"
    const val NOT_FOUND = "NOT_FOUND"
    const val INTERNAL = "INTERNAL"
}
