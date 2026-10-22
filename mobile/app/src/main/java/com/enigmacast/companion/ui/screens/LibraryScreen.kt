package com.enigmacast.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigmacast.companion.data.library.LibraryItem
import com.enigmacast.companion.ui.AppViewModel
import com.enigmacast.companion.ui.components.GlassCard
import com.enigmacast.companion.ui.components.SectionLabel
import com.enigmacast.companion.ui.components.TopBar
import com.enigmacast.companion.ui.theme.Ink
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun LibraryScreen(vm: AppViewModel) {
    val movies = vm.library.filter { it.tvKind == "m" || it.tvKind == "s" || it.tvKind == "e" }
    val resume = movies
        .filter {
            it.durationMs > 0 && it.positionMs > 5_000 && it.positionMs < it.durationMs - 10_000
        }
        .groupBy { resumeSeriesKey(it) }
        .values
        .mapNotNull { group -> group.maxByOrNull { it.updatedAt } }
        .sortedByDescending { it.updatedAt }
    val favorites = movies.filter { it.favorite && (it.tvKind == "m" || it.tvKind == "s") }
    Column(Modifier.fillMaxSize()) {
        TopBar("Mijn", "Verder kijken, favorieten en opnames")
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Synchronisatie", color = Ink.Text, style = MaterialTheme.typography.titleMedium)
                Text(
                    "De tv is leidend voor IPTV-favorieten en kijkposities. " +
                        "Live-zenders horen niet in Mijn; films en series wel.",
                    color = Ink.Subtle,
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = vm::syncNow) {
                    Text("Sync met de ontvanger", color = Ink.Accent, fontSize = 12.sp)
                }
            }

            SectionLabel("Verder kijken")
            if (resume.isEmpty()) {
                Text(
                    "Nog geen film of serie om verder te kijken.",
                    color = Ink.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                resume.forEach { item -> LibraryRow(item, vm, showProgress = true) }
            }

            SectionLabel("Favorieten")
            if (favorites.isEmpty()) {
                Text(
                    "Nog geen favoriete films of series. Tik op de ster in de speler.",
                    color = Ink.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                favorites.forEach { item -> LibraryRow(item, vm, showProgress = item.durationMs > 0) }
            }

            SectionLabel("Opnames op de ontvanger")
            if (vm.recordings.isEmpty()) {
                Text(
                    "Geen opnames gevonden op de ontvanger.",
                    color = Ink.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                vm.recordings.forEach { recording -> RecordingRow(recording) { vm.playRecording(recording) } }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RecordingRow(recording: com.enigmacast.companion.protocol.RecordingEntry, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().clickable(onClick = onClick), padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(38.dp)
                    .height(38.dp)
                    .background(Ink.Accent.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("●", color = Ink.Danger, fontSize = 14.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(recording.title, color = Ink.Text, style = MaterialTheme.typography.titleSmall)
                Text(
                    listOfNotNull(
                        recording.channel.takeIf { it.isNotBlank() },
                        recordingDate(recording.startMs).takeIf { it.isNotBlank() },
                        time(recording.durationMs).takeIf { recording.durationMs > 0 },
                    ).joinToString(" · "),
                    color = Ink.Subtle,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text("▶", color = Ink.Accent, fontSize = 12.sp)
        }
    }
}

private fun recordingDate(ms: Long): String =
    if (ms <= 0) "" else DateTimeFormatter.ofPattern("dd-MM HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(ms))

/** Eén kaart per serie: alleen de nieuwste aflevering blijft in Verder kijken. */
private fun resumeSeriesKey(item: LibraryItem): String {
    if (item.tvKind != "e") return item.url
    val marker = Regex(" - S\\d+E\\d+").find(item.title)
    return if (marker != null) {
        "${item.tvSource.orEmpty()}|${item.title.substring(0, marker.range.first)}"
    } else {
        item.url
    }
}

@Composable
private fun LibraryRow(item: LibraryItem, vm: AppViewModel, showProgress: Boolean) {
    GlassCard(
        Modifier.fillMaxWidth().clickable { vm.resume(item) },
        padding = 12.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(38.dp)
                    .height(38.dp)
                    .background(Ink.Accent.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("▶", color = Ink.Accent, fontSize = 14.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title.ifBlank { item.url }, color = Ink.Text, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${sourceLabel(item)} · ${time(item.positionMs)}" +
                        if (item.durationMs > 0) " / ${time(item.durationMs)}" else "",
                    color = Ink.Subtle,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = { vm.toggleFavorite(item.url) }) {
                Text(if (item.favorite) "★" else "☆", color = if (item.favorite) Ink.Warning else Ink.Muted, fontSize = 18.sp)
            }
        }
        if (showProgress && item.durationMs > 0) {
            val fraction = (item.positionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Ink.Border, RoundedCornerShape(2.dp)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(3.dp)
                        .background(Ink.Accent, RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

private fun sourceLabel(item: LibraryItem): String = when (item.source) {
    "live" -> "Live"
    "iptv" -> "IPTV"
    else -> "Stream"
}

private fun time(ms: Long): String {
    val total = (ms.coerceAtLeast(0L) / 1000L)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
