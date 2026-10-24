package com.enigmacast.companion.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.enigmacast.companion.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigmacast.companion.data.ControlMode
import com.enigmacast.companion.ui.Haptics
import com.enigmacast.companion.ui.theme.Ink
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Een richtingtoets die blijft herhalen zolang hij ingedrukt is: eerst 380 ms wachten,
 * daarna elke 120 ms (dezelfde cadans als `RemoteSettings.keyRepeatIntervalMs` op de TV).
 */
@Composable
fun HoldArrow(
    icon: ImageVector,
    label: String,
    haptics: Haptics,
    onDown: () -> Unit,
    onRepeat: (Int) -> Unit,
    onUp: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
) {
    val shape = CircleShape
    val scope = rememberCoroutineScope()
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(Ink.Raised)
            .border(1.dp, Ink.Border, shape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        haptics.tick()
                        onDown()
                        var repeat = 0
                        val job = scope.launch {
                            delay(380)
                            while (isActive) {
                                repeat += 1
                                onRepeat(repeat)
                                delay(120)
                            }
                        }
                        tryAwaitRelease()
                        job.cancel()
                        onUp()
                    },
                )
            }
            .semantics {
                contentDescription = label
                role = Role.Button
                onClick(label) { onDown(); onUp(); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Ink.Text, modifier = Modifier.size(size * 0.44f))
    }
}

@Composable
fun Dpad(
    haptics: Haptics,
    onDown: (Direction) -> Unit,
    onRepeat: (Direction, Int) -> Unit,
    onUp: (Direction) -> Unit,
    onOk: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = 264.dp,
) {
    Box(modifier.size(diameter)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Ink.Card)
                .border(1.dp, Ink.Border, CircleShape),
        )
        Box(
            Modifier
                .fillMaxSize(0.74f)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.02f))
                .border(1.dp, Color.White.copy(alpha = 0.06f), CircleShape),
        )
        HoldArrow(
            Icons.Filled.KeyboardArrowUp, "Omhoog", haptics,
            onDown = { onDown(Direction.UP) },
            onRepeat = { onRepeat(Direction.UP, it) },
            onUp = { onUp(Direction.UP) },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
        )
        HoldArrow(
            Icons.Filled.KeyboardArrowDown, "Omlaag", haptics,
            onDown = { onDown(Direction.DOWN) },
            onRepeat = { onRepeat(Direction.DOWN, it) },
            onUp = { onUp(Direction.DOWN) },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
        )
        HoldArrow(
            Icons.Filled.KeyboardArrowLeft, "Links", haptics,
            onDown = { onDown(Direction.LEFT) },
            onRepeat = { onRepeat(Direction.LEFT, it) },
            onUp = { onUp(Direction.LEFT) },
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp),
        )
        HoldArrow(
            Icons.Filled.KeyboardArrowRight, "Rechts", haptics,
            onDown = { onDown(Direction.RIGHT) },
            onRepeat = { onRepeat(Direction.RIGHT, it) },
            onUp = { onUp(Direction.RIGHT) },
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
        )
        Box(
            Modifier
                .align(Alignment.Center)
                .size(88.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Ink.Accent, Ink.AccentDim)))
                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        haptics.confirm()
                        onOk()
                        tryAwaitRelease()
                    })
                }
                .semantics {
                    contentDescription = "OK"
                    role = Role.Button
                    onClick("OK") { onOk(); true }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text("OK", color = Ink.OnAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

enum class Direction { UP, DOWN, LEFT, RIGHT }

@Composable
fun KeyButton(
    label: String,
    onClick: () -> Unit,
    haptics: Haptics,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    glyph: String? = null,
    size: Dp = 52.dp,
    accent: Boolean = false,
) {
    Surface(
        onClick = {
            haptics.tick()
            onClick()
        },
        modifier = modifier.size(size),
        shape = CircleShape,
        color = if (accent) Ink.Accent else Ink.Raised,
        contentColor = if (accent) Ink.OnAccent else Ink.Text,
        border = BorderStroke(1.dp, if (accent) Color.White.copy(alpha = 0.35f) else Ink.Border),
    ) {
        Box(contentAlignment = Alignment.Center) {
            when {
                icon != null -> Icon(icon, contentDescription = label, modifier = Modifier.size(size * 0.42f))
                glyph != null -> Text(glyph, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                else -> Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun TransportRow(
    haptics: Haptics,
    playing: Boolean,
    onPrevious: () -> Unit,
    onRewind: () -> Unit,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KeyButton("Vorige", onPrevious, haptics, glyph = "|‹")
        KeyButton("Terug", onRewind, haptics, glyph = "‹‹")
        KeyButton(
            if (playing) "Pauze" else "Afspelen",
            onPlayPause,
            haptics,
            glyph = if (playing) "II" else "▶",
            size = 60.dp,
            accent = true,
        )
        KeyButton("Stop", onStop, haptics, glyph = "■")
        KeyButton("Vooruit", onForward, haptics, glyph = "››")
    }
}

@Composable
fun ChannelRow(
    haptics: Haptics,
    number: Int?,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onNumbers: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier.fillMaxWidth(), padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KeyButton("Kanaal omhoog", onUp, haptics, glyph = "CH+", size = 50.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                SectionLabel("Zender")
                Text(
                    number?.toString()?.padStart(3, '0') ?: "—",
                    color = Ink.Text,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                )
            }
            Spacer(Modifier.width(10.dp))
            KeyButton("Cijfers", onNumbers, haptics, glyph = "123", size = 50.dp)
            Spacer(Modifier.width(10.dp))
            KeyButton("Kanaal omlaag", onDown, haptics, glyph = "CH−", size = 50.dp)
        }
    }
}

@Composable
fun NumberPad(
    haptics: Haptics,
    onDigit: (Int) -> Unit,
    onOk: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier.fillMaxWidth(), padding = 10.dp) {
        for (row in 0 until 4) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (column in 0 until 3) {
                    val index = row * 3 + column
                    val label = when (index) {
                        9 -> "0"
                        10 -> "OK"
                        11 -> ""
                        else -> (index + 1).toString()
                    }
                    val isOk = index == 10
                    val enabled = index != 11
                    Surface(
                        onClick = {
                            haptics.tick()
                            if (isOk) onOk() else onDigit(label.toInt())
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        enabled = enabled,
                        shape = RoundedCornerShape(14.dp),
                        color = if (isOk) Ink.Accent else Color.White.copy(alpha = 0.06f),
                        contentColor = if (isOk) Ink.OnAccent else Ink.Text,
                        border = BorderStroke(1.dp, if (isOk) Color.White.copy(alpha = 0.3f) else Ink.Border),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VolumeRow(
    haptics: Haptics,
    volume: Int,
    muted: Boolean,
    onDown: () -> Unit,
    onUp: () -> Unit,
    onMute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier.fillMaxWidth(), padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KeyButton("Volume omlaag", onDown, haptics, glyph = "−", size = 46.dp)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                SectionLabel(if (muted) "Geluid uit" else "Volume TV")
                Text(
                    if (muted) "—" else volume.toString(),
                    color = if (muted) Ink.Subtle else Ink.Text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            KeyButton("Volume omhoog", onUp, haptics, glyph = "+", size = 46.dp)
            Spacer(Modifier.width(10.dp))
            KeyButton(if (muted) "Geluid aan" else "Geluid uit", onMute, haptics, glyph = if (muted) "♪" else "×", size = 46.dp)
        }
    }
}

@Composable
fun ModeSwitch(
    mode: ControlMode,
    onSelect: (ControlMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Ink.Border, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ModeOption("EnigmaCast-app", mode == ControlMode.APP, Modifier.weight(1f)) { onSelect(ControlMode.APP) }
        ModeOption("Ontvanger", mode == ControlMode.RECEIVER, Modifier.weight(1f)) { onSelect(ControlMode.RECEIVER) }
    }
}

@Composable
private fun ModeOption(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Ink.Accent.copy(alpha = 0.20f) else Color.Transparent,
        contentColor = if (selected) Ink.Highlight else Ink.Subtle,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}
