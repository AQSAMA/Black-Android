package com.racas.black.controlcenter

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The touch layout editor. It is deliberately a preview rather than a second copy of the renderer: the background
 * is drawn in Compose, and every control position is the same share of width and height that native/touch.cpp reads
 * from layout.toml. Drag a control to move it; the geometry below the canvas writes every control, including the
 * menu D-pad and the controls hidden from the current preview.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LayoutEditorScreen(
    layout: TouchLayout,
    prefs: InterfacePreferences,
    onCancel: () -> Unit,
    onSave: (TouchLayout) -> Unit,
) {
    var working by remember { mutableStateOf(layout) }
    var selectedId by rememberSaveable { mutableStateOf<String?>("fire") }
    var menuPreview by rememberSaveable { mutableStateOf(false) }
    var showHidden by rememberSaveable { mutableStateOf(false) }
    var showLabels by rememberSaveable { mutableStateOf(prefs.showLabels) }

    BackHandler(onBack = onCancel)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            EditorHeader(
                onCancel = onCancel,
                onSave = { onSave(working) },
            )
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = maxWidth >= 900.dp
                if (wide) {
                    Row(Modifier.fillMaxSize()) {
                        PreviewArea(
                            layout = working,
                            selectedId = selectedId,
                            menuPreview = menuPreview,
                            showHidden = showHidden,
                            showLabels = showLabels,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onSelect = { selectedId = it },
                            onMove = { id, x, y ->
                                val place = working.placeOrDefault(id)
                                working = working.withPlace(
                                    place.copy(x = x.coerceIn(0f, 1f), y = y.coerceIn(0f, 1f)),
                                )
                            },
                        )
                        Surface(
                            Modifier
                                .width(388.dp)
                                .fillMaxHeight(),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        ) {
                            EditorPanel(
                                layout = working,
                                selectedId = selectedId,
                                menuPreview = menuPreview,
                                showHidden = showHidden,
                                showLabels = showLabels,
                                onSelect = { selectedId = it },
                                onMenuPreview = { menuPreview = it },
                                onShowHidden = { showHidden = it },
                                onShowLabels = { showLabels = it },
                                onLayout = { working = it },
                            )
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        PreviewArea(
                            layout = working,
                            selectedId = selectedId,
                            menuPreview = menuPreview,
                            showHidden = showHidden,
                            showLabels = showLabels,
                            modifier = Modifier.fillMaxWidth(),
                            onSelect = { selectedId = it },
                            onMove = { id, x, y ->
                                val place = working.placeOrDefault(id)
                                working = working.withPlace(
                                    place.copy(x = x.coerceIn(0f, 1f), y = y.coerceIn(0f, 1f)),
                                )
                            },
                        )
                        Surface(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        ) {
                            EditorPanel(
                                layout = working,
                                selectedId = selectedId,
                                menuPreview = menuPreview,
                                showHidden = showHidden,
                                showLabels = showLabels,
                                onSelect = { selectedId = it },
                                onMenuPreview = { menuPreview = it },
                                onShowHidden = { showHidden = it },
                                onShowLabels = { showLabels = it },
                                onLayout = { working = it },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorHeader(onCancel: () -> Unit, onSave: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Close editor")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "TOUCH LAYOUT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp,
                )
                Text(
                    "Drag controls into place",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 52.dp)) {
                Text("Cancel")
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onSave, modifier = Modifier.heightIn(min = 52.dp)) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Save layout")
            }
        }
    }
}

@Composable
private fun PreviewArea(
    layout: TouchLayout,
    selectedId: String?,
    menuPreview: Boolean,
    showHidden: Boolean,
    showLabels: Boolean,
    modifier: Modifier,
    onSelect: (String) -> Unit,
    onMove: (String, Float, Float) -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier.padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        val aspect = 16f / 9f
        val height = minOf(maxHeight, maxWidth / aspect)
        val width = height * aspect
        GameCanvas(
            layout = layout,
            selectedId = selectedId,
            menuPreview = menuPreview,
            showHidden = showHidden,
            showLabels = showLabels,
            width = width,
            height = height,
            onSelect = onSelect,
            onMove = onMove,
        )
    }
}

@Composable
private fun GameCanvas(
    layout: TouchLayout,
    selectedId: String?,
    menuPreview: Boolean,
    showHidden: Boolean,
    showLabels: Boolean,
    width: Dp,
    height: Dp,
    onSelect: (String) -> Unit,
    onMove: (String, Float, Float) -> Unit,
) {
    val density = LocalDensity.current
    val widthPx = with(density) { width.toPx() }
    val heightPx = with(density) { height.toPx() }
    val scale = layout.scale / 100f

    Surface(
        modifier = Modifier
            .size(width, height)
            .padding(1.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = Color.Black,
    ) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(15.dp))) {
            Canvas(Modifier.fillMaxSize()) { DrawGameScene() }
            CanvasOverlay(menuPreview)
            Text(
                if (menuPreview) "MENU PREVIEW" else "IN PLAY · TOUCH TO SELECT · DRAG TO MOVE",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.62f),
                letterSpacing = 1.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )

            val shown = TouchControl.all.filter { control ->
                val visibleInMode = when (control.group) {
                    ControlGroup.Always -> true
                    ControlGroup.Play -> !menuPreview
                    ControlGroup.Menus -> menuPreview
                }
                if (visibleInMode) true else showHidden
            }.sortedBy { it.id == selectedId }

            shown.forEach { control ->
                val place = layout.placeOrDefault(control.id)
                val shouldShow = when {
                    control.group == ControlGroup.Always -> true
                    control.group == ControlGroup.Play -> !menuPreview
                    else -> menuPreview
                }
                if (!shouldShow && !showHidden) return@forEach
                val isHiddenHere = !shouldShow || place.hidden
                val alpha = when {
                    !isHiddenHere -> 1f
                    showHidden -> 0.58f
                    else -> 0.32f
                }
                val selected = control.id == selectedId
                val isRect = control.shape == ControlShape.Rect
                val halfHeight = (place.r * heightPx * scale).coerceAtLeast(11.dp.value * density.density)
                val halfHeightDp = with(density) { halfHeight.toDp() }
                val boxWidth = if (isRect) halfHeightDp * 3.2f else halfHeightDp * 2f
                val boxHeight = halfHeightDp * 2f
                val shape = if (isRect) RoundedCornerShape(28) else CircleShape
                val cx = width * place.x - boxWidth / 2
                val cy = height * place.y - boxHeight / 2
                Box(
                    modifier = Modifier
                        .offset(x = cx, y = cy)
                        .size(boxWidth, boxHeight)
                        .zIndex(if (selected) 2f else 1f)
                        .alpha(alpha)
                        .then(
                            if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape)
                            else Modifier.border(
                                1.dp,
                                if (control.id.startsWith("fire")) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
                                } else {
                                    Color.White.copy(alpha = 0.55f)
                                },
                                shape,
                            ),
                        )
                        .clip(shape)
                        .background(Color(0x99101012))
                        .pointerInput(control.id, widthPx, heightPx, scale) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                onSelect(control.id)
                                var last = down.position
                                do {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (change.pressed) {
                                        val delta = change.position - last
                                        last = change.position
                                        if (delta != Offset.Zero) {
                                            change.consume()
                                            val p = layout.placeOrDefault(control.id)
                                            onMove(
                                                control.id,
                                                p.x + delta.x / widthPx,
                                                p.y + delta.y / heightPx,
                                            )
                                        }
                                    }
                                } while (change.pressed)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (showLabels) {
                        if (isRect) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "FIRE",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = (boxHeight.value * 0.24f).sp,
                                        letterSpacing = 3.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = Color(0xFFF0D69A),
                                )
                                Text(
                                    "PRIMARY",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = (boxHeight.value * 0.075f).sp,
                                        letterSpacing = 2.sp,
                                    ),
                                    color = Color.White.copy(alpha = 0.65f),
                                )
                            }
                        } else {
                            Text(
                                when (control.id) {
                                    "fire_left" -> "FIRE"
                                    "stick" -> "MOVE"
                                    "dpad_up" -> "UP"
                                    "dpad_down" -> "DOWN"
                                    "dpad_left" -> "LEFT"
                                    "dpad_right" -> "RIGHT"
                                    "zoom" -> "ZOOM"
                                    else -> control.label.uppercase()
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = (halfHeightDp.value * 0.34f).sp,
                                    lineHeight = (halfHeightDp.value * 0.38f).sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 2.dp),
                            )
                        }
                    } else {
                        Text(
                            if (isRect) "FIRE" else control.label.take(2).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f),
                        )
                    }
                    if (place.hidden) {
                        Icon(
                            Icons.Rounded.VisibilityOff,
                            contentDescription = "Hidden",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(
                                (minOf(boxWidth.value, boxHeight.value) * 0.28f).dp.coerceAtLeast(10.dp),
                            ),
                        )
                    }
                }
            }

            if (shown.isEmpty()) {
                Text(
                    "No controls are visible in this preview. Turn on Show hidden or switch mode.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(10.dp))
                        .padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun CanvasOverlay(menuPreview: Boolean) {
    if (!menuPreview) {
        Box(Modifier.fillMaxSize()) {
            Text(
                "OBJECTIVE",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFFCC66).copy(alpha = 0.8f),
                letterSpacing = 1.4.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 10.dp),
            )
            Text(
                "Black · Operation",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 14.dp, bottom = 10.dp),
            )
            Text(
                "30 / 120",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFF0D69A).copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 10.dp),
            )
        }
    } else {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.48f)),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text(
                "PAUSE",
                style = MaterialTheme.typography.displaySmall,
                color = Color(0xFFF0E5CF),
                modifier = Modifier.padding(top = 40.dp),
            )
        }
    }
}

private fun DrawScope.DrawGameScene() {
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFF1A2325),
            0.44f to Color(0xFF59635C),
            0.45f to Color(0xFF4A4638),
            1f to Color(0xFF1B1A15),
        ),
    )
    // The low sun and its haze.
    drawCircle(Color(0xFFD7A45C).copy(alpha = 0.40f), radius = size.minDimension * 0.18f,
        center = Offset(size.width * 0.78f, size.height * 0.33f))
    drawCircle(Color(0xFFE8BC75).copy(alpha = 0.75f), radius = size.minDimension * 0.075f,
        center = Offset(size.width * 0.78f, size.height * 0.33f))
    // A far skyline, then the ground and a road.
    val skyline = Path().apply {
        moveTo(0f, size.height * 0.47f)
        var x = 0f
        var i = 0
        while (x < size.width) {
            val w = size.width * (0.045f + (i % 4) * 0.013f)
            val h = size.height * (0.035f + (i % 5) * 0.013f)
            lineTo(x, size.height * 0.47f - h)
            lineTo(x + w * 0.82f, size.height * 0.47f - h)
            lineTo(x + w * 0.82f, size.height * 0.47f)
            x += w
            i++
        }
        lineTo(size.width, size.height * 0.50f)
        lineTo(size.width, size.height * 0.47f)
        lineTo(0f, size.height * 0.47f)
        close()
    }
    drawPath(skyline, Color(0xFF222521))
    drawRect(Color(0xFF222019), topLeft = Offset(0f, size.height * 0.47f), size = size.copy(height = size.height * 0.53f))
    drawRect(
        Color(0xFF373429),
        topLeft = Offset(size.width * 0.36f, size.height * 0.57f),
        size = androidx.compose.ui.geometry.Size(size.width * 0.28f, size.height * 0.32f),
    )
    drawLine(Color(0xFF887A59).copy(alpha = 0.35f), Offset(0f, size.height * 0.62f),
        Offset(size.width, size.height * 0.57f), strokeWidth = 2f)
    // Vignette.
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.maxDimension * 0.72f,
        ),
    )
    // An unobtrusive crosshair.
    val cx = size.width / 2f
    val cy = size.height / 2f
    drawCircle(Color.White.copy(alpha = 0.38f), radius = 2.5f, center = Offset(cx, cy))
    drawLine(Color.White.copy(alpha = 0.20f), Offset(cx - 12f, cy), Offset(cx - 5f, cy), 1.5f)
    drawLine(Color.White.copy(alpha = 0.20f), Offset(cx + 5f, cy), Offset(cx + 12f, cy), 1.5f)
    drawLine(Color.White.copy(alpha = 0.20f), Offset(cx, cy - 12f), Offset(cx, cy - 5f), 1.5f)
    drawLine(Color.White.copy(alpha = 0.20f), Offset(cx, cy + 5f), Offset(cx, cy + 12f), 1.5f)
    drawRect(Color.White.copy(alpha = 0.16f), size = size, style = Stroke(width = 1f))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorPanel(
    layout: TouchLayout,
    selectedId: String?,
    menuPreview: Boolean,
    showHidden: Boolean,
    showLabels: Boolean,
    onSelect: (String) -> Unit,
    onMenuPreview: (Boolean) -> Unit,
    onShowHidden: (Boolean) -> Unit,
    onShowLabels: (Boolean) -> Unit,
    onLayout: (TouchLayout) -> Unit,
) {
    val presets = buildList {
        addAll(Options.presets.filter { it.first != "custom" })
        if (layout.isCustom()) add("custom" to "Custom")
    }
    val selected = selectedId?.let { TouchControl.byId[it] }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Preview", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !menuPreview,
                            onClick = { onMenuPreview(false) },
                            label = { Text("In play") },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                        FilterChip(
                            selected = menuPreview,
                            onClick = { onMenuPreview(true) },
                            label = { Text("Menus") },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = showHidden,
                            onClick = { onShowHidden(!showHidden) },
                            label = { Text("Show hidden") },
                            leadingIcon = {
                                Icon(
                                    if (showHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                        FilterChip(
                            selected = showLabels,
                            onClick = { onShowLabels(!showLabels) },
                            label = { Text("Labels") },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                    Text(
                        "Drag a control to move it. Its position is stored as a share of the screen, so it lands " +
                            "in the same place on every phone and at every resolution.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            SettingsGroup("Layout") {
                ChoiceSetting(
                    title = "Preset",
                    subtitle = "Start from another grip. Your moved controls are replaced.",
                    options = presets.map { it.second },
                    selectedIndex = presets.indexOfFirst {
                        it.first == if (layout.isCustom()) "custom" else layout.preset
                    }.coerceAtLeast(0),
                    onSelect = { index ->
                        val id = presets.getOrNull(index)?.first ?: return@ChoiceSetting
                        if (id == "custom") return@ChoiceSetting
                        onLayout(
                            TouchLayout.fromPreset(id).copy(
                                opacity = layout.opacity,
                                scale = layout.scale,
                                skin = layout.skin,
                            ),
                        )
                    },
                )
                DividerInset()
                SliderSetting(
                    title = "Control size",
                    subtitle = "On top of each control's own radius.",
                    valueText = "${layout.scale}%",
                    value = layout.scale.toFloat(),
                    range = 50f..150f,
                    steps = 99,
                    onValueChange = { v -> onLayout(layout.copy(scale = v.toInt())) },
                )
                DividerInset()
                SliderSetting(
                    title = "Opacity",
                    subtitle = "The game applies this to every touch control.",
                    valueText = "${layout.opacity}%",
                    value = layout.opacity.toFloat(),
                    range = 20f..100f,
                    steps = 79,
                    onValueChange = { v -> onLayout(layout.copy(opacity = v.toInt())) },
                )
                DividerInset()
                ChoiceSetting(
                    title = "Button style",
                    options = Options.skins.map { it.second },
                    selectedIndex = Options.skins.indexOfFirst { it.first == layout.skin }.coerceAtLeast(0),
                    onSelect = { index -> onLayout(layout.copy(skin = Options.skins[index].first)) },
                )
            }
        }

        item {
            SettingsGroup("Selected control") {
                if (selected == null) {
                    SettingRow(
                        title = "No control selected",
                        subtitle = "Tap a control on the preview or pick one below.",
                    )
                } else {
                    val place = layout.placeOrDefault(selected.id)
                    SettingRow(
                        title = selected.label,
                        subtitle = "${selected.group.label} · ${if (place.hidden) "hidden" else "shown"}",
                        trailing = {
                            Text(
                                "${(place.x * 100).roundToInt()}%, ${(place.y * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                    )
                    DividerInset()
                    SliderSetting(
                        title = "Size",
                        subtitle = if (selected.shape == ControlShape.Rect) {
                            "Height of the fire rectangle as a share of the screen; its width follows at 1.6×."
                        } else {
                            "Radius as a share of the screen height. The global size multiplies it."
                        },
                        valueText = "${(place.r * 100).let { String.format(java.util.Locale.US, "%.1f", it) }}%",
                        value = place.r,
                        range = 0.02f..0.25f,
                        steps = 22,
                        onValueChange = { r ->
                            onLayout(layout.withPlace(place.copy(r = r)))
                        },
                    )
                    DividerInset()
                    SliderSetting(
                        title = "Horizontal position",
                        valueText = "${(place.x * 100).roundToInt()}%",
                        value = place.x,
                        range = 0f..1f,
                        steps = 99,
                        onValueChange = { x -> onLayout(layout.withPlace(place.copy(x = x))) },
                    )
                    DividerInset()
                    SliderSetting(
                        title = "Vertical position",
                        valueText = "${(place.y * 100).roundToInt()}%",
                        value = place.y,
                        range = 0f..1f,
                        steps = 99,
                        onValueChange = { y -> onLayout(layout.withPlace(place.copy(y = y))) },
                    )
                    DividerInset()
                    SwitchSetting(
                        title = "Hide from the game",
                        subtitle = if (selected.keep) {
                            "This control is the way back out of the menus and cannot be hidden."
                        } else {
                            "The control stays in the file, so you can show it again later."
                        },
                        checked = place.hidden,
                        enabled = !selected.keep,
                        onCheckedChange = { hidden ->
                            onLayout(layout.withPlace(place.copy(hidden = hidden && !selected.keep)))
                        },
                    )
                    DividerInset()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                val d = selected.defaults(layout.preset)
                                onLayout(
                                    layout.withPlace(
                                        ControlPlace(selected.id, d[0], d[1], d[2], selected.defaultHidden),
                                    ),
                                )
                            },
                            modifier = Modifier.heightIn(min = 52.dp),
                        ) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Reset control")
                        }
                        TextButton(
                            onClick = {
                                val p = layout.placeOrDefault(selected.id)
                                onLayout(layout.withPlace(p.copy(x = 0.5f, y = 0.5f)))
                            },
                            modifier = Modifier.heightIn(min = 52.dp),
                        ) {
                            Text("Centre")
                        }
                    }
                }
            }
        }

        item {
            SettingsGroup("All controls", "Tap a name to select it. The list includes the menu D-pad, Enter and Back.") {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ControlGroup.entries.forEach { group ->
                        Text(
                            group.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TouchControl.all.filter { it.group == group }.forEach { control ->
                                val p = layout.placeOrDefault(control.id)
                                FilterChip(
                                    selected = control.id == selectedId,
                                    onClick = { onSelect(control.id) },
                                    label = { Text(control.label) },
                                    leadingIcon = {
                                        if (p.hidden) {
                                            Icon(
                                                Icons.Rounded.VisibilityOff,
                                                contentDescription = "Hidden",
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            InfoBanner(
                title = "What gets saved",
                text = "Preset, opacity, size, skin, and every control's x, y, radius and hidden flag. The fire zone " +
                    "uses the same values as the round controls, drawn as a 1.6:1 rectangle. The native game reloads " +
                    "layout.toml when you come back from this screen.",
            )
        }

        item {
            OutlinedButton(
                onClick = {
                    onLayout(
                        TouchLayout.fromPreset(layout.preset).copy(
                            opacity = layout.opacity,
                            scale = layout.scale,
                            skin = layout.skin,
                        ),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Reset every control to the ${layout.preset} preset")
            }
        }
    }
}
