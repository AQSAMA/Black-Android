package com.racas.black.controlcenter

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CheatsScreen(
    settings: GameSettings,
    onSettings: ((GameSettings) -> GameSettings) -> Unit,
) {
    val anyOn = settings.infiniteAmmo || settings.godMode || settings.oneHitKill ||
        settings.rapidFire > 1 || settings.gameSpeed != 100 || settings.noRecoil ||
        settings.infiniteGrenades || settings.unlockLevels || settings.unlockBlackOps

    SettingsPage {
        item {
            PageHeader(
                eyebrow = "Play your way",
                title = "Cheats",
                subtitle = "Every cheat Black ships, with plain names and large switches. They only change this run " +
                    "of the game; saves and the game files are left alone.",
            )
        }
        if (anyOn) {
            item {
                InfoBanner(
                    title = "Cheats are active",
                    text = "The game will show these on the next frame. If you are playing, open the game's pause " +
                        "menu or the gear panel to see the effect.",
                    tonal = true,
                )
            }
        }
        item {
            SettingsGroup("Weapons & combat") {
                SwitchSetting(
                    title = "Infinite ammunition",
                    subtitle = "Magazine and reserve never go down.",
                    checked = settings.infiniteAmmo,
                    onCheckedChange = { on -> onSettings { it.copy(infiniteAmmo = on) } },
                )
                DividerInset()
                SwitchSetting(
                    title = "No recoil",
                    subtitle = "Shots no longer kick the view. Accuracy is unchanged.",
                    checked = settings.noRecoil,
                    onCheckedChange = { on -> onSettings { it.copy(noRecoil = on) } },
                )
                DividerInset()
                SwitchSetting(
                    title = "One-hit kill",
                    subtitle = "Any bullet that hits an enemy kills it.",
                    checked = settings.oneHitKill,
                    onCheckedChange = { on -> onSettings { it.copy(oneHitKill = on) } },
                )
                DividerInset()
                SwitchSetting(
                    title = "Infinite grenades",
                    subtitle = "Keep the count when a grenade is thrown.",
                    checked = settings.infiniteGrenades,
                    onCheckedChange = { on -> onSettings { it.copy(infiniteGrenades = on) } },
                )
                DividerInset()
                SliderSetting(
                    title = "Rapid fire",
                    subtitle = "Multiplies the weapon's rate of fire. 1× is the game's own.",
                    valueText = if (settings.rapidFire <= 1) "Off" else "${settings.rapidFire}×",
                    value = settings.rapidFire.toFloat().coerceIn(1f, 4f),
                    range = 1f..4f,
                    steps = 2,
                    onValueChange = { v -> onSettings { it.copy(rapidFire = v.toInt().coerceIn(1, 4)) } },
                )
            }
        }
        item {
            SettingsGroup("Player") {
                SwitchSetting(
                    title = "God mode",
                    subtitle = "No hit takes health from the player.",
                    checked = settings.godMode,
                    onCheckedChange = { on -> onSettings { it.copy(godMode = on) } },
                )
                DividerInset()
                SliderSetting(
                    title = "Game speed",
                    subtitle = "Menus and films keep their own speed.",
                    valueText = if (settings.gameSpeed == 100) "100% · default" else "${settings.gameSpeed}%",
                    value = settings.gameSpeed.toFloat().coerceIn(25f, 200f),
                    range = 25f..200f,
                    steps = 6,
                    onValueChange = { v ->
                        val snapped = (kotlin.math.round(v / 25f) * 25f).toInt().coerceIn(25, 200)
                        onSettings { it.copy(gameSpeed = snapped) }
                    },
                )
            }
        }
        item {
            SettingsGroup("Unlocks") {
                SwitchSetting(
                    title = "Unlock every mission",
                    subtitle = "Open all levels on every difficulty. The save file itself is not rewritten.",
                    checked = settings.unlockLevels,
                    onCheckedChange = { on -> onSettings { it.copy(unlockLevels = on) } },
                )
                DividerInset()
                SwitchSetting(
                    title = "Unlock Black Ops",
                    subtitle = "Black Ops missions become available wherever another difficulty has reached them.",
                    checked = settings.unlockBlackOps,
                    onCheckedChange = { on -> onSettings { it.copy(unlockBlackOps = on) } },
                )
            }
        }
        item {
            InfoBanner(
                title = "Achievements are the game's",
                text = "These are the port's own cheats. They are not the original cheat menu, and they do not " +
                    "change your save's difficulty or mission progress.",
            )
        }
        item {
            SettingsGroup("Reset") {
                ActionSetting(
                    title = "Turn every cheat off",
                    subtitle = "Restores normal damage, ammunition, speed and unlocks.",
                    button = "Reset",
                    icon = Icons.Rounded.RestartAlt,
                    onClick = {
                        val d = GameSettings()
                        onSettings {
                            it.copy(
                                infiniteAmmo = d.infiniteAmmo,
                                rapidFire = d.rapidFire,
                                noRecoil = d.noRecoil,
                                gameSpeed = d.gameSpeed,
                                godMode = d.godMode,
                                oneHitKill = d.oneHitKill,
                                infiniteGrenades = d.infiniteGrenades,
                                unlockLevels = d.unlockLevels,
                                unlockBlackOps = d.unlockBlackOps,
                            )
                        }
                    },
                )
            }
        }
        item {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Icon(
                    Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                androidx.compose.foundation.layout.Spacer(Modifier.padding(horizontal = 6.dp))
                Text(
                    "Cheats are saved with your settings, but they are not a substitute for the game's own " +
                        "difficulty options.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Icon(
                    Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                androidx.compose.foundation.layout.Spacer(Modifier.padding(horizontal = 6.dp))
                Text(
                    "Changes apply live in-game through the settings bridge.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
