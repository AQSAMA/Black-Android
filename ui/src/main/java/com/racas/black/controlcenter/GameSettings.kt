package com.racas.black.controlcenter

/**
 * Everything the native host keeps in black.toml, android.toml and layout.toml.
 *
 * The Control Center edits these files, then drops a marker the game checks between frames. That keeps the UI and the
 * engine decoupled: the Compose screen also runs in the preview APK, where libmain.so is not there.
 */
data class GameSettings(
    // Display
    val renderPercent: Int = 67,
    val fov: Int = 70,
    val viewmodelFov: Int = 70,
    val fmvScale: Int = 0,
    val screenFilter: Int = 0,
    val vsync: Boolean = false,
    val fpsCap: Int = 0,
    val fpsCounter: Boolean = false,
    val anisotropicFiltering: Int = 0,
    val drawDistance: Int = 1,
    val reloadBlur: Boolean = true,
    val bloom: Boolean = true,
    val fog: Boolean = true,
    val antiAliasing: Int = 0,
    val hdr: Boolean = false,

    // Touch controls
    val touchSensitivity: Float = 1.0f,
    val touchLayout: TouchLayout = TouchLayout.preset("default"),

    // Motion
    val gyroMode: Int = 0,
    val gyroSensitivity: Float = 1.5f,
    val gyroInvertX: Boolean = false,
    val gyroInvertY: Boolean = false,

    // Controller
    val leftDeadzone: Int = 0,
    val rightDeadzone: Int = 0,
    val leftSensitivity: Float = 1.0f,
    val rightSensitivity: Float = 1.0f,

    // Mouse and keyboard
    val mouseSensitivity: Float = 1.0f,
    val invertMouse: Boolean = false,
    val holdAim: Boolean = true,
    val walkSpeed: Int = 50,
    val bindingsOverrides: Map<String, String> = emptyMap(),

    // Audio and general
    val audioOutput: Int = 0,
    val language: String = "int",
    val skipIntros: Boolean = false,

    // Cheats
    val infiniteAmmo: Boolean = false,
    val rapidFire: Int = 1,
    val noRecoil: Boolean = false,
    val gameSpeed: Int = 100,
    val godMode: Boolean = false,
    val oneHitKill: Boolean = false,
    val infiniteGrenades: Boolean = false,
    val unlockLevels: Boolean = false,
    val unlockBlackOps: Boolean = false,

    /** Top-level keys this UI does not model (width, height, join_ip, ...), kept so saving never loses them. */
    val extraKeys: Map<String, String> = emptyMap(),
)

data class TouchLayout(
    val preset: String = "default",
    val opacity: Int = 85,
    val scale: Int = 100,
    val skin: String = "modern",
    val controls: Map<String, ControlPlace> = emptyMap(),
) {
    fun place(id: String): ControlPlace? = controls[id] ?: TouchControl.byId[id]?.let { c ->
        val d = c.defaults(preset)
        ControlPlace(id, d[0], d[1], d[2], c.defaultHidden)
    }

    fun withPlace(place: ControlPlace): TouchLayout =
        copy(controls = controls + (place.id to place))

    fun placeOrDefault(id: String): ControlPlace {
        place(id)?.let { return it }
        val c = TouchControl.byId.getValue(id)
        val d = c.defaults(preset)
        return ControlPlace(id, d[0], d[1], d[2], c.defaultHidden)
    }

    fun isCustom(): Boolean =
        TouchControl.all.any { c ->
            val d = c.defaults(preset)
            place(c.id)?.let { p ->
                kotlin.math.abs(p.x - d[0]) > 0.0005f ||
                    kotlin.math.abs(p.y - d[1]) > 0.0005f ||
                    kotlin.math.abs(p.r - d[2]) > 0.0005f ||
                    p.hidden != c.defaultHidden
            } ?: false
        }

    fun reset(): TouchLayout =
        copy(controls = emptyMap())

    companion object {
        fun preset(id: String): TouchLayout = TouchLayout(preset = id)

        fun fromPreset(id: String): TouchLayout = TouchLayout(
            preset = id,
            controls = TouchControl.all.associate { c ->
                val d = c.defaults(id)
                c.id to ControlPlace(c.id, d[0], d[1], d[2], c.defaultHidden)
            },
        )
    }
}

data class ControlPlace(
    val id: String,
    val x: Float,
    val y: Float,
    val r: Float,
    val hidden: Boolean,
)

enum class ControlGroup(val label: String) {
    Play("In play"),
    Menus("In menus"),
    Always("Everywhere"),
}

enum class ControlShape { Circle, Rect }

/**
 * The touch layout mirror of native/touch.cpp's kButtons/kTwoFinger/kThreeFinger/kFourFinger. Values are the arrays in
 * that file: x and y are shares of the screen, r a share of its height. Keep this list in the same order as the native
 * one when adding a control; layout.toml identifies controls by id, not by position.
 */
data class TouchControl(
    val id: String,
    val label: String,
    val group: ControlGroup,
    val keep: Boolean,
    private val presets: FloatArray,
    val shape: ControlShape = ControlShape.Circle,
    val defaultHidden: Boolean = false,
) {
    fun defaults(preset: String): FloatArray {
        val base = when (preset) {
            "three" -> 3
            "four" -> 6
            else -> 0
        }
        return floatArrayOf(presets[base], presets[base + 1], presets[base + 2])
    }

    companion object {
        val all: List<TouchControl> = listOf(
            TouchControl("fire", "Fire", ControlGroup.Play, false, floatArrayOf(0.924f, 0.660f, 0.095f, 0.900f, 0.680f, 0.080f, 0.900f, 0.700f, 0.075f)),
            TouchControl("fire_left", "Fire (left)", ControlGroup.Play, false, floatArrayOf(0.059f, 0.400f, 0.065f, 0.070f, 0.320f, 0.085f, 0.070f, 0.320f, 0.085f)),
            TouchControl("aim", "Aim", ControlGroup.Play, false, floatArrayOf(0.969f, 0.400f, 0.065f, 0.955f, 0.420f, 0.060f, 0.930f, 0.300f, 0.070f)),
            TouchControl("mode", "Firing mode", ControlGroup.Play, false, floatArrayOf(0.910f, 0.400f, 0.045f, 0.875f, 0.420f, 0.042f, 0.970f, 0.550f, 0.040f)),
            TouchControl("melee", "Melee", ControlGroup.Play, false, floatArrayOf(0.843f, 0.560f, 0.050f, 0.800f, 0.600f, 0.045f, 0.780f, 0.550f, 0.045f)),
            TouchControl("use", "Use", ControlGroup.Play, false, floatArrayOf(0.789f, 0.400f, 0.055f, 0.765f, 0.400f, 0.050f, 0.730f, 0.380f, 0.050f)),
            TouchControl("grenade", "Grenade", ControlGroup.Play, false, floatArrayOf(0.784f, 0.700f, 0.050f, 0.745f, 0.760f, 0.045f, 0.720f, 0.750f, 0.045f)),
            TouchControl("reload", "Reload", ControlGroup.Play, false, floatArrayOf(0.838f, 0.840f, 0.055f, 0.840f, 0.870f, 0.050f, 0.800f, 0.870f, 0.050f)),
            TouchControl("crouch", "Crouch", ControlGroup.Play, false, floatArrayOf(0.964f, 0.880f, 0.050f, 0.955f, 0.880f, 0.050f, 0.830f, 0.240f, 0.050f)),
            TouchControl("weapon", "Weapon", ControlGroup.Play, false, floatArrayOf(0.500f, 0.900f, 0.050f, 0.500f, 0.900f, 0.050f, 0.500f, 0.900f, 0.050f)),
            TouchControl("health", "Health pack", ControlGroup.Play, false, floatArrayOf(0.559f, 0.900f, 0.045f, 0.559f, 0.900f, 0.045f, 0.559f, 0.900f, 0.045f)),
            TouchControl("map", "Objectives", ControlGroup.Play, false, floatArrayOf(0.473f, 0.070f, 0.040f, 0.473f, 0.070f, 0.040f, 0.473f, 0.070f, 0.040f)),
            TouchControl("pause", "Pause", ControlGroup.Play, false, floatArrayOf(0.527f, 0.070f, 0.040f, 0.527f, 0.070f, 0.040f, 0.527f, 0.070f, 0.040f)),
            TouchControl("gyro", "Gyroscope", ControlGroup.Play, false, floatArrayOf(0.419f, 0.070f, 0.040f, 0.419f, 0.070f, 0.040f, 0.419f, 0.070f, 0.040f)),
            TouchControl("back", "Back", ControlGroup.Menus, true, floatArrayOf(0.036f, 0.080f, 0.045f, 0.036f, 0.080f, 0.045f, 0.036f, 0.080f, 0.045f)),
            TouchControl("suppressor", "Suppressor", ControlGroup.Play, false, floatArrayOf(0.700f, 0.560f, 0.042f, 0.690f, 0.560f, 0.042f, 0.655f, 0.550f, 0.042f)),
            TouchControl("dpad_up", "Up", ControlGroup.Menus, false, floatArrayOf(0.110f, 0.600f, 0.048f, 0.110f, 0.600f, 0.048f, 0.110f, 0.600f, 0.048f)),
            TouchControl("dpad_down", "Down", ControlGroup.Menus, false, floatArrayOf(0.110f, 0.800f, 0.048f, 0.110f, 0.800f, 0.048f, 0.110f, 0.800f, 0.048f)),
            TouchControl("dpad_left", "Left", ControlGroup.Menus, false, floatArrayOf(0.064f, 0.700f, 0.048f, 0.064f, 0.700f, 0.048f, 0.064f, 0.700f, 0.048f)),
            TouchControl("dpad_right", "Right", ControlGroup.Menus, false, floatArrayOf(0.156f, 0.700f, 0.048f, 0.156f, 0.700f, 0.048f, 0.156f, 0.700f, 0.048f)),
            TouchControl("panel", "Panel", ControlGroup.Always, false, floatArrayOf(0.581f, 0.070f, 0.040f, 0.581f, 0.070f, 0.040f, 0.581f, 0.070f, 0.040f)),
            TouchControl("enter", "Enter", ControlGroup.Menus, false, floatArrayOf(0.920f, 0.780f, 0.070f, 0.920f, 0.780f, 0.070f, 0.920f, 0.780f, 0.070f)),
            TouchControl("zoom", "Scope zoom", ControlGroup.Play, false, floatArrayOf(0.715f, 0.400f, 0.050f, 0.690f, 0.400f, 0.045f, 0.655f, 0.380f, 0.045f)),
            // a wide, transparent fire button for the left index finger; off until it is switched on, then dragged
            // and resized in the editor like any other control
            TouchControl("fire_zone", "Fire zone", ControlGroup.Play, false,
                floatArrayOf(0.20f, 0.15f, 0.125f, 0.20f, 0.15f, 0.125f, 0.20f, 0.15f, 0.125f),
                ControlShape.Rect, true),
            TouchControl("stick", "Move stick", ControlGroup.Play, true, floatArrayOf(0.108f, 0.700f, 0.120f, 0.130f, 0.720f, 0.120f, 0.130f, 0.720f, 0.120f)),
        )
        val byId: Map<String, TouchControl> = all.associateBy { it.id }
    }
}

/** Native option tables, in the order the engine uses them. */
object Options {
    val screenFilters = listOf(
        "Off", "CRT light", "CRT strong", "Sharpen", "Vibrant", "Noir", "Sepia", "Night vision", "VHS",
    )
    val antiAliasing = listOf("Off", "FXAA", "SSAA 2×")
    val fmvScale = listOf("Fill", "4:3")
    val drawDistances = listOf(1, 2, 4, 8)
    val anisotropic = listOf(0, 2, 4, 8, 16)
    val fpsCaps = listOf(30, 60, 75, 90, 120, 144, 165, 240, 360, 1, 0)
    val audioOutputs = listOf("Automatic", "Mono", "2.0 Stereo", "5.1 Surround")
    val gyroModes = listOf("Off", "Always", "While firing")
    val languages = listOf(
        "int" to "English",
        "fra" to "Français",
        "deu" to "Deutsch",
        "ita" to "Italiano",
        "esp" to "Español",
        "ind" to "Indonesian",
    )
    val skins = listOf(
        "modern" to "Modern",
        "xbox" to "Xbox",
        "clean" to "Clean",
        "classic" to "Classic",
    )
    val presets = listOf("default" to "Default", "three" to "3 Finger", "four" to "4 Finger", "custom" to "Custom")

    fun labelFps(cap: Int): String = when (cap) {
        0 -> "Unlimited"
        1 -> "Display refresh rate"
        else -> "$cap fps"
    }

    fun labelAniso(value: Int): String = if (value == 0) "Game default" else "${value}×"
    fun labelDraw(value: Int): String = if (value == 1) "Game default" else "${value}×"
}

object Bindings {
    data class Action(val id: String, val label: String, val default: String)

    val actions: List<Action> = listOf(
        Action("move_forward", "Move forward", "W"),
        Action("move_back", "Move back", "S"),
        Action("strafe_left", "Strafe left", "A"),
        Action("strafe_right", "Strafe right", "D"),
        Action("walk", "Walk", "Alt"),
        Action("crouch", "Crouch", "C"),
        Action("fire", "Fire", "LMB"),
        Action("aim", "Aim", "RMB"),
        Action("melee", "Melee", "V"),
        Action("reload", "Reload", "R"),
        Action("interact", "Interact", "F"),
        Action("grenade", "Throw grenade", "G"),
        Action("weapon_up", "Change weapon up", "E,Wheel Up"),
        Action("weapon_down", "Change weapon down", "Q,Wheel Down"),
        Action("suppressor", "Toggle suppressor", "T"),
        Action("firing_mode", "Firing mode / scope zoom", "X"),
        Action("health_pack", "Health pack", "H"),
        Action("next_track", "Next music track", "N"),
        Action("back", "Objectives", "Tab"),
        Action("pause", "Pause menu", "Escape"),
        Action("hide_hud", "Toggle HUD", ""),
        Action("settings_panel", "Settings panel", "F10"),
        Action("look_up", "Look up", "Up"),
        Action("look_down", "Look down", "Down"),
        Action("look_left", "Look left", "Left"),
        Action("look_right", "Look right", "Right"),
    )

    fun effective(overrides: Map<String, String>, id: String): String =
        overrides[id] ?: actions.firstOrNull { it.id == id }?.default.orEmpty()

    /** Android KeyEvent keyCode to the names native/touch.cpp's kNames parses. */
    fun androidKeyName(keyCode: Int): String? = when (keyCode) {
        android.view.KeyEvent.KEYCODE_SPACE -> "Space"
        android.view.KeyEvent.KEYCODE_ENTER -> "Enter"
        android.view.KeyEvent.KEYCODE_ESCAPE -> "Escape"
        android.view.KeyEvent.KEYCODE_DEL -> "Backspace"
        android.view.KeyEvent.KEYCODE_TAB -> "Tab"
        android.view.KeyEvent.KEYCODE_SHIFT_LEFT, android.view.KeyEvent.KEYCODE_SHIFT_RIGHT -> "Shift"
        android.view.KeyEvent.KEYCODE_CTRL_LEFT, android.view.KeyEvent.KEYCODE_CTRL_RIGHT -> "Ctrl"
        android.view.KeyEvent.KEYCODE_ALT_LEFT, android.view.KeyEvent.KEYCODE_ALT_RIGHT -> "Alt"
        android.view.KeyEvent.KEYCODE_CAPS_LOCK -> "CapsLock"
        android.view.KeyEvent.KEYCODE_DPAD_UP -> "Up"
        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> "Down"
        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> "Left"
        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> "Right"
        android.view.KeyEvent.KEYCODE_INSERT -> "Insert"
        android.view.KeyEvent.KEYCODE_FORWARD_DEL -> "Delete"
        android.view.KeyEvent.KEYCODE_MOVE_HOME -> "Home"
        android.view.KeyEvent.KEYCODE_MOVE_END -> "End"
        android.view.KeyEvent.KEYCODE_PAGE_UP -> "PageUp"
        android.view.KeyEvent.KEYCODE_PAGE_DOWN -> "PageDown"
        android.view.KeyEvent.KEYCODE_F1 -> "F1"
        android.view.KeyEvent.KEYCODE_F2 -> "F2"
        android.view.KeyEvent.KEYCODE_F3 -> "F3"
        android.view.KeyEvent.KEYCODE_F4 -> "F4"
        android.view.KeyEvent.KEYCODE_F5 -> "F5"
        android.view.KeyEvent.KEYCODE_F6 -> "F6"
        android.view.KeyEvent.KEYCODE_F7 -> "F7"
        android.view.KeyEvent.KEYCODE_F8 -> "F8"
        android.view.KeyEvent.KEYCODE_F9 -> "F9"
        android.view.KeyEvent.KEYCODE_F10 -> "F10"
        android.view.KeyEvent.KEYCODE_F11 -> "F11"
        android.view.KeyEvent.KEYCODE_F12 -> "F12"
        android.view.KeyEvent.KEYCODE_GRAVE -> "Backtick"
        android.view.KeyEvent.KEYCODE_MINUS -> "Minus"
        android.view.KeyEvent.KEYCODE_EQUALS -> "Equals"
        android.view.KeyEvent.KEYCODE_LEFT_BRACKET -> "LBracket"
        android.view.KeyEvent.KEYCODE_RIGHT_BRACKET -> "RBracket"
        android.view.KeyEvent.KEYCODE_BACKSLASH -> "Backslash"
        android.view.KeyEvent.KEYCODE_SEMICOLON -> "Semicolon"
        android.view.KeyEvent.KEYCODE_APOSTROPHE -> "Quote"
        android.view.KeyEvent.KEYCODE_COMMA -> "Comma"
        android.view.KeyEvent.KEYCODE_PERIOD -> "Period"
        android.view.KeyEvent.KEYCODE_SLASH -> "Slash"
        in android.view.KeyEvent.KEYCODE_A..android.view.KeyEvent.KEYCODE_Z ->
            ('A'.code + (keyCode - android.view.KeyEvent.KEYCODE_A)).toChar().toString()
        in android.view.KeyEvent.KEYCODE_0..android.view.KeyEvent.KEYCODE_9 ->
            ('0'.code + (keyCode - android.view.KeyEvent.KEYCODE_0)).toChar().toString()
        in android.view.KeyEvent.KEYCODE_NUMPAD_0..android.view.KeyEvent.KEYCODE_NUMPAD_9 ->
            "Numpad" + ('0'.code + (keyCode - android.view.KeyEvent.KEYCODE_NUMPAD_0)).toChar()
        android.view.KeyEvent.KEYCODE_NUMPAD_ADD -> "NumpadPlus"
        android.view.KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> "NumpadMinus"
        android.view.KeyEvent.KEYCODE_NUMPAD_MULTIPLY -> "NumpadStar"
        android.view.KeyEvent.KEYCODE_NUMPAD_DIVIDE -> "NumpadSlash"
        android.view.KeyEvent.KEYCODE_NUMPAD_DOT -> "NumpadPeriod"
        android.view.KeyEvent.KEYCODE_FORWARD -> "Mouse4"
        android.view.KeyEvent.KEYCODE_BACK -> "Mouse5"
        else -> null
    }
}
