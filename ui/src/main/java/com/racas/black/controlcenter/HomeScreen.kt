package com.racas.black.controlcenter

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Monitor
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    settings: GameSettings,
    prefs: InterfacePreferences,
    game: GameStatus,
    update: UpdateState,
    version: String,
    preview: Boolean,
    onPlay: () -> Unit,
    onImport: (android.net.Uri) -> Unit,
    onRequestStorage: () -> Unit,
    onCheckUpdates: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenSection: (ControlSection) -> Unit,
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImport(uri)
    }
    SettingsPage {
        item {
            HeroCard(
                settings = settings,
                version = version,
                preview = preview,
                game = game,
                onPlay = onPlay,
                onOpenSection = onOpenSection,
            )
        }
        item {
            GameCard(
                game = game,
                onPlay = onPlay,
                onImport = { picker.launch(arrayOf("*/*")) },
                onRequestStorage = onRequestStorage,
            )
        }
        item {
            SettingsGroup("At a glance") {
                NavigationSetting(
                    "Display",
                    "${settings.renderPercent}% render · ${settings.fov}° FOV · ${Options.labelFps(settings.fpsCap)}",
                ) { onOpenSection(ControlSection.Display) }
                DividerInset()
                NavigationSetting(
                    "Touch controls",
                    "${settings.touchLayout.scale}% size · ${settings.touchLayout.opacity}% opacity · " +
                        if (settings.touchLayout.isCustom()) "custom layout" else prefsLabel(settings.touchLayout.preset),
                ) { onOpenSection(ControlSection.Controls) }
                DividerInset()
                NavigationSetting(
                    "Audio",
                    Options.audioOutputs.getOrElse(settings.audioOutput) { "Automatic" },
                ) { onOpenSection(ControlSection.Audio) }
                DividerInset()
                NavigationSetting(
                    "Cheats",
                    if (settings.godMode || settings.infiniteAmmo) "Some cheats are on" else "Off",
                ) { onOpenSection(ControlSection.Cheats) }
            }
        }
        item {
            UpdateCard(update = update, version = version, onCheck = onCheckUpdates, onOpenUrl = onOpenUrl)
        }
        item {
            Text(
                "Settings save automatically. The game picks them up the next time it draws a frame, even if it is " +
                    "running behind this screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun HeroCard(
    settings: GameSettings,
    version: String,
    preview: Boolean,
    game: GameStatus,
    onPlay: () -> Unit,
    onOpenSection: (ControlSection) -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = Color(0xFF0B0A08),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
    ) {
        Box(
            Modifier.background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF050505),
                        Color(0xFF17120A),
                        Color(0xFF2A1D0A),
                    ),
                ),
            ),
        ) {
            Column(Modifier.padding(horizontal = 28.dp, vertical = 26.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "XBOX ORIGINAL · REBUILT FOR ANDROID",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        letterSpacing = 2.sp,
                    )
                    if (preview) {
                        Spacer(Modifier.width(12.dp))
                        Surface(
                            color = Color(0xFF2B2518),
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                "UI PREVIEW",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    "BLACK",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color(0xFFF2E9DA),
                    letterSpacing = 8.sp,
                )
                Text(
                    "The interface the port should have shipped with.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFBDB5A8),
                )
                Spacer(Modifier.size(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onPlay,
                        modifier = Modifier.heightIn(min = 60.dp),
                        contentPadding = PaddingValues(horizontal = 26.dp),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (game is GameStatus.Ready) "Play Black" else "Set up game",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    OutlinedButton(
                        onClick = { onOpenSection(ControlSection.Interface) },
                        modifier = Modifier.heightIn(min = 60.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF2E9DA)),
                        border = BorderStroke(1.dp, Color(0xFF6A5A3C)),
                    ) {
                        Icon(Icons.Rounded.Palette, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Customize")
                    }
                }
                Spacer(Modifier.size(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    HeroStat("VERSION", version)
                    HeroStat("RENDER", "${settings.renderPercent}%")
                    HeroStat("FPS", Options.labelFps(settings.fpsCap))
                    HeroStat("BUTTONS", "${settings.touchLayout.scale}%")
                }
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF8B8275), letterSpacing = 1.5.sp)
        Text(value, style = MaterialTheme.typography.titleMedium, color = Color(0xFFE9E1D4))
    }
}

@Composable
private fun GameCard(
    game: GameStatus,
    onPlay: () -> Unit,
    onImport: () -> Unit,
    onRequestStorage: () -> Unit,
) {
    SettingsGroup("Game files") {
        when (game) {
            GameStatus.Checking -> Row(
                Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LinearProgressIndicator(Modifier.weight(1f))
            }
            is GameStatus.Ready -> Column {
                SettingRow(
                    title = "Ready to play",
                    subtitle = game.path,
                    trailing = {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp),
                        )
                    },
                )
                DividerInset()
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onPlay, modifier = Modifier.heightIn(min = 56.dp)) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Play")
                    }
                    OutlinedButton(onClick = onImport, modifier = Modifier.heightIn(min = 56.dp)) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Import another image")
                    }
                }
            }
            is GameStatus.Missing -> Column {
                SettingRow(
                    title = "Game files not found",
                    subtitle = "Put default.xbe and its folders, or an .iso, in the Black folder on your phone.",
                    trailing = {
                        Icon(
                            Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(30.dp),
                        )
                    },
                )
                DividerInset()
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!game.storageGranted) {
                        InfoBanner(
                            title = "Storage access needed",
                            text = "Black reads the game from /storage/emulated/0/Black. Grant all-files access, or " +
                                "import the image below; the import works without it.",
                            tonal = true,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onImport, modifier = Modifier.heightIn(min = 56.dp)) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Import .iso")
                        }
                        OutlinedButton(onClick = onRequestStorage, modifier = Modifier.heightIn(min = 56.dp)) {
                            Text(if (game.storageGranted) "Rescan" else "Allow access")
                        }
                    }
                }
            }
            is GameStatus.Importing -> Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(game.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text("${(game.progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.size(12.dp))
                LinearProgressIndicator(progress = { game.progress }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.size(8.dp))
                Text(
                    "Copying the disc image. Keep this screen open; the copy is written as .part and renamed when it " +
                        "finishes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            is GameStatus.Error -> Column {
                SettingRow(
                    title = "Could not find or import the game",
                    subtitle = game.message,
                    trailing = {
                        Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                    },
                )
                DividerInset()
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onImport, modifier = Modifier.heightIn(min = 56.dp)) {
                        Text("Try again")
                    }
                    OutlinedButton(onClick = onRequestStorage, modifier = Modifier.heightIn(min = 56.dp)) {
                        Text("Storage settings")
                    }
                }
            }
        }
    }
}

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
                "Checking GitHub…",
                "Looking for a newer release.",
                trailing = { LinearProgressIndicator(Modifier.width(120.dp)) },
            )
            UpdateState.Latest -> SettingRow(
                "You are up to date",
                "The latest release is installed.",
                trailing = { Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
            )
            is UpdateState.Available -> Column {
                SettingRow(
                    title = update.name.ifBlank { update.tag },
                    subtitle = update.changes.ifBlank { "A newer release is available." },
                )
                DividerInset()
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onOpenUrl(update.apk ?: update.page) },
                        modifier = Modifier.heightIn(min = 56.dp),
                    ) {
                        Icon(Icons.Rounded.Download, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Download")
                    }
                    TextButton(onClick = { onOpenUrl(update.page) }, modifier = Modifier.heightIn(min = 56.dp)) {
                        Text("Release notes")
                    }
                }
            }
            is UpdateState.Failed -> Column {
                SettingRow(
                    "Update check failed",
                    update.message,
                    trailing = { Icon(Icons.Rounded.SystemUpdate, null, tint = MaterialTheme.colorScheme.error) },
                )
                DividerInset()
                ActionSetting("Try again", "GitHub could not be reached.", "Check", Icons.Rounded.Refresh, onClick = onCheck)
            }
        }
    }
}

private fun prefsLabel(preset: String): String = when (preset) {
    "three" -> "3 finger"
    "four" -> "4 finger"
    else -> "default layout"
}
