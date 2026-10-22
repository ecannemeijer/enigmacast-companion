package com.enigmacast.companion.protocol

/**
 * Version 1 van het Companion-protocol.
 *
 * Alle waarden hier zijn onderdeel van het contract: wijzig ze niet zonder [VERSION] te
 * verhogen en `protocol/v1` bij te werken. De TV-brug en de telefoon delen deze module.
 */
object Protocol {
    const val VERSION = 1

    /** Eén TLS-poort voor HTTPS en WSS. */
    const val DEFAULT_PORT = 8750

    /** mDNS-servicetype waarmee de TV zich aankondigt. */
    const val SERVICE_TYPE = "_enigmacast._tcp."

    /** Pad van de WebSocket-upgrade. */
    const val WS_PATH = "/ws"

    object Path {
        const val HEALTH = "/api/v1/health"
        const val PAIR = "/api/v1/pair"
        const val INFO = "/api/v1/info"
        const val STATE = "/api/v1/state"
        const val PROFILES = "/api/v1/profiles"
        const val CATALOG_SETTINGS = "/api/v1/catalog/settings"
        const val SETTINGS = "/api/v1/settings"
        const val RECEIVERS = "/api/v1/receivers"
        const val FORMS = "/api/v1/forms"
        const val LIBRARY = "/api/v1/library"
        const val COMMANDS = "/api/v1/commands"
        const val DEVICES = "/api/v1/devices"
    }

    /** Limieten die beide kanten handhaven. */
    object Limits {
        const val PHOTO_MAX_BYTES = 10 * 1024 * 1024
        const val PHOTO_MAX_PIXELS = 50_000_000L
        const val PHOTO_OUTPUT_PX = 512
        const val WS_FRAME_MAX_BYTES = 64 * 1024
        const val SETTINGS_PATCH_MAX_BYTES = 256 * 1024
        const val PAIR_CODE_TTL_MS = 120_000L
        const val PAIR_MAX_ATTEMPTS_PER_WINDOW = 5
        const val PAIR_WINDOW_MS = 5 * 60_000L
        const val AUTH_CLOCK_SKEW_MS = 120_000L
        const val TOKEN_TTL_MS = 90L * 24 * 60 * 60 * 1000
    }
}

/** WS-berichttypen. Onbekende typen worden genegeerd (forward-compatible). */
object MessageType {
    // client → server
    const val CMD_KEY = "cmd.key"
    const val CMD_NAV = "cmd.nav"
    const val CMD_ZAP = "cmd.zap"
    const val CMD_VOLUME = "cmd.volume"
    const val CMD_PLAYER = "cmd.player"
    const val CMD_FORM = "cmd.form"
    const val CMD_SETTINGS = "cmd.settings"
    const val CMD_PROFILE = "cmd.profile"
    const val CMD_LIBRARY = "cmd.library"
    const val CMD_RECENT = "cmd.recent"
    const val CMD_POWER = "cmd.power"
    const val CMD_SYNC = "cmd.sync"
    const val PING = "ping"

    // server → client
    const val HELLO = "hello"
    const val ACK = "ack"
    const val PONG = "pong"
    const val STATE_PLAYER = "state.player"
    const val STATE_SCREEN = "state.screen"
    const val STATE_SETTINGS = "state.settings"
    const val STATE_PROFILES = "state.profiles"
    const val STATE_LIBRARY = "state.library"
    const val STATE_RECENT = "state.recent"
    const val STATE_IPTV = "state.iptv"
    const val STATE_TUNER = "state.tuner"
    const val FORM = "form"
    const val TOAST = "toast"
    const val ERROR = "error"
}

/** Capaciteiten die de TV in [MessageType.HELLO] meldt; de telefoon verbergt wat ontbreekt. */
object Capability {
    const val KEY = "key"
    const val NAV = "nav"
    const val ZAP = "zap"
    const val VOLUME = "volume"
    const val TEXT_FORM = "text.form"
    const val SETTINGS = "settings"
    const val PROFILES = "profiles"
    const val PHOTO = "photo"
    const val LIBRARY = "library"
    const val IPTV = "iptv"
    const val STATE = "state"
    const val POWER = "power"
}
