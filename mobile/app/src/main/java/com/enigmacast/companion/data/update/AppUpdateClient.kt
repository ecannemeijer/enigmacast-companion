package com.enigmacast.companion.data.update

import android.content.Context
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
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
        val own = fetch(UPDATE_MANIFEST_URL)?.let { text ->
            runCatching { json.decodeFromString<RemoteRelease>(text) }.getOrNull()
        }
        val fromTv = fetch(UPDATE_FALLBACK_MANIFEST_URL)?.let { text ->
            runCatching { json.decodeFromString<UpdateManifest>(text) }.getOrNull()?.companion
        }
        newestRelease(own, fromTv)
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

    /**
     * Installs through PackageInstaller: Android asks the user to confirm (the first time, or
     * always when another app installed the Companion); later updates of itself may go through
     * without a question on Android 12+. Falls back to the classic install screen.
     */
    fun install(apk: File) {
        runCatching { installWithSession(apk) }.onFailure { installWithIntent(apk) }
    }

    private fun installWithSession(apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            session.openWrite("enigmacast-companion.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val callback = Intent(context, UpdateInstallReceiver::class.java).setPackage(context.packageName)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            session.commit(PendingIntent.getBroadcast(context, id, callback, flags).intentSender)
        }
    }

    private fun installWithIntent(apk: File) {
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
