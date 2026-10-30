package com.racas.black.controlcenter

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Monitor
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

enum class ControlSection(val id: String, val title: String, val icon: ImageVector) {
    Home("home", "Play", Icons.Rounded.Home),
    Display("display", "Graphics", Icons.Rounded.Monitor),
    Controls("controls", "Controls", Icons.Rounded.Gamepad),
    Audio("audio", "Audio", Icons.Rounded.VolumeUp),
    More("more", "More", Icons.Rounded.MoreHoriz),
    Interface("interface", "Interface", Icons.Rounded.Palette),
    Cheats("cheats", "Cheats", Icons.Rounded.Bolt),
    About("about", "About", Icons.Rounded.Info),
    ;

    // the bar/rail get five destinations; interface, cheats and about live under more
    val primary: Boolean get() = this in navBar

    companion object {
        val navBar = listOf(Home, Display, Controls, Audio, More)

        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: Home
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlackControlCenter(host: ControlCenterHost) {
    val prefs by host.preferencesStore.prefs.collectAsState()
    BlackControlCenterTheme(prefs) {
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }
        val game by host.gameStatus.collectAsState()
        val update by host.updateState.collectAsState()
        var settings by remember { mutableStateOf(host.settingsStore.load()) }
        var sectionId by rememberSaveable { mutableStateOf(ControlSection.Controls.id) }
        var editingLayout by rememberSaveable { mutableStateOf(false) }
        val section = ControlSection.of(sectionId)
        val reduceMotion = LocalReduceMotion.current
        // Back, Play and the system all leave the activity without waiting for the 280 ms save debounce
        val latestSettings = rememberUpdatedState(settings)
        val lifecycleOwner = LocalLifecycleOwner.current

        fun flushSettings(): Boolean = runCatching {
            host.settingsStore.save(latestSettings.value)
        }.onFailure {
            scope.launch { snackbar.showSnackbar("Could not save settings. Check available storage and try again.") }
        }.isSuccess

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> settings = host.settingsStore.load()
                    Lifecycle.Event.ON_STOP -> flushSettings()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                flushSettings()
            }
        }

        LaunchedEffect(Unit) {
            snapshotFlow { settings }
                .drop(1)
                .debounce(280)
                .collect { next ->
                    runCatching { host.settingsStore.save(next) }.onFailure {
                        snackbar.showSnackbar("Could not save settings. Check available storage and try again.")
                    }
                }
        }

        LaunchedEffect(prefs.keepScreenOn) {
            host.setKeepScreenOn(prefs.keepScreenOn)
        }

        BackHandler(enabled = editingLayout) { editingLayout = false }
        // a page reached from more has no bar item of its own, so back returns to more
        BackHandler(enabled = !editingLayout && !section.primary) { sectionId = ControlSection.More.id }

        if (editingLayout) {
            LayoutEditorScreen(
                layout = settings.touchLayout,
                prefs = prefs,
                onCancel = { editingLayout = false },
                onSave = { layout ->
                    settings = settings.copy(touchLayout = layout)
                    editingLayout = false
                    scope.launch { snackbar.showSnackbar("Touch layout saved") }
                },
            )
            return@BlackControlCenterTheme
        }

        Box(Modifier.fillMaxSize()) {
            BattleSettings(host, settings, game, section,
                onSection = { sectionId = it.id },
                onSettings = { block -> settings = block(settings) },
                onPreview = { editingLayout = true },
                onPlay = { if (flushSettings()) host.launchGame() },
                onLive = { if (flushSettings()) host.editControlsInGame() },
                advanced = { current -> when(current) {
                    ControlSection.Interface -> InterfaceScreen(prefs) { block -> host.preferencesStore.update(block) }
                    ControlSection.Cheats -> CheatsScreen(settings) { block -> settings = block(settings) }
                    ControlSection.About -> AboutScreen(host, prefs, host.appVersion, settings)
                    else -> Unit
                } },
            )
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
}
