package com.racas.black.controlcenter

import org.junit.Assert.*
import org.junit.Test

class LayoutEditorStateTest {
    @Test fun dragIsOneUndoAndNewEditClearsRedo() {
        val original = TouchLayout.fromPreset("default")
        val state = LayoutEditorState(original)
        state.beginGesture()
        repeat(20) { index -> state.change(state.layout.withPlace(state.layout.placeOrDefault("fire").copy(x = index / 30f))) }
        state.endGesture()
        val moved = state.layout
        state.undo()
        assertEquals(original, state.layout)
        assertFalse(state.canUndo)
        state.redo()
        assertEquals(moved, state.layout)
        state.undo()
        state.change(original.copy(opacity = 60))
        assertFalse(state.canRedo)
    }

    @Test fun tapDoesNotAddUndoAndHistoryIsBounded() {
        val state = LayoutEditorState(TouchLayout())
        state.beginGesture()
        state.change(state.layout)
        state.endGesture()
        assertFalse(state.canUndo)
        repeat(100) { state.change(state.layout.copy(opacity = it)) }
        var count = 0
        while (state.canUndo) { state.undo(); count++ }
        assertEquals(50, count)
    }

    @Test fun edgesContainWholeCircularAndRectangularTargetsAtEveryScale() {
        for (scale in listOf(50, 100, 150)) for (aspect in listOf(16f / 9f, 20f / 9f)) {
            val layout = TouchLayout(scale = scale)
            for (id in listOf("fire", "fire_zone", "stick")) {
                for (edge in listOf(-1f, 0f, 1f, 2f)) {
                    val p = layout.contain(ControlPlace(id, edge, edge, 0.25f, false), aspect)
                    val hh = p.r * scale / 100f
                    val hw = hh * (if (id == "fire_zone") 1.6f else 1f) / aspect
                    assertTrue(p.x - hw >= -0.0001f && p.x + hw <= 1.0001f)
                    assertTrue(p.y - hh >= -0.0001f && p.y + hh <= 1.0001f)
                }
            }
        }
    }
}
