package com.sherif.ledger.feature.update.presentation

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.sherif.ledger.BuildConfig

/**
 * Shared by both the automatic launch-time check (MainActivity) and the
 * manual "Check for Updates" row (ProfileScreen) — same dialog, same
 * install flow, so there's only one place that has to get the
 * REQUEST_INSTALL_PACKAGES redirect right.
 */
@Composable
fun UpdateDialog(
    state: UpdateUiState,
    onDismiss: () -> Unit,
    onInstall: () -> Unit,
) {
    val context = LocalContext.current
    val update = state.available

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    state.checking -> "Checking for updates…"
                    update != null -> "Update available"
                    else -> "You're up to date"
                },
            )
        },
        text = {
            Text(
                when {
                    state.checking -> "Looking for a newer build on GitHub."
                    update != null ->
                        "Version ${update.versionName} is available (you have ${BuildConfig.VERSION_NAME})."
                    state.downloadFailed -> "Couldn't download the update. Check your connection and try again."
                    else -> "You're running the latest build."
                },
            )
        },
        confirmButton = {
            if (update != null) {
                TextButton(
                    onClick = {
                        if (context.packageManager.canRequestPackageInstalls()) {
                            onInstall()
                        } else {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:${context.packageName}"),
                                ),
                            )
                        }
                    },
                    enabled = !state.downloading,
                ) {
                    Text(if (state.downloading) "Downloading…" else "Update")
                }
            } else {
                TextButton(onClick = onDismiss) { Text("OK") }
            }
        },
        dismissButton = {
            if (update != null) {
                TextButton(onClick = onDismiss) { Text("Later") }
            }
        },
    )
}
