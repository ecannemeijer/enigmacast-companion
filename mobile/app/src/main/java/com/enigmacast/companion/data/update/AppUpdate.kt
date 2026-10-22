package com.enigmacast.companion.data.update

import kotlinx.serialization.Serializable

const val UPDATE_MANIFEST_URL = "https://update.velixatv.com/enigmacast-v2.json"
const val UPDATE_FALLBACK_MANIFEST_URL = "https://update.velixatv.com/enigmacast-companion.json"
const val UPDATE_APK_URL = "https://update.velixatv.com/enigmacast-companion.apk"

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
