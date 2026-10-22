package com.enigmacast.companion.ui

enum class AppScreen {
    WELCOME,
    SCAN,
    PAIR,
    DEVICES,
    REMOTE,
    LIVE,
    LIVE_PLAYER,
    IPTV,
    XTREAM,
    LIBRARY,
    SETTINGS,
    PROFILES,
    PLAYER,
    ;

    /** Schermen met een tab in de onderbalk. */
    val isTab: Boolean
        get() = this == REMOTE || this == LIVE || this == IPTV || this == LIBRARY || this == SETTINGS
}
