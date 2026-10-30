package com.racas.black.controlcenter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.listSaver

/** One history entry per drag or slider gesture; bounded to keep editing sessions lightweight. */
class LayoutEditorState(initial: TouchLayout) {
    var layout by mutableStateOf(initial)
        private set
    private var past by mutableStateOf(emptyList<TouchLayout>())
    private var future by mutableStateOf(emptyList<TouchLayout>())
    private var gesture = false
    private var recorded = false
    val canUndo get() = past.isNotEmpty()
    val canRedo get() = future.isNotEmpty()
    fun beginGesture() { if (!gesture) { gesture = true; recorded = false } }
    fun endGesture() { gesture = false; recorded = false }
    fun change(next: TouchLayout) {
        if (next == layout) return
        if (!gesture || !recorded) past = (past + layout).takeLast(50)
        recorded = true
        future = emptyList()
        layout = next
    }
    fun undo() {
        endGesture()
        if (!canUndo) return
        future = future + layout
        layout = past.last()
        past = past.dropLast(1)
    }
    fun redo() {
        endGesture()
        if (!canRedo) return
        past = past + layout
        layout = future.last()
        future = future.dropLast(1)
    }
    companion object {
        // Preserve the draft across Activity recreation. History is session-local.
        val Saver = listSaver<LayoutEditorState, Any>(
            save = { state -> with(state.layout) {
                listOf(preset, opacity, scale, skin, fixedStick, stickDeadzone) +
                    TouchControl.all.flatMap { c -> placeOrDefault(c.id).let { listOf(it.id, it.x, it.y, it.r, it.hidden) } }
            } },
            restore = { values ->
                val controls = values.drop(6).chunked(5).associate { v ->
                    (v[0] as String) to ControlPlace(v[0] as String, v[1] as Float, v[2] as Float, v[3] as Float, v[4] as Boolean)
                }
                LayoutEditorState(TouchLayout(values[0] as String, values[1] as Int, values[2] as Int,
                    values[3] as String, controls, values[4] as Boolean, values[5] as Int))
            },
        )
    }
}

/** Match the playable screen's geometry and keep the entire target reachable. */
fun TouchLayout.contain(place: ControlPlace, aspect: Float): ControlPlace {
    val halfHeight = (place.r * scale / 100f).coerceAtMost(0.49f)
    val halfWidth = (halfHeight * if (TouchControl.byId[place.id]?.shape == ControlShape.Rect) 1.6f else 1f) / aspect
    return place.copy(x = place.x.coerceIn(halfWidth, 1f - halfWidth), y = place.y.coerceIn(halfHeight, 1f - halfHeight))
}
