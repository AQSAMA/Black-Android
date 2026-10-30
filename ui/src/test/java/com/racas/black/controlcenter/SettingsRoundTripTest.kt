package com.racas.black.controlcenter

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class SettingsRoundTripTest {
    @Test fun savedMovementAndCustomGeometrySurviveReload() {
        val root = Files.createTempDirectory("black-settings").toFile()
        try {
            val store = TomlSettingsStore(root)
            val layout = TouchLayout.fromPreset("four").copy(fixedStick = true, stickDeadzone = 17,
                opacity = 63, scale = 115, skin = "clean").withPlace(ControlPlace("fire", 0.72f, 0.64f, 0.09f, false))
            val settings = GameSettings(touchLayout = layout, gyroMode = 2, touchSensitivity = 1.7f, language = "fre", bindingsOverrides = mapOf("fire" to "F"))
            store.save(settings)
            assertEquals(layout, store.load().touchLayout)
            assertEquals("fre", store.load().language)
            assertEquals(mapOf("fire" to "F"), store.load().bindingsOverrides)
            assertEquals(2, store.load().gyroMode)
            assertEquals(1.7f, store.load().touchSensitivity)
            assertTrue(root.resolve(SettingsStore.MARKER).isFile)
        } finally { root.deleteRecursively() }
    }
}
