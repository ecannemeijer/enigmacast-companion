package com.enigmacast.companion.data.update

import kotlinx.serialization.Serializable

/** GitHub Releases: "latest" always points at the newest Companion release. */
const val COMPANION_RELEASES = "https://github.com/ecannemeijer/enigmacast-companion/releases/latest/download"
const val UPDATE_MANIFEST_URL = "$COMPANION_RELEASES/enigmacast-companion.json"
const val UPDATE_APK_URL = "$COMPANION_RELEASES/enigmacast-companion.apk"

/** The TV app's manifest also names a Companion release (`companion`); used when the one above fails. */
const val UPDATE_FALLBACK_MANIFEST_URL = "https://github.com/ecannemeijer/enigmacast/releases/latest/download/enigmacast-v2.json"

/** How often a running Companion looks for an update again (besides each start). */
const val UPDATE_CHECK_INTERVAL_MS = 6L * 60 * 60 * 1000

/** De Companion-release in het gecombineerde TV-manifest of in enigmacast-companion.json. */
@Serializable
data class RemoteRelease(
    val versionCode: Int = 0,
    val versionName: String = "",
    val apkUrl: String = UPDATE_APK_URL,
    val notes: String = "",
    val reinstallRequired: Boolean = false,
    val reinstallMessage: String = "",
    val reinstallMessageNl: String = "",
)

/** Het TV-manifest heeft de Companion onder `companion`; de TV-velden staan bovenaan. */
@Serializable
data class UpdateManifest(
    val companion: RemoteRelease? = null,
)

fun isNewerRelease(localVersionCode: Int, remote: RemoteRelease): Boolean =
    remote.versionCode > localVersionCode

/** The newest of the releases that could be read (Companion repo and TV manifest). */
fun newestRelease(vararg releases: RemoteRelease?): RemoteRelease? =
    releases.filterNotNull().filter { it.versionCode > 0 }.maxByOrNull { it.versionCode }

/** A check is due on start ([lastCheckMs] 0) and then every [UPDATE_CHECK_INTERVAL_MS]. */
fun updateCheckDue(lastCheckMs: Long, nowMs: Long): Boolean =
    lastCheckMs <= 0L || nowMs - lastCheckMs >= UPDATE_CHECK_INTERVAL_MS
