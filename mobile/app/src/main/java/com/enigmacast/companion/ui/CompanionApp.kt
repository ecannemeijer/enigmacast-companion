package com.enigmacast.companion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.enigmacast.companion.data.LinkState
import com.enigmacast.companion.data.LinkStatus
import com.enigmacast.companion.ui.components.CompanionBackground
import com.enigmacast.companion.ui.components.FormSheet
import com.enigmacast.companion.ui.components.MediaInfoDialog
import com.enigmacast.companion.ui.components.StatusDot
import com.enigmacast.companion.ui.components.ToastHost
import com.enigmacast.companion.ui.components.UpdateDialog
import com.enigmacast.companion.ui.screens.DevicesScreen
import com.enigmacast.companion.ui.screens.FeatureScreen
import com.enigmacast.companion.ui.screens.IptvScreen
import com.enigmacast.companion.ui.screens.LibraryScreen
import com.enigmacast.companion.ui.screens.LiveScreen
import com.enigmacast.companion.ui.screens.PairScreen
import com.enigmacast.companion.ui.screens.PlayerScreen
import com.enigmacast.companion.ui.screens.ProfilesScreen
import com.enigmacast.companion.ui.screens.RemoteScreen
import com.enigmacast.companion.ui.screens.ScanScreen
import com.enigmacast.companion.ui.screens.SettingsScreen
import com.enigmacast.companion.ui.screens.WelcomeScreen
import com.enigmacast.companion.ui.screens.XtreamScreen
import com.enigmacast.companion.ui.theme.CompanionTheme
import com.enigmacast.companion.ui.theme.Ink

@Composable
fun CompanionApp(vm: AppViewModel) {
    val link by vm.link.collectAsStateWithLifecycle()
    val haptics = rememberHaptics(vm.haptics)

    CompanionTheme {
        BackHandler(enabled = !vm.screen.isTab && vm.screen != AppScreen.WELCOME) { vm.back() }
        Box(Modifier.fillMaxSize()) {
            CompanionBackground()
            val isPlayer = vm.screen == AppScreen.LIVE_PLAYER
            Column(
                Modifier.fillMaxSize().then(
                    if (isPlayer) Modifier else Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                ),
            ) {
                if (vm.screen == AppScreen.REMOTE && link.state != LinkState.READY) {
                    OfflineBanner(link) { vm.pairedTv?.let(vm::connectTo) }
                }
                Box(Modifier.weight(1f)) {
                    when (vm.screen) {
                        AppScreen.WELCOME -> WelcomeScreen(vm)
                        AppScreen.SCAN -> ScanScreen(vm)
                        AppScreen.PAIR -> PairScreen(vm)
                        AppScreen.DEVICES -> DevicesScreen(vm)
                        AppScreen.REMOTE, AppScreen.PLAYER -> RemoteScreen(
                            vm = vm,
                            link = link,
                            haptics = haptics,
                            onOpenSettings = { vm.open(AppScreen.SETTINGS) },
                            onOpenProfiles = { vm.open(AppScreen.PROFILES) },
                        )
                        AppScreen.SETTINGS -> SettingsScreen(vm, link)
                        AppScreen.PROFILES -> ProfilesScreen(vm)
                        AppScreen.LIVE -> LiveScreen(vm, link)
                        AppScreen.LIVE_PLAYER -> PlayerScreen(vm)
                        AppScreen.IPTV -> IptvScreen(vm)
                        AppScreen.XTREAM -> XtreamScreen(vm)
                        AppScreen.LIBRARY -> LibraryScreen(vm)
                    }
                }
                if (vm.screen.isTab) {
                    BottomBar(current = vm.screen, onSelect = vm::open)
                }
            }
            ToastHost(
                toast = vm.toast,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (vm.screen.isTab) 92.dp else 28.dp),
            )
            if (vm.form != null && link.state == LinkState.READY) FormSheet(vm)
            if (vm.updateAvailable != null) UpdateDialog(vm)
            if (vm.infoItem != null) MediaInfoDialog(vm)
        }
    }
}

@Composable
private fun OfflineBanner(link: LinkStatus, onRetry: () -> Unit) {
    val text = when (link.state) {
        LinkState.CONNECTING -> "Verbinden met de tv…"
        LinkState.OFFLINE -> link.message ?: "De verbinding is weggevallen"
        LinkState.ERROR -> link.message ?: "De tv is niet bereikbaar"
        LinkState.IDLE -> "Niet verbonden"
        LinkState.READY -> return
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(Ink.Danger.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(Ink.Danger, 7.dp)
        Spacer(Modifier.width(8.dp))
        Text(text, color = Ink.Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        if (link.state != LinkState.CONNECTING) {
            TextButton(onClick = onRetry) { Text("Opnieuw", color = Ink.Accent, fontSize = 12.sp) }
        }
    }
}

private data class Tab(val screen: AppScreen, val glyph: String, val label: String)

private val TABS = listOf(
    Tab(AppScreen.REMOTE, "◉", "Remote"),
    Tab(AppScreen.LIVE, "▷", "Live"),
    Tab(AppScreen.IPTV, "☰", "IPTV"),
    Tab(AppScreen.LIBRARY, "★", "Mijn"),
    Tab(AppScreen.SETTINGS, "⋯", "Meer"),
)

@Composable
private fun BottomBar(current: AppScreen, onSelect: (AppScreen) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Ink.Canvas.copy(alpha = 0.96f))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TABS.forEach { tab ->
            val selected = current == tab.screen
            Column(
                Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .clickable { onSelect(tab.screen) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    tab.glyph,
                    color = if (selected) Ink.Accent else Ink.Subtle,
                    fontSize = 18.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    tab.label,
                    color = if (selected) Ink.Accent else Ink.Subtle,
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}
