package com.racas.black.controlcenter

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class SettingsRoundTripTest {
    @Test fun legacyFireDuplicatesAreHiddenOnceButNewOptionalButtonsStayEditable() {
        val root = Files.createTempDirectory("black-layout-migration").toFile()
        try {
            root.resolve("layout.toml").writeText("""preset = "default"
[controls.fire_left]
x = 0.12
y = 0.32
hidden = false
[controls.fire_zone]
hidden = false
""")
            root.resolve("black.toml").writeText("touch_sensitivity = 1.0\n")
            val store = TomlSettingsStore(root)
            val migrated = store.load()
            assertEquals(2.0f, migrated.touchSensitivity)
            assertTrue(migrated.touchLayout.placeOrDefault("fire_left").hidden)
            assertTrue(migrated.touchLayout.placeOrDefault("fire_zone").hidden)
            assertEquals(0.12f, migrated.touchLayout.placeOrDefault("fire_left").x)
            val optional = migrated.touchLayout.withPlace(migrated.touchLayout.placeOrDefault("fire_left").copy(hidden = false))
            store.save(migrated.copy(touchLayout = optional))
            assertFalse(store.load().touchLayout.placeOrDefault("fire_left").hidden)
        } finally { root.deleteRecursively() }
    }

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
