package com.racas.black.controlcenter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ControlsScreen(
    settings: GameSettings,
    onSettings: ((GameSettings) -> GameSettings) -> Unit,
    onEditLayout: () -> Unit,
) {
    var capturing by remember { mutableStateOf<Bindings.Action?>(null) }
    val layout = settings.touchLayout
    val presets = buildList {
        addAll(Options.presets.filter { it.first != "custom" })
        if (layout.isCustom()) add("custom" to "Custom")
    }
    val presetIndex = presets.indexOfFirst { it.first == if (layout.isCustom()) "custom" else layout.preset }
        .takeIf { it >= 0 } ?: 0

    SettingsPage {
        item {
            PageHeader(
                eyebrow = "Hands on",
                title = "Controls",
                subtitle = "Every touch button, the layout, gyroscope, controller sticks, mouse and keyboard. " +
                    "Nothing here is locked to a preset.",
            )
        }
        item {
            SettingsGroup("Touch layout", "The controls are drawn over the game; the preview stays in the editor.") {
                ChoiceSetting(
                    title = "Preset",
                    subtitle = "A starting point. Moving any control makes it a custom layout.",
                    options = presets.map { it.second },
                    selectedIndex = presetIndex,
                    onSelect = { i ->
                        val id = presets.getOrNull(i)?.first ?: "default"
                        if (id != "custom") {
                            val next = TouchLayout.fromPreset(id).copy(
                                opacity = layout.opacity,
                                scale = layout.scale,
                                skin = layout.skin,
                            )
                            onSettings { it.copy(touchLayout = next) }
                        }
                    },
                )
                DividerInset()
                SliderSetting(
                    title = "Button size",
                    subtitle = "Every touch control's size, on top of its own radius.",
                    valueText = "${layout.scale}%",
                    value = layout.scale.toFloat(),
                    range = 50f..150f,
                    steps = 99,
                    onValueChange = { v -> onSettings { it.copy(touchLayout = layout.copy(scale = v.toInt())) } },
                )
                DividerInset()
                SliderSetting(
                    title = "Opacity",
                    subtitle = "How strongly the controls sit over the picture.",
                    valueText = "${layout.opacity}%",
                    value = layout.opacity.toFloat(),
                    range = 20f..100f,
                    steps = 79,
                    onValueChange = { v -> onSettings { it.copy(touchLayout = layout.copy(opacity = v.toInt())) } },
                )
                DividerInset()
                ChoiceSetting(
                    title = "Button style",
                    subtitle = "Modern glass, Xbox marks, clean icons, or the original drawn buttons.",
                    options = Options.skins.map { it.second },
                    selectedIndex = Options.skins.indexOfFirst { it.first == layout.skin }.coerceAtLeast(0),
                    onSelect = { i -> onSettings { it.copy(touchLayout = layout.copy(skin = Options.skins[i].first)) } },
                )
                DividerInset()
                val zone = layout.placeOrDefault("fire_zone")
                SwitchSetting(
                    title = "Top-left fire zone",
                    subtitle = "A wide, transparent primary fire button for the left index finger. " +
                        "Turn it on, then drag or resize it in the editor.",
                    checked = !zone.hidden,
                    onCheckedChange = { on ->
                        onSettings {
                            it.copy(touchLayout = layout.withPlace(zone.copy(hidden = !on)))
                        }
                    },
                )
                if (!zone.hidden) {
                    DividerInset()
                    SliderSetting(
                        title = "Fire zone size",
                        subtitle = "Height of the rectangle as a share of the screen; its width follows.",
                        valueText = "${(zone.r * 100).toInt()}%",
                        value = zone.r * 100f,
                        range = 8f..22f,
                        steps = 13,
                        onValueChange = { v ->
                            onSettings {
                                it.copy(touchLayout = layout.withPlace(zone.copy(r = v / 100f)))
                            }
                        },
                    )
                }
                DividerInset()
                ActionSetting(
                    title = "Edit control positions",
                    subtitle = "Drag any button, resize it with the slider or pinch, and hide what you do not use.",
                    button = "Open editor",
                    icon = Icons.Rounded.Gesture,
                    onClick = onEditLayout,
                )
            }
        }
        item {
            InfoBanner(
                title = "Full layout editor",
                text = "The editor shows the game canvas at your current layout, in play or menu mode. " +
                    "Moves and sizes are written to layout.toml for all controls, including the menu D-pad.",
            )
        }
        item {
            SettingsGroup("Aim") {
                SliderSetting(
                    title = "Touch sensitivity",
                    subtitle = "How far a drag on the right side turns the view. Separate from the mouse.",
                    valueText = String.format(java.util.Locale.US, "%.1f×", settings.touchSensitivity),
                    value = settings.touchSensitivity,
                    range = 0.1f..5.0f,
                    steps = 48,
                    onValueChange = { v -> onSettings { it.copy(touchSensitivity = round1(v)) } },
                )
                DividerInset()
                SegmentedSetting(
                    title = "Gyroscope",
                    subtitle = "Use the phone's motion sensor, or a DualShock 4 / DualSense gyro.",
                    options = Options.gyroModes,
                    selectedIndex = settings.gyroMode.coerceIn(0, 2),
                    onSelect = { i -> onSettings { it.copy(gyroMode = i) } },
                )
                DividerInset()
                SliderSetting(
                    title = "Gyro sensitivity",
                    subtitle = "View degrees per degree the phone turns.",
                    valueText = String.format(java.util.Locale.US, "%.1f×", settings.gyroSensitivity),
                    value = settings.gyroSensitivity,
                    range = 0.1f..5.0f,
                    steps = 48,
                    enabled = settings.gyroMode != 0,
                    onValueChange = { v -> onSettings { it.copy(gyroSensitivity = round1(v)) } },
                )
                DividerInset()
                SwitchSetting(
                    title = "Invert gyro X",
                    subtitle = "A turn left looks right.",
                    checked = settings.gyroInvertX,
                    enabled = settings.gyroMode != 0,
                    onCheckedChange = { on -> onSettings { it.copy(gyroInvertX = on) } },
                )
                DividerInset()
                SwitchSetting(
                    title = "Invert gyro Y",
                    subtitle = "A tilt up looks down.",
                    checked = settings.gyroInvertY,
                    enabled = settings.gyroMode != 0,
                    onCheckedChange = { on -> onSettings { it.copy(gyroInvertY = on) } },
                )
            }
        }
        item {
            SettingsGroup("Controller sticks", "A radial dead zone and a sensitivity for each stick.") {
                SliderSetting(
                    title = "Left stick dead zone",
                    subtitle = "0% leaves the game's own dead zone.",
                    valueText = if (settings.leftDeadzone == 0) "Game default" else "${settings.leftDeadzone}%",
                    value = settings.leftDeadzone.toFloat(),
                    range = 0f..50f,
                    steps = 49,
                    onValueChange = { v -> onSettings { it.copy(leftDeadzone = v.toInt()) } },
                )
                DividerInset()
                SliderSetting(
                    title = "Left stick sensitivity",
                    valueText = String.format(java.util.Locale.US, "%.1f×", settings.leftSensitivity),
                    value = settings.leftSensitivity,
                    range = 0.1f..3.0f,
                    steps = 28,
                    onValueChange = { v -> onSettings { it.copy(leftSensitivity = round1(v)) } },
                )
                DividerInset()
                SliderSetting(
                    title = "Right stick dead zone",
                    subtitle = "0% leaves the game's own dead zone.",
                    valueText = if (settings.rightDeadzone == 0) "Game default" else "${settings.rightDeadzone}%",
                    value = settings.rightDeadzone.toFloat(),
                    range = 0f..50f,
                    steps = 49,
                    onValueChange = { v -> onSettings { it.copy(rightDeadzone = v.toInt()) } },
                )
                DividerInset()
                SliderSetting(
                    title = "Right stick sensitivity",
                    valueText = String.format(java.util.Locale.US, "%.1f×", settings.rightSensitivity),
                    value = settings.rightSensitivity,
                    range = 0.1f..3.0f,
                    steps = 28,
                    onValueChange = { v -> onSettings { it.copy(rightSensitivity = round1(v)) } },
                )
            }
        }
        item {
            SettingsGroup("Mouse") {
                SliderSetting(
                    title = "Mouse sensitivity",
                    valueText = String.format(java.util.Locale.US, "%.1f×", settings.mouseSensitivity),
                    value = settings.mouseSensitivity,
                    range = 0.1f..5.0f,
                    steps = 48,
                    onValueChange = { v -> onSettings { it.copy(mouseSensitivity = round1(v)) } },
                )
                DividerInset()
                SwitchSetting(
                    title = "Invert look",
                    checked = settings.invertMouse,
                    onCheckedChange = { on -> onSettings { it.copy(invertMouse = on) } },
                )
                DividerInset()
                SwitchSetting(
                    title = "Hold to aim",
                    subtitle = "Scopes too. Off makes aim a toggle.",
                    checked = settings.holdAim,
                    onCheckedChange = { on -> onSettings { it.copy(holdAim = on) } },
                )
                DividerInset()
                SliderSetting(
                    title = "Walk speed",
                    subtitle = "Share of running while the walk key is held.",
                    valueText = "${settings.walkSpeed}%",
                    value = settings.walkSpeed.toFloat(),
                    range = 10f..100f,
                    steps = 17,
                    onValueChange = { v -> onSettings { it.copy(walkSpeed = (v / 5).toInt() * 5) } },
                )
            }
        }
        item {
            SettingsGroup(
                "Keyboard bindings",
                "Tap a binding and press a key on a hardware keyboard. An external mouse's buttons work too; " +
                    "the names follow the PC build.",
            ) {
                Bindings.actions.forEachIndexed { index, action ->
                    if (index > 0) DividerInset()
                    BindingRow(
                        action = action,
                        value = Bindings.effective(settings.bindingsOverrides, action.id),
                        onClick = { capturing = action },
                    )
                }
            }
        }
        item {
            SettingsGroup("Reset") {
                ActionSetting(
                    title = "Restore control defaults",
                    subtitle = "Touch layout, aim, gyro, sticks, mouse and keyboard all go back to the port's defaults.",
                    button = "Restore",
                    icon = Icons.Rounded.RestartAlt,
                    onClick = {
                        val d = GameSettings()
                        onSettings {
                            it.copy(
                                touchSensitivity = d.touchSensitivity,
                                touchLayout = d.touchLayout,
                                gyroMode = d.gyroMode,
                                gyroSensitivity = d.gyroSensitivity,
                                gyroInvertX = d.gyroInvertX,
                                gyroInvertY = d.gyroInvertY,
                                leftDeadzone = d.leftDeadzone,
                                rightDeadzone = d.rightDeadzone,
                                leftSensitivity = d.leftSensitivity,
                                rightSensitivity = d.rightSensitivity,
                                mouseSensitivity = d.mouseSensitivity,
                                invertMouse = d.invertMouse,
                                holdAim = d.holdAim,
                                walkSpeed = d.walkSpeed,
                                bindingsOverrides = emptyMap(),
                            )
                        }
                    },
                )
            }
        }
    }

    capturing?.let { action ->
        KeyCaptureDialog(
            action = action,
            current = Bindings.effective(settings.bindingsOverrides, action.id),
            onDismiss = { capturing = null },
            onBind = { key ->
                capturing = null
                onSettings {
                    it.copy(bindingsOverrides = it.bindingsOverrides + (action.id to key))
                }
            },
            onClear = {
                capturing = null
                // an explicit empty value: removing the override would put the engine's default back
                onSettings {
                    it.copy(bindingsOverrides = it.bindingsOverrides + (action.id to ""))
                }
            },
        )
    }
}

@Composable
private fun BindingRow(action: Bindings.Action, value: String, onClick: () -> Unit) {
    SettingRow(
        title = action.label,
        subtitle = if (value.isBlank()) "Not bound" else null,
        onClick = onClick,
        trailing = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Keyboard,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        value.ifBlank { "—" },
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        },
    )
}

@Composable
private fun KeyCaptureDialog(
    action: Bindings.Action,
    current: String,
    onDismiss: () -> Unit,
    onBind: (String) -> Unit,
    onClear: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    var message by remember { mutableStateOf("Press a key or mouse button") }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
                .focusRequester(focus)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val code = event.nativeKeyEvent.keyCode
                    if (code == android.view.KeyEvent.KEYCODE_BACK) return@onPreviewKeyEvent false
                    val name = Bindings.androidKeyName(code)
                    if (name == null) {
                        message = "That key has no PC binding. Try a letter, number, F-key or arrow."
                        true
                    } else {
                        onBind(name)
                        true
                    }
                },
        ) {
            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Rebind ${action.label}", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Now: ${current.ifBlank { "not bound" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Text(
                        message,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(24.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onDismiss, modifier = Modifier.heightIn(min = 56.dp)) { Text("Cancel") }
                    OutlinedButton(onClick = onClear, modifier = Modifier.heightIn(min = 56.dp)) {
                        Icon(Icons.Rounded.Backspace, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Clear")
                    }
                }
                Text(
                    "Bindings with two keys (like the weapon wheel) are supported by the engine; this screen sets " +
                        "the primary key. Use the in-game options for an alternate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun round1(v: Float): Float = kotlin.math.round(v * 10f) / 10f
