package com.racas.black.controlcenter

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class ThemeMode(val id: String, val label: String) {
    Oled("oled", "OLED black"),
    Dark("dark", "Dark"),
    Light("light", "Light"),
    System("system", "System"),
    ;

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: Oled
    }
}

enum class Accent(val id: String, val label: String) {
    Brass("brass", "Brass"),
    Ember("ember", "Ember"),
    Crimson("crimson", "Crimson"),
    Violet("violet", "Violet"),
    Azure("azure", "Azure"),
    Teal("teal", "Teal"),
    Lime("lime", "Lime"),
    Ice("ice", "Ice"),
    ;

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: Brass
    }
}

enum class CornerStyle(val id: String, val label: String) {
    Square("square", "Square"),
    Soft("soft", "Soft"),
    Round("round", "Round"),
    ;

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: Round
    }
}

enum class NavigationStyle(val id: String, val label: String) {
    Auto("auto", "Automatic"),
    Rail("rail", "Side rail"),
    Bottom("bottom", "Bottom bar"),
    ;

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: Auto
    }
}

data class InterfacePreferences(
    val themeMode: ThemeMode = ThemeMode.Oled,
    val accent: Accent = Accent.Brass,
    val uiScale: Float = 1.0f,
    val fontScale: Float = 1.0f,
    val cornerStyle: CornerStyle = CornerStyle.Round,
    val navigation: NavigationStyle = NavigationStyle.Auto,
    val highContrast: Boolean = false,
    val reduceMotion: Boolean = false,
    val haptics: Boolean = true,
    val showLabels: Boolean = true,
    val keepScreenOn: Boolean = false,
    val gradients: Boolean = true,
)

private val Context.controlCenterStore by preferencesDataStore(name = "control_center")

class InterfacePreferencesStore(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _prefs = MutableStateFlow(InterfacePreferences())
    val prefs: StateFlow<InterfacePreferences> = _prefs

    init {
        scope.launch {
            appContext.controlCenterStore.data.collectLatest { values ->
                _prefs.value = InterfacePreferences(
                    themeMode = ThemeMode.of(values[Keys.THEME]),
                    accent = Accent.of(values[Keys.ACCENT]),
                    uiScale = values[Keys.UI_SCALE] ?: 1.0f,
                    fontScale = values[Keys.FONT_SCALE] ?: 1.0f,
                    cornerStyle = CornerStyle.of(values[Keys.CORNERS]),
                    navigation = NavigationStyle.of(values[Keys.NAVIGATION]),
                    highContrast = values[Keys.HIGH_CONTRAST] ?: false,
                    reduceMotion = values[Keys.REDUCE_MOTION] ?: false,
                    haptics = values[Keys.HAPTICS] ?: true,
                    showLabels = values[Keys.SHOW_LABELS] ?: true,
                    keepScreenOn = values[Keys.KEEP_SCREEN_ON] ?: false,
                    gradients = values[Keys.GRADIENTS] ?: true,
                )
            }
        }
    }

    fun update(transform: (InterfacePreferences) -> InterfacePreferences) {
        val next = transform(_prefs.value)
        _prefs.value = next
        scope.launch {
            appContext.controlCenterStore.edit { values ->
                values[Keys.THEME] = next.themeMode.id
                values[Keys.ACCENT] = next.accent.id
                values[Keys.UI_SCALE] = next.uiScale
                values[Keys.FONT_SCALE] = next.fontScale
                values[Keys.CORNERS] = next.cornerStyle.id
                values[Keys.NAVIGATION] = next.navigation.id
                values[Keys.HIGH_CONTRAST] = next.highContrast
                values[Keys.REDUCE_MOTION] = next.reduceMotion
                values[Keys.HAPTICS] = next.haptics
                values[Keys.SHOW_LABELS] = next.showLabels
                values[Keys.KEEP_SCREEN_ON] = next.keepScreenOn
                values[Keys.GRADIENTS] = next.gradients
            }
        }
    }

    fun reset() = update { InterfacePreferences() }

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val ACCENT = stringPreferencesKey("accent")
        val UI_SCALE = floatPreferencesKey("ui_scale")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val CORNERS = stringPreferencesKey("corners")
        val NAVIGATION = stringPreferencesKey("navigation")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val HAPTICS = booleanPreferencesKey("haptics")
        val SHOW_LABELS = booleanPreferencesKey("show_labels")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val GRADIENTS = booleanPreferencesKey("gradients")
    }
}
