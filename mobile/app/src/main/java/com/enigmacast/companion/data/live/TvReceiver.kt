package com.enigmacast.companion.data.live

import com.enigmacast.companion.protocol.ReceiverStatePayload
import com.enigmacast.domain.model.ReceiverConfig

/** The TV's active receiver (or its first one) as a Live configuration; null when it has none. */
fun tvReceiverConfig(payload: ReceiverStatePayload): ReceiverConfig? {
    val shared = payload.receivers.firstOrNull { it.id == payload.activeId && it.host.isNotBlank() }
        ?: payload.receivers.firstOrNull { it.host.isNotBlank() }
        ?: return null
    return ReceiverConfig(
        id = shared.id,
        name = shared.name,
        host = shared.host,
        httpPort = shared.httpPort,
        useHttps = shared.useHttps,
        username = shared.username,
        password = shared.password,
        streamingPort = shared.streamingPort,
        allowInsecureCertificates = shared.allowInsecureCertificates,
        timeoutSeconds = shared.timeoutSeconds,
    )
}

/** Changes when the TV's receiver changes; the password only enters as a hash. */
fun tvReceiverStamp(config: ReceiverConfig): String = listOf(
    config.id, config.name, config.host, config.httpPort, config.useHttps, config.username,
    config.password.hashCode(), config.streamingPort, config.allowInsecureCertificates,
).joinToString("|")
