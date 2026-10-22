package com.enigmacast.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Gebruikersentiteiten die de gedeelde sync- en bibliotheeklogica nodig heeft.
 *
 * Deze drie klassen komen uit `UserDatabase.kt` in EnigmaCast. Ze staan hier zonder DAO's:
 * de telefoon bouwt zijn eigen `UserDatabase` bovenop dezelfde entiteiten, zodat
 * `LibrarySync` en `IptvLibraryRows` ongewijzigd blijven werken.
 */

/** An IPTV film ([kind] "m") or series ("s") in Library › IPTV favorites. [source] is the playlist account key. */
@Entity(tableName = "iptv_library", primaryKeys = ["source", "kind", "id", "profileId"])
data class IptvLibraryRow(
    val source: String,
    val kind: String,
    val id: String,
    val title: String,
    val poster: String?,
    val backdrop: String?,
    val year: Int?,
    val categoryId: String,
    val extension: String,
    val url: String,
    val addedMs: Long,
    /** Viewer who owns this favorite. Legacy rows are the original profile. */
    val profileId: String = com.enigmacast.domain.profile.ProfileState.LEGACY_UUID,
)

/** Where the viewer stopped in an IPTV film ("m") or episode ("e"), for Continue watching. */
@Entity(tableName = "iptv_resume", primaryKeys = ["source", "kind", "id", "profileId"])
data class IptvResumeRow(
    val source: String,
    val kind: String,
    val id: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedMs: Long,
    val title: String,
    val poster: String?,
    val extension: String,
    val url: String,
    val categoryId: String,
    val seriesId: String?,
    val seriesTitle: String?,
    val season: Int,
    val episode: Int,
    val profileId: String = com.enigmacast.domain.profile.ProfileState.LEGACY_UUID,
)

/** A local change waiting for the EnigmaCast sync server. [op] is "upsert" or "delete". */
@Entity(tableName = "sync_outbox")
data class SyncOutboxRow(
    @PrimaryKey val changeId: String,
    val profileId: String,
    val entity: String,
    val op: String,
    val payload: String,
    val updatedMs: Long,
    val deviceId: String,
)

/** Last pulled cursor per profile. The server's cursor is opaque. */
@Entity(tableName = "sync_cursor")
data class SyncCursorRow(
    @PrimaryKey val profileId: String,
    val cursor: String,
)
