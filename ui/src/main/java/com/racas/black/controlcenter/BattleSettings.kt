package com.racas.black.controlcenter

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A landscape game settings screen: direct controls first, advanced tools deliberately separate. */
@Composable
fun BattleSettings(
    host: ControlCenterHost, settings: GameSettings, game: GameStatus, section: ControlSection,
    onSection: (ControlSection) -> Unit, onSettings: ((GameSettings) -> GameSettings) -> Unit,
    onPreview: () -> Unit, onPlay: () -> Unit, onLive: () -> Unit,
    advanced: @Composable (ControlSection) -> Unit,
) {
    val background = MaterialTheme.colorScheme.background
    Row(Modifier.fillMaxSize().background(background).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Column(Modifier.width(148.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainerLow)
            .verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("BLACK", fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp,
                modifier = Modifier.padding(start = 10.dp, top = 6.dp, bottom = 14.dp))
            listOf(ControlSection.Home, ControlSection.Controls, ControlSection.Display, ControlSection.Audio, ControlSection.More).forEach { item ->
                val active = item == section || (item == ControlSection.More && !section.primary)
                Surface(color = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().clickable { onSection(item) }) {
                    Text(when(item) { ControlSection.Home -> "Play"; ControlSection.Display -> "Graphics"; else -> item.title },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(when(section) { ControlSection.Home -> "Ready when you are"; ControlSection.Display -> "Graphics"; else -> section.title },
                    style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Button(onClick = onPlay, enabled = game is GameStatus.Ready || host.isPreview,
                    modifier = Modifier.heightIn(min = 48.dp)) { Text(if (host.isPreview) "Preview" else "Play / return") }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            when(section) {
                ControlSection.Home -> BattlePlay(host, game, onPlay)
                ControlSection.Controls -> BattleControls(settings, onSettings, onPreview, onLive, host.supportsLiveEditor)
                ControlSection.Display -> BattleGraphics(settings, onSettings)
                ControlSection.Audio -> BattleAudio(settings, onSettings)
                ControlSection.More -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                    Text("Advanced tools", style = MaterialTheme.typography.labelLarge)
                    listOf(ControlSection.Interface to "Appearance & accessibility", ControlSection.About to "Files, export & about",
                        ControlSection.Cheats to "Gameplay modifiers").forEach { (page, label) ->
                        TextButton(onClick = { onSection(page) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(label, modifier = Modifier.fillMaxWidth()) }
                    }
                    Text("Keyboard, controller and advanced graphics remain available below.", style = MaterialTheme.typography.bodySmall)
                    var extra by remember { mutableStateOf(false) }
                    OutlinedButton(onClick = { extra = !extra }, modifier = Modifier.padding(top = 12.dp)) { Text(if(extra) "Hide advanced settings" else "Show advanced settings") }
                    if (extra) {
                        Box(Modifier.height(480.dp)) { ControlsScreen(settings, onSettings, onPreview, onLive, host.supportsLiveEditor) }
                    }
                }
                else -> advanced(section)
            }
        }
    }
}

@Composable private fun BattleBody(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
}

@Composable private fun Tuning(label: String, value: Float, range: ClosedFloatingPointRange<Float>, text: String,
    change: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(text, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Slider(value = value.coerceIn(range), onValueChange = change, valueRange = range, modifier = Modifier.height(42.dp))
    }
}

@Composable private fun Toggle(label: String, value: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = change)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun BattleControls(s: GameSettings, update: ((GameSettings) -> GameSettings) -> Unit,
    preview: () -> Unit, live: () -> Unit, liveSupported: Boolean) {
    val layout = s.touchLayout
    BattleBody {
        Tuning("Camera speed", s.touchSensitivity, 0.25f..12f,
            "${(s.touchSensitivity * 100).toInt()}%") { v -> update { it.copy(touchSensitivity = (v * 20).toInt() / 20f) } }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = live, enabled = liveSupported, modifier = Modifier.heightIn(min = 48.dp)) { Text("Edit on game screen") }
            OutlinedButton(onClick = preview, modifier = Modifier.heightIn(min = 48.dp)) { Text("Layout studio") }
        }
        Text("In play: hold and release the settings gear to move or resize your HUD.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            Column(Modifier.weight(1f)) {
                Toggle("Fixed movement stick", layout.fixedStick) { v -> update { it.copy(touchLayout = layout.copy(fixedStick = v)) } }
                Tuning("Movement deadzone", layout.stickDeadzone.toFloat(), 0f..30f, "${layout.stickDeadzone}%") { v -> update { it.copy(touchLayout = layout.copy(stickDeadzone = v.toInt())) } }
            }
            Column(Modifier.weight(1f)) {
                Tuning("HUD opacity", layout.opacity.toFloat(), 20f..100f, "${layout.opacity}%") { v -> update { it.copy(touchLayout = layout.copy(opacity = v.toInt())) } }
                Toggle("Motion aiming", s.gyroMode != 0) { v -> update { it.copy(gyroMode = if(v) 1 else 0) } }
            }
        }
        Text("Grip preset", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Options.presets.filter { it.first != "custom" }.forEach { (id,label) ->
                FilterChip(selected = layout.preset == id && !layout.isCustom(), onClick = {
                    update { it.copy(touchLayout = TouchLayout.fromPreset(id).copy(opacity = layout.opacity,
                        scale = layout.scale, fixedStick = layout.fixedStick, stickDeadzone = layout.stickDeadzone)) }
                }, label = { Text(label) }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
        HorizontalDivider()
        Text("Optional extra fire buttons", style = MaterialTheme.typography.labelLarge)
        for ((id,label) in listOf("fire_left" to "Left fire button", "fire_zone" to "Wide fire zone")) {
            val place = layout.placeOrDefault(id)
            Toggle(label, !place.hidden) { show -> update { it.copy(touchLayout = layout.withPlace(place.copy(hidden = !show))) } }
        }
    }
}

@Composable private fun BattleGraphics(s: GameSettings, update: ((GameSettings) -> GameSettings) -> Unit) {
    BattleBody {
        Tuning("Render resolution", s.renderPercent.toFloat(), 20f..200f, "${s.renderPercent}%") { v -> update { it.copy(renderPercent = v.toInt()) } }
        Tuning("Field of view", s.fov.toFloat(), 45f..140f, "${s.fov}°") { v -> update { it.copy(fov = v.toInt()) } }
        Tuning("Frame limit", s.fpsCap.toFloat(), 0f..144f, if(s.fpsCap == 0) "Unlimited" else "${s.fpsCap} FPS") { v -> update { it.copy(fpsCap = v.toInt()) } }
        Toggle("Show FPS", s.fpsCounter) { v -> update { it.copy(fpsCounter = v) } }
        Toggle("V-sync", s.vsync) { v -> update { it.copy(vsync = v) } }
        Toggle("Reload blur", s.reloadBlur) { v -> update { it.copy(reloadBlur = v) } }
        Toggle("Bloom", s.bloom) { v -> update { it.copy(bloom = v) } }
        var details by remember { mutableStateOf(false) }
        TextButton(onClick = { details = !details }) { Text(if(details) "Less" else "Advanced graphics") }
        if(details) Box(Modifier.height(420.dp)) { DisplayScreen(s, update) }
    }
}

@Composable private fun BattleAudio(s: GameSettings, update: ((GameSettings) -> GameSettings) -> Unit) {
    BattleBody {
        Text("Output", style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow {
            listOf("Auto", "Mono", "Stereo", "Surround").forEachIndexed { i,label ->
                SegmentedButton(selected = s.audioOutput == i, onClick = { update { it.copy(audioOutput = i) } },
                    shape = SegmentedButtonDefaults.itemShape(i, 4)) { Text(label) }
            }
        }
        Toggle("Skip intro videos", s.skipIntros) { v -> update { it.copy(skipIntros = v) } }
        var extra by remember { mutableStateOf(false) }
        TextButton(onClick = { extra = !extra }) { Text("Language & advanced audio") }
        if(extra) Box(Modifier.height(360.dp)) { AudioScreen(s, update) }
    }
}

@Composable private fun BattlePlay(host: ControlCenterHost, game: GameStatus, play: () -> Unit) {
    val iso = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { if(it != null) host.importGame(it) }
    val folder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { if(it != null) host.importGame(it) }
    BattleBody {
        Text(when(game) { is GameStatus.Ready -> "Game ready"; is GameStatus.Importing -> "Importing ${game.name}";
            is GameStatus.Checking -> "Checking game files…"; is GameStatus.Error -> game.message; else -> "Choose your game files" },
            style = MaterialTheme.typography.headlineSmall)
        if(game is GameStatus.Ready) Text(game.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if(game is GameStatus.Importing) LinearProgressIndicator(progress = { game.progress }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = play, enabled = game is GameStatus.Ready || host.isPreview, modifier = Modifier.heightIn(min = 52.dp)) { Text("Start game") }
            OutlinedButton(onClick = { folder.launch(null) }, enabled = game !is GameStatus.Importing) { Text("Choose folder") }
            OutlinedButton(onClick = { iso.launch(arrayOf("*/*")) }, enabled = game !is GameStatus.Importing) { Text("Import ISO") }
        }
        if(game is GameStatus.Missing && !game.storageGranted) TextButton(onClick = host::requestStorageAccess) { Text("Allow storage access") }
        Text("${host.appVersion}  ·  Personal edition", style = MaterialTheme.typography.bodySmall)
    }
}
