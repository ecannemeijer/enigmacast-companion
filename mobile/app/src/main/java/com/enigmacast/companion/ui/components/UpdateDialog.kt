package com.enigmacast.companion.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.enigmacast.companion.ui.components.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.enigmacast.companion.BuildConfig
import com.enigmacast.companion.ui.AppViewModel
import com.enigmacast.companion.ui.theme.Ink

/** Update-melding met download- en installatiestap. */
@Composable
fun UpdateDialog(vm: AppViewModel) {
    val release = vm.updateAvailable ?: return
    AlertDialog(
        onDismissRequest = { if (!vm.updateDownloading) vm.dismissUpdate() },
        title = { Text("Update beschikbaar") },
        text = {
            Column {
                Text(
                    "Versie ${release.versionName} (build ${release.versionCode}) is beschikbaar.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Je gebruikt ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE}).",
                    color = Ink.Subtle,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (release.notes.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(release.notes, style = MaterialTheme.typography.bodySmall)
                }
                if (vm.updateDownloading) {
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(
                        "Downloaden… ${vm.updateProgress}%",
                        color = Ink.Subtle,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                vm.updateReady?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Download klaar. Tik op Installeren en bevestig de installatie.",
                        color = Ink.Success,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                vm.updateError?.let { error ->
                    Spacer(Modifier.height(10.dp))
                    Text(error, color = Ink.Danger, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            when {
                vm.updateReady != null -> TextButton(onClick = vm::installUpdate) { Text("Installeren") }
                vm.updateDownloading -> TextButton(onClick = {}, enabled = false) { Text("Downloaden…") }
                else -> TextButton(onClick = vm::downloadUpdate) { Text("Downloaden") }
            }
        },
        dismissButton = {
            if (!vm.updateDownloading) {
                TextButton(onClick = vm::dismissUpdate) { Text("Later") }
            }
        },
    )
}
