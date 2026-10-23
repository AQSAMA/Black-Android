package com.racas.black.controlcenter

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DisplayScreen(
    settings: GameSettings,
    onSettings: ((GameSettings) -> GameSettings) -> Unit,
) {
    SettingsPage {
        item {
            PageHeader(
                eyebrow = "Picture",
                title = "Display & performance",
                subtitle = "Resolution, field of view, frame rate and the image treatments the port adds. " +
                    "None of this touches the original game files.",
            )
        }
        item {
            SettingsGroup("Display") {
                SliderSetting(
                    title = "Render resolution",
                    subtitle = "Share of the screen's height. Lower is faster; 67% is the phone default.",
                    valueText = "${settings.renderPercent}%",
                    value = settings.renderPercent.toFloat(),
                    range = 20f..200f,
                    steps = 35,
                    onValueChange = { v -> onSettings { it.copy(renderPercent = snap(v, 5)) } },
                )
                DividerInset()
                SliderSetting(
                    title = "Field of view",
                    subtitle = "Horizontal degrees on a 4:3 frame. 70° is the original.",
                    valueText = if (settings.fov == 70) "70° · default" else "${settings.fov}°",
                    value = settings.fov.toFloat(),
                    range = 45f..140f,
                    steps = 94,
                    onValueChange = { v -> onSettings { it.copy(fov = v.toInt()) } },
                )
                DividerInset()
                SliderSetting(
                    title = "Viewmodel field of view",
                    subtitle = "The weapon and arms in front of the camera.",
                    valueText = if (settings.viewmodelFov == 70) "70° · default" else "${settings.viewmodelFov}°",
                    value = settings.viewmodelFov.toFloat(),
                    range = 45f..140f,
                    steps = 94,
                    onValueChange = { v -> onSettings { it.copy(viewmodelFov = v.toInt()) } },
                )
                DividerInset()
                ChoiceSetting(
                    title = "FMV scale",
                    subtitle = "How the 4:3 cutscenes sit on a wide screen.",
                    options = Options.fmvScale,
                    selectedIndex = settings.fmvScale.coerceIn(0, 1),
                    onSelect = { i -> onSettings { it.copy(fmvScale = i) } },
                )
                DividerInset()
                ChoiceSetting(
                    title = "Screen filter",
                    subtitle = "A look applied over the finished frame.",
                    options = Options.screenFilters,
                    selectedIndex = settings.screenFilter.coerceIn(0, Options.screenFilters.lastIndex),
                    onSelect = { i -> onSettings { it.copy(screenFilter = i) } },
                )
            }
        }
        item {
            SettingsGroup("Performance") {
                SwitchSetting(
                    title = "Vertical sync",
                    subtitle = "Off by default. On removes tearing but adds latency.",
                    checked = settings.vsync,
                    onCheckedChange = { on -> onSettings { it.copy(vsync = on) } },
                )
                DividerInset()
                ChoiceSetting(
                    title = "Frame rate cap",
                    subtitle = "The Xbox ran at 30. Unlimited lets the display stretch its legs.",
                    options = Options.fpsCaps.map(Options::labelFps),
                    selectedIndex = Options.fpsCaps.indexOf(settings.fpsCap).takeIf { it >= 0 } ?: 10,
                    onSelect = { i -> onSettings { it.copy(fpsCap = Options.fpsCaps[i]) } },
                )
                DividerInset()
                SwitchSetting(
                    title = "Frame rate counter",
                    subtitle = "A small counter in the corner while playing.",
                    checked = settings.fpsCounter,
                    onCheckedChange = { on -> onSettings { it.copy(fpsCounter = on) } },
                )
            }
        }
        item {
            SettingsGroup("Quality") {
                ChoiceSetting(
                    title = "Anisotropic filtering",
                    subtitle = "Sharpens textures that stretch into the distance.",
                    options = Options.anisotropic.map(Options::labelAniso),
                    selectedIndex = Options.anisotropic.indexOf(settings.anisotropicFiltering).takeIf { it >= 0 } ?: 0,
                    onSelect = { i -> onSettings { it.copy(anisotropicFiltering = Options.anisotropic[i]) } },
                )
                DividerInset()
                ChoiceSetting(
                    title = "Draw distance",
                    subtitle = "Multiplies the level's own level of detail and culling distances.",
                    options = Options.drawDistances.map(Options::labelDraw),
                    selectedIndex = Options.drawDistances.indexOf(settings.drawDistance).takeIf { it >= 0 } ?: 0,
                    onSelect = { i -> onSettings { it.copy(drawDistance = Options.drawDistances[i]) } },
                )
                DividerInset()
                ChoiceSetting(
                    title = "Anti-aliasing",
                    subtitle = "FXAA smooths the final frame; SSAA renders at twice the size, then averages.",
                    options = Options.antiAliasing,
                    selectedIndex = settings.antiAliasing.coerceIn(0, Options.antiAliasing.lastIndex),
                    onSelect = { i -> onSettings { it.copy(antiAliasing = i) } },
                )
                DividerInset()
                SwitchSetting(
                    title = "HDR tone mapping",
                    subtitle = "Brings back highlight colour that the console's over-bright look burns out.",
                    checked = settings.hdr,
                    onCheckedChange = { on -> onSettings { it.copy(hdr = on) } },
                )
                DividerInset()
                SwitchSetting(
                    title = "Bloom",
                    subtitle = "The soft glow over bright parts of the frame.",
                    checked = settings.bloom,
                    onCheckedChange = { on -> onSettings { it.copy(bloom = on) } },
                )
                DividerInset()
                SwitchSetting(
                    title = "Fog",
                    subtitle = "The original atmospheric depth. Off is clearer, and flatter.",
                    checked = settings.fog,
                    onCheckedChange = { on -> onSettings { it.copy(fog = on) } },
                )
                DividerInset()
                SwitchSetting(
                    title = "Reload blur",
                    subtitle = "The motion blur while a weapon is reloaded.",
                    checked = settings.reloadBlur,
                    onCheckedChange = { on -> onSettings { it.copy(reloadBlur = on) } },
                )
            }
        }
        item {
            SettingsGroup("Reset") {
                ActionSetting(
                    title = "Restore display defaults",
                    subtitle = "Resolution, field of view, quality and frame pacing only. Controls and cheats stay.",
                    button = "Restore",
                    icon = Icons.Rounded.RestartAlt,
                    onClick = {
                        val d = GameSettings()
                        onSettings {
                            it.copy(
                                renderPercent = d.renderPercent,
                                fov = d.fov,
                                viewmodelFov = d.viewmodelFov,
                                fmvScale = d.fmvScale,
                                screenFilter = d.screenFilter,
                                vsync = d.vsync,
                                fpsCap = d.fpsCap,
                                fpsCounter = d.fpsCounter,
                                anisotropicFiltering = d.anisotropicFiltering,
                                drawDistance = d.drawDistance,
                                reloadBlur = d.reloadBlur,
                                bloom = d.bloom,
                                fog = d.fog,
                                antiAliasing = d.antiAliasing,
                                hdr = d.hdr,
                            )
                        }
                    },
                )
            }
        }
        item {
            Text(
                "Some renderer changes need a new frame to show. The Control Center writes the settings; the game " +
                    "reloads them between frames, without restarting.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

private fun snap(value: Float, step: Int): Int {
    val v = (kotlin.math.round(value / step) * step).toInt()
    return v.coerceIn(20, 200)
}
