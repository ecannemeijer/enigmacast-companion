package com.enigmacast.companion.data.live

/** Eén live-zender in de zenderlijst van de fullscreen-speler. */
data class PlayerChannel(
    val id: String,
    val title: String,
    val url: String,
    val logo: String = "",
    val headers: Map<String, String> = emptyMap(),
)
