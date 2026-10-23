package com.racas.black.controlcenter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The pages that do not deserve their own bar destination, plus the version and the update check that used to
 * sit on Home.
 */
@Composable
fun MoreScreen(
    version: String,
    preview: Boolean,
    update: UpdateState,
    onCheckUpdates: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenSection: (ControlSection) -> Unit,
) {
    SettingsPage {
        item {
            SettingsGroup("Control Center") {
                NavigationSetting(
                    title = "Interface",
                    subtitle = "Theme, accent, size, corners and navigation",
                ) { onOpenSection(ControlSection.Interface) }
                DividerInset()
                NavigationSetting(
                    title = "Cheats",
                    subtitle = "Infinite ammo, god mode, unlocks and game speed",
                ) { onOpenSection(ControlSection.Cheats) }
                DividerInset()
                NavigationSetting(
                    title = "About",
                    subtitle = "Settings export, project links and credits",
                ) { onOpenSection(ControlSection.About) }
                DividerInset()
                SettingRow(
                    title = "Version",
                    subtitle = if (preview) "UI preview build; the game is not included."
                    else "Installed over the game, sharing its settings and saves.",
                    trailing = { Text(version, style = MaterialTheme.typography.titleMedium) },
                )
            }
        }
        item {
            UpdateCard(update = update, version = version, onCheck = onCheckUpdates, onOpenUrl = onOpenUrl)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UpdateCard(
    update: UpdateState,
    version: String,
    onCheck: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    SettingsGroup("Updates") {
        when (update) {
            UpdateState.Idle -> ActionSetting(
                title = "Check for updates",
                subtitle = "You are on $version. The app also checks each time it opens.",
                button = "Check",
                icon = Icons.Rounded.Refresh,
                onClick = onCheck,
            )
            UpdateState.Checking -> SettingRow(
                title = "Checking GitHub…",
                subtitle = "Looking for a newer release.",
                trailing = { LinearProgressIndicator(Modifier.width(120.dp)) },
            )
            UpdateState.Latest -> SettingRow(
                title = "You are up to date",
                subtitle = "The latest release is installed.",
                trailing = {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
            )
            is UpdateState.Available -> Column {
                SettingRow(
                    title = update.name.ifBlank { update.tag },
                    subtitle = update.changes.ifBlank { "A newer release is available." },
                )
                DividerInset()
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = { onOpenUrl(update.apk ?: update.page) },
                        modifier = Modifier.heightIn(min = 52.dp),
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Download")
                    }
                    TextButton(
                        onClick = { onOpenUrl(update.page) },
                        modifier = Modifier.heightIn(min = 52.dp),
                    ) {
                        Text("Release notes")
                    }
                }
            }
            is UpdateState.Failed -> ActionSetting(
                title = "Update check failed",
                subtitle = update.message,
                button = "Retry",
                icon = Icons.Rounded.SystemUpdate,
                onClick = onCheck,
            )
        }
    }
}
