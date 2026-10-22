package com.enigmacast.companion.data.net

import android.annotation.SuppressLint
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import com.enigmacast.companion.data.DiscoveredTv
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Zoekt `_enigmacast._tcp.` op het lokale netwerk. De TV publiceert id, naam, app-versie
 * en de SPKI-pin in zijn TXT-record, zodat de telefoon meteen kan pinnen.
 */
class NsdDiscovery(private val context: Context) {

    @SuppressLint("MissingPermission")
    suspend fun discover(timeoutMs: Long = 4_500): List<DiscoveredTv> = withContext(Dispatchers.Main) {
        val manager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
            ?: return@withContext emptyList()
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val lock = runCatching {
            wifi?.createMulticastLock("enigmacast-nsd")?.apply {
                setReferenceCounted(false)
                acquire()
            }
        }.getOrNull()

        val found = java.util.concurrent.CopyOnWriteArrayList<NsdServiceInfo>()
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                found += serviceInfo
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                found -= serviceInfo
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        }

        return@withContext try {
            manager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
            delay(timeoutMs)
            // Resolve vóór stopServiceDiscovery: resolving na het stoppen faalt op veel
            // Android-versies en dan lijkt de tv niet gevonden te zijn.
            found.distinctBy { it.serviceName }
                .mapNotNull { resolve(manager, it) }
                .distinctBy { "${it.id}@${it.host}:${it.port}" }
        } catch (_: Exception) {
            // Geen NSD of geen netwerk: de handmatige route blijft werken.
            emptyList()
        } finally {
            runCatching { manager.stopServiceDiscovery(listener) }
            runCatching { lock?.release() }
        }
    }

    private suspend fun resolve(manager: NsdManager, info: NsdServiceInfo): DiscoveredTv? =
        withTimeoutOrNull(2_000) {
            suspendCancellableCoroutine { continuation ->
                manager.resolveService(
                    info,
                    object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            if (continuation.isActive) continuation.resume(null)
                        }

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            if (!continuation.isActive) return
                            val host = serviceInfo.host?.hostAddress
                                ?.substringBefore('%')
                                ?.takeIf { it.isNotBlank() && !it.contains(':') }
                            if (host == null) {
                                continuation.resume(null)
                                return
                            }
                            val txt = runCatching { NsdTxt.parse(serviceInfo.attributes) }
                                .getOrDefault(NsdTxtData())
                            continuation.resume(
                                DiscoveredTv(
                                    id = txt.id.ifBlank { serviceInfo.serviceName },
                                    name = txt.name.ifBlank { serviceInfo.serviceName },
                                    host = host,
                                    port = serviceInfo.port,
                                    pin = txt.pin,
                                    appVersion = txt.app,
                                ),
                            )
                        }
                    },
                )
            }
        }

    private companion object {
        const val SERVICE_TYPE = "_enigmacast._tcp."
    }
}
