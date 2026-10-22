package com.enigmacast.companion.data

import com.enigmacast.companion.protocol.Envelope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * De verbinding met de TV. De echte implementatie (fase 2, TV-brug) gebruikt WSS + TLS;
 * [MockBridge] spreekt hetzelfde protocol in-process, zodat de app nu al werkt.
 */
interface CompanionConnection {
    val link: StateFlow<LinkStatus>

    /** Ruwe protocolberichten (`hello`, `ack`, `state.*`, `toast`, `error`). */
    val events: SharedFlow<Envelope>

    fun connect(tv: PairedTv)

    fun disconnect()

    fun send(envelope: Envelope)
}
