package com.enigmacast.companion.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.enigmacast.companion.ui.components.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigmacast.companion.ui.AppScreen
import com.enigmacast.companion.ui.AppViewModel
import com.enigmacast.companion.ui.components.ChannelLogo
import com.enigmacast.companion.ui.components.GhostButton
import com.enigmacast.companion.ui.components.GlassCard
import com.enigmacast.companion.ui.components.PrimaryButton
import com.enigmacast.companion.ui.components.TopBar
import com.enigmacast.companion.ui.theme.Ink
import com.enigmacast.domain.xtream.XtreamEpg
import com.enigmacast.domain.xtream.XtreamItem
import com.enigmacast.domain.xtream.XtreamSection
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun XtreamScreen(vm: AppViewModel) {
    Column(Modifier.fillMaxSize()) {
        TopBar(
            "Xtream Codes",
            vm.xtreamSeriesTitle ?: vm.xtreamAccount?.name ?: "IPTV-login",
            onBack = {
                if (vm.xtreamSeriesTitle != null) vm.closeXtreamSeries() else vm.open(AppScreen.IPTV)
            },
        )
        if (vm.xtreamAccount == null) {
            XtreamLogin(vm)
        } else {
            XtreamContent(vm)
        }
    }
    if (vm.catchupItem != null) {
        XtreamCatchupDialog(vm)
    }
}

@Composable
private fun XtreamLogin(vm: AppViewModel) {
    var server by remember(vm.xtreamServer) { mutableStateOf(vm.xtreamServer) }
    var user by remember(vm.xtreamUser) { mutableStateOf(vm.xtreamUser) }
    var password by remember(vm.xtreamPassword) { mutableStateOf(vm.xtreamPassword) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (vm.iptvFromTv) {
            Text(
                "IPTV-bron gesynchroniseerd van de tv",
                color = Ink.Success,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Xtream Codes", color = Ink.Text, style = MaterialTheme.typography.titleMedium)
            Text(
                "Log in met het adres en de inloggegevens van je provider. De app leest de live-categorieën " +
                    "en speelt de streams rechtstreeks.",
                color = Ink.Subtle,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = server,
                onValueChange = { server = it },
                label = { Text("Server") },
                placeholder = { Text("http://provider.example:8080") },
                singleLine = true,
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = user,
                onValueChange = { user = it },
                label = { Text("Gebruikersnaam") },
                singleLine = true,
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Wachtwoord") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val error = vm.xtreamError
        if (error != null) {
            Text(error, color = Ink.Danger, style = MaterialTheme.typography.bodySmall)
        }
        PrimaryButton(
            "Inloggen",
            onClick = { vm.loginXtream(server, user, password) },
            enabled = server.trim().isNotEmpty() && user.trim().isNotEmpty() && password.isNotEmpty(),
        )
        GhostButton("Demo-Xtream (lokaal) gebruiken", onClick = { vm.demoXtream() })
        Text(
            "Het wachtwoord wordt lokaal versleuteld bewaard.",
            color = Ink.Subtle,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun XtreamContent(vm: AppViewModel) {
    Column(Modifier.fillMaxSize()) {
        if (vm.xtreamSeriesTitle != null) {
            XtreamSeriesContent(vm)
            return@Column
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            XtreamSection.entries.forEach { section ->
                FilterChip(
                    selected = section == vm.xtreamSection,
                    onClick = { vm.selectXtreamSection(section) },
                    label = {
                        Text(
                            when (section) {
                                XtreamSection.LIVE -> "Live"
                                XtreamSection.MOVIES -> "Films"
                                XtreamSection.SERIES -> "Series"
                            },
                        )
                    },
                    colors = chipColors(),
                )
            }
        }
        if (vm.xtreamLoading) {
            Row(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(color = Ink.Accent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Laden…", color = Ink.Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        val error = vm.xtreamError
        if (error != null) {
            Text(error, color = Ink.Danger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 20.dp))
        }
        if (vm.xtreamCategories.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                vm.xtreamCategories.forEach { category ->
                    FilterChip(
                        selected = category.id == vm.xtreamCategory?.id,
                        onClick = { vm.selectXtreamCategory(category) },
                        label = { Text(category.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Ink.Accent.copy(alpha = 0.22f),
                            selectedLabelColor = Ink.Accent,
                            labelColor = Ink.Muted,
                        ),
                    )
                }
            }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        ) {
            items(vm.xtreamItems, key = { it.id }) { item ->
                XtreamRow(
                    item = item,
                    now = vm.xtreamNow[item.id],
                    onClick = {
                        if (item.section == XtreamSection.LIVE) vm.watchXtream(item) else vm.openInfo(item)
                    },
                    onCatchup = { vm.openCatchup(item) },
                    onNeedEpg = { vm.ensureXtreamEpg(item) },
                )
            }
            if (vm.xtreamItems.isEmpty() && !vm.xtreamLoading && error == null) {
                item { Text("Geen streams in deze categorie.", color = Ink.Muted) }
            }
        }
    }
}

@Composable
private fun XtreamSeriesContent(vm: AppViewModel) {
    Column(Modifier.fillMaxSize()) {
        Text(
            "Afleveringen",
            color = Ink.Subtle,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        if (vm.xtreamLoading) {
            Row(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(color = Ink.Accent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Laden…", color = Ink.Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        val error = vm.xtreamError
        if (error != null) {
            Text(error, color = Ink.Danger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 20.dp))
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        ) {
            items(vm.xtreamEpisodes, key = { it.id }) { episode ->
                GlassCard(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { vm.watchXtreamEpisode(episode) },
                    padding = 12.dp,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChannelLogo(episode.cover, episode.title, size = 46.dp, round = false)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "S${episode.season} · E${episode.episode} · ${episode.title}",
                                color = Ink.Text,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text("▶  Bekijk", color = Ink.Accent, fontSize = 11.sp)
                        }
                    }
                }
            }
            if (vm.xtreamEpisodes.isEmpty() && !vm.xtreamLoading && error == null) {
                item { Text("Geen afleveringen gevonden.", color = Ink.Muted) }
            }
        }
    }
}

@Composable
private fun XtreamCatchupDialog(vm: AppViewModel) {
    val item = vm.catchupItem ?: return
    AlertDialog(
        onDismissRequest = vm::closeCatchup,
        title = { Text("Terugkijken · ${item.name}") },
        text = {
            Column {
                when {
                    vm.catchupLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = Ink.Accent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Programma's laden…", color = Ink.Muted)
                    }
                    vm.catchupError != null -> Text(vm.catchupError.orEmpty(), color = Ink.Danger)
                    vm.catchupPrograms.isEmpty() -> Text("Geen programma's in het archief.", color = Ink.Muted)
                    else -> LazyColumn(Modifier.height(300.dp)) {
                        items(vm.catchupPrograms, key = { it.startSec }) { event ->
                            TextButton(onClick = { vm.playCatchup(event) }) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(event.title, color = Ink.Text, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${time(event.startSec)}–${time(event.endSec)}",
                                        color = Ink.Subtle,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = vm::closeCatchup) { Text("Sluiten") }
        },
    )
}

private fun time(epochSec: Long): String =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochSecond(epochSec))

@Composable
private fun XtreamRow(
    item: XtreamItem,
    now: XtreamEpg?,
    onClick: () -> Unit,
    onCatchup: () -> Unit,
    onNeedEpg: () -> Unit,
) {
    LaunchedEffect(item.id) {
        if (item.section == XtreamSection.LIVE) onNeedEpg()
    }
    GlassCard(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick), padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChannelLogo(item.icon, item.name, size = 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, color = Ink.Text, style = MaterialTheme.typography.titleSmall)
                Text(
                    when {
                        item.section == XtreamSection.LIVE && now != null ->
                            "${time(now.startSec)}–${time(now.endSec)} · ${now.title}"
                        item.section == XtreamSection.LIVE -> "Live"
                        item.section == XtreamSection.MOVIES -> "Film · ${item.extension.ifBlank { "video" }}"
                        else -> "Serie · bekijk afleveringen"
                    },
                    color = Ink.Subtle,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (item.section == XtreamSection.LIVE && item.hasArchive) {
                    TextButton(onClick = onCatchup) {
                        Text("Terugkijken (laatste uur)", color = Ink.Muted, fontSize = 11.sp)
                    }
                }
            }
            Text(
                if (item.section == XtreamSection.LIVE) "▶" else "Info",
                color = Ink.Accent,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = Ink.Accent.copy(alpha = 0.22f),
    selectedLabelColor = Ink.Accent,
    labelColor = Ink.Muted,
)

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Ink.Accent,
    unfocusedBorderColor = Ink.Border,
    focusedLabelColor = Ink.Accent,
    unfocusedLabelColor = Ink.Subtle,
    cursorColor = Ink.Accent,
)
