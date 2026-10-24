package com.enigmacast.companion

import android.content.Context
import com.enigmacast.companion.data.LocalStore
import com.enigmacast.companion.data.MockBridge
import com.enigmacast.companion.data.library.LibraryStore
import com.enigmacast.companion.data.live.EnigmaLiveClient
import com.enigmacast.companion.data.live.IptvClient
import com.enigmacast.companion.data.live.XtreamClient
import com.enigmacast.companion.data.net.DeviceIdentity
import com.enigmacast.companion.data.net.NsdDiscovery
import com.enigmacast.companion.data.net.PairingClient
import com.enigmacast.companion.data.net.PhotoUploadClient
import com.enigmacast.companion.data.net.WebSocketConnection
import com.enigmacast.companion.data.tmdb.TmdbClient
import com.enigmacast.companion.data.update.AppUpdateClient

/**
 * Handmatige dependency injection. Bewust geen Hilt in fase 2: de app heeft één store,
 * één echte verbinding en één mock, en de build moet offline kunnen draaien.
 */
class AppContainer(context: Context) {
    val store: LocalStore = LocalStore(context)
    val mock: MockBridge = MockBridge()
    val identity: DeviceIdentity = DeviceIdentity()
    val pairing: PairingClient = PairingClient(identity)
    val photoUpload: PhotoUploadClient = PhotoUploadClient(identity)
    val discovery: NsdDiscovery = NsdDiscovery(context)
    val real: WebSocketConnection = WebSocketConnection(
        identity,
        resolve = { old ->
            discovery.discover().firstOrNull { it.id == old.id }?.let { found ->
                old.copy(host = found.host, port = found.port, pin = found.pin.ifBlank { old.pin })
            }
        },
    )
    val live: EnigmaLiveClient = EnigmaLiveClient()
    val iptv: IptvClient = IptvClient()
    val xtream: XtreamClient = XtreamClient()
    val library: LibraryStore = LibraryStore(context)
    val updates: AppUpdateClient = AppUpdateClient(context)
    val tmdb: TmdbClient = TmdbClient()
}
