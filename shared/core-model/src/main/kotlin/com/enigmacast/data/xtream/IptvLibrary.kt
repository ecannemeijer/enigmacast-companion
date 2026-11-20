package com.enigmacast.data.xtream

/** Resume and favorite key for an IPTV film, series or episode: source/kind/id. */
object IptvLibrary {
    fun key(source: String, kind: String, id: String) = "$source/$kind/$id"
}
