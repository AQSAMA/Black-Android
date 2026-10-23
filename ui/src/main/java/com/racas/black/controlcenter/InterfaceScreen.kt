package com.racas.black.controlcenter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InterfaceScreen(
    prefs: InterfacePreferences,
    onPrefs: ((InterfacePreferences) -> InterfacePreferences) -> Unit,
) {
    SettingsPage {
        item {
            PageHeader(
                eyebrow = "Make it yours",
                title = "Interface",
                subtitle = "The Control Center is built from Material 3 components with large touch targets. " +
                    "Theme, accent, scale, corner shape and navigation are yours.",
            )
        }
        item {
            SettingsGroup("Appearance") {
                SegmentedSetting(
                    title = "Theme",
                    subtitle = "OLED black is true black; System follows Android.",
                    options = ThemeMode.entries.map { it.label },
                    selectedIndex = ThemeMode.entries.indexOf(prefs.themeMode),
                    onSelect = { i -> onPrefs { it.copy(themeMode = ThemeMode.entries[i]) } },
                )
                DividerInset()
                Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Accent colour", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Surface(color = accentColor(prefs.accent), shape = CircleShape) {
                            Spacer(Modifier.size(22.dp))
                        }
                    }
                    Spacer(Modifier.size(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Accent.entries.forEach { accent ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AccentDot(
                                    color = accentColor(accent),
                                    selected = accent == prefs.accent,
                                    onClick = { onPrefs { it.copy(accent = accent) } },
                                )
                                Spacer(Modifier.size(4.dp))
                                Text(
                                    accent.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                DividerInset()
                SegmentedSetting(
                    title = "Corner shape",
                    subtitle = "Square is closest to the original menus; round is the default.",
                    options = CornerStyle.entries.map { it.label },
                    selectedIndex = CornerStyle.entries.indexOf(prefs.cornerStyle),
                    onSelect = { i -> onPrefs { it.copy(cornerStyle = CornerStyle.entries[i]) } },
                )
                DividerInset()
                SwitchSetting(
                    title = "High contrast",
                    subtitle = "Stronger text and outline contrast, for sunlight and low-vision use.",
                    checked = prefs.highContrast,
                    onCheckedChange = { on -> onPrefs { it.copy(highContrast = on) } },
                )
            }
        }
        item {
            SettingsGroup("Size & layout", "The whole interface scales, not just text.") {
                SliderSetting(
                    title = "Interface scale",
                    subtitle = "Grows touch targets, spacing and type together.",
                    valueText = "${(prefs.uiScale * 100).toInt()}%",
                    value = prefs.uiScale,
                    range = 0.8f..1.3f,
                    steps = 9,
                    onValueChange = { v -> onPrefs { it.copy(uiScale = round05(v)) } },
                )
                DividerInset()
                SliderSetting(
                    title = "Text scale",
                    subtitle = "Affects type only, on top of the interface scale.",
                    valueText = "${(prefs.fontScale * 100).toInt()}%",
                    value = prefs.fontScale,
                    range = 0.85f..1.35f,
                    steps = 9,
                    onValueChange = { v -> onPrefs { it.copy(fontScale = round05(v)) } },
                )
                DividerInset()
                SegmentedSetting(
                    title = "Navigation",
                    subtitle = "Automatic uses the side rail on tablets and landscape phones.",
                    options = NavigationStyle.entries.map { it.label },
                    selectedIndex = NavigationStyle.entries.indexOf(prefs.navigation),
                    onSelect = { i -> onPrefs { it.copy(navigation = NavigationStyle.entries[i]) } },
                )
            }
        }
        item {
            SettingsGroup("Comfort") {
                SwitchSetting(
                    title = "Haptics",
                    subtitle = "A short tick on selection and save.",
                    checked = prefs.haptics,
                    onCheckedChange = { on -> onPrefs { it.copy(haptics = on) } },
                )
                DividerInset()
                SwitchSetting(
                    title = "Reduce motion",
                    subtitle = "Cuts transitions and scrolling flourishes.",
                    checked = prefs.reduceMotion,
                    onCheckedChange = { on -> onPrefs { it.copy(reduceMotion = on) } },
                )
                DividerInset()
                SwitchSetting(
                    title = "Keep screen on",
                    subtitle = "Stops Android dimming the panel while the Control Center or game is open.",
                    checked = prefs.keepScreenOn,
                    onCheckedChange = { on -> onPrefs { it.copy(keepScreenOn = on) } },
                )
            }
        }
        item {
            SettingsGroup("Preview") {
                Row(
                    Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.width(96.dp),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("BLACK", style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp)
                            Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) {
                                Spacer(Modifier.size(20.dp))
                            }
                            Text("Aa", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Icon(
                                Icons.Rounded.Contrast,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("${prefs.accent.label} · ${prefs.themeMode.label}", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            "Every screen uses these colours. The game's own ImGui panel keeps its brass theme.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            SettingsGroup("Reset") {
                ActionSetting(
                    title = "Restore interface defaults",
                    subtitle = "OLED black, brass accent, 100% scale, round corners, automatic navigation.",
                    button = "Restore",
                    icon = Icons.Rounded.RestartAlt,
                    onClick = { onPrefs { InterfacePreferences() } },
                )
            }
        }
    }
}

private fun round05(v: Float): Float = kotlin.math.round(v * 20f) / 20f
