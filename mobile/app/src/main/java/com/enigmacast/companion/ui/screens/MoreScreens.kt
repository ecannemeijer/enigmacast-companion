package com.enigmacast.companion.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.enigmacast.companion.ui.components.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigmacast.companion.data.ControlMode
import com.enigmacast.companion.R
import com.enigmacast.companion.data.CompanionLanguage
import com.enigmacast.companion.data.LinkState
import com.enigmacast.companion.data.LinkStatus
import com.enigmacast.companion.protocol.ProfileInfo
import com.enigmacast.companion.protocol.SettingEntry
import com.enigmacast.companion.ui.AppScreen
import com.enigmacast.companion.ui.AppViewModel
import com.enigmacast.companion.ui.components.GhostButton
import com.enigmacast.companion.ui.components.GlassCard
import com.enigmacast.companion.ui.components.Pill
import com.enigmacast.companion.ui.components.SectionLabel
import com.enigmacast.companion.ui.theme.Ink
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun SettingsScreen(vm: AppViewModel, link: LinkStatus) {
    val context = LocalContext.current
    val tv = vm.pairedTv
    var pending by remember { mutableStateOf<PendingSetting?>(null) }
    var pinTarget by remember { mutableStateOf<PendingSetting?>(null) }
    var languageOpen by remember { mutableStateOf(false) }
    val settingsLocked = vm.settingsCatalog?.locked == true
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Meer", style = MaterialTheme.typography.headlineMedium, color = Ink.Text)
        Text("Verbinding, apparaten en instellingen", color = Ink.Subtle, style = MaterialTheme.typography.bodySmall)
        Text(
            "Versie ${com.enigmacast.companion.BuildConfig.VERSION_NAME} · build ${com.enigmacast.companion.BuildConfig.VERSION_CODE}",
            color = Ink.Subtle,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(18.dp))

        SectionLabel("Verbinding")
        Spacer(Modifier.height(8.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tv?.name ?: "Geen tv gekoppeld", color = Ink.Text, style = MaterialTheme.typography.titleMedium)
                    Text(
                        tv?.let { "${it.host}:${it.port}" } ?: "Koppel een tv om te beginnen",
                        color = Ink.Subtle,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Pill(
                    text = when (link.state) {
                        LinkState.READY -> "Verbonden"
                        LinkState.CONNECTING -> "Verbinden"
                        LinkState.OFFLINE -> "Offline"
                        LinkState.ERROR -> "Fout"
                        LinkState.IDLE -> "Los"
                    },
                    color = when (link.state) {
                        LinkState.READY -> Ink.Success
                        LinkState.CONNECTING -> Ink.Warning
                        LinkState.OFFLINE, LinkState.ERROR -> Ink.Danger
                        LinkState.IDLE -> Ink.Subtle
                    },
                    dot = true,
                )
            }
            if (tv != null) {
                Spacer(Modifier.height(12.dp))
                GhostButton("Opnieuw verbinden", onClick = vm::reconnect)
                if (!tv.real) {
                    Spacer(Modifier.height(8.dp))
                    GhostButton("Simuleer storing", onClick = vm::simulateDrop)
                }
            }
            Spacer(Modifier.height(10.dp))
            SettingRow("Apparaten beheren", "Gekoppelde tv's en opnieuw zoeken") { vm.open(AppScreen.DEVICES) }
            SettingRow("Profielen", "Wie kijkt er mee?") { vm.open(AppScreen.PROFILES) }
        }

        Spacer(Modifier.height(18.dp))
        vm.settingsCatalog?.let { catalog ->
            SectionLabel("TV-instellingen")
            Spacer(Modifier.height(8.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                SettingRow(
                    "TV-instellingen openen",
                    "Ga naar het instellingenscherm op de tv",
                ) { vm.nav(com.enigmacast.companion.protocol.NavTarget.SETTINGS) }
            }
            if (settingsLocked) {
                Spacer(Modifier.height(8.dp))
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("Gevorderde instellingen vergrendeld", color = Ink.Warning, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "De tv heeft een Settings-PIN. Bij een gevorderde instelling vraagt de app om de pincode.",
                        color = Ink.Subtle,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            catalog.entries.groupBy { it.category }.forEach { (category, entries) ->
                Spacer(Modifier.height(8.dp))
                Text(category.ifBlank { "Overig" }, color = Ink.Muted, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                GlassCard(Modifier.fillMaxWidth()) {
                    entries.forEach { entry ->
                        SettingControl(entry) { value ->
                            if (entry.dangerous) pending = PendingSetting(entry, value)
                            else vm.updateSetting(entry.path, value)
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
        }
        SectionLabel("Weergave en feedback")
        Spacer(Modifier.height(8.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Trilfeedback", color = Ink.Text, style = MaterialTheme.typography.titleSmall)
                    Text("Trilt bij elke toets", color = Ink.Subtle, style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = vm.haptics,
                    onCheckedChange = { vm.toggleHaptics() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Ink.OnAccent,
                        checkedTrackColor = Ink.Accent,
                        uncheckedThumbColor = Ink.Subtle,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.08f),
                    ),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Bedieningsdoel", color = Ink.Text, style = MaterialTheme.typography.titleSmall)
                    Text("App of ontvanger", color = Ink.Subtle, style = MaterialTheme.typography.bodySmall)
                }
                Pill(if (vm.mode == ControlMode.APP) "EnigmaCast" else "Ontvanger", color = Ink.Accent)
            }
            Spacer(Modifier.height(10.dp))
            SettingRow(
                title = stringResource(R.string.language_title),
                subtitle = if (vm.language == CompanionLanguage.SYSTEM) stringResource(R.string.language_system) else vm.language.label,
            ) { languageOpen = true }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Diagnose")
        Spacer(Modifier.height(8.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            InfoRow("Brug", if (tv?.real == true) "Echte TV-brug" else "Demo")
            InfoRow("Certificaatpin", tv?.pin?.take(24)?.ifBlank { "—" } ?: "—")
            InfoRow("Laatste toets", vm.lastKeyLabel ?: "—")
            InfoRow("Ack-latentie", vm.lastLatencyMs?.let { "$it ms" } ?: "—")
            InfoRow("Bron", vm.snapshot.source)
            InfoRow("Scherm op tv", vm.snapshot.screenApp.ifBlank { "—" })
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Nog in ontwikkeling")
        Spacer(Modifier.height(8.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            SettingRow("Kijkgeschiedenis delen", "Tussen tv en telefoon", badge = "Sync") {}
            SettingRow("Foto's van de tv", "GET/DELETE van profielfoto's", badge = "Foto") {}
            SettingRow("Ontvanger-login", "Meerveldsformulieren", badge = "Form") {}
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Over deze app")
        Spacer(Modifier.height(8.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("EnigmaCast Companion", color = Ink.Text, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Versie ${com.enigmacast.companion.BuildConfig.VERSION_NAME} · build ${com.enigmacast.companion.BuildConfig.VERSION_CODE}",
                        color = Ink.Subtle,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Pill(if (tv?.real == true) "Echte brug" else "Demo", color = if (tv?.real == true) Ink.Success else Ink.Warning)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = vm::checkForUpdates, enabled = !vm.updateChecking) {
                    Text(if (vm.updateChecking) "Controleren…" else "Controleer op updates")
                }
                vm.updateAvailable?.let { release ->
                    Text(
                        if (vm.updateReady != null) {
                            "Versie ${release.versionName} klaar om te installeren"
                        } else {
                            "Versie ${release.versionName} beschikbaar"
                        },
                        color = Ink.Accent,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            vm.updateStatus?.let { status ->
                Text(status, color = Ink.Muted, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Deze build spreekt het echte Companion-protocol over HTTPS/WSS. Koppel een tv via " +
                    "de 6-cijferige code of start de demo; de schermen blijven gelijk.",
                color = Ink.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (languageOpen) {
        AlertDialog(
            onDismissRequest = { languageOpen = false },
            title = { Text(stringResource(R.string.language_title)) },
            text = {
                Column {
                    CompanionLanguage.entries.forEach { language ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                vm.setLanguage(language)
                                languageOpen = false
                                (context as? Activity)?.recreate()
                            }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = language == vm.language,
                                onClick = {
                                    vm.setLanguage(language)
                                    languageOpen = false
                                    (context as? Activity)?.recreate()
                                },
                            )
                            Text(
                                if (language == CompanionLanguage.SYSTEM) stringResource(R.string.language_system) else language.label,
                                color = Ink.Text,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { languageOpen = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    pending?.let { request ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Gevorderde instelling") },
            text = {
                Text("${request.entry.label}: deze instelling kan beeld of geluid beïnvloeden. Doorgaan?")
            },
            confirmButton = {
                TextButton(onClick = {
                    if (settingsLocked) {
                        pinTarget = request
                    } else {
                        vm.updateSetting(request.entry.path, request.value)
                    }
                    pending = null
                }) { Text("Doorgaan") }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text("Annuleren") }
            },
        )
    }

    pinTarget?.let { request ->
        SettingsPinDialog(
            label = request.entry.label,
            onDismiss = { pinTarget = null },
            onConfirm = { pin ->
                vm.updateSetting(request.entry.path, request.value, pin)
                pinTarget = null
            },
        )
    }
}

private data class PendingSetting(val entry: SettingEntry, val value: JsonElement)

@Composable
private fun SettingsPinDialog(
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings-pincode") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Voer de pincode van de tv in om \"$label\" te wijzigen.", color = Ink.Muted)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(4) },
                    label = { Text("Pincode") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pin) }, enabled = pin.length == 4) {
                Text("Ontgrendelen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        },
    )
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = Ink.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, color = Ink.Text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink.Text, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, color = Ink.Subtle, style = MaterialTheme.typography.bodySmall)
        }
        if (badge != null) Pill(badge, color = Ink.Warning)
        else Icon(Icons.Filled.Check, contentDescription = null, tint = Ink.Subtle, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingControl(entry: SettingEntry, onChange: (JsonElement) -> Unit) {
    when (entry.type) {
        "BOOL" -> Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(entry.label, color = Ink.Text, style = MaterialTheme.typography.titleSmall)
                if (entry.description.isNotBlank()) {
                    Text(entry.description, color = Ink.Subtle, style = MaterialTheme.typography.bodySmall)
                }
                SettingBadges(entry)
            }
            Switch(
                checked = entry.value?.jsonPrimitive?.booleanOrNull == true,
                onCheckedChange = { onChange(JsonPrimitive(it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Ink.OnAccent,
                    checkedTrackColor = Ink.Accent,
                    uncheckedThumbColor = Ink.Subtle,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.08f),
                ),
            )
        }
        "CHOICE" -> Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(entry.label, color = Ink.Text, style = MaterialTheme.typography.titleSmall)
            if (entry.description.isNotBlank()) {
                Text(entry.description, color = Ink.Subtle, style = MaterialTheme.typography.bodySmall)
            }
            SettingBadges(entry)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                entry.options.forEach { option ->
                    FilterChip(
                        selected = entry.value?.jsonPrimitive?.contentOrNull == option.value,
                        onClick = { onChange(JsonPrimitive(option.value)) },
                        label = { Text(option.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Ink.Accent.copy(alpha = 0.22f),
                            selectedLabelColor = Ink.Accent,
                            labelColor = Ink.Muted,
                        ),
                    )
                }
            }
        }
        "NUMBER" -> {
            val current = entry.value?.jsonPrimitive?.doubleOrNull ?: (entry.min ?: 0.0)
            var local by remember(entry.path, current) { mutableStateOf(current.toFloat()) }
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.label, color = Ink.Text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text("${local.toInt()}", color = Ink.Accent, style = MaterialTheme.typography.titleSmall)
                }
                if (entry.description.isNotBlank()) {
                    Text(entry.description, color = Ink.Subtle, style = MaterialTheme.typography.bodySmall)
                }
                SettingBadges(entry)
                val min = (entry.min ?: 0.0).toFloat()
                val max = (entry.max ?: 100.0).toFloat().coerceAtLeast(min + 1f)
                Slider(
                    value = local.coerceIn(min, max),
                    onValueChange = { local = it },
                    valueRange = min..max,
                    onValueChangeFinished = { onChange(JsonPrimitive(local.toInt())) },
                    colors = SliderDefaults.colors(
                        thumbColor = Ink.Accent,
                        activeTrackColor = Ink.Accent,
                        inactiveTrackColor = Ink.Border,
                    ),
                )
            }
        }
        else -> Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(entry.label, color = Ink.Text, modifier = Modifier.weight(1f))
            Text(entry.value?.jsonPrimitive?.contentOrNull.orEmpty(), color = Ink.Muted)
        }
    }
}

@Composable
private fun SettingBadges(entry: SettingEntry) {
    if (!entry.requiresRestart && !entry.dangerous) return
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (entry.requiresRestart) Pill("Herstart", color = Ink.Warning)
        if (entry.dangerous) Pill("Gevorderd", color = Ink.Danger)
    }
}

@Composable
fun ProfilesScreen(vm: AppViewModel) {
    val snapshot = vm.snapshot
    var photoTarget by remember { mutableStateOf<String?>(null) }
    var editor by remember { mutableStateOf<ProfileEditorTarget?>(null) }
    var deleting by remember { mutableStateOf<ProfileInfo?>(null) }
    var pinRequest by remember { mutableStateOf<ProfilePinRequest?>(null) }
    var verified by remember { mutableStateOf<Pair<String, String>?>(null) }
    var photoProfile by remember { mutableStateOf<ProfileInfo?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val target = photoTarget
        if (uri != null && target != null) vm.uploadProfilePhoto(target, uri)
    }
    val atLimit = snapshot.profiles.size >= MAX_PROFILES
    Column(Modifier.fillMaxSize()) {
        com.enigmacast.companion.ui.components.TopBar(
            "Profielen",
            snapshot.tvName.ifBlank { "Kies wie er kijkt" },
            onBack = { vm.open(AppScreen.SETTINGS) },
        )
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (snapshot.profiles.isEmpty()) {
                Text("De tv stuurt nog geen profielen.", color = Ink.Muted)
            }
            snapshot.profiles.forEachIndexed { index, profile ->
                ProfileRow(
                    profile = profile,
                    active = profile.id == snapshot.activeProfileId,
                    index = index,
                    canDelete = snapshot.profiles.size > 1 && profile.id != snapshot.activeProfileId,
                    onSelect = {
                        if (profile.hasPin) {
                            pinRequest = ProfilePinRequest(profile, ProfilePinAction.SELECT)
                        } else {
                            vm.selectProfile(profile.id)
                        }
                    },
                    onEdit = {
                        if (profile.hasPin) {
                            pinRequest = ProfilePinRequest(profile, ProfilePinAction.EDIT)
                        } else {
                            editor = ProfileEditorTarget.Edit(profile)
                        }
                    },
                    onDelete = {
                        if (profile.hasPin) {
                            pinRequest = ProfilePinRequest(profile, ProfilePinAction.DELETE)
                        } else {
                            deleting = profile
                        }
                    },
                    onClearPhoto = {
                        if (profile.hasPin) {
                            pinRequest = ProfilePinRequest(profile, ProfilePinAction.CLEAR_PHOTO)
                        } else {
                            vm.clearProfilePhoto(profile.id, profile.avatar)
                        }
                    },
                    onPickPhoto = {
                        photoTarget = profile.id
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onTestPhoto = { vm.uploadTestPhoto(profile.id) },
                    onViewPhoto = {
                        photoProfile = profile
                        vm.loadProfilePhoto(profile.id)
                    },
                )
            }
            if (vm.photoBusy) {
                Text("Foto uploaden…", color = Ink.Accent, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            GhostButton(
                "Profiel toevoegen",
                onClick = { editor = ProfileEditorTarget.Create },
                enabled = !atLimit,
            )
            Text(
                if (atLimit) {
                    "Maximaal $MAX_PROFILES profielen."
                } else {
                    "Foto's worden op de tv opgeslagen (vierkant, zonder EXIF)."
                },
                color = Ink.Subtle,
                fontSize = 11.sp,
            )
        }
    }

    editor?.let { target ->
        val existing = (target as? ProfileEditorTarget.Edit)?.profile
        ProfileEditorDialog(
            profile = existing,
            onDismiss = { editor = null },
            onSave = { name, avatar, clearPhoto ->
                val pin = verified?.takeIf { it.first == existing?.id }?.second
                if (existing == null) {
                    vm.createProfile(name, avatar)
                } else {
                    vm.updateProfile(existing.id, name, avatar, clearPhoto, pin)
                }
                editor = null
                verified = null
            },
        )
    }

    deleting?.let { profile ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Profiel verwijderen") },
            text = {
                Text(
                    "${profile.name} verwijderen? De kijkgeschiedenis en favorieten van dit profiel " +
                        "worden ook opgeruimd. Dit kan niet ongedaan worden gemaakt.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val pin = verified?.takeIf { it.first == profile.id }?.second
                    vm.deleteProfile(profile.id, pin)
                    deleting = null
                    verified = null
                }) { Text("Verwijderen", color = Ink.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Annuleren") }
            },
        )
    }

    pinRequest?.let { request ->
        ProfilePinDialog(
            profile = request.profile,
            onDismiss = { pinRequest = null },
            onConfirm = { pin ->
                when (request.action) {
                    ProfilePinAction.SELECT -> vm.selectProfile(request.profile.id, pin)
                    ProfilePinAction.EDIT -> {
                        verified = request.profile.id to pin
                        editor = ProfileEditorTarget.Edit(request.profile)
                    }
                    ProfilePinAction.DELETE -> {
                        verified = request.profile.id to pin
                        deleting = request.profile
                    }
                    ProfilePinAction.CLEAR_PHOTO -> {
                        vm.clearProfilePhoto(request.profile.id, request.profile.avatar, pin)
                    }
                }
                pinRequest = null
            },
        )
    }

    photoProfile?.let { profile ->
        ProfilePhotoDialog(
            name = profile.name,
            preview = vm.photoPreview,
            loading = vm.photoPreviewLoading,
            error = vm.photoPreviewError,
            onDismiss = {
                photoProfile = null
                vm.clearPhotoPreview()
            },
        )
    }
}

private const val MAX_PROFILES = 6

private enum class ProfilePinAction { SELECT, EDIT, DELETE, CLEAR_PHOTO }

private data class ProfilePinRequest(val profile: ProfileInfo, val action: ProfilePinAction)

private sealed interface ProfileEditorTarget {
    data object Create : ProfileEditorTarget
    data class Edit(val profile: ProfileInfo) : ProfileEditorTarget
}

@Composable
private fun ProfileRow(
    profile: ProfileInfo,
    active: Boolean,
    index: Int,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClearPhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onTestPhoto: () -> Unit,
    onViewPhoto: () -> Unit,
) {
    val colors = listOf(Ink.Accent, Ink.Highlight, Ink.Success, Ink.Warning, Color(0xFFB1A4FF), Ink.Danger)
    GlassCard(
        Modifier
            .fillMaxWidth()
            .then(if (active) Modifier.border(1.dp, Ink.Accent.copy(alpha = 0.5f), RoundedCornerShape(24.dp)) else Modifier)
            .clickable(onClick = onSelect),
        padding = 14.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(colors[index % colors.size], colors[index % colors.size].copy(alpha = 0.45f)))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    profile.name.firstOrNull()?.uppercase() ?: "?",
                    color = Ink.OnAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(profile.name, color = Ink.Text, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (profile.photoStamp > 0) "Eigen foto" else "Avatar ${profile.avatar + 1}",
                    color = Ink.Subtle,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (profile.hasPin) Pill("PIN", color = Ink.Warning)
            if (active) {
                Spacer(Modifier.width(6.dp))
                Pill("Actief", color = Ink.Success, dot = true)
            }
        }
        Spacer(Modifier.height(6.dp))
        if (profile.hasPin) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Foto's beheren via de tv (profiel-PIN).", color = Ink.Subtle, style = MaterialTheme.typography.bodySmall)
                if (profile.photoStamp > 0) {
                    TextButton(onClick = onViewPhoto) {
                        Text("Bekijk foto", color = Ink.Accent, fontSize = 12.sp)
                    }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onPickPhoto) {
                    Text("Foto kiezen", color = Ink.Accent, fontSize = 12.sp)
                }
                TextButton(onClick = onTestPhoto) {
                    Text("Testfoto", color = Ink.Muted, fontSize = 12.sp)
                }
                if (profile.photoStamp > 0) {
                    TextButton(onClick = onViewPhoto) {
                        Text("Bekijk foto", color = Ink.Muted, fontSize = 12.sp)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onEdit) {
                Text("Bewerken", color = Ink.Muted, fontSize = 12.sp)
            }
            TextButton(onClick = onDelete, enabled = canDelete) {
                Text("Verwijderen", color = if (canDelete) Ink.Danger else Ink.Subtle, fontSize = 12.sp)
            }
            if (profile.photoStamp > 0 && !profile.hasPin) {
                TextButton(onClick = onClearPhoto) {
                    Text("Foto wissen", color = Ink.Muted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ProfileEditorDialog(
    profile: ProfileInfo?,
    onDismiss: () -> Unit,
    onSave: (String, Int, Boolean) -> Unit,
) {
    var name by remember(profile) { mutableStateOf(profile?.name.orEmpty()) }
    var avatar by remember(profile) { mutableStateOf(profile?.avatar ?: 0) }
    var clearPhoto by remember(profile) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (profile == null) "Nieuw profiel" else "Profiel bewerken") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(20) },
                    label = { Text("Naam") },
                    singleLine = true,
                )
                Text("Avatar", color = Ink.Muted, style = MaterialTheme.typography.labelLarge)
                ProfileAvatarGrid(selected = avatar, onSelect = { avatar = it })
                if (profile != null && profile.photoStamp > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = clearPhoto, onCheckedChange = { clearPhoto = it })
                        Text("Eigen foto verwijderen", color = Ink.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), avatar, clearPhoto) },
                enabled = name.trim().isNotEmpty(),
            ) { Text("Opslaan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        },
    )
}

@Composable
private fun ProfileAvatarGrid(selected: Int, onSelect: (Int) -> Unit) {
    val colors = listOf(
        Ink.Accent, Ink.Highlight, Ink.Success, Ink.Warning,
        Color(0xFFB1A4FF), Ink.Danger, Color(0xFF58D6C8), Color(0xFFF5A3C7),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(4) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(4) { column ->
                    val index = row * 4 + column
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(colors[index % colors.size])
                            .border(
                                if (index == selected) 3.dp else 1.dp,
                                if (index == selected) Ink.Text else Ink.Border,
                                CircleShape,
                            )
                            .clickable { onSelect(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${index + 1}", color = Ink.OnAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfilePhotoDialog(
    name: String,
    preview: ImageBitmap?,
    loading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Foto van $name") },
        text = {
            Box(
                Modifier.fillMaxWidth().height(240.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    loading -> CircularProgressIndicator(color = Ink.Accent)
                    preview != null -> Image(
                        bitmap = preview,
                        contentDescription = "Profielfoto van $name",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                    error != null -> Text(error, color = Ink.Danger, style = MaterialTheme.typography.bodySmall)
                    else -> Text("Geen foto op de tv.", color = Ink.Muted)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Sluiten") }
        },
    )
}

@Composable
private fun ProfilePinDialog(
    profile: ProfileInfo,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pincode") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${profile.name} is met een pincode beveiligd.", color = Ink.Muted)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(4) },
                    label = { Text("Pincode") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pin) }, enabled = pin.length == 4) {
                Text("Ontgrendelen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        },
    )
}

@Composable
fun FeatureScreen(
    title: String,
    subtitle: String,
    glyph: String,
    body: String,
    badge: String,
    status: LinkStatus,
) {
    Column(Modifier.fillMaxSize()) {
        com.enigmacast.companion.ui.components.TopBar(title, subtitle)
        Column(
            Modifier.fillMaxSize().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Ink.Accent.copy(alpha = 0.10f))
                    .border(1.dp, Ink.Accent.copy(alpha = 0.25f), RoundedCornerShape(30.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(glyph, color = Ink.Accent, fontSize = 36.sp)
            }
            Spacer(Modifier.height(22.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, color = Ink.Text, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(body, color = Ink.Muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Pill(badge, color = Ink.Warning)
            Spacer(Modifier.height(26.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    when (status.state) {
                        LinkState.READY -> "TV verbonden — klaar voor fase 5"
                        else -> "TV niet verbonden"
                    },
                    color = Ink.Subtle,
                    fontSize = 11.sp,
                )
            }
        }
    }
}
