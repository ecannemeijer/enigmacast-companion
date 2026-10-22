package com.enigmacast.util

/**
 * Log facade voor de gedeelde code. De JVM-module kent geen Timber/`android.util.Log`;
 * de app (TV of telefoon) zet [sink] op zijn eigen logger. Standaard wordt niets gelogd.
 *
 * Nooit geheimen loggen: gebruik [sanitizeUrl] voor URL's en log geen wachtwoorden,
 * tokens of formulierwaarden.
 */
object EnigmaLog {
    /** (prioriteit, tag, bericht) — prioriteit is "I" of "W". */
    @Volatile
    var sink: (priority: String, tag: String, message: String) -> Unit = { _, _, _ -> }

    private fun info(tag: String, message: String) = sink("I", tag, message)
    private fun warn(tag: String, message: String) = sink("W", tag, message)

    fun network(message: String) = info("EnigmaNetwork", message)
    fun networkError(message: String) = warn("EnigmaNetwork", message)
    fun remote(message: String) = info("EnigmaRemote", message)
    fun osd(message: String) = info("EnigmaOsd", message)
    fun subs(message: String) = info("EcSubs", message)
    fun player(message: String) = info("EnigmaPlayer", message)
    fun service(message: String) = info("EnigmaService", message)
    fun connection(message: String) = info("EnigmaConnection", message)
    fun osdInfo(message: String) = info("EnigmaOsdInfo", message)
    fun discovery(message: String) = info("EnigmaDiscovery", message)
    fun discoveryError(message: String) = warn("EnigmaDiscovery", message)

    /** [url] met `user:password@` en wachtwoordachtige querywaarden gemaskeerd. */
    fun sanitizeUrl(url: String): String {
        return url.replace(Regex("://[^/@\\s]+@"), "://***@")
            .replace(ENCODED_USERINFO, "$1***$2")
            .replace(Regex("(?i)([?&](?:password|pass|pwd|token)=)[^&#\\s]*"), "$1***")
    }

    private val ENCODED_USERINFO = Regex(
        "(?i)(%(?:25)?3a(?:%(?:25)?2f|/){2})(?:(?!%(?:25)?2f)[^/\\s&?#@])+?(@|%(?:25)?40)",
    )
}
