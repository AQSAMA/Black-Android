package com.racas.black.controlcenter

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Monitor
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

enum class ControlSection(val id: String, val title: String, val icon: ImageVector) {
    Home("home", "Overview", Icons.Rounded.Home),
    Display("display", "Display", Icons.Rounded.Monitor),
    Controls("controls", "Controls", Icons.Rounded.Gamepad),
    Interface("interface", "Interface", Icons.Rounded.Palette),
    Audio("audio", "Audio", Icons.Rounded.VolumeUp),
    Cheats("cheats", "Cheats", Icons.Rounded.Bolt),
    About("about", "About", Icons.Rounded.Info),
    ;

    companion object {
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
        var sectionId by rememberSaveable { mutableStateOf(ControlSection.Home.id) }
        var editingLayout by rememberSaveable { mutableStateOf(false) }
        val section = ControlSection.of(sectionId)
        val reduceMotion = LocalReduceMotion.current
        // Back, Play and the system all leave the activity without waiting for the 280 ms save debounce
        val latestSettings = rememberUpdatedState(settings)
        val lifecycleOwner = LocalLifecycleOwner.current

        fun flushSettings() {
            runCatching { host.settingsStore.save(latestSettings.value) }
        }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) flushSettings()
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
                .collect { host.settingsStore.save(it) }
        }

        LaunchedEffect(prefs.keepScreenOn) {
            host.setKeepScreenOn(prefs.keepScreenOn)
        }

        BackHandler(enabled = editingLayout) { editingLayout = false }

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

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 840.dp
            val useRail = when (prefs.navigation) {
                NavigationStyle.Rail -> true
                NavigationStyle.Bottom -> false
                NavigationStyle.Auto -> wide
            }

            val screen: @Composable (Boolean) -> Unit = { compact ->
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = {
                        SnackbarHost(snackbar, Modifier.windowInsetsPadding(WindowInsets.navigationBars))
                    },
                    topBar = {
                        Column {
                            TopAppBar(
                                title = {
                                    Column {
                                        Text(
                                            "BLACK",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 3.sp,
                                        )
                                        Text(
                                            if (section == ControlSection.Home) "Control Center" else section.title,
                                            style = MaterialTheme.typography.titleLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                },
                                actions = {
                                    if (game !is GameStatus.Importing) {
                                        Button(
                                            onClick = {
                                                flushSettings()
                                                host.launchGame()
                                            },
                                            modifier = Modifier.heightIn(min = 52.dp),
                                            contentPadding = PaddingValues(horizontal = 18.dp),
                                        ) {
                                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                            Spacer(Modifier.width(6.dp))
                                            Text(if (game is GameStatus.Ready) "Play" else "Start")
                                        }
                                        Spacer(Modifier.width(12.dp))
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                                ),
                                windowInsets = WindowInsets.statusBars,
                            )
                            if (compact) CompactTabs(section) { sectionId = it.id }
                        }
                    },
                ) { padding ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                    ) {
                        Crossfade(
                            targetState = section,
                            modifier = Modifier.fillMaxSize(),
                            animationSpec = if (reduceMotion) snap() else tween(durationMillis = 180),
                            label = "section",
                        ) { current ->
                            when (current) {
                                ControlSection.Home -> HomeScreen(
                                    settings = settings,
                                    prefs = prefs,
                                    game = game,
                                    update = update,
                                    version = host.appVersion,
                                    preview = host.isPreview,
                                    onPlay = {
                                        flushSettings()
                                        host.launchGame()
                                    },
                                    onImport = { uri -> host.importGame(uri) },
                                    onRequestStorage = { host.requestStorageAccess() },
                                    onCheckUpdates = { host.checkForUpdates() },
                                    onOpenUrl = { host.openUrl(it) },
                                    onOpenSection = { sectionId = it.id },
                                )
                                ControlSection.Display -> DisplayScreen(
                                    settings = settings,
                                    onSettings = { block -> settings = block(settings) },
                                )
                                ControlSection.Controls -> ControlsScreen(
                                    settings = settings,
                                    onSettings = { block -> settings = block(settings) },
                                    onEditLayout = { editingLayout = true },
                                )
                                ControlSection.Interface -> InterfaceScreen(
                                    prefs = prefs,
                                    onPrefs = { block -> host.preferencesStore.update(block) },
                                )
                                ControlSection.Audio -> AudioScreen(
                                    settings = settings,
                                    onSettings = { block -> settings = block(settings) },
                                )
                                ControlSection.Cheats -> CheatsScreen(
                                    settings = settings,
                                    onSettings = { block -> settings = block(settings) },
                                )
                                ControlSection.About -> AboutScreen(
                                    host = host,
                                    prefs = prefs,
                                    update = update,
                                    version = host.appVersion,
                                    settings = settings,
                                )
                            }
                        }
                    }
                }
            }

            if (useRail) {
                Row(Modifier.fillMaxSize()) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .width(104.dp)
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.statusBars),
                        header = {
                            Surface(
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .size(52.dp),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "B",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        },
                    ) {
                        Spacer(Modifier.weight(0.2f))
                        ControlSection.entries.forEach { entry ->
                            NavigationRailItem(
                                selected = entry == section,
                                onClick = { sectionId = entry.id },
                                icon = { Icon(entry.icon, contentDescription = null, modifier = Modifier.size(26.dp)) },
                                label = { Text(entry.title, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.heightIn(min = 68.dp),
                            )
                        }
                        Spacer(Modifier.weight(1f))
                    }
                    Box(Modifier.weight(1f)) { screen(false) }
                }
            } else {
                screen(true)
            }
        }
    }
}

@Composable
private fun CompactTabs(selected: ControlSection, onSelect: (ControlSection) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = selected.ordinal,
        edgePadding = 12.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        divider = {},
    ) {
        ControlSection.entries.forEach { entry ->
            Tab(
                selected = entry == selected,
                onClick = { onSelect(entry) },
                modifier = Modifier.heightIn(min = 58.dp),
                text = { Text(entry.title, maxLines = 1) },
                icon = { Icon(entry.icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
            )
        }
    }
}
