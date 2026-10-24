package com.enigmacast.companion.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.enigmacast.companion.data.LinkState
import com.enigmacast.companion.data.PairedTv
import com.enigmacast.companion.ui.AppViewModel
import com.enigmacast.companion.ui.components.GhostButton
import com.enigmacast.companion.ui.components.GlassCard
import com.enigmacast.companion.ui.components.Pill
import com.enigmacast.companion.ui.components.PrimaryButton
import com.enigmacast.companion.ui.components.SectionLabel
import com.enigmacast.companion.ui.components.StatusDot
import com.enigmacast.companion.ui.components.TopBar
import com.enigmacast.companion.ui.theme.Ink

@Composable
fun WelcomeScreen(vm: AppViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.widthIn(max = 520.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        Spacer(Modifier.height(48.dp))
        BrandMark()
        Spacer(Modifier.height(28.dp))
        Text(
            "EnigmaCast\nCompanion",
            style = MaterialTheme.typography.displaySmall,
            color = Ink.Text,
            textAlign = TextAlign.Center,
            lineHeight = 40.sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "De afstandsbediening van je tv, in je broekzak. Bedien EnigmaCast, typ op je telefoon en beheer je profielen.",
            style = MaterialTheme.typography.bodyMedium,
            color = Ink.Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        FeatureRow(Icons.Filled.PlayArrow, "Afstandsbediening", "D-pad, OK, terug, media en zappen")
        FeatureRow(Icons.Filled.Edit, "Toetsenbord", "Vul wachtwoorden en adressen veilig in")
        FeatureRow(Icons.Filled.Person, "Profielen", "Kies een profiel en zet een foto op de tv")
        Spacer(Modifier.height(36.dp))
        PrimaryButton("TV zoeken", onClick = vm::scan, icon = Icons.Filled.Search)
        Spacer(Modifier.height(10.dp))
        GhostButton("Demo starten", onClick = {
            vm.connectTo(PairedTv(id = "tv-demo", name = "Demo TV", host = "192.168.1.50", online = true))
        })
        Spacer(Modifier.height(32.dp))
        Text(
            "Lokaal netwerk · geen cloud · echte brug of demo",
            color = Ink.Subtle,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Ink.Accent.copy(alpha = 0.10f))
                .border(1.dp, Ink.Accent.copy(alpha = 0.22f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Ink.Accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, color = Ink.Text, style = MaterialTheme.typography.titleSmall)
            Text(body, color = Ink.Subtle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun BrandMark() {
    Canvas(Modifier.size(96.dp)) {
        val corner = 26.dp.toPx()
        drawRoundRect(
            color = Ink.Accent.copy(alpha = 0.12f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
        )
        drawRoundRect(
            color = Ink.Accent.copy(alpha = 0.75f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
            style = Stroke(width = 2.dp.toPx()),
        )
        val play = Path().apply {
            moveTo(size.width * 0.42f, size.height * 0.34f)
            lineTo(size.width * 0.70f, size.height * 0.50f)
            lineTo(size.width * 0.42f, size.height * 0.66f)
            close()
        }
        drawPath(play, Ink.Highlight)
        drawLine(
            color = Ink.Accent.copy(alpha = 0.60f),
            start = Offset(size.width * 0.38f, size.height * 0.97f),
            end = Offset(size.width * 0.62f, size.height * 0.97f),
            strokeWidth = 2.dp.toPx(),
        )
    }
}

@Composable
fun ScanScreen(vm: AppViewModel) {
    var manual by remember { mutableStateOf(false) }
    var denied by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        denied = !granted
        if (granted) vm.startScan()
    }
    val startSearch = {
        if (Build.VERSION.SDK_INT >= 37 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_LOCAL_NETWORK) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        } else {
            denied = false
            vm.startScan()
        }
    }
    LaunchedEffect(Unit) { startSearch() }
    Column(Modifier.fillMaxSize()) {
        TopBar("TV zoeken", "Zelfde netwerk als je telefoon", onBack = vm::back)
        Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            Radar(active = vm.scanning)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (vm.scanning) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(color = Ink.Accent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Zoeken naar EnigmaCast…", color = Ink.Muted, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                if (denied) {
                    Text(
                        "Zonder toestemming voor lokale netwerken kan de app geen tv's vinden. " +
                            "Geef EnigmaCast toestemming of voer het adres handmatig in.",
                        color = Ink.Warning,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                SectionLabel("Gevonden op je netwerk")
                vm.scanResults.forEach { tv ->
                    TvRow(tv) { vm.startPairing(tv) }
                }
                if (vm.scanResults.isEmpty()) {
                    Text(
                        "Geen tv gevonden. Zorg dat EnigmaCast openstaat op de tv en dat je telefoon op hetzelfde wifi zit.",
                        color = Ink.Muted,
                    )
                }
                Spacer(Modifier.height(8.dp))
                GhostButton("Opnieuw zoeken", onClick = startSearch)
                Spacer(Modifier.height(8.dp))
                GhostButton("Adres invoeren", onClick = { manual = true })
                Spacer(Modifier.height(16.dp))
                Text(
                    "De app zoekt _enigmacast._tcp op je netwerk. Werkt dat niet (gastnetwerk, repeater), " +
                        "vul dan het IP-adres van de tv in; de poort is 8750.",
                    color = Ink.Subtle,
                    fontSize = 11.sp,
                )
            }
        }
    }
    if (manual) {
        ManualAddressDialog(
            onDismiss = { manual = false },
            onConfirm = { host, port ->
                manual = false
                vm.pairManually(host, port)
            },
        )
    }
}

@Composable
private fun ManualAddressDialog(onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8750") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adres van de tv") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Vul het IP-adres van de tv in. Je vindt het op de tv onder Instellingen › Ontvangers.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("IP-adres") },
                    placeholder = { Text("192.168.1.50") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter(Char::isDigit).take(5) },
                    label = { Text("Poort") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(host, port.toIntOrNull() ?: 0) },
                enabled = host.isNotBlank() && (port.toIntOrNull() ?: 0) in 1..65535,
            ) { Text("Doorgaan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        },
    )
}

@Composable
private fun Radar(active: Boolean) {
    val transition = rememberInfiniteTransition(label = "radar")
    val pulse by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_600), RepeatMode.Restart),
        label = "pulse",
    )
    val alpha = if (active) 1f - pulse else 0.25f
    Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.minDimension / 2
            for (ring in 1..3) {
                drawCircle(
                    color = Ink.Accent.copy(alpha = 0.10f),
                    radius = maxRadius * ring / 3,
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            if (active) {
                drawCircle(
                    color = Ink.Accent.copy(alpha = alpha * 0.35f),
                    radius = maxRadius * pulse,
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
            drawCircle(color = Ink.Accent, radius = 5.dp.toPx(), center = center)
        }
        Text(
            if (active) "…" else "✓",
            color = Ink.Highlight,
            fontSize = 24.sp,
            modifier = Modifier.padding(top = 90.dp),
        )
    }
}

@Composable
private fun TvRow(tv: PairedTv, onClick: () -> Unit) {
    GlassCard(
        Modifier.fillMaxWidth().then(if (tv.online) Modifier.clickable(onClick = onClick) else Modifier),
        padding = 14.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("▭", color = if (tv.online) Ink.Accent else Ink.Subtle, fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tv.name, color = Ink.Text, style = MaterialTheme.typography.titleSmall)
                Text("${tv.host}:${tv.port}", color = Ink.Subtle, style = MaterialTheme.typography.bodySmall)
            }
            Pill(
                if (tv.online) "Gevonden" else "Offline",
                color = if (tv.online) Ink.Success else Ink.Subtle,
                dot = true,
            )
        }
    }
}

@Composable
fun PairScreen(vm: AppViewModel) {
    val tv = vm.pendingTv
    var code by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        TopBar("Koppelen", tv?.name, onBack = {
            vm.clearPairError()
            vm.open(com.enigmacast.companion.ui.AppScreen.SCAN)
        })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (tv?.real == true) {
                    "Voer de 6-cijferige code in die op de tv staat (Instellingen › Telefoon)."
                } else {
                    "Demo: voer 123456 in. 000000 wordt geweigerd."
                },
                color = Ink.Muted,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(6) { index ->
                    val filled = index < code.length
                    Box(
                        Modifier
                            .size(width = 44.dp, height = 56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (filled) Ink.Accent.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f))
                            .border(
                                1.dp,
                                if (filled) Ink.Accent.copy(alpha = 0.6f) else Ink.Border,
                                RoundedCornerShape(14.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (filled) code[index].toString() else "",
                            color = Ink.Text,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            vm.pairError?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = Ink.Danger, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(24.dp))
            PairKeypad(
                onDigit = { if (code.length < 6) code += it },
                onBackspace = { if (code.isNotEmpty()) code = code.dropLast(1) },
            )
            Spacer(Modifier.height(20.dp))
            if (vm.pairingBusy) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Ink.Accent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Koppelen…", color = Ink.Muted, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                PrimaryButton(
                    "Koppelen",
                    onClick = { vm.pair(code) },
                    enabled = code.length == 6,
                    icon = Icons.Filled.Check,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (tv?.real == true) {
                    "De tv toont hierna dezelfde certificaatpin. Klopt die niet, dan weigert de app de verbinding."
                } else {
                    "Dit is de demo-brug: de code wordt lokaal gecontroleerd."
                },
                color = Ink.Subtle,
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PairKeypad(onDigit: (String) -> Unit, onBackspace: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in 0 until 4) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (column in 0 until 3) {
                    val index = row * 3 + column
                    when (index) {
                        9 -> KeypadKey("", enabled = false) {}
                        10 -> KeypadKey("0") { onDigit("0") }
                        11 -> KeypadKey("⌫") { onBackspace() }
                        else -> KeypadKey((index + 1).toString()) { onDigit((index + 1).toString()) }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(width = 78.dp, height = 52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.06f),
        contentColor = Ink.Text,
        border = androidx.compose.foundation.BorderStroke(1.dp, Ink.Border),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun DevicesScreen(vm: AppViewModel) {
    Column(Modifier.fillMaxSize()) {
        TopBar("Apparaten", "Gekoppelde tv's", onBack = vm::back)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            vm.pairedTv?.let { tv ->
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(tv.name, color = Ink.Text, style = MaterialTheme.typography.titleMedium)
                            Text("${tv.host}:${tv.port}", color = Ink.Subtle, style = MaterialTheme.typography.bodySmall)
                        }
                        Pill(
                            if (tv.real) "Echte brug" else "Demo",
                            color = if (tv.real) Ink.Success else Ink.Warning,
                            dot = true,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    if (tv.real) {
                        Text("Certificaatpin", color = Ink.Muted, style = MaterialTheme.typography.bodySmall)
                        Text(
                            tv.pin.ifBlank { "—" },
                            color = Ink.Subtle,
                            fontSize = 11.sp,
                            maxLines = 2,
                        )
                        Text(
                            "Dezelfde pin staat op de tv. Klopt hij niet, verbreek dan de koppeling en probeer opnieuw.",
                            color = Ink.Subtle,
                            fontSize = 11.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GhostButton("Opnieuw verbinden", onClick = vm::reconnect, modifier = Modifier.weight(1f))
                        GhostButton("Vergeten", onClick = vm::forgetTv, modifier = Modifier.weight(1f))
                    }
                }
            } ?: Text("Nog geen tv gekoppeld.", color = Ink.Muted)
            Spacer(Modifier.height(4.dp))
            PrimaryButton("Nieuwe tv zoeken", onClick = vm::scan, icon = Icons.Filled.Search)
            Text(
                "De telefoon bewaart alleen het token (versleuteld) en de publieke pin. De private sleutel " +
                    "blijft in de Android Keystore.",
                color = Ink.Subtle,
                fontSize = 11.sp,
            )
        }
    }
}
