package com.enigmacast.companion.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.enigmacast.companion.BuildConfig
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/** Leest het update-manifest en downloadt/installeert de sideload-APK. */
class AppUpdateClient(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun check(): RemoteRelease? = withContext(Dispatchers.IO) {
        val combined = fetch(UPDATE_MANIFEST_URL)?.let { text ->
            runCatching { json.decodeFromString<UpdateManifest>(text) }.getOrNull()
        }
        combined?.companion?.let { return@withContext it }

        val fallback = fetch(UPDATE_FALLBACK_MANIFEST_URL) ?: return@withContext null
        runCatching { json.decodeFromString<RemoteRelease>(fallback) }.getOrNull()
    }

    suspend fun download(release: RemoteRelease, onProgress: suspend (Int) -> Unit): File =
        withContext(Dispatchers.IO) {
            val url = release.apkUrl.ifBlank { UPDATE_APK_URL }
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Download mislukt (HTTP ${response.code})")
                val body = response.body
                val total = body.contentLength()
                val dir = File(context.cacheDir, "updates").apply { mkdirs() }
                val target = File(dir, "enigmacast-companion-update.apk")
                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var readTotal = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            readTotal += count
                            if (total > 0) {
                                onProgress(((readTotal * 100) / total).toInt().coerceIn(0, 100))
                            }
                        }
                    }
                }
                target
            }
        }

    private fun fetch(url: String): String? = runCatching {
        val request = Request.Builder()
            .url(url)
            .header("Cache-Control", "no-cache")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            response.body.string()
        }
    }.getOrNull()

    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    fun openInstallPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun install(apk: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            apk,
        )
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
