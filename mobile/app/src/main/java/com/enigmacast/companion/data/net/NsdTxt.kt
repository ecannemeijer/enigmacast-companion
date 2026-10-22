package com.enigmacast.companion.data.net

/** TXT-record van de TV-brug, zoals de TV het publiceert. */
data class NsdTxtData(
    val id: String = "",
    val name: String = "",
    val pin: String = "",
    val app: String = "",
)

/** Pure parser, zodat de TXT-vorm los van Android getest kan worden. */
object NsdTxt {
    fun parse(attributes: Map<String, ByteArray>): NsdTxtData {
        fun read(key: String): String =
            attributes[key]?.toString(Charsets.UTF_8)?.trim().orEmpty()

        return NsdTxtData(
            id = read("id"),
            name = read("name"),
            pin = read("fp").ifBlank { read("pin") },
            app = read("app"),
        )
    }
}
