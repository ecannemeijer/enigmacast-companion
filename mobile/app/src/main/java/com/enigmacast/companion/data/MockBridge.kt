package com.enigmacast.companion.data

import com.enigmacast.companion.protocol.AckPayload
import com.enigmacast.companion.protocol.Capability
import com.enigmacast.companion.protocol.ChannelInfo
import com.enigmacast.companion.protocol.Envelope
import com.enigmacast.companion.protocol.ErrorCode
import com.enigmacast.companion.protocol.HelloPayload
import com.enigmacast.companion.protocol.KeyCommand
import com.enigmacast.companion.protocol.KeyPhase
import com.enigmacast.companion.protocol.MessageType
import com.enigmacast.companion.protocol.NavCommand
import com.enigmacast.companion.protocol.NowInfo
import com.enigmacast.companion.protocol.PlayerCommand
import com.enigmacast.companion.protocol.PlayerCommandAction
import com.enigmacast.companion.protocol.PlayerInfo
import com.enigmacast.companion.protocol.PowerCommand
import com.enigmacast.companion.protocol.ProfileCommand
import com.enigmacast.companion.protocol.ProfileCommandAction
import com.enigmacast.companion.protocol.ProfileInfo
import com.enigmacast.companion.protocol.ProfileSummary
import com.enigmacast.companion.protocol.Protocol
import com.enigmacast.companion.protocol.ProtocolError
import com.enigmacast.companion.protocol.ProtocolJson
import com.enigmacast.companion.protocol.ReceiverSummary
import com.enigmacast.companion.protocol.RemoteKey
import com.enigmacast.companion.protocol.ScreenState
import com.enigmacast.companion.protocol.StatePlayerPayload
import com.enigmacast.companion.protocol.StateProfilesPayload
import com.enigmacast.companion.protocol.ToastPayload
import com.enigmacast.companion.protocol.TunerInfo
import com.enigmacast.companion.protocol.TvInfo
import com.enigmacast.companion.protocol.VolumeCommand
import com.enigmacast.companion.protocol.VolumeTarget
import com.enigmacast.companion.protocol.ZapCommand
import kotlin.random.Random
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Een in-process namaak-TV die het **echte** protocol spreekt: de app bouwt en leest
 * dezelfde [Envelope]s, [KeyCommand]s en [StatePlayerPayload]s als straks tegen de brug.
 *
 * Zo kan de afstandsbediening, de statuskaart en de reconnect-logica nu al getest worden
 * zonder de EnigmaCast TV-app te wijzigen. Fase 2 vervangt deze klasse door een
 * WebSocket-client met dezelfde [CompanionConnection]-interface.
 */
class MockBridge : CompanionConnection {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _link = MutableStateFlow(LinkStatus())
    override val link: StateFlow<LinkStatus> = _link.asStateFlow()

    private val _events = MutableSharedFlow<Envelope>(extraBufferCapacity = 64)
    override val events: SharedFlow<Envelope> = _events.asSharedFlow()

    private val mutex = Mutex()
    private var tv: PairedTv? = null
    private var generation = 0
    private var seq = 0L
    private var channelIndex = 0
    private var paused = false
    private var positionMs = 0L
    private var volume = 30
    private var muted = false
    private var activeProfileId = DEFAULT_PROFILE
    private var receiverMode = "own_tuner"
    private var pendingDigits = ""
    private var digitJob: Job? = null
    private var ticker: Job? = null
    private var focusIndex = 0
    private val programmeAnchor = System.currentTimeMillis() - PROGRAMME_ELAPSED_MS

    private val profiles = listOf(
        ProfileInfo(id = DEFAULT_PROFILE, name = "Thuis", avatar = 3),
        ProfileInfo(id = "kids", name = "Kinderen", avatar = 7),
    )

    private val channels = listOf(
        MockChannel(1, "NPO 1 HD", "1:0:19:4B6F:2E:19:C00000:0:0:0:", "NOS Journaal", "EenVandaag"),
        MockChannel(2, "NPO 2", "1:0:19:4B70:2E:19:C00000:0:0:0:", "De Avondshow", "Nieuwsuur"),
        MockChannel(4, "RTL 4", "1:0:19:4B71:2E:19:C00000:0:0:0:", "RTL Nieuws", "GTST"),
        MockChannel(5, "Net5", "1:0:19:4B72:2E:19:C00000:0:0:0:", "Huis Gemaakt", "BinnensteBuiten"),
        MockChannel(6, "SBS6", "1:0:19:4B73:2E:19:C00000:0:0:0:", "Hart van Nederland", "Shownieuws"),
        MockChannel(7, "RTL 7", "1:0:19:4B74:2E:19:C00000:0:0:0:", "Voetbal Inside", "RTL Z Nieuws"),
        MockChannel(8, "Veronica", "1:0:19:4B75:2E:19:C00000:0:0:0:", "De Verraders", "Wie is de Mol?"),
    )

    override fun connect(tv: PairedTv) {
        generation += 1
        val current = generation
        this.tv = tv
        ticker?.cancel()
        digitJob?.cancel()
        pendingDigits = ""
        _link.value = LinkStatus(LinkState.CONNECTING, tv.name)
        scope.launch {
            delay(CONNECT_DELAY_MS)
            if (current != generation) return@launch
            if (!tv.online) {
                _link.value = LinkStatus(LinkState.ERROR, tv.name, message = "TV niet bereikbaar")
                return@launch
            }
            seq = 0
            _link.value = LinkStatus(LinkState.READY, tv.name)
            emitHello()
            emitState()
            startTicker(current)
        }
    }

    override fun disconnect() {
        generation += 1
        ticker?.cancel()
        digitJob?.cancel()
        tv = null
        _link.value = LinkStatus(LinkState.IDLE)
    }

    override fun send(envelope: Envelope) {
        val started = System.nanoTime()
        scope.launch {
            mutex.withLock {
                if (envelope.type != MessageType.PING && _link.value.state != LinkState.READY) {
                    emitError(envelope.id, ErrorCode.APP_NOT_FOREGROUND, "De TV-app is niet verbonden")
                    return@withLock
                }
                delay(Random.nextLong(8, 24))
                handle(envelope)
                val latency = (System.nanoTime() - started) / 1_000_000
                val id = envelope.id
                if (id != null) emitAck(id, latency)
            }
        }
    }

    /** Demo: de TV valt weg en komt na twee seconden terug. */
    fun simulateDrop() {
        val current = tv ?: return
        generation += 1
        val dropped = generation
        ticker?.cancel()
        _link.value = LinkStatus(LinkState.OFFLINE, current.name, message = "Verbinding verbroken")
        scope.launch {
            delay(RECONNECT_DELAY_MS)
            if (dropped != generation) return@launch
            connect(current)
        }
    }

    private suspend fun handle(envelope: Envelope) {
        when (envelope.type) {
            MessageType.PING -> emit(ProtocolJson.envelope(MessageType.PONG, Unit))
            MessageType.CMD_KEY -> handleKey(ProtocolJson.decodePayload<KeyCommand>(envelope))
            MessageType.CMD_NAV -> {
                val nav = ProtocolJson.decodePayload<NavCommand>(envelope)
                emitToast("Ga naar ${nav.target.name.lowercase().replace('_', ' ')}")
            }
            MessageType.CMD_ZAP -> {
                val zap = ProtocolJson.decodePayload<ZapCommand>(envelope)
                val target = zap.serviceRef?.let { ref -> channels.indexOfFirst { it.ref == ref } } ?: -1
                val byNumber = zap.number?.let { number -> channels.indexOfFirst { it.number == number } } ?: -1
                when {
                    target >= 0 -> selectChannel(target)
                    byNumber >= 0 -> selectChannel(byNumber)
                    else -> emitToast("Zender niet gevonden", level = "warn")
                }
            }
            MessageType.CMD_VOLUME -> handleVolume(ProtocolJson.decodePayload<VolumeCommand>(envelope))
            MessageType.CMD_PLAYER -> handlePlayer(ProtocolJson.decodePayload<PlayerCommand>(envelope))
            MessageType.CMD_PROFILE -> handleProfile(ProtocolJson.decodePayload<ProfileCommand>(envelope))
            MessageType.CMD_POWER -> {
                val power = ProtocolJson.decodePayload<PowerCommand>(envelope)
                emitToast("Ontvanger: ${power.action.name.lowercase().replace('_', ' ')}")
            }
            MessageType.CMD_SYNC -> emitToast("Synchronisatie gestart")
            else -> emitError(envelope.id, ErrorCode.NOT_FOUND, "Onbekend commando: ${envelope.type}")
        }
    }

    private suspend fun handleKey(command: KeyCommand) {
        // Een UP is alleen de afsluiting van een press; de actie gebeurt op DOWN.
        if (command.phase == KeyPhase.UP) return
        when (command.key) {
            RemoteKey.CHANNEL_UP, RemoteKey.NEXT -> selectChannel(channelIndex + 1)
            RemoteKey.CHANNEL_DOWN, RemoteKey.PREVIOUS -> selectChannel(channelIndex - 1)
            RemoteKey.PLAY_PAUSE -> {
                paused = !paused
                emitState()
                emitToast(if (paused) "Gepauzeerd" else "Speelt weer")
            }
            RemoteKey.STOP -> {
                paused = true
                positionMs = 0
                emitState()
            }
            RemoteKey.FAST_FORWARD -> emitToast("+30 seconden")
            RemoteKey.REWIND -> emitToast("−30 seconden")
            RemoteKey.VOLUME_UP -> adjustVolume(5)
            RemoteKey.VOLUME_DOWN -> adjustVolume(-5)
            RemoteKey.MUTE -> {
                muted = !muted
                emitToast(if (muted) "Geluid uit" else "Geluid aan")
            }
            RemoteKey.HOME -> emitToast("Home van EnigmaCast")
            RemoteKey.MENU -> emitToast("Menu")
            RemoteKey.BACK -> emitToast("Terug")
            RemoteKey.OK -> {
                if (pendingDigits.isNotEmpty()) applyDigits() else emitToast("OK")
            }
            RemoteKey.INFO, RemoteKey.GUIDE -> emitToast("Programma-informatie")
            RemoteKey.RED, RemoteKey.GREEN, RemoteKey.YELLOW, RemoteKey.BLUE ->
                emitToast("Kleurtoets ${command.key.name.lowercase()}")
            RemoteKey.DPAD_UP, RemoteKey.DPAD_DOWN, RemoteKey.DPAD_LEFT, RemoteKey.DPAD_RIGHT -> {
                focusIndex = (focusIndex + when (command.key) {
                    RemoteKey.DPAD_DOWN, RemoteKey.DPAD_RIGHT -> 1
                    else -> -1
                }).coerceIn(0, 7)
                emitState()
            }
            else -> {
                val digit = command.key.name.removePrefix("DIGIT_").toIntOrNull()
                if (digit != null) appendDigit(digit) else emitToast(command.key.name.lowercase())
            }
        }
    }

    private suspend fun handleVolume(command: VolumeCommand) {
        if (command.target == VolumeTarget.PHONE) {
            emitToast("Telefoonvolume")
            return
        }
        command.mute?.let { muted = it }
        if (command.delta != 0) volume = (volume + command.delta * 5).coerceIn(0, 100)
        emitState()
        emitToast(if (muted) "Geluid uit" else "Volume $volume")
    }

    private suspend fun handlePlayer(command: PlayerCommand) {
        when (command.action) {
            PlayerCommandAction.PLAY -> paused = false
            PlayerCommandAction.PAUSE -> paused = true
            PlayerCommandAction.STOP -> { paused = true; positionMs = 0 }
            PlayerCommandAction.SEEK -> positionMs = command.positionMs ?: positionMs
            PlayerCommandAction.SPEED -> emitToast("Snelheid ${command.speed ?: 1f}×")
        }
        emitState()
    }

    private suspend fun handleProfile(command: ProfileCommand) {
        when (command.action) {
            ProfileCommandAction.SELECT -> {
                val id = command.uuid ?: return
                if (profiles.none { it.id == id }) {
                    emitToast("Profiel niet gevonden", level = "warn")
                    return
                }
                activeProfileId = id
                emit(ProtocolJson.envelope(MessageType.STATE_PROFILES, StateProfilesPayload(ProfileSummary(activeProfileId, profiles))))
                emitToast("Profiel: ${profiles.first { it.id == id }.name}")
            }
            else -> emitToast("Profielactie ${command.action.name.lowercase()}")
        }
    }

    private suspend fun appendDigit(digit: Int) {
        if (pendingDigits.length >= 3) pendingDigits = ""
        pendingDigits += digit
        emitToast("Zender ${pendingDigits}…")
        digitJob?.cancel()
        digitJob = scope.launch {
            delay(DIGIT_TIMEOUT_MS)
            mutex.withLock { applyDigits() }
        }
    }

    private suspend fun applyDigits() {
        val number = pendingDigits.toIntOrNull()
        pendingDigits = ""
        if (number == null) return
        val index = channels.indexOfFirst { it.number == number }
        if (index >= 0) selectChannel(index) else emitToast("Zender $number niet gevonden", level = "warn")
    }

    private suspend fun selectChannel(index: Int) {
        channelIndex = ((index % channels.size) + channels.size) % channels.size
        paused = false
        positionMs = 0
        emitState()
        emitToast("${channels[channelIndex].number}  ${channels[channelIndex].name}")
    }

    private suspend fun adjustVolume(delta: Int) {
        volume = (volume + delta).coerceIn(0, 100)
        muted = false
        emitState()
        emitToast("Volume $volume")
    }

    private fun startTicker(current: Int) {
        ticker?.cancel()
        ticker = scope.launch {
            while (current == generation) {
                delay(TICK_MS)
                if (_link.value.state == LinkState.READY) {
                    mutex.withLock { emitState() }
                }
            }
        }
    }

    private suspend fun emitHello() {
        val paired = tv ?: return
        val payload = HelloPayload(
            tv = TvInfo(paired.id, paired.name, "1.0.0-beta.72"),
            capabilities = listOf(
                Capability.KEY, Capability.NAV, Capability.ZAP, Capability.VOLUME,
                Capability.TEXT_FORM, Capability.SETTINGS, Capability.PROFILES,
                Capability.PHOTO, Capability.STATE, Capability.POWER,
            ),
            screen = ScreenState("dashboard", "home"),
            profile = ProfileSummary(activeProfileId, profiles),
            receiver = ReceiverSummary("sf8008", "SF8008", online = true, mode = receiverMode),
            settingsRevision = 1,
        )
        _events.emit(ProtocolJson.envelope(MessageType.HELLO, payload, seq = 0, ts = System.currentTimeMillis()))
    }

    private suspend fun emitState() {
        val channel = channels[channelIndex]
        val nowStart = programmeAnchor
        val nowEnd = nowStart + PROGRAMME_MS
        val progress = ((System.currentTimeMillis() - nowStart).toFloat() / PROGRAMME_MS).coerceIn(0f, 1f)
        val payload = StatePlayerPayload(
            source = "enigma2",
            channel = ChannelInfo(channel.ref, channel.name, channel.number, channel.picon),
            now = NowInfo(
                title = channel.now,
                startMs = nowStart,
                endMs = nowEnd,
                progress = progress,
                nextTitle = channel.next,
                nextStartMs = nowEnd,
            ),
            player = PlayerInfo(
                playing = true,
                paused = paused,
                positionMs = if (paused) positionMs else 0,
                durationMs = 0,
                bufferedMs = 0,
            ),
            tuner = TunerInfo(used = 1, total = 2, status = if (paused) "PAUSED" else "PLAYING"),
            route = "home",
        )
        _events.emit(ProtocolJson.envelope(MessageType.STATE_PLAYER, payload, seq = ++seq, ts = System.currentTimeMillis()))
    }

    private suspend fun emitAck(id: String, latencyMs: Long) {
        _events.emit(ProtocolJson.envelope(MessageType.ACK, AckPayload(ok = true, latencyMs = latencyMs), id = id))
    }

    private suspend fun emitToast(message: String, level: String = "info") {
        _events.emit(ProtocolJson.envelope(MessageType.TOAST, ToastPayload(level, message)))
    }

    private suspend fun emitError(id: String?, code: String, message: String) {
        _events.emit(
            ProtocolJson.envelope(
                MessageType.ERROR,
                ProtocolError(code = code, message = message),
                id = id,
            ),
        )
    }

    private fun emit(envelope: Envelope) {
        scope.launch { _events.emit(envelope) }
    }

    private data class MockChannel(
        val number: Int,
        val name: String,
        val ref: String,
        val now: String,
        val next: String,
        val picon: String = "",
    )

    private companion object {
        const val DEFAULT_PROFILE = "default"
        const val CONNECT_DELAY_MS = 450L
        const val RECONNECT_DELAY_MS = 2_200L
        const val DIGIT_TIMEOUT_MS = 1_200L
        const val TICK_MS = 5_000L
        const val PROGRAMME_MS = 30 * 60_000L
        const val PROGRAMME_ELAPSED_MS = 12 * 60_000L
    }
}
