package com.enigmacast.companion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.enigmacast.companion.ui.AppViewModel
import com.enigmacast.companion.ui.theme.Ink
import com.enigmacast.domain.xtream.XtreamSection

/** TMDB-informatie bij een IPTV-film of -serie, met poster en afspeelknop. */
@Composable
fun MediaInfoDialog(vm: AppViewModel) {
    val item = vm.infoItem ?: return
    val info = vm.info
    AlertDialog(
        onDismissRequest = vm::closeInfo,
        title = {
            Text(info?.title?.ifBlank { item.name } ?: item.name)
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val poster = info?.posterUrl?.ifBlank { item.icon }.orEmpty()
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (poster.isNotBlank()) {
                        AsyncImage(
                            model = poster,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.matchParentSize(),
                        )
                    } else {
                        ChannelLogo(item.icon, item.name, size = 80.dp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (item.section == XtreamSection.SERIES) "Serie" else "Film",
                        color = Ink.Accent,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    info?.year?.takeIf { it.isNotBlank() }?.let { Text(it, color = Ink.Subtle) }
                    info?.rating?.let { rating ->
                        Text("★ ${"%.1f".format(rating)}", color = Ink.Warning)
                    }
                }
                if (vm.infoLoading) {
                    Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = Ink.Accent, strokeWidth = 2.dp, modifier = Modifier.width(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("TMDB-info laden…", color = Ink.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                } else if (info?.overview.isNullOrBlank()) {
                    Text(
                        "Geen beschrijving gevonden. De aflevering of film kan wel worden gestart.",
                        color = Ink.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                } else {
                    Text(
                        info?.overview.orEmpty(),
                        color = Ink.Text,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = vm::playInfo) {
                Text(if (item.section == XtreamSection.SERIES) "Afleveringen" else "Afspelen")
            }
        },
        dismissButton = {
            TextButton(onClick = vm::closeInfo) { Text("Sluiten") }
        },
    )
}
