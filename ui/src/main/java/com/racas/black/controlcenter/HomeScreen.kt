package com.racas.black.controlcenter

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    game: GameStatus,
    version: String,
    preview: Boolean,
    onPlay: () -> Unit,
    onImport: (android.net.Uri) -> Unit,
    onRequestStorage: () -> Unit,
) {
    // the host accepts both single files and SAF folders; the folder picker is how a game folder gets in
    val isoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImport(uri)
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) onImport(uri)
    }
    SettingsPage {
        item {
            HeroCard(version = version, preview = preview, game = game, onPlay = onPlay)
        }
        item {
            GameCard(
                game = game,
                preview = preview,
                onPlay = onPlay,
                onChooseFolder = { folderPicker.launch(null) },
                onImportIso = { isoPicker.launch(arrayOf("*/*")) },
                onRequestStorage = onRequestStorage,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroCard(
    version: String,
    preview: Boolean,
    game: GameStatus,
    onPlay: () -> Unit,
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
            Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                Text(
                    "BLACK",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color(0xFFF2E9DA),
                    letterSpacing = 6.sp,
                    maxLines = 1,
                )
                Spacer(Modifier.size(8.dp))
                // a flow row so a long version string and the badge wrap at large font scales instead of overflowing
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Control Center · $version",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFBDB5A8),
                    )
                    if (preview) {
                        Surface(color = Color(0xFF2B2518), shape = RoundedCornerShape(50)) {
                            Text(
                                "UI PREVIEW",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.size(18.dp))
                Button(
                    onClick = onPlay,
                    modifier = Modifier.heightIn(min = 56.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            preview -> "Preview only"
                            game is GameStatus.Ready -> "Play Black"
                            else -> "Set up game"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GameCard(
    game: GameStatus,
    preview: Boolean,
    onPlay: () -> Unit,
    onChooseFolder: () -> Unit,
    onImportIso: () -> Unit,
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
                    title = if (preview) "Game files found" else "Ready to play",
                    subtitle = game.path,
                    trailing = {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                    },
                )
                DividerInset()
                CardActions {
                    Button(onClick = onPlay, modifier = Modifier.heightIn(min = 52.dp)) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (preview) "Preview only" else "Play")
                    }
                    OutlinedButton(onClick = onChooseFolder, modifier = Modifier.heightIn(min = 52.dp)) {
                        Text("Choose game folder")
                    }
                    OutlinedButton(onClick = onImportIso, modifier = Modifier.heightIn(min = 52.dp)) {
                        Text("Import .iso")
                    }
                }
            }
            is GameStatus.Missing -> Column {
                SettingRow(
                    title = "Game files not found",
                    subtitle = "Put the game files in the Black folder, or pick them below.",
                    trailing = {
                        Icon(
                            Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp),
                        )
                    },
                )
                DividerInset()
                CardActions {
                    Button(onClick = onChooseFolder, modifier = Modifier.heightIn(min = 52.dp)) {
                        Text("Choose game folder")
                    }
                    OutlinedButton(onClick = onImportIso, modifier = Modifier.heightIn(min = 52.dp)) {
                        Text("Import .iso")
                    }
                    OutlinedButton(onClick = onRequestStorage, modifier = Modifier.heightIn(min = 52.dp)) {
                        Text(if (game.storageGranted) "Rescan" else "Allow access")
                    }
                }
                if (!game.storageGranted) {
                    Text(
                        "All-files access lets Black read /storage/emulated/0/Black on its own. Both pickers work " +
                            "without it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
                    )
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
                    "Copying the game files. Keep this screen open.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            is GameStatus.Error -> Column {
                SettingRow(
                    title = "Could not find or import the game",
                    subtitle = game.message,
                    trailing = {
                        Icon(
                            Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp),
                        )
                    },
                )
                DividerInset()
                CardActions {
                    Button(onClick = onChooseFolder, modifier = Modifier.heightIn(min = 52.dp)) {
                        Text("Choose game folder")
                    }
                    OutlinedButton(onClick = onImportIso, modifier = Modifier.heightIn(min = 52.dp)) {
                        Text("Import .iso")
                    }
                    OutlinedButton(onClick = onRequestStorage, modifier = Modifier.heightIn(min = 52.dp)) {
                        Text("Rescan")
                    }
                }
            }
        }
    }
}

// flow row rather than one row: the three actions wrap on a phone instead of shrinking below a 48 dp target
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardActions(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}
