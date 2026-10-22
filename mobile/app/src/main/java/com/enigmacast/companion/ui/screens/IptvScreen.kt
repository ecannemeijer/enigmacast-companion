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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigmacast.companion.ui.AppViewModel
import com.enigmacast.companion.ui.components.ChannelLogo
import com.enigmacast.companion.ui.components.GhostButton
import com.enigmacast.companion.ui.components.GlassCard
import com.enigmacast.companion.ui.components.PrimaryButton
import com.enigmacast.companion.ui.components.TopBar
import com.enigmacast.companion.ui.theme.Ink
import com.enigmacast.domain.settingsimport.M3uEntry

/**
 * Eén IPTV-ingang: als er een M3U- of Xtream-bron is, gaan we direct naar de inhoud.
 * De setup verschijnt alleen als er nog niets is, of via "Wijzigen".
 */
@Composable
fun IptvScreen(vm: AppViewModel) {
    var setup by remember { mutableStateOf(false) }
    val account = vm.xtreamAccount
    val hasM3u = vm.iptvUrl.isNotBlank()

    LaunchedEffect(Unit) { vm.ensureIptvLoaded() }

    Column(Modifier.fillMaxSize()) {
        TopBar(
            "IPTV",
            when {
                account != null -> "Live · Films · Series"
                hasM3u -> "Live zenders"
                else -> "Kies je bron"
            },
        )
        when {
            account != null && !setup -> {
                SourceBar("Xtream · ${account.name}", "Wijzigen") { setup = true }
                XtreamContent(vm)
            }
            hasM3u && !setup -> {
                M3uContent(vm) { setup = true }
            }
            else -> {
                IptvSetup(
                    vm = vm,
                    initialXtream = vm.iptvKind == "XTREAM" || vm.xtreamServer.isNotBlank(),
                    onClose = { setup = false },
                )
            }
        }
    }
}

@Composable
private fun SourceBar(source: String, action: String, onAction: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            source,
            color = Ink.Subtle,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onAction) {
            Text(action, color = Ink.Accent, fontSize = 12.sp)
        }
    }
}

@Composable
private fun M3uContent(vm: AppViewModel, onEdit: () -> Unit) {
    val visible = vm.iptvEntries.filter { entry ->
        vm.iptvGroup == null || entry.groups.contains(vm.iptvGroup)
    }
    Column(Modifier.fillMaxSize()) {
        SourceBar("M3U · ${vm.iptvUrl.substringAfter("//").substringBefore("/")}", "Wijzigen", onEdit)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${visible.size} zenders",
                color = Ink.Text,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.weight(1f))
            if (vm.iptvFromTv) {
                Text("via tv-sync", color = Ink.Success, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (vm.iptvGroups.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = vm.iptvGroup == null,
                    onClick = { vm.selectIptvGroup(null) },
                    label = { Text("Alles") },
                    colors = chipColors(),
                )
                vm.iptvGroups.forEach { group ->
                    FilterChip(
                        selected = vm.iptvGroup == group,
                        onClick = { vm.selectIptvGroup(group) },
                        label = { Text(group) },
                        colors = chipColors(),
                    )
                }
            }
        }
        if (vm.iptvLoading) {
            LoadingRow("Playlist laden…")
        }
        vm.iptvError?.let {
            Text(it, color = Ink.Danger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 20.dp))
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        ) {
            items(visible, key = { it.url + it.name }) { entry ->
                IptvRow(entry) { vm.watchIptv(entry) }
            }
            if (visible.isEmpty() && !vm.iptvLoading && vm.iptvError == null) {
                item {
                    Text(
                        "Geen zenders in deze playlist.",
                        color = Ink.Muted,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun IptvRow(entry: M3uEntry, onClick: () -> Unit) {
    GlassCard(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        padding = 12.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChannelLogo(entry.attributes["tvg-logo"].orEmpty(), entry.name, size = 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.name, color = Ink.Text, style = MaterialTheme.typography.titleSmall)
                Text(
                    entry.groups.joinToString(" · ").ifBlank { "IPTV" },
                    color = Ink.Subtle,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text("▶", color = Ink.Accent, fontSize = 12.sp)
        }
    }
}

@Composable
private fun IptvSetup(vm: AppViewModel, initialXtream: Boolean, onClose: () -> Unit) {
    var mode by remember { mutableStateOf(if (initialXtream) "XTREAM" else "M3U") }
    var url by remember(vm.iptvUrl) { mutableStateOf(vm.iptvUrl) }
    var server by remember(vm.xtreamServer) { mutableStateOf(vm.xtreamServer) }
    var user by remember(vm.xtreamUser) { mutableStateOf(vm.xtreamUser) }
    var password by remember(vm.xtreamPassword) { mutableStateOf(vm.xtreamPassword) }
    var submitted by remember { mutableStateOf(false) }
    LaunchedEffect(vm.xtreamAccount, submitted) {
        if (submitted && vm.xtreamAccount != null && mode == "XTREAM") onClose()
    }
    LaunchedEffect(vm.iptvEntries.size, submitted) {
        if (submitted && vm.iptvEntries.isNotEmpty() && mode == "M3U") onClose()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Kies je IPTV-bron. Als de tv hem al heeft, vult de app hem automatisch in.",
            color = Ink.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == "M3U",
                onClick = { mode = "M3U" },
                label = { Text("M3U-playlist") },
                colors = chipColors(),
            )
            FilterChip(
                selected = mode == "XTREAM",
                onClick = { mode = "XTREAM" },
                label = { Text("Xtream Codes") },
                colors = chipColors(),
            )
        }
        GlassCard(Modifier.fillMaxWidth()) {
            if (mode == "M3U") {
                Text("M3U-playlist", color = Ink.Text, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Plak het playlistadres van je provider. De app leest de zenders en logo's.",
                    color = Ink.Subtle,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("M3U-playlistadres") },
                    placeholder = { Text("https://provider.example/playlist.m3u") },
                    singleLine = true,
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton("Laden", onClick = { submitted = true; vm.loadIptv(url) }, modifier = Modifier.weight(1f))
                    GhostButton("Demo", onClick = { vm.demoIptv() }, modifier = Modifier.width(110.dp))
                }
            } else {
                Text("Xtream Codes", color = Ink.Text, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Log in met het adres en de gegevens van je provider.",
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton(
                        "Inloggen",
                        onClick = { submitted = true; vm.loginXtream(server, user, password) },
                        modifier = Modifier.weight(1f),
                    )
                    GhostButton("Demo", onClick = { vm.demoXtream() }, modifier = Modifier.width(110.dp))
                }
            }
            val error = if (mode == "M3U") vm.iptvError else vm.xtreamError
            if (error != null) {
                Text(error, color = Ink.Danger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            }
            if (vm.iptvFromTv) {
                Text(
                    "Deze bron kwam automatisch van de tv.",
                    color = Ink.Success,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        TextButton(onClick = onClose) { Text("Annuleren", color = Ink.Muted) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LoadingRow(text: String) {
    Row(
        Modifier.fillMaxWidth().padding(20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(color = Ink.Accent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = Ink.Muted, style = MaterialTheme.typography.bodySmall)
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
