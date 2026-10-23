package com.racas.black.controlcenter

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AudioScreen(
    settings: GameSettings,
    onSettings: ((GameSettings) -> GameSettings) -> Unit,
) {
    SettingsPage {
        item {
            PageHeader(
                eyebrow = "Sound",
                title = "Audio & language",
                subtitle = "The output Android should decode to, the language the game boots in, and the intro films. " +
                    "Black's own music and effects levels stay in the game's pause menu.",
            )
        }
        item {
            SettingsGroup("Output") {
                ChoiceSetting(
                    title = "Audio output",
                    subtitle = "Automatic follows the device and its headphones. Choose a layout only if Android " +
                        "guesses wrong.",
                    options = Options.audioOutputs,
                    selectedIndex = settings.audioOutput.coerceIn(0, Options.audioOutputs.lastIndex),
                    onSelect = { index -> onSettings { it.copy(audioOutput = index) } },
                )
                DividerInset()
                InfoBanner(
                    title = "Applied on the next sound",
                    text = "The port switches its mixer without restarting. Headphones and Bluetooth devices are " +
                        "still handled by Android.",
                )
            }
        }
        item {
            SettingsGroup("Language") {
                ChoiceSetting(
                    title = "Game language",
                    subtitle = "The port's own menus are translated separately. A change takes effect at the next launch.",
                    options = Options.languages.map { it.second },
                    selectedIndex = Options.languages.indexOfFirst { it.first == settings.language }.coerceAtLeast(0),
                    onSelect = { index -> onSettings { it.copy(language = Options.languages[index].first) } },
                )
                DividerInset()
                SwitchSetting(
                    title = "Skip intro films",
                    subtitle = "Jump straight to the main menu instead of playing the logos and the opening film.",
                    checked = settings.skipIntros,
                    onCheckedChange = { on -> onSettings { it.copy(skipIntros = on) } },
                )
            }
        }
        item {
            SettingsGroup("Accessibility") {
                InfoBanner(
                    title = "Mono is a real downmix",
                    text = "Mono output is mixed for one speaker or one earbud, rather than letting Android drop half " +
                        "of a stereo image. It is the safer choice with a single earbud.",
                )
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                )
                Text(
                    "The game's subtitles are controlled by the game and by the language above. The interface's text " +
                        "scale is in Interface, not here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
            }
        }
        item {
            SettingsGroup("Reset") {
                ActionSetting(
                    title = "Restore audio and language defaults",
                    subtitle = "Automatic output, English and the intro films on.",
                    button = "Restore",
                    icon = Icons.Rounded.RestartAlt,
                    onClick = {
                        val d = GameSettings()
                        onSettings {
                            it.copy(
                                audioOutput = d.audioOutput,
                                language = d.language,
                                skipIntros = d.skipIntros,
                            )
                        }
                    },
                )
                DividerInset()
                ActionSetting(
                    title = "Language codes",
                    subtitle = "English, French, German, Italian, Spanish and Indonesian.",
                    button = "About",
                    icon = Icons.Rounded.Translate,
                    onClick = {},
                    enabled = false,
                )
            }
        }
        item {
            Text(
                "Output modes are mixed by the port. Music and effects volumes are part of the original game's " +
                    "options, and the port does not replace them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}
