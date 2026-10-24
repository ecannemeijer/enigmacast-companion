package com.enigmacast.companion.ui

import android.app.Application
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enigmacast.companion.CompanionApplication
import com.enigmacast.companion.BuildConfig
import com.enigmacast.companion.data.CompanionConnection
import com.enigmacast.companion.data.ControlMode
import com.enigmacast.companion.data.LinkStatus
import com.enigmacast.companion.data.MockBridge
import com.enigmacast.companion.data.PairedTv
import com.enigmacast.companion.data.TvSnapshot
import com.enigmacast.companion.data.live.PlayerChannel
import com.enigmacast.companion.data.library.LibraryItem
import com.enigmacast.companion.data.net.PairingResult
import com.enigmacast.companion.data.tmdb.TmdbInfo
import com.enigmacast.companion.data.update.RemoteRelease
import com.enigmacast.companion.data.update.isNewerRelease
import com.enigmacast.companion.protocol.Envelope
import com.enigmacast.companion.protocol.LibraryAction
import com.enigmacast.companion.protocol.LibraryCommand
import com.enigmacast.companion.protocol.LibraryEntry
import com.enigmacast.companion.protocol.LibraryStatePayload
import com.enigmacast.companion.protocol.RecentChannel
import com.enigmacast.companion.protocol.RecentCommand
import com.enigmacast.companion.protocol.RecentStatePayload
import com.enigmacast.companion.protocol.RecordingEntry
import com.enigmacast.companion.protocol.SyncCommand
import com.enigmacast.domain.model.BouquetEntry
import com.enigmacast.domain.model.ReceiverConfig
import com.enigmacast.domain.settingsimport.M3uEntry
import com.enigmacast.domain.tuner.NowEvent
import com.enigmacast.domain.xtream.Catchup
import com.enigmacast.domain.xtream.airingAt
import com.enigmacast.domain.xtream.XtreamAccount
import com.enigmacast.domain.xtream.XtreamCategory
import com.enigmacast.domain.xtream.XtreamEpisode
import com.enigmacast.domain.xtream.XtreamEpg
import com.enigmacast.domain.xtream.XtreamItem
import com.enigmacast.domain.xtream.XtreamSection
import com.enigmacast.companion.protocol.FormDescriptor
import com.enigmacast.companion.protocol.FormSubmitCommand
import com.enigmacast.companion.protocol.HelloPayload
import com.enigmacast.companion.protocol.IptvProviderPayload
import com.enigmacast.companion.protocol.IptvSourcePayload
import com.enigmacast.companion.protocol.KeyCommand
import com.enigmacast.companion.protocol.KeyPhase
import com.enigmacast.companion.protocol.MessageType
import com.enigmacast.companion.protocol.NavCommand
import com.enigmacast.companion.protocol.NavTarget
import com.enigmacast.companion.protocol.PlayerCommand
import com.enigmacast.companion.protocol.PlayerCommandAction
import com.enigmacast.companion.protocol.PowerCommand
import com.enigmacast.companion.protocol.PowerCommandAction
import com.enigmacast.companion.protocol.ProfileCommand
import com.enigmacast.companion.protocol.ProfileCommandAction
import com.enigmacast.companion.protocol.ProtocolError
import com.enigmacast.companion.protocol.ProtocolJson
import com.enigmacast.companion.protocol.RemoteKey
import com.enigmacast.companion.protocol.SettingsCatalogPayload
import com.enigmacast.companion.protocol.SettingsCommand
import com.enigmacast.companion.protocol.SettingsPatchOp
import com.enigmacast.companion.protocol.StatePlayerPayload
import com.enigmacast.companion.protocol.StateProfilesPayload
import com.enigmacast.companion.protocol.ToastPayload
import com.enigmacast.companion.protocol.VolumeCommand
import com.enigmacast.companion.protocol.VolumeTarget
import com.enigmacast.companion.protocol.ZapCommand
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement

data class UiToast(val message: String, val level: String = "info", val token: Long = 0)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as CompanionApplication).container
    private val store = container.store
    private val mock = container.mock
    private val real = container.real
    private val updates = container.updates

    /** Naar welke verbinding de UI luistert: de echte brug of de demo. */
    private val active = MutableStateFlow<CompanionConnection>(mock)

    val link: StateFlow<LinkStatus> = active
        .flatMapLatest { it.link }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LinkStatus())

    var screen by mutableStateOf(if (store.pairedTv() != null) AppScreen.REMOTE else AppScreen.WELCOME)
        private set
    var pairedTv by mutableStateOf(store.pairedTv())
        private set
    var pendingTv by mutableStateOf<PairedTv?>(null)
        private set
    var snapshot by mutableStateOf(TvSnapshot())
        private set
    var scanning by mutableStateOf(false)
        private set
    var scanResults by mutableStateOf<List<PairedTv>>(emptyList())
        private set
    var pairingBusy by mutableStateOf(false)
        private set
    var pairError by mutableStateOf<String?>(null)
        private set
    var form by mutableStateOf<FormDescriptor?>(null)
        private set
    var formValues by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var settingsCatalog by mutableStateOf<SettingsCatalogPayload?>(null)
        private set
    var photoBusy by mutableStateOf(false)
        private set
    var photoPreview by mutableStateOf<ImageBitmap?>(null)
        private set
    var photoPreviewLoading by mutableStateOf(false)
        private set
    var photoPreviewError by mutableStateOf<String?>(null)
        private set
    var liveReceiver by mutableStateOf(store.liveReceiver())
        private set
    var liveBouquets by mutableStateOf<List<BouquetEntry>>(emptyList())
        private set
    var liveBouquet by mutableStateOf<BouquetEntry?>(null)
        private set
    var liveChannels by mutableStateOf<List<BouquetEntry>>(emptyList())
        private set
    var liveNow by mutableStateOf<Map<String, NowEvent>>(emptyMap())
        private set
    var liveLoading by mutableStateOf(false)
        private set
    var liveError by mutableStateOf<String?>(null)
        private set
    var recentTv by mutableStateOf<List<RecentChannel>>(emptyList())
        private set
    var recentTvHost by mutableStateOf("")
        private set
    var playbackTitle by mutableStateOf("")
        private set
    var playbackUrl by mutableStateOf("")
        private set
    var playbackHeaders by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var playbackStartMs by mutableStateOf(0L)
        private set
    var iptvUrl by mutableStateOf(store.iptvUrl().orEmpty())
        private set
    var iptvEntries by mutableStateOf<List<M3uEntry>>(emptyList())
        private set
    var iptvGroups by mutableStateOf<List<String>>(emptyList())
        private set
    var iptvGroup by mutableStateOf<String?>(null)
        private set
    var iptvLoading by mutableStateOf(false)
        private set
    var iptvError by mutableStateOf<String?>(null)
        private set
    var iptvFromTv by mutableStateOf(false)
        private set
    var iptvKind by mutableStateOf(store.iptvKind().orEmpty())
        private set
    var iptvProviders by mutableStateOf<List<IptvProviderPayload>>(emptyList())
        private set
    var iptvProviderId by mutableStateOf<String?>(null)
        private set
    val iptvProviderName: String
        get() = iptvProviders.firstOrNull { it.id == iptvProviderId }?.name.orEmpty()
    private var iptvSourceStamp = ""
    var playerQueue by mutableStateOf<List<PlayerChannel>>(emptyList())
        private set
    var playerQueueIndex by mutableStateOf(-1)
        private set
    var infoItem by mutableStateOf<XtreamItem?>(null)
        private set
    var info by mutableStateOf<TmdbInfo?>(null)
        private set
    var infoLoading by mutableStateOf(false)
        private set
    var playbackSource by mutableStateOf("stream")
    var playbackLive by mutableStateOf(false)
        private set
    var playbackReturn by mutableStateOf(AppScreen.LIVE)
        private set
    var playbackRef by mutableStateOf<LibraryRef?>(null)
        private set
    private var lastLibrarySyncMs = 0L
    /** Laatste positie, ook opnieuw verstuurd als de tv even weg was tijdens het stoppen. */
    private var pendingProgress: LibraryCommand? = null
    var library by mutableStateOf(container.library.all())
        private set
    var recordings by mutableStateOf<List<RecordingEntry>>(emptyList())
        private set
    var xtreamServer by mutableStateOf(store.xtreamServer().orEmpty())
        private set
    var xtreamUser by mutableStateOf(store.xtreamUser().orEmpty())
        private set
    var xtreamPassword by mutableStateOf(store.xtreamPassword().orEmpty())
        private set
    var xtreamAccount by mutableStateOf<XtreamAccount?>(null)
        private set
    var xtreamCategories by mutableStateOf<List<XtreamCategory>>(emptyList())
        private set
    var xtreamCategory by mutableStateOf<XtreamCategory?>(null)
        private set
    var xtreamItems by mutableStateOf<List<XtreamItem>>(emptyList())
        private set
    var xtreamNow by mutableStateOf<Map<String, XtreamEpg>>(emptyMap())
        private set
    private val xtreamEpgRequested = mutableSetOf<String>()
    var xtreamSection by mutableStateOf(XtreamSection.LIVE)
        private set
    var xtreamEpisodes by mutableStateOf<List<XtreamEpisode>>(emptyList())
        private set
    var xtreamSeriesTitle by mutableStateOf<String?>(null)
        private set
    var xtreamLoading by mutableStateOf(false)
        private set
    var xtreamError by mutableStateOf<String?>(null)
        private set
    var catchupItem by mutableStateOf<XtreamItem?>(null)
        private set
    var catchupPrograms by mutableStateOf<List<XtreamEpg>>(emptyList())
        private set
    var catchupLoading by mutableStateOf(false)
        private set
    var catchupError by mutableStateOf<String?>(null)
        private set
    var haptics by mutableStateOf(store.haptics)
        private set
    var toast by mutableStateOf<UiToast?>(null)
        private set
    var lastLatencyMs by mutableStateOf<Long?>(null)
        private set
    var lastKeyLabel by mutableStateOf<String?>(null)
        private set
    var showNumbers by mutableStateOf(false)
        private set
    var mode by mutableStateOf(ControlMode.APP)
        private set

    var updateAvailable by mutableStateOf<RemoteRelease?>(null)
        private set
    var updateChecking by mutableStateOf(false)
        private set
    var updateDownloading by mutableStateOf(false)
        private set
    var updateProgress by mutableStateOf(0)
        private set
    var updateError by mutableStateOf<String?>(null)
        private set
    var updateStatus by mutableStateOf<String?>(null)
        private set
    var updateReady by mutableStateOf<File?>(null)
        private set

    private var toastJob: Job? = null
    private var formJob: Job? = null
    private var toastToken = 0L
    private var nextId = 0
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            // Connectivity callbacks run off main; Compose state must be updated there.
            viewModelScope.launch {
                container.iptv.evictConnections()
                container.xtream.evictConnections()
                if (screen == AppScreen.IPTV && iptvEntries.isEmpty() && !iptvLoading && !xtreamLoading) {
                    ensureIptvLoaded()
                }
            }
        }
    }

    init {
        runCatching {
            getApplication<Application>().getSystemService(ConnectivityManager::class.java)
                ?.registerDefaultNetworkCallback(networkCallback)
        }
        viewModelScope.launch {
            active.flatMapLatest { it.events }.collect { envelope ->
                runCatching { handleEvent(envelope) }
                    .onFailure { android.util.Log.w("Companion", "event ${envelope.type} failed", it) }
            }
        }
        real.onHostChanged = { changed ->
            store.savePairedTv(changed)
            pairedTv = changed
        }
        pairedTv?.let { saved -> if (saved.real) useReal(saved) else useMock(saved) }
        viewModelScope.launch { refreshUpdate(auto = true) }
    }

    // ------------------------------------------------------------ updates

    fun checkForUpdates() {
        viewModelScope.launch { refreshUpdate(auto = false) }
    }

    val language: com.enigmacast.companion.data.CompanionLanguage
        get() = com.enigmacast.companion.data.CompanionLanguage.entries
            .firstOrNull { it.tag == store.language() }
            ?: com.enigmacast.companion.data.CompanionLanguage.SYSTEM

    fun setLanguage(language: com.enigmacast.companion.data.CompanionLanguage) {
        store.saveLanguage(language.tag)
    }

    private suspend fun refreshUpdate(auto: Boolean) {
        if (updateChecking) return
        updateChecking = true
        updateError = null
        if (!auto) updateStatus = "Controleren…"
        val release = runCatching { updates.check() }.getOrNull()
        updateChecking = false
        if (release != null && isNewerRelease(BuildConfig.VERSION_CODE, release)) {
            updateAvailable = release
            updateReady = null
            updateProgress = 0
            updateStatus = "Versie ${release.versionName} is beschikbaar"
        } else if (!auto) {
            updateStatus = if (release == null) {
                "Kon de update-server niet bereiken"
            } else {
                "Je hebt de nieuwste versie"
            }
            showToast(updateStatus ?: "", if (release == null) "error" else "ok")
        }
    }

    fun downloadUpdate() {
        val release = updateAvailable ?: return
        if (updateDownloading) return
        updateDownloading = true
        updateProgress = 0
        updateError = null
        updateReady = null
        viewModelScope.launch {
            runCatching {
                updates.download(release) { progress -> updateProgress = progress }
            }.onSuccess { file ->
                updateReady = file
            }.onFailure { failure ->
                updateError = failure.message ?: "Download mislukt"
                showToast(updateError ?: "Download mislukt", "error")
            }
            updateDownloading = false
        }
    }

    fun installUpdate() {
        val file = updateReady ?: return
        if (!updates.canInstall()) {
            updates.openInstallPermission()
            showToast("Sta installatie uit onbekende bronnen toe en tik opnieuw op Installeren", "info")
            return
        }
        runCatching { updates.install(file) }
            .onFailure {
                updateError = it.message ?: "Installatie mislukt"
                showToast(updateError ?: "Installatie mislukt", "error")
            }
    }

    fun dismissUpdate() {
        if (updateDownloading) return
        updateAvailable = null
        updateReady = null
        updateError = null
        updateStatus = null
    }
    // ------------------------------------------------------------ navigatie

    fun open(target: AppScreen) {
        screen = target
        if (target != AppScreen.REMOTE) showNumbers = false
        if (target == AppScreen.LIVE && liveReceiver != null && liveBouquets.isEmpty() && !liveLoading) {
            loadLiveBouquets()
        }
        if (target == AppScreen.SETTINGS && updateAvailable == null && !updateChecking) {
            viewModelScope.launch { refreshUpdate(auto = true) }
        }
        if (target == AppScreen.IPTV && iptvUrl.isNotBlank() && iptvEntries.isEmpty() && !iptvLoading) {
            loadIptv()
        }
        if (target == AppScreen.XTREAM && xtreamAccount == null && xtreamServer.isNotBlank() && xtreamUser.isNotBlank()) {
            loginXtream(xtreamServer, xtreamUser, xtreamPassword)
        }
    }

    fun back() {
        screen = when (screen) {
            AppScreen.SCAN, AppScreen.PAIR, AppScreen.DEVICES -> AppScreen.REMOTE
            AppScreen.PROFILES -> AppScreen.SETTINGS
            AppScreen.PLAYER -> AppScreen.LIVE
            AppScreen.LIVE_PLAYER -> playbackReturn
            AppScreen.XTREAM -> {
                if (xtreamSeriesTitle != null) {
                    closeXtreamSeries()
                    AppScreen.XTREAM
                } else {
                    AppScreen.IPTV
                }
            }
            else -> AppScreen.REMOTE
        }
    }

    // ------------------------------------------------------------ live (Enigma2)

    fun saveLiveReceiver(config: ReceiverConfig) {
        store.saveLiveReceiver(config)
        liveReceiver = config
        liveBouquets = emptyList()
        liveBouquet = null
        liveChannels = emptyList()
        liveNow = emptyMap()
        loadLiveBouquets()
    }

    fun demoLiveReceiver() {
        saveLiveReceiver(
            ReceiverConfig(
                name = "Demo-ontvanger",
                host = "127.0.0.1",
                httpPort = 8090,
                streamingPort = 8081,
            ),
        )
    }

    fun loadLiveBouquets() {
        val config = liveReceiver ?: return
        if (liveLoading) return
        liveLoading = true
        liveError = null
        liveBouquets = emptyList()
        liveBouquet = null
        liveChannels = emptyList()
        liveNow = emptyMap()
        viewModelScope.launch {
            container.live.bouquets(config).fold(
                onSuccess = { list ->
                    liveBouquets = list
                    val first = list.firstOrNull()
                    if (first != null) {
                        selectLiveBouquet(first)
                    } else {
                        liveError = "De ontvanger gaf geen zenderlijsten."
                        liveLoading = false
                    }
                },
                onFailure = {
                    liveError = it.message ?: "Ontvanger niet bereikbaar"
                    liveLoading = false
                },
            )
        }
    }

    fun selectLiveBouquet(bouquet: BouquetEntry) {
        val config = liveReceiver ?: return
        liveBouquet = bouquet
        liveChannels = emptyList()
        liveNow = emptyMap()
        liveLoading = true
        liveError = null
        viewModelScope.launch {
            container.live.services(config, bouquet).fold(
                onSuccess = { liveChannels = it },
                onFailure = { liveError = it.message ?: "Zenders laden mislukt" },
            )
            container.live.now(config, bouquet).onSuccess { events ->
                liveNow = events.associateBy { it.reference }
            }
            liveLoading = false
        }
    }

    fun watchLive(channel: BouquetEntry) {
        val config = liveReceiver ?: return
        playUrl(
            title = channel.name,
            url = container.live.streamUrl(config, channel.reference),
            headers = container.live.streamHeaders(config),
            source = "live",
            returnTo = AppScreen.LIVE,
        )
        if (config.host.isNotBlank() && config.host.equals(recentTvHost, ignoreCase = true)) {
            send(
                ProtocolJson.envelope(
                    MessageType.CMD_RECENT,
                    RecentCommand(
                        receiverHost = config.host,
                        reference = channel.reference,
                        name = channel.name,
                        bouquet = liveBouquet?.reference.orEmpty(),
                    ),
                    id = nextId(),
                ),
            )
        }
    }

    fun watchRecentChannel(channel: RecentChannel) {
        watchLive(BouquetEntry(channel.name.ifBlank { channel.reference }, channel.reference))
    }

    fun playUrl(
        title: String,
        url: String,
        headers: Map<String, String> = emptyMap(),
        startMs: Long = 0L,
        source: String = "stream",
        returnTo: AppScreen = AppScreen.LIVE,
        ref: LibraryRef? = null,
        queue: List<PlayerChannel> = emptyList(),
        queueIndex: Int = -1,
        live: Boolean = source == "live",
    ) {
        playbackTitle = title
        playbackUrl = url
        playbackHeaders = headers
        playbackStartMs = startMs
        playbackSource = source
        playbackLive = live
        playbackReturn = returnTo
        playbackRef = ref
        playerQueue = queue
        playerQueueIndex = queueIndex
        lastLibrarySyncMs = 0L
        screen = AppScreen.LIVE_PLAYER
    }

    fun playPlayerChannel(index: Int) {
        val channel = playerQueue.getOrNull(index) ?: return
        playerQueueIndex = index
        playbackTitle = channel.title
        playbackUrl = channel.url
        playbackHeaders = channel.headers
        playbackStartMs = 0L
        playbackRef = null
        playbackLive = true
    }

    fun closePlayer() {
        screen = playbackReturn
    }

    fun recordProgress(positionMs: Long, durationMs: Long, force: Boolean = false) {
        if (playbackUrl.isBlank()) return
        val position = positionMs.coerceAtLeast(0L)
        val duration = if (durationMs > 0L) durationMs else 0L
        library = container.library.record(
            title = playbackTitle,
            url = playbackUrl,
            positionMs = position,
            durationMs = duration,
            source = playbackSource,
            tvSource = playbackRef?.source,
            tvKind = playbackRef?.kind,
            tvId = playbackRef?.id,
        )
        val ref = playbackRef ?: return
        val now = System.currentTimeMillis()
        if (force || now - lastLibrarySyncMs >= 10_000L) {
            val command = LibraryCommand(
                action = LibraryAction.PROGRESS,
                entry = LibraryEntry(
                    title = playbackTitle,
                    url = playbackUrl,
                    source = ref.source,
                    kind = ref.kind,
                    id = ref.id,
                    positionMs = position,
                    durationMs = duration,
                    favorite = isFavorite(playbackUrl),
                    updatedMs = now,
                ),
            )
            pendingProgress = command
            send(
                ProtocolJson.envelope(
                    MessageType.CMD_LIBRARY,
                    command,
                    id = nextId(),
                ),
            )
            lastLibrarySyncMs = now
        }
    }

    fun toggleFavorite(url: String = playbackUrl) {
        if (url.isBlank()) return
        if (container.library.all().none { it.url == url }) {
            library = container.library.record(playbackTitle, url, 0L, 0L, playbackSource)
        }
        library = container.library.toggleFavorite(url)
        val item = library.firstOrNull { it.url == url } ?: return
        val ref = item.tvSource?.let { source ->
            LibraryRef(source, item.tvKind.orEmpty(), item.tvId.orEmpty())
        } ?: playbackRef?.takeIf { url == playbackUrl }
        if (ref == null) return
        send(
            ProtocolJson.envelope(
                MessageType.CMD_LIBRARY,
                LibraryCommand(
                    action = LibraryAction.FAVORITE,
                    entry = LibraryEntry(
                        title = item.title,
                        url = item.url,
                        source = ref.source,
                        kind = ref.kind,
                        id = ref.id,
                        positionMs = item.positionMs,
                        durationMs = item.durationMs,
                        favorite = item.favorite,
                        updatedMs = System.currentTimeMillis(),
                    ),
                ),
                id = nextId(),
            ),
        )
    }

    fun isFavorite(url: String): Boolean = library.firstOrNull { it.url == url }?.favorite == true

    fun resume(item: LibraryItem) {
        if (item.tvKind == "s") {
            openSeriesFavorite(item)
            return
        }
        viewModelScope.launch {
            val direct = item.url.takeIf {
                (it.startsWith("http://", true) || it.startsWith("https://", true)) &&
                    (item.extension.isNotBlank() || xtreamAccount == null)
            }
            val url = direct ?: buildLibraryUrl(item)
            if (url.isBlank()) {
                showToast("Geen stream-URL voor dit item; open eerst IPTV", "error")
                return@launch
            }
            playUrl(
                title = item.title,
                url = url,
                startMs = item.positionMs,
                source = item.source,
                returnTo = AppScreen.LIBRARY,
                ref = item.tvSource?.let { source ->
                    LibraryRef(source, item.tvKind.orEmpty(), item.tvId.orEmpty())
                },
            )
        }
    }

    private suspend fun buildLibraryUrl(item: LibraryItem): String {
        val account = xtreamAccount ?: return ""
        val id = item.tvId.orEmpty()
        if (id.isBlank()) return ""
        return when (item.tvKind) {
            "m" -> {
                val extension = item.extension.ifBlank {
                    container.xtream.movieExtension(account, id).orEmpty()
                }
                com.enigmacast.domain.xtream.XtreamUrls.movie(account, id, extension)
            }
            "e" -> com.enigmacast.domain.xtream.XtreamUrls.episode(account, id, item.extension)
            else -> ""
        }
    }

    private fun openSeriesFavorite(item: LibraryItem) {
        val account = xtreamAccount
        if (account == null) {
            showToast("Open eerst IPTV om deze serie af te spelen", "info")
            open(AppScreen.IPTV)
            return
        }
        val seriesId = item.tvId.orEmpty()
        if (seriesId.isBlank()) return
        xtreamSection = XtreamSection.SERIES
        xtreamSeriesTitle = item.title
        xtreamEpisodes = emptyList()
        xtreamLoading = true
        xtreamError = null
        screen = AppScreen.XTREAM
        viewModelScope.launch {
            container.xtream.episodes(account, seriesId).fold(
                onSuccess = { xtreamEpisodes = it },
                onFailure = { xtreamError = it.message ?: "Afleveringen laden mislukt" },
            )
            xtreamLoading = false
        }
    }

    fun playRecording(recording: RecordingEntry) {
        val headers = if (recording.authorization.isBlank()) emptyMap() else {
            mapOf("Authorization" to recording.authorization)
        }
        playUrl(
            title = recording.title,
            url = recording.url,
            headers = headers,
            source = "recording",
            returnTo = AppScreen.LIBRARY,
        )
    }

    fun removeFromLibrary(item: LibraryItem) {
        library = container.library.remove(item.url)
    }

    fun syncNow() {
        send(
            ProtocolJson.envelope(
                MessageType.CMD_SYNC,
                SyncCommand(now = true),
                id = nextId(),
            ),
        )
        showToast("Sync met de ontvanger gestart", "ok")
    }

    // ------------------------------------------------------------ IPTV (M3U)

    fun selectIptvProvider(id: String) {
        val provider = iptvProviders.firstOrNull { it.id == id } ?: return
        iptvProviderId = id
        iptvSourceStamp = "${provider.id}|${provider.kind}|${provider.m3uUrl}|${provider.server}|${provider.username}|${provider.password.hashCode()}"
        applyIptvProvider(provider)
    }

    /** Reload the selected provider after a network change or an empty/failed list. */
    fun reloadIptv() {
        val provider = iptvProviders.firstOrNull { it.id == iptvProviderId }
        if (provider != null) {
            applyIptvProvider(provider)
        } else if (iptvKind == "XTREAM" && xtreamServer.isNotBlank() && xtreamUser.isNotBlank()) {
            loginXtream(xtreamServer, xtreamUser, xtreamPassword)
        } else if (iptvUrl.isNotBlank()) {
            loadIptv(iptvUrl)
        }
    }

    private fun applyIptvProvider(provider: IptvProviderPayload) {
        iptvFromTv = true
        iptvKind = provider.kind.uppercase()
        store.saveIptvKind(iptvKind)
        when (iptvKind) {
            "M3U" -> if (provider.m3uUrl.isNotBlank()) {
                iptvUrl = provider.m3uUrl
                store.saveIptvUrl(provider.m3uUrl)
                loadIptv(provider.m3uUrl)
            }
            "XTREAM" -> if (provider.server.isNotBlank() && provider.username.isNotBlank()) {
                loginXtream(provider.server, provider.username, provider.password)
            }
        }
    }

    fun ensureIptvLoaded() {
        if (xtreamAccount != null) return
        when (iptvKind) {            "XTREAM" -> if (xtreamServer.isNotBlank() && xtreamUser.isNotBlank() &&
                xtreamPassword.isNotBlank() && !xtreamLoading
            ) {
                loginXtream(xtreamServer, xtreamUser, xtreamPassword)
            }
            "M3U" -> if (iptvUrl.isNotBlank() && iptvEntries.isEmpty() && !iptvLoading) loadIptv()
            else -> if (xtreamServer.isNotBlank() && xtreamUser.isNotBlank()) {
                if (!xtreamLoading) loginXtream(xtreamServer, xtreamUser, xtreamPassword)
            } else if (iptvUrl.isNotBlank() && iptvEntries.isEmpty() && !iptvLoading) {
                loadIptv()
            }
        }
    }

    /** Opened from the IPTV tab: drop sockets from a previous Wi-Fi network and retry a failed load. */
    fun prepareIptv() {
        container.iptv.evictConnections()
        container.xtream.evictConnections()
        if (xtreamAccount == null) {
            ensureIptvLoaded()
        } else if (xtreamItems.isEmpty() && !xtreamLoading && xtreamError != null) {
            loadXtreamCategories(xtreamSection)
        }
    }

    fun openInfo(item: XtreamItem) {
        infoItem = item
        info = null
        infoLoading = true
        viewModelScope.launch {
            val year = Regex("\\((?:19|20)\\d{2}\\)").find(item.name)?.value?.trim('(', ')')
            info = container.tmdb.lookup(item.name, item.section == XtreamSection.SERIES, year)
            infoLoading = false
        }
    }

    fun closeInfo() {
        infoItem = null
        info = null
        infoLoading = false
    }

    fun playInfo() {
        val item = infoItem ?: return
        closeInfo()
        watchXtream(item)
    }

    fun loadIptv(url: String = iptvUrl) {
        val clean = url.trim()
        if (clean.isBlank()) {
            iptvError = "Vul eerst een M3U-adres in."
            return
        }
        if (iptvLoading) return
        iptvUrl = clean
        iptvKind = "M3U"
        store.saveIptvUrl(clean)
        store.saveIptvKind(iptvKind)
        iptvLoading = true
        iptvError = null
        iptvEntries = emptyList()
        iptvGroups = emptyList()
        iptvGroup = null
        viewModelScope.launch {
            container.iptv.load(clean).fold(
                onSuccess = { list ->
                    iptvEntries = list
                    iptvGroups = list.flatMap { it.groups }.distinct()
                    if (list.isEmpty()) iptvError = "De playlist bevat geen streams."
                },
                onFailure = { iptvError = it.message ?: "Playlist laden mislukt" },
            )
            iptvLoading = false
        }
    }

    fun demoIptv() {
        loadIptv("http://127.0.0.1:8092/playlist.m3u")
    }

    fun selectIptvGroup(group: String?) {
        iptvGroup = group
    }

    fun watchIptv(entry: M3uEntry) {
        val visible = iptvEntries.filter { iptvGroup == null || it.groups.contains(iptvGroup) }
        val index = visible.indexOf(entry)
        val queue = visible.map {
            PlayerChannel(
                id = it.url,
                title = it.name,
                url = it.url,
                logo = it.attributes["tvg-logo"].orEmpty(),
                headers = container.iptv.headers(it),
            )
        }
        playUrl(
            entry.name,
            entry.url,
            container.iptv.headers(entry),
            source = "iptv",
            returnTo = AppScreen.IPTV,
            queue = queue,
            queueIndex = index,
            live = !entry.url.substringBefore('?').lowercase()
                .let { url -> listOf(".mp4", ".mkv", ".avi", ".mov", ".m4v").none(url::endsWith) },
        )
    }

    // ------------------------------------------------------------ Xtream Codes

    fun loginXtream(server: String, username: String, password: String) {
        val cleanServer = server.trim()
        if (cleanServer.isBlank()) {
            xtreamError = "Vul het serveradres in."
            return
        }
        if (xtreamLoading) return
        store.saveXtream(cleanServer, username.trim(), password)
        iptvKind = "XTREAM"
        store.saveIptvKind(iptvKind)
        xtreamServer = cleanServer
        xtreamUser = username.trim()
        xtreamPassword = password
        xtreamLoading = true
        xtreamError = null
        xtreamAccount = null
        xtreamCategories = emptyList()
        xtreamCategory = null
        xtreamItems = emptyList()
        xtreamSection = XtreamSection.LIVE
        xtreamEpisodes = emptyList()
        xtreamSeriesTitle = null
        viewModelScope.launch {
            container.xtream.login(cleanServer, username.trim(), password).fold(
                onSuccess = { account ->
                    xtreamAccount = account
                    loadXtreamCategories(XtreamSection.LIVE)
                },
                onFailure = {
                    xtreamError = it.message ?: "Xtream-login mislukt"
                    xtreamLoading = false
                },
            )
        }
    }

    fun selectXtreamSection(section: XtreamSection) {
        if (section == xtreamSection && xtreamCategories.isNotEmpty()) return
        xtreamSection = section
        xtreamSeriesTitle = null
        xtreamEpisodes = emptyList()
        loadXtreamCategories(section)
    }

    private fun loadXtreamCategories(section: XtreamSection) {
        val account = xtreamAccount ?: return
        xtreamCategories = emptyList()
        xtreamCategory = null
        xtreamItems = emptyList()
        xtreamLoading = true
        xtreamError = null
        viewModelScope.launch {
            container.xtream.categories(account, section).fold(
                onSuccess = { list ->
                    xtreamCategories = list
                    val first = list.firstOrNull()
                    if (first != null) {
                        loadXtreamItems(first)
                    } else {
                        xtreamLoading = false
                    }
                },
                onFailure = {
                    xtreamError = it.message ?: "Categorieën laden mislukt"
                    xtreamLoading = false
                },
            )
        }
    }

    fun selectXtreamCategory(category: XtreamCategory) {
        loadXtreamItems(category)
    }

    private fun loadXtreamItems(category: XtreamCategory) {
        val account = xtreamAccount ?: return
        xtreamCategory = category
        xtreamItems = emptyList()
        xtreamNow = emptyMap()
        synchronized(xtreamEpgRequested) { xtreamEpgRequested.clear() }
        xtreamLoading = true
        xtreamError = null
        viewModelScope.launch {
            container.xtream.items(account, xtreamSection, category.id).fold(
                onSuccess = { xtreamItems = it },
                onFailure = { xtreamError = it.message ?: "Streams laden mislukt" },
            )
            xtreamLoading = false
        }
    }

    /** Fetches now/next only for the live rows the list actually shows. */
    fun ensureXtreamEpg(item: XtreamItem) {
        if (item.section != XtreamSection.LIVE) return
        val account = xtreamAccount ?: return
        synchronized(xtreamEpgRequested) {
            if (xtreamNow.containsKey(item.id) || !xtreamEpgRequested.add(item.id)) return
        }
        viewModelScope.launch {
            container.xtream.epg(account, item.id).fold(
                onSuccess = { events ->
                    val current = events.airingAt(System.currentTimeMillis() / 1000)
                    if (current != null) xtreamNow = xtreamNow + (item.id to current)
                },
                onFailure = {
                    synchronized(xtreamEpgRequested) { xtreamEpgRequested.remove(item.id) }
                },
            )
        }
    }

    fun openXtreamSeries(item: XtreamItem) {
        val account = xtreamAccount ?: return
        xtreamSeriesTitle = item.name
        xtreamEpisodes = emptyList()
        xtreamLoading = true
        xtreamError = null
        viewModelScope.launch {
            container.xtream.episodes(account, item.id).fold(
                onSuccess = { xtreamEpisodes = it },
                onFailure = { xtreamError = it.message ?: "Afleveringen laden mislukt" },
            )
            xtreamLoading = false
        }
    }

    fun closeXtreamSeries() {
        xtreamSeriesTitle = null
        xtreamEpisodes = emptyList()
    }

    fun demoXtream() {
        loginXtream("http://127.0.0.1:8093", "demo", "demo")
    }

    fun watchXtream(item: XtreamItem) {
        val account = xtreamAccount ?: return
        if (item.section == XtreamSection.SERIES) {
            openXtreamSeries(item)
            return
        }
        val queue = if (item.section == XtreamSection.LIVE) {
            xtreamItems.filter { it.section == XtreamSection.LIVE }.map {
                PlayerChannel(
                    id = it.id,
                    title = it.name,
                    url = container.xtream.streamUrl(account, it),
                    logo = it.icon,
                )
            }
        } else {
            emptyList()
        }
        val index = queue.indexOfFirst { it.id == item.id }
        playUrl(
            title = item.name,
            url = container.xtream.streamUrl(account, item),
            source = "iptv",
            returnTo = AppScreen.XTREAM,
            ref = if (item.section == XtreamSection.MOVIES) {
                LibraryRef(account.key, "m", item.id)
            } else {
                null
            },
            queue = queue,
            queueIndex = index,
            live = item.section == XtreamSection.LIVE,
        )
    }

    fun openCatchup(item: XtreamItem) {
        val account = xtreamAccount ?: return
        if (!item.hasArchive) return
        catchupItem = item
        catchupPrograms = emptyList()
        catchupError = null
        catchupLoading = true
        viewModelScope.launch {
            container.xtream.epg(account, item.id).fold(
                onSuccess = { events ->
                    val now = System.currentTimeMillis() / 1000
                    catchupPrograms = events
                        .filter { Catchup.canPlay(item, it, now) }
                        .sortedByDescending { it.startSec }
                    if (catchupPrograms.isEmpty()) catchupError = "Geen programma's in het archief."
                },
                onFailure = { catchupError = it.message ?: "EPG laden mislukt" },
            )
            catchupLoading = false
        }
    }

    fun closeCatchup() {
        catchupItem = null
        catchupPrograms = emptyList()
        catchupError = null
        catchupLoading = false
    }

    fun playCatchup(event: XtreamEpg) {
        val item = catchupItem ?: return
        val account = xtreamAccount ?: return
        val now = System.currentTimeMillis() / 1000
        val url = Catchup.url(account, item, event, now) ?: return
        playUrl(
            title = "${item.name} · ${event.title}",
            url = url,
            source = "iptv",
            returnTo = AppScreen.XTREAM,
            live = false,
        )
        catchupItem = null
        catchupPrograms = emptyList()
    }

    fun watchXtreamEpisode(episode: XtreamEpisode) {
        val account = xtreamAccount ?: return
        val title = listOfNotNull(
            xtreamSeriesTitle,
            "S${episode.season}E${episode.episode}",
            episode.title,
        ).joinToString(" · ")
        playUrl(
            title,
            container.xtream.episodeUrl(account, episode),
            source = "iptv",
            returnTo = AppScreen.XTREAM,
            ref = LibraryRef(account.key, "e", episode.id),
        )
    }

    // ------------------------------------------------------------ koppelen

    fun scan() {
        if (scanning) return
        screen = AppScreen.SCAN
        scanResults = emptyList()
        scanning = false
    }

    /** Start de echte mDNS-zoektocht; de UI vraagt eerst de lokale-netwerkpermissie. */
    fun startScan() {
        if (scanning) return
        scanning = true
        scanResults = emptyList()
        viewModelScope.launch {
            val found = runCatching { container.discovery.discover() }.getOrDefault(emptyList())
            scanResults = found.map { it.toPairedTv() }
            scanning = false
        }
    }

    fun startPairing(tv: PairedTv) {
        pendingTv = tv
        pairError = null
        pairingBusy = false
        screen = AppScreen.PAIR
    }

    /** Handmatig adres (bijvoorbeeld als mDNS op het netwerk geblokkeerd is). */
    fun pairManually(host: String, port: Int) {
        val clean = host.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
            .substringBefore('/')
        if (clean.isBlank() || port !in 1..65535) {
            pairError = "Vul een geldig adres en poort in"
            return
        }
        startPairing(
            PairedTv(
                id = "manual-$clean:$port",
                name = "TV $clean",
                host = clean,
                port = port,
                real = true,
            ),
        )
    }

    fun pair(code: String) {
        val tv = pendingTv ?: return
        if (code.length != 6 || code.any { !it.isDigit() }) {
            pairError = "Voer de 6-cijferige code van de TV in"
            return
        }
        if (!tv.real) {
            pairDemo(tv, code)
            return
        }
        if (pairingBusy) return
        pairError = null
        pairingBusy = true
        viewModelScope.launch {
            try {
                when (val result = container.pairing.pair(tv, code)) {
                    is PairingResult.Success -> {
                        store.savePairedTv(result.tv)
                        pairedTv = result.tv
                        useReal(result.tv)
                        screen = AppScreen.REMOTE
                    }
                    is PairingResult.Failure -> pairError = result.message
                }
            } finally {
                pairingBusy = false
            }
        }
    }

    private fun pairDemo(tv: PairedTv, code: String) {
        if (code == "000000") {
            pairError = "De TV heeft de code geweigerd"
            return
        }
        pairError = null
        store.savePairedTv(tv)
        pairedTv = tv
        useMock(tv)
        screen = AppScreen.REMOTE
    }

    fun connectTo(tv: PairedTv) {
        store.savePairedTv(tv)
        pairedTv = tv
        if (tv.real) useReal(tv) else useMock(tv)
        screen = AppScreen.REMOTE
    }

    /** Opnieuw verbinden zonder van scherm te wisselen (instellingen, offlinebanner). */
    fun reconnect() {
        val tv = pairedTv ?: return
        if (!tv.real) {
            active.value = mock
            mock.connect(tv)
            return
        }
        viewModelScope.launch {
            val found = runCatching { container.discovery.discover() }.getOrDefault(emptyList())
                .firstOrNull { it.id == tv.id }
            val target = if (found != null) {
                tv.copy(host = found.host, port = found.port, pin = found.pin.ifBlank { tv.pin })
            } else {
                tv
            }
            if (target.host != tv.host || target.port != tv.port || target.pin != tv.pin) {
                store.savePairedTv(target)
                pairedTv = target
            }
            active.value = real
            real.connect(target)
        }
    }

    fun forgetTv() {
        mock.disconnect()
        real.disconnect()
        store.forgetTv()
        pairedTv = null
        pendingTv = null
        snapshot = TvSnapshot()
        lastLatencyMs = null
        settingsCatalog = null
        dropForm()
        screen = AppScreen.WELCOME
    }

    fun clearPairError() {
        pairError = null
    }

    // ------------------------------------------------------------ formulieren

    fun updateFormField(id: String, text: String) {
        formValues = formValues + (id to text)
        val sensitive = form?.fields?.firstOrNull { it.id == id }?.sensitive == true
        if (sensitive) return
        formJob?.cancel()
        formJob = viewModelScope.launch {
            delay(90)
            sendForm("TEXT")
        }
    }

    fun submitForm() {
        formJob?.cancel()
        sendForm("SUBMIT")
        clearForm()
    }

    fun cancelForm() {
        formJob?.cancel()
        sendForm("CANCEL")
        clearForm()
    }

    /** Lokaal sluiten zonder commando (bijvoorbeeld bij wegvallen van de verbinding). */
    fun dropForm() {
        formJob?.cancel()
        clearForm()
    }

    private fun clearForm() {
        form = null
        formValues = emptyMap()
    }

    private fun sendForm(action: String) {
        val descriptor = form ?: return
        val values = if (action == "TEXT") {
            formValues.filterKeys { id ->
                descriptor.fields.firstOrNull { it.id == id }?.sensitive != true
            }
        } else {
            formValues
        }
        if (action == "TEXT" && values.isEmpty()) return
        send(
            ProtocolJson.envelope(
                MessageType.CMD_FORM,
                FormSubmitCommand(descriptor.formId, descriptor.revision, values, action),
                id = nextId(),
            ),
        )
    }

    // ------------------------------------------------------------ instellingen

    fun updateSetting(path: String, value: JsonElement, pin: String? = null) {
        val catalog = settingsCatalog ?: return
        send(
            ProtocolJson.envelope(
                MessageType.CMD_SETTINGS,
                SettingsCommand(catalog.revision, listOf(SettingsPatchOp(path, value)), pin = pin),
                id = nextId(),
            ),
        )
    }

    // ------------------------------------------------------------ profielfoto

    fun uploadProfilePhoto(profileId: String, uri: Uri) {
        if (photoBusy) return
        val tv = pairedTv ?: return
        if (!tv.real) {
            showToast("Demo: foto's werken alleen met een echte tv")
            return
        }
        photoBusy = true
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        val out = ByteArrayOutputStream()
                        val chunk = ByteArray(64 * 1024)
                        var total = 0
                        while (true) {
                            val read = input.read(chunk)
                            if (read < 0) break
                            total += read
                            if (total > MAX_PHOTO_BYTES) return@runCatching null
                            out.write(chunk, 0, read)
                        }
                        out.toByteArray()
                    }
                }.getOrNull()
            }
            if (bytes == null) {
                photoBusy = false
                showToast("Foto is te groot of onleesbaar", "error")
                return@launch
            }
            finishUpload(container.photoUpload.upload(tv, profileId, bytes))
        }
    }

    /** Diagnose/demo: een gegenereerde foto, zonder de systeemfotokiezer. */
    fun uploadTestPhoto(profileId: String) {
        if (photoBusy) return
        val tv = pairedTv ?: return
        if (!tv.real) {
            showToast("Demo: foto's werken alleen met een echte tv")
            return
        }
        val bytes = runCatching {
            val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.rgb(90, 169, 255))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = Color.WHITE
            canvas.drawCircle(256f, 256f, 150f, paint)
            paint.color = Color.rgb(3, 18, 46)
            paint.textSize = 120f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("EC", 256f, 300f, paint)
            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                bitmap.recycle()
                out.toByteArray()
            }
        }.getOrNull() ?: return
        photoBusy = true
        viewModelScope.launch {
            finishUpload(container.photoUpload.upload(tv, profileId, bytes))
        }
    }

    private fun finishUpload(result: Result<Long>) {
        photoBusy = false
        result.fold(
            onSuccess = { showToast("Profielfoto bijgewerkt") },
            onFailure = { showToast("Foto uploaden mislukt: ${it.message ?: "onbekend"}", "error") },
        )
    }

    private fun useReal(tv: PairedTv) {
        mock.disconnect()
        active.value = real
        snapshot = TvSnapshot()
        lastLatencyMs = null
        settingsCatalog = null
        dropForm()
        real.connect(tv)
    }

    private fun useMock(tv: PairedTv) {
        real.disconnect()
        active.value = mock
        snapshot = TvSnapshot()
        lastLatencyMs = null
        settingsCatalog = null
        dropForm()
        mock.connect(tv)
    }

    // ------------------------------------------------------------ bediening

    fun pressKey(key: RemoteKey, phase: KeyPhase = KeyPhase.DOWN, repeat: Int = 0, long: Boolean = false) {
        lastKeyLabel = key.name
        send(
            ProtocolJson.envelope(
                MessageType.CMD_KEY,
                KeyCommand(key = key, phase = phase, repeat = repeat, long = long),
                id = nextId(),
            ),
        )
    }

    /** Eén tik: down + up, zoals een echte afstandsbediening. */
    fun tapKey(key: RemoteKey) {
        pressKey(key, KeyPhase.DOWN)
        viewModelScope.launch {
            delay(45)
            pressKey(key, KeyPhase.UP)
        }
    }

    fun nav(target: NavTarget) {
        send(ProtocolJson.envelope(MessageType.CMD_NAV, NavCommand(target), id = nextId()))
    }

    fun zapNumber(number: Int) {
        send(ProtocolJson.envelope(MessageType.CMD_ZAP, ZapCommand(number = number), id = nextId()))
    }

    fun zapChannel(reference: String) {
        send(ProtocolJson.envelope(MessageType.CMD_ZAP, ZapCommand(serviceRef = reference), id = nextId()))
    }

    fun volume(delta: Int) {
        send(ProtocolJson.envelope(MessageType.CMD_VOLUME, VolumeCommand(target = VolumeTarget.TV, delta = delta), id = nextId()))
    }

    fun toggleMute() {
        send(
            ProtocolJson.envelope(
                MessageType.CMD_VOLUME,
                VolumeCommand(target = VolumeTarget.TV, mute = !snapshot.muted),
                id = nextId(),
            ),
        )
    }

    fun player(action: PlayerCommandAction) {
        send(ProtocolJson.envelope(MessageType.CMD_PLAYER, PlayerCommand(action), id = nextId()))
    }

    fun selectProfile(id: String, pin: String? = null) {
        send(
            ProtocolJson.envelope(
                MessageType.CMD_PROFILE,
                ProfileCommand(ProfileCommandAction.SELECT, uuid = id, pin = pin),
                id = nextId(),
            ),
        )
    }

    fun createProfile(name: String, avatar: Int) {
        send(
            ProtocolJson.envelope(
                MessageType.CMD_PROFILE,
                ProfileCommand(ProfileCommandAction.CREATE, name = name, avatar = avatar),
                id = nextId(),
            ),
        )
    }

    fun updateProfile(id: String, name: String, avatar: Int, clearPhoto: Boolean, pin: String?) {
        send(
            ProtocolJson.envelope(
                MessageType.CMD_PROFILE,
                ProfileCommand(ProfileCommandAction.RENAME, uuid = id, name = name, pin = pin),
                id = nextId(),
            ),
        )
        send(
            ProtocolJson.envelope(
                MessageType.CMD_PROFILE,
                ProfileCommand(
                    ProfileCommandAction.SET_AVATAR,
                    uuid = id,
                    avatar = avatar,
                    pin = pin,
                    clearPhoto = clearPhoto,
                ),
                id = nextId(),
            ),
        )
    }

    fun deleteProfile(id: String, pin: String? = null) {
        send(
            ProtocolJson.envelope(
                MessageType.CMD_PROFILE,
                ProfileCommand(ProfileCommandAction.DELETE, uuid = id, pin = pin),
                id = nextId(),
            ),
        )
    }

    fun loadProfilePhoto(profileId: String) {
        val tv = pairedTv ?: return
        photoPreview = null
        photoPreviewError = null
        photoPreviewLoading = true
        viewModelScope.launch {
            container.photoUpload.fetch(tv, profileId).fold(
                onSuccess = { bytes ->
                    val bitmap = runCatching {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }.getOrNull()
                    if (bitmap == null) {
                        photoPreviewError = "Foto is onleesbaar."
                    } else {
                        photoPreview = bitmap.asImageBitmap()
                    }
                },
                onFailure = { photoPreviewError = it.message ?: "Foto ophalen mislukt" },
            )
            photoPreviewLoading = false
        }
    }

    fun clearPhotoPreview() {
        photoPreview = null
        photoPreviewError = null
        photoPreviewLoading = false
    }

    fun clearProfilePhoto(id: String, avatar: Int, pin: String? = null) {
        send(
            ProtocolJson.envelope(
                MessageType.CMD_PROFILE,
                ProfileCommand(
                    ProfileCommandAction.SET_AVATAR,
                    uuid = id,
                    avatar = avatar,
                    pin = pin,
                    clearPhoto = true,
                ),
                id = nextId(),
            ),
        )
    }

    fun power(action: PowerCommandAction) {
        send(ProtocolJson.envelope(MessageType.CMD_POWER, PowerCommand(action), id = nextId()))
    }

    fun chooseMode(value: ControlMode) {
        mode = value
        showToast(if (value == ControlMode.APP) "Bediening: EnigmaCast-app" else "Bediening: ontvanger")
    }

    fun toggleNumbers() {
        showNumbers = !showNumbers
    }

    fun simulateDrop() {
        (active.value as? MockBridge)?.simulateDrop()
    }

    fun toggleHaptics() {
        haptics = !haptics
        store.haptics = haptics
    }

    // ------------------------------------------------------------ events

    private suspend fun handleEvent(envelope: Envelope) {
        when (envelope.type) {
            MessageType.HELLO -> {
                val hello = ProtocolJson.decodePayload<HelloPayload>(envelope)
                pendingProgress?.let { command ->
                    send(ProtocolJson.envelope(MessageType.CMD_LIBRARY, command, id = nextId()))
                }
                snapshot = snapshot.copy(
                    tvName = hello.tv.name,
                    appVersion = hello.tv.app,
                    api = hello.api,
                    capabilities = hello.capabilities,
                    profiles = hello.profile?.profiles.orEmpty(),
                    activeProfileId = hello.profile?.activeId.orEmpty(),
                    receiverName = hello.receiver?.name.orEmpty(),
                    receiverOnline = hello.receiver?.online == true,
                    receiverMode = hello.receiver?.mode ?: "own_tuner",
                    settingsRevision = hello.settingsRevision,
                    screenApp = hello.screen?.app.orEmpty(),
                    screenSection = hello.screen?.section,
                )
            }
            MessageType.STATE_PLAYER -> {
                val state = ProtocolJson.decodePayload<StatePlayerPayload>(envelope)
                snapshot = snapshot.copy(
                    source = state.source,
                    channel = state.channel,
                    now = state.now,
                    player = state.player,
                    tuner = state.tuner,
                    route = state.route,
                    nextTitle = state.now?.nextTitle,
                    nextStartMs = state.now?.nextStartMs,
                )
            }
            MessageType.STATE_PROFILES -> {
                val profiles = ProtocolJson.decodePayload<StateProfilesPayload>(envelope).profile
                snapshot = snapshot.copy(profiles = profiles.profiles, activeProfileId = profiles.activeId)
            }
            MessageType.STATE_SETTINGS -> {
                settingsCatalog = ProtocolJson.decodePayload<SettingsCatalogPayload>(envelope)
            }
            MessageType.STATE_LIBRARY -> {
                val payload = ProtocolJson.decodePayload<LibraryStatePayload>(envelope)
                library = container.library.syncFromTv(payload.favorites + payload.resume)
                recordings = payload.recordings
            }
            MessageType.STATE_RECENT -> {
                val payload = ProtocolJson.decodePayload<RecentStatePayload>(envelope)
                recentTv = payload.channels
                recentTvHost = payload.receiverHost
            }
            MessageType.STATE_IPTV -> {
                val source = ProtocolJson.decodePayload<IptvSourcePayload>(envelope)
                if (source.configured) {
                    val providers = source.providers.sortedBy { it.sortOrder }
                    iptvProviders = providers
                    val chosen = providers.firstOrNull { it.id == iptvProviderId }
                        ?: providers.firstOrNull { it.enabled }
                        ?: providers.firstOrNull()
                    if (chosen != null) {
                        iptvProviderId = chosen.id
                        val stamp = "${chosen.id}|${chosen.kind}|${chosen.m3uUrl}|${chosen.server}|${chosen.username}|${chosen.password.hashCode()}"
                        if (stamp != iptvSourceStamp) {
                            iptvSourceStamp = stamp
                            applyIptvProvider(chosen)
                        }
                    } else {
                        val stamp = "${source.kind}|${source.m3uUrl}|${source.server}|${source.username}|${source.password.hashCode()}"
                        if (stamp != iptvSourceStamp) {
                            iptvSourceStamp = stamp
                            iptvFromTv = true
                            iptvKind = source.kind.uppercase()
                            store.saveIptvKind(iptvKind)
                            when (source.kind.uppercase()) {
                                "M3U" -> if (source.m3uUrl.isNotBlank()) {
                                    iptvUrl = source.m3uUrl
                                    store.saveIptvUrl(source.m3uUrl)
                                    loadIptv(source.m3uUrl)
                                }
                                "XTREAM" -> if (source.server.isNotBlank() && source.username.isNotBlank()) {
                                    loginXtream(source.server, source.username, source.password)
                                }
                            }
                        }
                    }
                }
            }
            MessageType.FORM -> {
                val descriptor = ProtocolJson.decodePayload<FormDescriptor>(envelope)
                if (descriptor.closed || descriptor.fields.isEmpty()) {
                    clearForm()
                } else {
                    val isNew = form?.formId != descriptor.formId
                    form = descriptor
                    formValues = if (isNew) {
                        descriptor.fields.associate { field ->
                            field.id to if (field.sensitive) "" else field.value.orEmpty()
                        }
                    } else {
                        val merged = formValues.toMutableMap()
                        descriptor.fields.forEach { field ->
                            if (!field.sensitive) {
                                val value = field.value
                                if (value != null && value != merged[field.id]) merged[field.id] = value
                            }
                        }
                        merged
                    }
                }
            }
            MessageType.ACK -> {
                val ack = ProtocolJson.decodePayload<com.enigmacast.companion.protocol.AckPayload>(envelope)
                lastLatencyMs = ack.latencyMs
            }
            MessageType.TOAST -> {
                val toast = ProtocolJson.decodePayload<ToastPayload>(envelope)
                showToast(toast.message, toast.level)
            }
            MessageType.ERROR -> {
                val error = ProtocolJson.decodePayload<ProtocolError>(envelope)
                val key = error.message.ifBlank { error.code }
                val message = when (key) {
                    "PROFILE_PIN_WRONG" -> "Onjuiste pincode"
                    "PROFILE_PIN_COOLDOWN" -> "Te veel pogingen; wacht even"
                    "PROFILE_LIMIT" -> "Maximaal 6 profielen"
                    "PROFILE_NAME_INVALID" -> "Gebruik een naam van 1–20 tekens"
                    "PROFILE_CANNOT_DELETE" -> "Dit profiel kan niet verwijderd worden"
                    "PROFILE_NOT_FOUND" -> "Profiel niet gevonden"
                    "PROFILE_ACTION_UNKNOWN" -> "Onbekende profielactie"
                    "SETTINGS_CONFLICT" -> "Instellingen zijn gewijzigd; probeer opnieuw"
                    "SETTINGS_LOCKED" -> "Onjuiste pincode of te veel pogingen"
                    else -> error.message
                }
                showToast(message, "error")
            }
            MessageType.PONG -> Unit
        }
    }

    private fun showToast(message: String, level: String = "info") {
        toastToken += 1
        val token = toastToken
        toast = UiToast(message, level, token)
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            delay(1_800)
            if (toast?.token == token) toast = null
        }
    }

    override fun onCleared() {
        runCatching {
            getApplication<Application>().getSystemService(ConnectivityManager::class.java)
                ?.unregisterNetworkCallback(networkCallback)
        }
        super.onCleared()
    }

    private fun send(envelope: Envelope) {
        active.value.send(envelope)
    }

    private fun nextId(): String = "c-${++nextId}"

    private companion object {
        const val MAX_PHOTO_BYTES = 10 * 1024 * 1024
    }
}

/** Verwijzing naar het IPTV-record dat de TV in zijn bibliotheek gebruikt. */
data class LibraryRef(val source: String, val kind: String, val id: String)
