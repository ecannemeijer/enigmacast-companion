package com.enigmacast.companion.data.net

import android.util.Base64
import com.enigmacast.companion.data.CompanionConnection
import com.enigmacast.companion.data.LinkState
import com.enigmacast.companion.data.LinkStatus
import com.enigmacast.companion.data.PairedTv
import com.enigmacast.companion.protocol.AuthHeaders
import com.enigmacast.companion.protocol.AuthSignature
import com.enigmacast.companion.protocol.Envelope
import com.enigmacast.companion.protocol.MessageType
import com.enigmacast.companion.protocol.ProtocolJson
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID
import javax.net.ssl.SSLException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * De echte TV-verbinding: WSS met SPKI-pinning, een getekende upgrade en automatische
 * reconnect met backoff. De UI ziet dezelfde [CompanionConnection] als de mock.
 */
class WebSocketConnection(
    private val identity: DeviceIdentity,
    /** Finds the TV again after DHCP changed its address; null keeps retrying the old host. */
    private val resolve: (suspend (PairedTv) -> PairedTv?)? = null,
) : CompanionConnection {

    /** Called when a reconnect discovered a new IP for the same TV. */
    var onHostChanged: ((PairedTv) -> Unit)? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _link = MutableStateFlow(LinkStatus())
    override val link: StateFlow<LinkStatus> = _link.asStateFlow()

    private val _events = MutableSharedFlow<Envelope>(extraBufferCapacity = 64)
    override val events: SharedFlow<Envelope> = _events.asSharedFlow()

    private var tv: PairedTv? = null
    private var webSocket: WebSocket? = null
    private var client: OkHttpClient? = null
    private var reconnectJob: Job? = null
    private var heartbeatJob: Job? = null
    private var helloJob: Job? = null
    private var generation = 0
    private var attempt = 0
    @Volatile private var closedByUser = false
    @Volatile private var lastMessageMs = 0L

    override fun connect(tv: PairedTv) {
        if (tv.token.isBlank() || tv.deviceId.isBlank()) {
            _link.value = LinkStatus(LinkState.ERROR, tv.name, message = "Nog niet gekoppeld")
            return
        }
        closedByUser = false
        this.tv = tv
        attempt = 0
        reconnectJob?.cancel()
        reconnectJob = null
        open(tv)
    }

    override fun disconnect() {
        closedByUser = true
        reconnectJob?.cancel()
        reconnectJob = null
        teardown()
        _link.value = LinkStatus(LinkState.IDLE)
    }

    override fun send(envelope: Envelope) {
        val socket = webSocket
        if (socket == null || !socket.send(ProtocolJson.encodeEnvelope(envelope))) {
            if (!closedByUser) {
                _link.value = LinkStatus(LinkState.OFFLINE, tv?.name, message = "Verbinding verbroken")
                scheduleReconnect()
            }
        }
    }

    // ------------------------------------------------------------ verbinden

    private fun open(tv: PairedTv) {
        teardown()
        val gen = ++generation
        _link.value = LinkStatus(LinkState.CONNECTING, tv.name)

        val timestamp = System.currentTimeMillis().toString()
        val nonce = UUID.randomUUID().toString()
        val signature = Base64.encodeToString(
            identity.sign(AuthSignature.canonical(timestamp, nonce, "GET", "/ws", ByteArray(0))),
            Base64.NO_WRAP,
        )
        val request = Request.Builder()
            .url("wss://${tv.host}:${tv.port}/ws")
            .header("Authorization", "Bearer ${tv.token}")
            .header(AuthHeaders.DEVICE_ID, tv.deviceId)
            .header(AuthHeaders.TIMESTAMP, timestamp)
            .header(AuthHeaders.NONCE, nonce)
            .header(AuthHeaders.SIGNATURE, signature)
            .build()

        val okHttp = PinnedTls(tv.pin, tv.host).client()
        client = okHttp
        webSocket = okHttp.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    if (gen != generation) return
                    lastMessageMs = System.currentTimeMillis()
                    startHeartbeat(gen)
                    helloJob = scope.launch {
                        delay(12_000)
                        if (gen == generation && _link.value.state == LinkState.CONNECTING) {
                            _link.value = LinkStatus(LinkState.ERROR, tv.name, message = "TV antwoordt niet")
                            webSocket.cancel()
                        }
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (gen != generation) return
                    lastMessageMs = System.currentTimeMillis()
                    val envelope = runCatching { ProtocolJson.decodeEnvelope(text) }.getOrNull() ?: return
                    if (envelope.type == MessageType.HELLO) {
                        attempt = 0
                        helloJob?.cancel()
                        _link.value = LinkStatus(LinkState.READY, tv.name)
                    }
                    _events.tryEmit(envelope)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (gen != generation || closedByUser) return
                    onLost(t)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (gen != generation || closedByUser) return
                    onLost(IllegalStateException("Verbinding gesloten ($code)"))
                }
            },
        )
    }

    private fun onLost(error: Throwable) {
        helloJob?.cancel()
        helloJob = null
        // Alleen een echt certificaatprobleem is fataal; een reset tijdens een herstart
        // van de TV moet gewoon opnieuw proberen.
        val fatal = error is java.security.cert.CertificateException ||
            error.cause is java.security.cert.CertificateException
        val state = if (fatal) LinkState.ERROR else LinkState.OFFLINE
        _link.value = LinkStatus(state, tv?.name, message = describe(error))
        if (!fatal) scheduleReconnect()
    }

    private fun scheduleReconnect() {
        if (closedByUser) return
        reconnectJob?.cancel()
        val wait = Backoff.delayMs(attempt)
        attempt = (attempt + 1).coerceAtMost(8)
        reconnectJob = scope.launch {
            delay(wait)
            if (closedByUser) return@launch
            val old = tv ?: return@launch
            val found = runCatching { resolve?.invoke(old) }.getOrNull()
            val target = if (found != null && found.id == old.id) {
                old.copy(host = found.host, port = found.port, pin = found.pin.ifBlank { old.pin })
            } else {
                old
            }
            if (target.host != old.host || target.port != old.port || target.pin != old.pin) {
                tv = target
                onHostChanged?.invoke(target)
            }
            if (!closedByUser) open(target)
        }
    }

    private fun startHeartbeat(gen: Int) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (true) {
                delay(10_000)
                if (gen != generation) break
                val socket = webSocket ?: break
                if (System.currentTimeMillis() - lastMessageMs > 30_000) {
                    socket.cancel()
                    break
                }
                socket.send(
                    ProtocolJson.encodeEnvelope(
                        ProtocolJson.envelopeOf("ping", null, id = "hb-${System.currentTimeMillis()}"),
                    ),
                )
            }
        }
    }

    private fun teardown() {
        generation++
        heartbeatJob?.cancel()
        heartbeatJob = null
        helloJob?.cancel()
        helloJob = null
        webSocket?.cancel()
        webSocket = null
        client?.dispatcher?.executorService?.shutdown()
        client?.connectionPool?.evictAll()
        client = null
    }

    private fun describe(error: Throwable): String = when {
        error is java.security.cert.CertificateException ||
            error.cause is java.security.cert.CertificateException -> "Certificaatpin klopt niet"
        error is SSLException -> "Verbinding verbroken"
        error is ConnectException || error is SocketTimeoutException || error is UnknownHostException ->
            "TV niet bereikbaar"
        else -> error.message ?: "Verbinding verbroken"
    }
}
