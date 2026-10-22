package com.enigmacast.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigmacast.companion.data.LinkState
import com.enigmacast.companion.data.LinkStatus
import com.enigmacast.companion.ui.AppViewModel
import com.enigmacast.companion.ui.components.GhostButton
import com.enigmacast.companion.ui.components.GlassCard
import com.enigmacast.companion.ui.components.Pill
import com.enigmacast.companion.ui.components.PrimaryButton
import com.enigmacast.companion.ui.components.TopBar
import com.enigmacast.companion.ui.theme.Ink
import com.enigmacast.domain.model.BouquetEntry
import com.enigmacast.domain.model.ReceiverConfig
import com.enigmacast.domain.tuner.NowEvent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun LiveScreen(vm: AppViewModel, link: LinkStatus) {
    var editing by remember(vm.liveReceiver) { mutableStateOf(vm.liveReceiver == null) }
    Column(Modifier.fillMaxSize()) {
        TopBar(
            "Live tv",
            vm.liveReceiver?.let { "${it.name.ifBlank { it.host }} · ${it.host}" }
                ?: "Enigma2 rechtstreeks op je telefoon",
        )
        if (editing || vm.liveReceiver == null) {
            LiveSetup(
                config = vm.liveReceiver,
                onSave = {
                    vm.saveLiveReceiver(it)
                    editing = false
                },
                onDemo = {
                    vm.demoLiveReceiver()
                    editing = false
                },
            )
        } else {
            LiveContent(
                vm = vm,
                link = link,
                onEdit = { editing = true },
            )
        }
    }
}

@Composable
private fun LiveSetup(
    config: ReceiverConfig?,
    onSave: (ReceiverConfig) -> Unit,
    onDemo: () -> Unit,
) {
    var name by remember(config) { mutableStateOf(config?.name ?: "Woonkamer") }
    var host by remember(config) { mutableStateOf(config?.host.orEmpty()) }
    var httpPort by remember(config) { mutableStateOf((config?.httpPort ?: 80).toString()) }
    var streamPort by remember(config) { mutableStateOf((config?.streamingPort ?: 8001).toString()) }
    var username by remember(config) { mutableStateOf(config?.username.orEmpty()) }
    var password by remember(config) { mutableStateOf(config?.password.orEmpty()) }
    var https by remember(config) { mutableStateOf(config?.useHttps == true) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Enigma2-ontvanger", color = Ink.Text, style = MaterialTheme.typography.titleMedium)
            Text(
                "De telefoon leest de zenderlijst via OpenWebif en speelt de stream rechtstreeks van de ontvanger. " +
                    "Je hebt geen EnigmaCast-TV nodig.",
                color = Ink.Subtle,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Naam") },
                singleLine = true,
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("IP-adres") },
                placeholder = { Text("192.168.1.20") },
                singleLine = true,
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = httpPort,
                    onValueChange = { httpPort = it.filter(Char::isDigit).take(5) },
                    label = { Text("Webif-poort") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors(),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = streamPort,
                    onValueChange = { streamPort = it.filter(Char::isDigit).take(5) },
                    label = { Text("Streampoort") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors(),
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Gebruikersnaam (optioneel)") },
                singleLine = true,
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Wachtwoord (optioneel)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("HTTPS", color = Ink.Text, style = MaterialTheme.typography.titleSmall)
                    Text("Alleen als de webif alleen https aanbiedt.", color = Ink.Subtle, style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = https,
                    onCheckedChange = { https = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Ink.OnAccent,
                        checkedTrackColor = Ink.Accent,
                        uncheckedThumbColor = Ink.Subtle,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.08f),
                    ),
                )
            }
        }
        PrimaryButton(
            "Verbinden",
            onClick = {
                val http = httpPort.toIntOrNull() ?: 80
                val stream = streamPort.toIntOrNull() ?: 8001
                onSave(
                    ReceiverConfig(
                        id = config?.id.orEmpty(),
                        name = name.trim(),
                        host = host.trim(),
                        httpPort = http.coerceIn(1, 65535),
                        streamingPort = stream.coerceIn(1, 65535),
                        username = username.trim(),
                        password = password,
                        useHttps = https,
                        allowInsecureCertificates = https,
                    ),
                )
            },
            enabled = host.trim().isNotEmpty(),
        )
        GhostButton("Demo-ontvanger (lokaal) gebruiken", onClick = onDemo)
        Text(
            "De inloggegevens worden lokaal versleuteld bewaard en alleen naar de ontvanger gestuurd.",
            color = Ink.Subtle,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LiveContent(vm: AppViewModel, link: LinkStatus, onEdit: () -> Unit) {
    val config = vm.liveReceiver ?: return
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(config.name.ifBlank { config.host }, color = Ink.Text, style = MaterialTheme.typography.titleLarge)
                Text("${config.host}:${config.httpPort} · stream ${config.streamingPort}", color = Ink.Subtle, style = MaterialTheme.typography.bodySmall)
            }
            GhostButton("Wijzigen", onClick = onEdit, modifier = Modifier.width(120.dp))
        }
        if (vm.recentTv.isNotEmpty()) {
            Text(
                "Recent op de tv",
                color = Ink.Subtle,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                vm.recentTv.forEach { channel ->
                    FilterChip(
                        selected = false,
                        onClick = { vm.watchRecentChannel(channel) },
                        label = { Text(channel.name.ifBlank { channel.reference }) },
                        colors = FilterChipDefaults.filterChipColors(labelColor = Ink.Muted),
                    )
                }
            }
        }
        if (vm.liveBouquets.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                vm.liveBouquets.forEach { bouquet ->
                    FilterChip(
                        selected = bouquet.reference == vm.liveBouquet?.reference,
                        onClick = { vm.selectLiveBouquet(bouquet) },
                        label = { Text(bouquet.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Ink.Accent.copy(alpha = 0.22f),
                            selectedLabelColor = Ink.Accent,
                            labelColor = Ink.Muted,
                        ),
                    )
                }
            }
        }
        val error = vm.liveError
        if (error != null) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(error, color = Ink.Danger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                TextButton(onClick = vm::loadLiveBouquets) { Text("Opnieuw", color = Ink.Accent) }
            }
        }
        if (vm.liveLoading && vm.liveChannels.isEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(color = Ink.Accent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Zenders laden…", color = Ink.Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
            items(vm.liveChannels, key = { it.reference }) { channel ->
                LiveChannelRow(channel, vm.liveNow[channel.reference]) { vm.watchLive(channel) }
            }
            if (vm.liveChannels.isEmpty() && !vm.liveLoading && error == null) {
                item {
                    Text("Geen zenders in deze lijst.", color = Ink.Muted, modifier = Modifier.padding(vertical = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun LiveChannelRow(
    channel: BouquetEntry,
    now: NowEvent?,
    onClick: () -> Unit,
) {
    GlassCard(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        padding = 12.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .background(Ink.Accent.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(channel.number.takeIf { it > 0 }?.toString() ?: "•", color = Ink.Accent, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(channel.name, color = Ink.Text, style = MaterialTheme.typography.titleSmall)
                if (now != null) {
                    Text(
                        "${time(now.beginEpochSec)}–${time(now.beginEpochSec + now.durationSec)} · ${now.title}",
                        color = Ink.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                    )
                } else {
                    Text("Geen EPG", color = Ink.Subtle, style = MaterialTheme.typography.bodySmall)
                }
            }
            Pill("LIVE", color = Ink.Success, dot = true)
        }
    }
}

private fun time(epochSec: Long): String =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochSecond(epochSec))

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Ink.Accent,
    unfocusedBorderColor = Ink.Border,
    focusedLabelColor = Ink.Accent,
    unfocusedLabelColor = Ink.Subtle,
    cursorColor = Ink.Accent,
)
