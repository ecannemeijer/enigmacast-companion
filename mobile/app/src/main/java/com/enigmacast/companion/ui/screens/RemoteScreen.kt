package com.enigmacast.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigmacast.companion.data.ControlMode
import com.enigmacast.companion.data.LinkState
import com.enigmacast.companion.data.LinkStatus
import com.enigmacast.companion.data.PairedTv
import com.enigmacast.companion.data.TvSnapshot
import com.enigmacast.companion.protocol.NowInfo
import com.enigmacast.companion.protocol.RemoteKey
import com.enigmacast.companion.ui.AppViewModel
import com.enigmacast.companion.ui.Haptics
import com.enigmacast.companion.ui.components.ChannelRow
import com.enigmacast.companion.ui.components.Direction
import com.enigmacast.companion.ui.components.Dpad
import com.enigmacast.companion.ui.components.GlassCard
import com.enigmacast.companion.ui.components.KeyButton
import com.enigmacast.companion.ui.components.NumberPad
import com.enigmacast.companion.ui.components.Pill
import com.enigmacast.companion.ui.components.SectionLabel
import com.enigmacast.companion.ui.components.StatusDot
import com.enigmacast.companion.ui.components.TransportRow
import com.enigmacast.companion.ui.components.VolumeRow
import com.enigmacast.companion.ui.theme.Ink
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(
    vm: AppViewModel,
    link: LinkStatus,
    haptics: Haptics,
    onOpenSettings: () -> Unit,
    onOpenProfiles: () -> Unit,
) {
    val snapshot = vm.snapshot
    val paired = vm.pairedTv
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        if (landscape) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Header(paired, snapshot, link, vm.lastLatencyMs, onOpenSettings, onOpenProfiles)
                    StatusPanel(snapshot, link, vm.mode) { vm.chooseMode(if (vm.mode == ControlMode.APP) ControlMode.RECEIVER else ControlMode.APP) }
                    ChannelRow(
                        haptics = haptics,
                        number = snapshot.channel?.number,
                        onUp = { vm.tapKey(RemoteKey.CHANNEL_UP) },
                        onDown = { vm.tapKey(RemoteKey.CHANNEL_DOWN) },
                        onNumbers = vm::toggleNumbers,
                    )
                    VolumeRow(
                        haptics = haptics,
                        volume = snapshot.volume,
                        muted = snapshot.muted,
                        onDown = { vm.volume(-1) },
                        onUp = { vm.volume(1) },
                        onMute = vm::toggleMute,
                    )
                }
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Dpad(
                        haptics = haptics,
                        onDown = { vm.pressKey(it.toKey(), com.enigmacast.companion.protocol.KeyPhase.DOWN) },
                        onRepeat = { direction, n -> vm.pressKey(direction.toKey(), com.enigmacast.companion.protocol.KeyPhase.REPEAT, n) },
                        onUp = { vm.pressKey(it.toKey(), com.enigmacast.companion.protocol.KeyPhase.UP) },
                        onOk = { vm.tapKey(RemoteKey.OK) },
                        diameter = 220.dp,
                    )
                    QuickKeys(vm, haptics)
                    TransportRow(
                        haptics = haptics,
                        playing = snapshot.isPlaying,
                        onPrevious = { vm.tapKey(RemoteKey.PREVIOUS) },
                        onRewind = { vm.tapKey(RemoteKey.REWIND) },
                        onPlayPause = { vm.tapKey(RemoteKey.PLAY_PAUSE) },
                        onStop = { vm.tapKey(RemoteKey.STOP) },
                        onForward = { vm.tapKey(RemoteKey.FAST_FORWARD) },
                    )
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Header(paired, snapshot, link, vm.lastLatencyMs, onOpenSettings, onOpenProfiles)
                StatusPanel(snapshot, link, vm.mode) { vm.chooseMode(if (vm.mode == ControlMode.APP) ControlMode.RECEIVER else ControlMode.APP) }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Dpad(
                        haptics = haptics,
                        onDown = { vm.pressKey(it.toKey(), com.enigmacast.companion.protocol.KeyPhase.DOWN) },
                        onRepeat = { direction, n -> vm.pressKey(direction.toKey(), com.enigmacast.companion.protocol.KeyPhase.REPEAT, n) },
                        onUp = { vm.pressKey(it.toKey(), com.enigmacast.companion.protocol.KeyPhase.UP) },
                        onOk = { vm.tapKey(RemoteKey.OK) },
                        diameter = 224.dp,
                    )
                    QuickKeys(vm, haptics, vertical = true)
                }
                TransportRow(
                    haptics = haptics,
                    playing = snapshot.isPlaying,
                    onPrevious = { vm.tapKey(RemoteKey.PREVIOUS) },
                    onRewind = { vm.tapKey(RemoteKey.REWIND) },
                    onPlayPause = { vm.tapKey(RemoteKey.PLAY_PAUSE) },
                    onStop = { vm.tapKey(RemoteKey.STOP) },
                    onForward = { vm.tapKey(RemoteKey.FAST_FORWARD) },
                )
                ChannelRow(
                    haptics = haptics,
                    number = snapshot.channel?.number,
                    onUp = { vm.tapKey(RemoteKey.CHANNEL_UP) },
                    onDown = { vm.tapKey(RemoteKey.CHANNEL_DOWN) },
                    onNumbers = vm::toggleNumbers,
                )
                VolumeRow(
                    haptics = haptics,
                    volume = snapshot.volume,
                    muted = snapshot.muted,
                    onDown = { vm.volume(-1) },
                    onUp = { vm.volume(1) },
                    onMute = vm::toggleMute,
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        if (vm.showNumbers) {
            ModalBottomSheet(
                onDismissRequest = vm::toggleNumbers,
                containerColor = Ink.CardSolid,
                contentColor = Ink.Text,
            ) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text("Zender kiezen", style = MaterialTheme.typography.titleMedium, color = Ink.Text)
                    Spacer(Modifier.height(10.dp))
                    NumberPad(
                        haptics = haptics,
                        onDigit = { vm.tapKey(digitKey(it)) },
                        onOk = {
                            vm.tapKey(RemoteKey.OK)
                            vm.toggleNumbers()
                        },
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun Header(
    paired: PairedTv?,
    snapshot: TvSnapshot,
    link: LinkStatus,
    latencyMs: Long?,
    onOpenSettings: () -> Unit,
    onOpenProfiles: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                snapshot.tvName.ifBlank { paired?.name ?: "EnigmaCast" },
                style = MaterialTheme.typography.titleLarge,
                color = Ink.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                paired?.let { "${it.host}:${it.port}" } ?: "Niet gekoppeld",
                style = MaterialTheme.typography.bodySmall,
                color = Ink.Subtle,
            )
        }
        ProfileChip(snapshot, onClick = onOpenProfiles)
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Instellingen", tint = Ink.Muted)
        }
    }
    ConnectionLine(link, latencyMs)
}

@Composable
private fun ConnectionLine(link: LinkStatus, latencyMs: Long?) {
    val (label, color) = when (link.state) {
        LinkState.READY -> (latencyMs?.let { "Verbonden · $it ms" } ?: "Verbonden") to Ink.Success
        LinkState.CONNECTING -> "Verbinden…" to Ink.Warning
        LinkState.OFFLINE -> (link.message ?: "Offline") to Ink.Danger
        LinkState.ERROR -> (link.message ?: "Fout") to Ink.Danger
        LinkState.IDLE -> "Niet verbonden" to Ink.Subtle
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StatusDot(color, 7.dp)
        Text(label, color = color, fontSize = 12.sp)
    }
}

@Composable
private fun ProfileChip(snapshot: TvSnapshot, onClick: () -> Unit) {
    val profile = snapshot.activeProfile
    val initial = profile?.name?.firstOrNull()?.uppercase() ?: "?"
    Row(
        Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(30.dp).clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Ink.Accent, Ink.AccentDim))),
            contentAlignment = Alignment.Center,
        ) {
            Text(initial, color = Ink.OnAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text(profile?.name ?: "Profiel", color = Ink.Text, fontSize = 12.sp)
        Spacer(Modifier.width(10.dp))
    }
}

@Composable
private fun StatusPanel(snapshot: TvSnapshot, link: LinkStatus, mode: ControlMode, onToggleMode: () -> Unit) {
    val tunerKnown = snapshot.tuner.total > 0
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pill(
                        when (link.state) {
                            LinkState.READY -> "LIVE"
                            LinkState.CONNECTING -> "VERBINDEN"
                            LinkState.OFFLINE -> "OFFLINE"
                            LinkState.ERROR -> "FOUT"
                            LinkState.IDLE -> "IDLE"
                        },
                        color = when (link.state) {
                            LinkState.READY -> Ink.Success
                            LinkState.CONNECTING -> Ink.Warning
                            LinkState.OFFLINE, LinkState.ERROR -> Ink.Danger
                            LinkState.IDLE -> Ink.Subtle
                        },
                        filled = true,
                        dot = true,
                    )
                    Pill(
                        if (tunerKnown) "Tuner ${snapshot.tuner.used}/${snapshot.tuner.total}" else "Tuner —",
                        color = when {
                            !tunerKnown -> Ink.Subtle
                            snapshot.tuner.used >= snapshot.tuner.total -> Ink.Warning
                            else -> Ink.Muted
                        },
                    )
                    Pill(if (mode == ControlMode.APP) "App" else "Ontvanger", color = Ink.Accent, modifier = Modifier.clickable(onClick = onToggleMode))
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        snapshot.channel?.number?.toString() ?: "—",
                        color = Ink.Highlight,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        snapshot.channel?.name ?: "Geen zender",
                        color = Ink.Text,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                val now = snapshot.now
                if (now != null) {
                    Text(
                        now.title,
                        color = Ink.Muted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    ProgressLine(now)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${timeLabel(now.startMs)} – ${timeLabel(now.endMs)}",
                        color = Ink.Subtle,
                        fontSize = 11.sp,
                    )
                    snapshot.nextTitle?.let {
                        Text(
                            "Straks: $it",
                            color = Ink.Subtle,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    Text("Wachten op status…", color = Ink.Subtle, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (snapshot.player.paused) "PAUZE" else if (snapshot.player.playing) "SPEELT" else "STIL",
                    color = if (snapshot.player.paused) Ink.Warning else Ink.Success,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    snapshot.receiverName.ifBlank { "Ontvanger" },
                    color = Ink.Subtle,
                    fontSize = 11.sp,
                )
                Text(
                    if (snapshot.receiverOnline) "online" else "offline",
                    color = if (snapshot.receiverOnline) Ink.Success else Ink.Danger,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun ProgressLine(now: NowInfo) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(now.progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(Ink.AccentDim, Ink.Highlight))),
        )
    }
}

@Composable
private fun QuickKeys(vm: AppViewModel, haptics: Haptics, vertical: Boolean = false) {
    val keys: @Composable () -> Unit = {
        KeyButton("Terug", { vm.tapKey(RemoteKey.BACK) }, haptics, icon = Icons.AutoMirrored.Filled.ArrowBack)
        KeyButton("Home", { vm.nav(com.enigmacast.companion.protocol.NavTarget.HOME) }, haptics, icon = Icons.Filled.Home)
        KeyButton("Menu", { vm.tapKey(RemoteKey.MENU) }, haptics, icon = Icons.Filled.Menu)
        KeyButton("Info", { vm.tapKey(RemoteKey.INFO) }, haptics, icon = Icons.Filled.Info)
    }
    if (vertical) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { keys() }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { keys() }
    }
}

@Composable
private fun DebugStrip(snapshot: TvSnapshot, lastKey: String?, latency: Long?) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "Laatste toets: ${lastKey ?: "—"}",
            color = Ink.Subtle,
            fontSize = 11.sp,
        )
        Text(
            buildString {
                append("Bron: ${snapshot.source}")
                if (snapshot.route != "home") append(" · ${snapshot.route}")
                latency?.let { append(" · ack $it ms") }
            },
            color = Ink.Subtle,
            fontSize = 11.sp,
        )
    }
}

private fun Direction.toKey(): RemoteKey = when (this) {
    Direction.UP -> RemoteKey.DPAD_UP
    Direction.DOWN -> RemoteKey.DPAD_DOWN
    Direction.LEFT -> RemoteKey.DPAD_LEFT
    Direction.RIGHT -> RemoteKey.DPAD_RIGHT
}

private fun digitKey(number: Int): RemoteKey = RemoteKey.valueOf("DIGIT_$number")

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun timeLabel(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalTime().format(TIME_FORMAT)
