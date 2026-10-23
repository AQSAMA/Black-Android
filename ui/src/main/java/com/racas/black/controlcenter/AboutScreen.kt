package com.racas.black.controlcenter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val PROJECT_URL = "https://github.com/RanggaCasper/Black-Android"
private const val ISSUES_URL = "$PROJECT_URL/issues"
private const val RELEASES_URL = "$PROJECT_URL/releases"

@Composable
fun AboutScreen(
    host: ControlCenterHost,
    prefs: InterfacePreferences,
    version: String,
    settings: GameSettings,
) {
    SettingsPage {
        item {
            PageHeader(
                eyebrow = "About",
                title = "Black Control Center",
                subtitle = "A Material 3 front end for the Black Android port. Settings are written into the game's " +
                    "own files, so the game and this screen always agree.",
            )
        }
        item {
            SettingsGroup("This install") {
                SettingRow(
                    title = "Version",
                    subtitle = if (host.isPreview) {
                        "UI preview build. The game is not included; the controls run against demo data."
                    } else {
                        "Installed over the game, sharing its settings and saves."
                    },
                    trailing = {
                        Text(version, style = MaterialTheme.typography.titleMedium)
                    },
                )
                DividerInset()
                SettingRow(
                    title = "Game settings folder",
                    subtitle = host.settingsStore.directory.absolutePath,
                )
                DividerInset()
                SettingRow(
                    title = "Theme in use",
                    subtitle = "${prefs.themeMode.label} · ${prefs.accent.label} · ${(prefs.uiScale * 100).toInt()}%",
                )
            }
        }
        item {
            SettingsGroup("Settings") {
                ActionSetting(
                    title = "Share a settings snapshot",
                    subtitle = "Sends black.toml, android.toml and layout.toml as readable text. Useful in a bug report.",
                    button = "Share",
                    icon = Icons.Rounded.Share,
                    onClick = { host.shareSettings(settings) },
                )
                DividerInset()
                ActionSetting(
                    title = "Export to the Downloads folder",
                    subtitle = "Writes the same snapshot to a file you can copy off the phone.",
                    button = "Export",
                    icon = Icons.Rounded.Description,
                    onClick = { host.exportSettings(settings) },
                )
            }
        }
        item {
            SettingsGroup("Project") {
                ActionSetting(
                    title = "Source code",
                    subtitle = "Black Android on GitHub. The port, its native code and this interface.",
                    button = "Open",
                    icon = Icons.Rounded.Code,
                    onClick = { host.openUrl(PROJECT_URL) },
                )
                DividerInset()
                ActionSetting(
                    title = "Report a problem",
                    subtitle = "Open an issue with the settings snapshot and what happened.",
                    button = "Issues",
                    icon = Icons.Rounded.BugReport,
                    onClick = { host.openUrl(ISSUES_URL) },
                )
                DividerInset()
                ActionSetting(
                    title = "All releases",
                    subtitle = "Stable builds and preview APKs.",
                    button = "Releases",
                    icon = Icons.Rounded.OpenInNew,
                    onClick = { host.openUrl(RELEASES_URL) },
                )
            }
        }
        item {
            SettingsGroup("Made with") {
                Text(
                    "Jetpack Compose and Material 3 for the interface. SDL3, Vulkan, Dear ImGui and toml++ underneath " +
                        "the port. The game itself is the original Xbox release; it is not included in this app or " +
                        "its preview APK.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
                DividerInset()
                Row(
                    Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Independent rebuild", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "No game data is bundled. Bring your own disc image.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "BLACK",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp,
                    )
                }
            }
        }
        item {
            Row(
                Modifier.padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Links open in your browser. The Control Center never uploads your settings on its own.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
