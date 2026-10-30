package com.racas.black.controlcenter

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.runtime.rememberUpdatedState
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
fun LayoutEditorScreen(layout: TouchLayout, prefs: InterfacePreferences, onCancel: () -> Unit, onSave: (TouchLayout) -> Unit) {
    val editor = rememberSaveable(saver = LayoutEditorState.Saver) { LayoutEditorState(layout) }
    var selectedId by rememberSaveable { mutableStateOf("fire") }
    var tools by rememberSaveable { mutableStateOf(true) }
    var hidden by rememberSaveable { mutableStateOf(false) }
    var menus by rememberSaveable { mutableStateOf(false) }
    var discard by remember { mutableStateOf(false) }
    var choosing by remember { mutableStateOf(false) }
    var presets by remember { mutableStateOf(false) }
    val leave = { if (editor.layout != layout) discard = true else onCancel() }
    BackHandler(onBack = leave)
    if(discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard changes?") },
        confirmButton = { TextButton(onClick = onCancel) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep editing") } })
    Surface(Modifier.fillMaxSize(), color = Color(0xFF0D141E)) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val aspect = maxWidth.value / maxHeight.value.coerceAtLeast(1f)
            val current = editor.layout
            GameCanvas(current, selectedId, menus, hidden, prefs.showLabels, maxWidth, maxHeight,
                onSelect = { selectedId = it }, aspect = aspect,
                onGestureStart = editor::beginGesture, onGestureEnd = editor::endGesture,
                onTransform = { id,x,y,r ->
                    val now = editor.layout
                    editor.change(now.withPlace(now.contain(now.placeOrDefault(id).copy(x=x,y=y,r=r), aspect)))
                })
            Surface(Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), shape = RoundedCornerShape(8.dp)) {
                Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = leave) { Icon(Icons.Rounded.Close, "Exit editor") }
                    Text("HUD STUDIO", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal=8.dp))
                    IconButton(onClick = editor::undo, enabled = editor.canUndo) { Icon(Icons.AutoMirrored.Rounded.Undo,"Undo") }
                    IconButton(onClick = editor::redo, enabled = editor.canRedo) { Icon(Icons.AutoMirrored.Rounded.Redo,"Redo") }
                    Box {
                        TextButton(onClick = { presets = true }) { Text("Preset") }
                        DropdownMenu(expanded = presets, onDismissRequest = { presets = false }) {
                            Options.presets.filter { it.first != "custom" }.forEach { (id,label) ->
                                DropdownMenuItem(text = { Text(label) }, onClick = {
                                    editor.endGesture()
                                    editor.change(TouchLayout.fromPreset(id).copy(opacity=current.opacity, scale=current.scale,
                                        skin=current.skin, fixedStick=current.fixedStick, stickDeadzone=current.stickDeadzone))
                                    presets = false
                                })
                            }
                        }
                    }
                    TextButton(onClick = { tools = !tools }) { Text(if(tools) "Hide tools" else "Show tools") }
                    }
                    Button(onClick = { onSave(editor.layout) }, modifier = Modifier.padding(start=8.dp)) { Text("Save") }
                }
            }
            if(tools) Surface(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top=72.dp).width(316.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f), shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.padding(horizontal=14.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            TextButton(onClick = { choosing = true }) { Text(TouchControl.byId[selectedId]?.label ?: "Choose control") }
                            DropdownMenu(expanded=choosing,onDismissRequest={choosing=false},modifier=Modifier.heightIn(max=240.dp)) {
                                TouchControl.all.forEach { control -> DropdownMenuItem(text={Text(control.label)},onClick={
                                    selectedId=control.id; hidden=true; choosing=false
                                }) }
                            }
                        }
                        val control=TouchControl.byId[selectedId]
                        val place=current.placeOrDefault(selectedId)
                        TextButton(enabled=control?.keep != true,onClick={
                            editor.endGesture();editor.change(current.withPlace(place.copy(hidden=!place.hidden)))
                        }) { Text(if(place.hidden) "Show" else "Hide") }
                    }
                    val place=current.placeOrDefault(selectedId)
                    Row { Text("Size",modifier=Modifier.weight(1f));Text("${(place.r*200).roundToInt()}%") }
                    Slider(value=place.r.coerceIn(0.02f,0.25f),valueRange=0.02f..0.25f,
                        onValueChange={r -> editor.beginGesture();val now=editor.layout
                            editor.change(now.withPlace(now.contain(now.placeOrDefault(selectedId).copy(r=r),aspect))) },
                        onValueChangeFinished=editor::endGesture,modifier=Modifier.height(34.dp))
                    Row { Text("Opacity",modifier=Modifier.weight(1f));Text("${current.opacity}%") }
                    Slider(value=current.opacity.toFloat(),valueRange=20f..100f,
                        onValueChange={v -> editor.beginGesture();editor.change(editor.layout.copy(opacity=v.toInt())) },
                        onValueChangeFinished=editor::endGesture,modifier=Modifier.height(34.dp))
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        FilterChip(selected=hidden,onClick={hidden=!hidden},label={Text("Hidden")})
                        Spacer(Modifier.width(8.dp))
                        FilterChip(selected=menus,onClick={menus=!menus},label={Text("Menu keys")})
                    }
                    Text("Drag to move · pinch to resize",style=MaterialTheme.typography.labelSmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
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
    aspect: Float,
    onGestureStart: () -> Unit,
    onGestureEnd: () -> Unit,
    onTransform: (String, Float, Float, Float) -> Unit,
) {
    val density = LocalDensity.current
    val widthPx = with(density) { width.toPx() }
    val heightPx = with(density) { height.toPx() }
    val scale = layout.scale / 100f
    // a drag lasts across recompositions: the callbacks and placement are read through the latest state, and its
    // position is measured once from the down point rather than from the layout captured when the gesture started
    val latestLayout by rememberUpdatedState(layout)
    val latestOnSelect by rememberUpdatedState(onSelect)
    val latestOnTransform by rememberUpdatedState(onTransform)
    val latestOnStart by rememberUpdatedState(onGestureStart)
    val latestOnEnd by rememberUpdatedState(onGestureEnd)

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
            }

            shown.forEach { control ->
                val place = layout.contain(layout.placeOrDefault(control.id), aspect)
                val shouldShow = when {
                    control.group == ControlGroup.Always -> true
                    control.group == ControlGroup.Play -> !menuPreview
                    else -> menuPreview
                }
                if (!shouldShow && !showHidden) return@forEach
                if (place.hidden && !showHidden) return@forEach
                val isHiddenHere = !shouldShow || place.hidden
                val alpha = when {
                    !isHiddenHere -> layout.opacity / 100f
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
                                latestOnSelect(control.id)
                                latestOnStart()
                                try {
                                    var pointer = down.id
                                    var transformed = latestLayout.contain(latestLayout.placeOrDefault(control.id), aspect)
                                    do {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == pointer && it.pressed }
                                            ?: event.changes.firstOrNull { it.pressed } ?: break
                                        pointer = change.id
                                        val delta = change.positionChange()
                                        val zoom = event.calculateZoom()
                                        transformed = latestLayout.contain(transformed.copy(
                                            x = transformed.x + delta.x / widthPx,
                                            y = transformed.y + delta.y / heightPx,
                                            r = (transformed.r * zoom).coerceIn(0.02f, 0.25f),
                                        ), aspect)
                                        latestOnTransform(control.id, transformed.x, transformed.y, transformed.r)
                                        event.changes.forEach { it.consume() }
                                    } while (event.changes.any { it.pressed })
                                } finally { latestOnEnd() }
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
