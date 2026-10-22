package com.enigmacast.util

/**
 * Message-id's die in de gedeelde code worden gebruikt in plaats van `R.string.*`.
 *
 * De Android-app beeldt deze ids af op zijn eigen stringresources (één `when`-tabel).
 * Nieuwe ids krijgen een nieuw, nooit hergebruikt nummer.
 */
object CoreStrings {
    const val VALID_NAME_REQUIRED = 1001
    const val VALID_HOST = 1002
    const val VALID_HTTP_PORT = 1003
    const val VALID_STREAM_PORT = 1004
    const val VALID_TIMEOUT = 1005
    const val VALID_REMOTE_HOST = 1006
    const val VALID_REMOTE_PORT = 1007
    const val VALID_TRANSCODING_PORT = 1008
    const val VALID_WIREGUARD = 1009

    const val ERROR_SLEEP_UNSUPPORTED = 1020
    const val ERROR_SLEEP_FAILED = 1021
}
