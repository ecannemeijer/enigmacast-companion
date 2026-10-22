package com.enigmacast.companion.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.compose.PlayerSurface
import com.enigmacast.companion.ui.AppViewModel
import com.enigmacast.companion.ui.components.ChannelLogo
import com.enigmacast.companion.ui.components.Pill
import com.enigmacast.companion.ui.theme.Ink
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(vm: AppViewModel) {
    BackHandler { vm.closePlayer() }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (vm.playbackUrl.isBlank()) {
            Text("Geen stream gekozen.", color = Ink.Muted, modifier = Modifier.align(Alignment.Center))
        } else {
            StreamPlayer(vm)
        }
    }
}

@Composable
private fun StreamPlayer(vm: AppViewModel) {
    val context = LocalContext.current
    val player = remember {
        val renderers = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
        ExoPlayer.Builder(context, renderers).build()
    }
    var playing by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var videoAspect by remember { mutableFloatStateOf(16f / 9f) }
    var showChannels by remember { mutableStateOf(false) }
    var osdVisible by remember { mutableStateOf(true) }
    var osdTick by remember { mutableIntStateOf(0) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var seeking by remember { mutableStateOf(false) }
    var seekPositionMs by remember { mutableLongStateOf(0L) }

    fun revealOsd() {
        osdVisible = true
        osdTick++
    }

    fun closeWithProgress() {
        runCatching { vm.recordProgress(player.currentPosition, player.duration, force = true) }
        vm.closePlayer()
    }

    fun seekTo(position: Long) {
        val duration = durationMs.coerceAtLeast(0L)
        val target = position.coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)
        player.seekTo(target)
        positionMs = target
        revealOsd()
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (playing && !isPlaying) {
                    runCatching { vm.recordProgress(player.currentPosition, player.duration, force = true) }
                    revealOsd()
                }
                playing = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_ENDED) {
                    runCatching { vm.recordProgress(player.duration, player.duration, force = true) }
                    revealOsd()
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.height > 0) {
                    val ratio = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                    if (ratio.isFinite() && ratio > 0f) videoAspect = ratio
                }
            }

            override fun onPlayerError(playerError: PlaybackException) {
                error = "${playerError.errorCodeName}: ${playerError.message.orEmpty()}"
            }
        }
        player.addListener(listener)
        onDispose {
            runCatching { vm.recordProgress(player.currentPosition, player.duration, force = true) }
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            delay(5_000)
            runCatching { vm.recordProgress(player.currentPosition, player.duration) }
        }
    }

    LaunchedEffect(player) {
        while (true) {
            if (!seeking) {
                positionMs = player.currentPosition.coerceAtLeast(0L)
                durationMs = player.duration.takeIf { it > 0 } ?: 0L
            }
            delay(500)
        }
    }

    LaunchedEffect(osdVisible, osdTick, playing, buffering) {
        if (!osdVisible || !playing || buffering) return@LaunchedEffect
        delay(5_000)
        osdVisible = false
    }

    DisposableEffect(context) {
        val activity = context.findActivity()
        val window = activity?.window
        val previousOrientation = activity?.requestedOrientation
        val previousStatusColor = window?.statusBarColor
        val previousNavigationColor = window?.navigationBarColor
        val previousCutout = if (window != null && Build.VERSION.SDK_INT >= 28) {
            window.attributes.layoutInDisplayCutoutMode
        } else {
            0
        }
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            if (Build.VERSION.SDK_INT >= 28) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            controller?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            if (window != null) {
                controller?.show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
                previousStatusColor?.let { window.statusBarColor = it }
                previousNavigationColor?.let { window.navigationBarColor = it }
                if (Build.VERSION.SDK_INT >= 28) {
                    window.attributes = window.attributes.apply {
                        layoutInDisplayCutoutMode = previousCutout
                    }
                }
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            activity?.requestedOrientation =
                previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(vm.playbackUrl, vm.playbackStartMs) {
        if (vm.playbackUrl.isBlank()) return@LaunchedEffect
        error = null
        val dataSource = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(vm.playbackHeaders)
            .setAllowCrossProtocolRedirects(true)
        val source = DefaultMediaSourceFactory(dataSource)
            .createMediaSource(MediaItem.fromUri(vm.playbackUrl))
        player.setMediaSource(source)
        if (vm.playbackStartMs > 0) player.seekTo(vm.playbackStartMs)
        player.prepare()
        player.playWhenReady = true
    }

    val isLive = vm.playbackLive ||
        (vm.playbackRef?.kind != "m" &&
            vm.playbackRef?.kind != "e" &&
            vm.playbackSource != "recording" &&
            durationMs <= 0L)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val screenAspect = if (maxHeight.value > 0f) maxWidth.value / maxHeight.value else 16f / 9f
            val safeAspect = videoAspect.coerceIn(0.5f, 3.5f)
            val videoModifier = if (screenAspect > safeAspect) {
                Modifier.fillMaxHeight().aspectRatio(safeAspect)
            } else {
                Modifier.fillMaxWidth().aspectRatio(safeAspect)
            }
            PlayerSurface(player, videoModifier)
        }

        // Full-screen tap target: tap once to reveal the OSD, tap again to hide it.
        Box(
            Modifier
                .fillMaxSize()
                .clickable {
                    if (osdVisible) osdVisible = false else revealOsd()
                },
        )

        if (isLive && vm.playerQueue.size > 1 && showChannels && osdVisible) {
            Column(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(300.dp)
                    .background(Color.Black.copy(alpha = 0.78f))
                    .padding(top = 64.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Zenders (${vm.playerQueue.size})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { showChannels = false; revealOsd() }) {
                        Text("×", color = Color.White, fontSize = 22.sp)
                    }
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(vm.playerQueue, key = { _, channel -> channel.id }) { index, channel ->
                        val selected = index == vm.playerQueueIndex
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(if (selected) Ink.Accent.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { vm.playPlayerChannel(index); revealOsd() }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ChannelLogo(channel.logo, channel.title, size = 32.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                channel.title,
                                color = if (selected) Ink.Accent else Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        if (osdVisible) {
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                    )
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = ::closeWithProgress) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Terug",
                        tint = Color.White,
                    )
                }
                Text(
                    if (isLive) "LIVE" else "VOD",
                    color = if (isLive) Ink.Success else Ink.Accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.width(8.dp))
                Text(vm.playbackTitle, color = Color.White, fontSize = 13.sp)
                if (isLive && vm.playerQueue.size > 1) {
                    IconButton(onClick = { showChannels = !showChannels; revealOsd() }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Zenderlijst", tint = Color.White)
                    }
                }
            }

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                if (!isLive && durationMs > 0) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(clock(if (seeking) seekPositionMs else positionMs), color = Color.White, fontSize = 12.sp)
                        Slider(
                            value = (if (seeking) seekPositionMs else positionMs).coerceIn(0L, durationMs).toFloat(),
                            onValueChange = {
                                seeking = true
                                seekPositionMs = it.toLong()
                                revealOsd()
                            },
                            onValueChangeFinished = {
                                seeking = false
                                seekTo(seekPositionMs)
                            },
                            valueRange = 0f..durationMs.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = Ink.Accent,
                                activeTrackColor = Ink.Accent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                            ),
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        )
                        Text(clock(durationMs), color = Color.White, fontSize = 12.sp)
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { seekTo(positionMs - 10_000) }) {
                            Text("−10 s", color = Color.White, fontSize = 13.sp)
                        }
                        TextButton(onClick = { seekTo(positionMs + 30_000) }) {
                            Text("+30 s", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = {
                        if (player.isPlaying) player.pause() else player.play()
                        revealOsd()
                    }) {
                        Text(if (playing) "❚❚" else "▶", color = Color.White, fontSize = 20.sp)
                    }
                    val favorite = vm.isFavorite(vm.playbackUrl)
                    TextButton(onClick = { vm.toggleFavorite(); revealOsd() }) {
                        Text(
                            if (favorite) "★" else "☆",
                            color = if (favorite) Ink.Warning else Color.White,
                            fontSize = 20.sp,
                        )
                    }
                    Text(
                        when {
                            buffering -> "Laden…"
                            playing -> "Speelt"
                            else -> "Gepauzeerd"
                        },
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.weight(1f))
                    Pill(
                        if (isLive) "LIVE" else "VOD",
                        color = if (playing) Ink.Success else Ink.Warning,
                        dot = true,
                    )
                }
            }
        }

        error?.let { message ->
            Column(
                Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Stream niet afspeelbaar", color = Ink.Danger, style = MaterialTheme.typography.titleMedium)
                Text(message, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                Text(
                    "Controleer het adres, de poorten en of de ontvanger de stream toestaat.",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun clock(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val total = ms / 1000
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
