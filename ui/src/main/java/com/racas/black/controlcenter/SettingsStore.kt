package com.racas.black.controlcenter

import android.util.Log
import java.io.File
import java.util.Locale

interface SettingsStore {
    fun load(): GameSettings
    fun save(settings: GameSettings)
    fun exportText(settings: GameSettings): String
    val directory: File

    companion object {
        const val MARKER = ".settings-changed"
    }
}

/**
 * Reads and writes the files native/main.cpp, native/touch.cpp and native/gyro.cpp use. After every save it touches
 * [MARKER]; the game's event loop sees it and reloads without needing a JNI bridge.
 */
class TomlSettingsStore(private val root: File) : SettingsStore {

    override val directory: File get() = root

    private val blackFile get() = File(root, "black.toml")
    private val layoutFile get() = File(root, "layout.toml")
    private val androidFile get() = File(root, "android.toml")
    private val markerFile get() = File(root, SettingsStore.MARKER)

    override fun load(): GameSettings {
        root.mkdirs()
        val app = read(blackFile)
        val layoutDoc = read(layoutFile)
        val android = read(androidFile)

        val layout = readLayout(layoutDoc)
        val appDoc = app
        val extra = linkedMapOf<String, String>()
        val known = KNOWN_TOP_LEVEL
        for ((key, value) in appDoc.table("")) {
            if (key !in known) extra[key] = raw(value)
        }

        return GameSettings(
            renderPercent = appDoc.long("", "render_percent")?.toInt() ?: 67,
            fov = appDoc.long("", "fov")?.toInt() ?: 70,
            viewmodelFov = appDoc.long("", "viewmodel_fov")?.toInt() ?: 70,
            fmvScale = appDoc.long("", "fmv_scale")?.toInt() ?: 0,
            screenFilter = appDoc.long("", "screen_filter")?.toInt() ?: 0,
            vsync = appDoc.bool("", "vsync") ?: false,
            fpsCap = appDoc.long("", "fps_cap")?.toInt() ?: 0,
            fpsCounter = appDoc.bool("", "fps_counter") ?: false,
            anisotropicFiltering = appDoc.long("", "anisotropic_filtering")?.toInt() ?: 0,
            drawDistance = appDoc.long("", "draw_distance")?.toInt() ?: 1,
            reloadBlur = appDoc.bool("", "reload_blur") ?: true,
            bloom = appDoc.bool("", "bloom") ?: true,
            fog = appDoc.bool("", "fog") ?: true,
            antiAliasing = appDoc.long("", "anti_aliasing")?.toInt() ?: 0,
            hdr = appDoc.bool("", "hdr") ?: false,
            touchSensitivity = appDoc.double("", "touch_sensitivity")?.toFloat() ?: 1.0f,
            touchLayout = layout,
            gyroMode = android.long("", "gyro")?.toInt() ?: 0,
            gyroSensitivity = android.double("", "gyro_sensitivity")?.toFloat() ?: 1.5f,
            gyroInvertX = android.bool("", "gyro_invert_x") ?: false,
            gyroInvertY = android.bool("", "gyro_invert_y") ?: false,
            leftDeadzone = appDoc.long("", "left_stick_deadzone")?.toInt() ?: 0,
            rightDeadzone = appDoc.long("", "right_stick_deadzone")?.toInt() ?: 0,
            leftSensitivity = appDoc.double("", "left_stick_sensitivity")?.toFloat() ?: 1.0f,
            rightSensitivity = appDoc.double("", "right_stick_sensitivity")?.toFloat() ?: 1.0f,
            mouseSensitivity = appDoc.double("", "mouse_sensitivity")?.toFloat() ?: 1.0f,
            invertMouse = appDoc.bool("", "invert_mouse") ?: false,
            holdAim = appDoc.bool("", "hold_aim") ?: true,
            walkSpeed = appDoc.long("", "walk_speed")?.toInt() ?: 50,
            bindingsOverrides = appDoc.table("keys").mapNotNull { (k, v) ->
                (v as? TomlValue.Str)?.let { k to it.value }
            }.toMap(),
            audioOutput = appDoc.long("", "audio_output")?.toInt() ?: 0,
            language = appDoc.string("", "language") ?: "int",
            skipIntros = appDoc.bool("", "skip_intros") ?: false,
            infiniteAmmo = appDoc.bool("", "infinite_ammo") ?: false,
            rapidFire = appDoc.long("", "rapid_fire")?.toInt() ?: 1,
            noRecoil = appDoc.bool("", "no_recoil") ?: false,
            gameSpeed = appDoc.long("", "game_speed")?.toInt() ?: 100,
            godMode = appDoc.bool("", "god_mode") ?: false,
            oneHitKill = appDoc.bool("", "one_hit_kill") ?: false,
            infiniteGrenades = appDoc.bool("", "infinite_grenades") ?: false,
            unlockLevels = appDoc.bool("", "unlock_levels") ?: false,
            unlockBlackOps = appDoc.bool("", "unlock_black_ops") ?: false,
            extraKeys = extra,
        )
    }

    override fun save(settings: GameSettings) {
        root.mkdirs()
        writeAtomically(blackFile, blackToml(settings))
        writeAtomically(layoutFile, layoutToml(settings.touchLayout))
        writeAtomically(androidFile, androidToml(settings))
        // The marker is written last: the game only reloads complete files.
        runCatching { markerFile.writeText(System.currentTimeMillis().toString()) }
    }

    override fun exportText(settings: GameSettings): String = buildString {
        appendLine("# Black Android — Control Center export")
        appendLine("# ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(java.util.Date())}")
        appendLine()
        appendLine("# black.toml")
        appendLine(blackToml(settings))
        appendLine("# android.toml")
        appendLine(androidToml(settings))
        appendLine("# layout.toml")
        appendLine(layoutToml(settings.touchLayout))
    }

    private fun blackToml(s: GameSettings): String {
        val app = read(blackFile)
        val b = TomlWriter.Builder()
            .topLevel("display_mode", app.string("", "display_mode") ?: "fullscreen")
            .topLevel("width", app.long("", "width") ?: 1920L)
            .topLevel("height", app.long("", "height") ?: 1080L)
            .topLevel("render_percent", s.renderPercent)
            .topLevel("fps_cap", s.fpsCap)
            .topLevel("vsync", s.vsync)
            .topLevel("anisotropic_filtering", s.anisotropicFiltering)
            .topLevel("join_ip", app.string("", "join_ip") ?: "")
            .topLevel("defaults", 2L)
            .topLevel("skip_intros", s.skipIntros)
            .topLevel("fps_counter", s.fpsCounter)
            .topLevel("reload_blur", s.reloadBlur)
            .topLevel("bloom", s.bloom)
            .topLevel("fog", s.fog)
            .topLevel("anti_aliasing", s.antiAliasing)
            .topLevel("hdr", s.hdr)
            .topLevel("fmv_scale", s.fmvScale)
            .topLevel("screen_filter", s.screenFilter)
            .topLevel("invert_mouse", s.invertMouse)
            .topLevel("hold_aim", s.holdAim)
            .topLevel("walk_speed", s.walkSpeed)
            .topLevel("left_stick_deadzone", s.leftDeadzone)
            .topLevel("right_stick_deadzone", s.rightDeadzone)
            .topLevel("fov", s.fov)
            .topLevel("draw_distance", s.drawDistance)
            .topLevel("viewmodel_fov", s.viewmodelFov)
            .topLevel("audio_output", s.audioOutput)
            .topLevel("infinite_ammo", s.infiniteAmmo)
            .topLevel("rapid_fire", s.rapidFire)
            .topLevel("no_recoil", s.noRecoil)
            .topLevel("game_speed", s.gameSpeed)
            .topLevel("god_mode", s.godMode)
            .topLevel("one_hit_kill", s.oneHitKill)
            .topLevel("infinite_grenades", s.infiniteGrenades)
            .topLevel("unlock_levels", s.unlockLevels)
            .topLevel("unlock_black_ops", s.unlockBlackOps)
            .topLevel("mouse_sensitivity", s.mouseSensitivity)
            .topLevel("touch_sensitivity", s.touchSensitivity)
            .topLevel("left_stick_sensitivity", s.leftSensitivity)
            .topLevel("right_stick_sensitivity", s.rightSensitivity)
            .topLevel("language", s.language)

        val known = KNOWN_TOP_LEVEL
        for ((key, value) in s.extraKeys) if (key !in known) b.rawTopLevel(key, value)

        b.table("keys")
        for ((id, keys) in s.bindingsOverrides) b.value(id, keys)
        return b.toString()
    }

    private fun androidToml(s: GameSettings): String = TomlWriter.Builder()
        .topLevel("gyro", s.gyroMode)
        .topLevel("gyro_sensitivity", s.gyroSensitivity)
        .topLevel("gyro_invert_x", s.gyroInvertX)
        .topLevel("gyro_invert_y", s.gyroInvertY)
        .toString()

    private fun layoutToml(layout: TouchLayout): String {
        val b = TomlWriter.Builder()
            .topLevel("preset", layout.preset)
            .topLevel("opacity", (layout.opacity.coerceIn(20, 100).toDouble() / 100.0))
            .topLevel("scale", (layout.scale.coerceIn(50, 150).toDouble() / 100.0))
            .topLevel("skin", layout.skin)
        for (control in TouchControl.all) {
            val p = layout.placeOrDefault(control.id)
            b.table("controls.${control.id}")
                .value("x", p.x.toDouble().coerceIn(0.0, 1.0))
                .value("y", p.y.toDouble().coerceIn(0.0, 1.0))
                .value("r", p.r.toDouble().coerceIn(0.02, 0.25))
                .value("hidden", p.hidden && !control.keep)
        }
        return b.toString()
    }

    private fun readLayout(doc: TomlDocument): TouchLayout {
        val base = doc.string("", "preset")?.takeIf { it in setOf("default", "three", "four") } ?: "default"
        val opacity = ((doc.double("", "opacity") ?: 0.85) * 100.0).toInt().coerceIn(20, 100)
        val scale = ((doc.double("", "scale") ?: 1.0) * 100.0).toInt().coerceIn(50, 150)
        val skin = doc.string("", "skin")?.takeIf { it in setOf("modern", "xbox", "clean", "classic") } ?: "modern"
        val places = linkedMapOf<String, ControlPlace>()
        for (control in TouchControl.all) {
            val table = doc.table("controls.${control.id}")
            val defaults = control.defaults(base)
            places[control.id] = ControlPlace(
                control.id,
                (table["x"] as? TomlValue.DoubleValue)?.value?.toFloat()
                    ?: (table["x"] as? TomlValue.IntValue)?.value?.toFloat()
                    ?: defaults[0],
                (table["y"] as? TomlValue.DoubleValue)?.value?.toFloat()
                    ?: (table["y"] as? TomlValue.IntValue)?.value?.toFloat()
                    ?: defaults[1],
                (table["r"] as? TomlValue.DoubleValue)?.value?.toFloat()
                    ?: (table["r"] as? TomlValue.IntValue)?.value?.toFloat()
                    ?: defaults[2],
                (((table["hidden"] as? TomlValue.Bool)?.value) ?: control.defaultHidden) && !control.keep,
            )
        }
        return TouchLayout(base, opacity, scale, skin, places)
    }

    private fun read(file: File): TomlDocument {
        if (!file.isFile) return TomlDocument.parse("")
        return try {
            TomlDocument.parse(file.readText())
        } catch (e: Exception) {
            Log.w("BlackSettings", "could not read ${file.name}", e)
            TomlDocument.parse("")
        }
    }

    private fun writeAtomically(file: File, text: String) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.writeText(text)
            tmp.delete()
        }
    }

    private fun raw(value: TomlValue): String = when (value) {
        is TomlValue.Str -> TomlWriter.scalar(value.value)
        is TomlValue.Bool -> if (value.value) "true" else "false"
        is TomlValue.IntValue -> value.value.toString()
        is TomlValue.DoubleValue -> TomlWriter.scalar(value.value)
    }

    private companion object {
        val KNOWN_TOP_LEVEL = setOf(
            "display_mode", "width", "height", "render_percent", "fps_cap", "vsync", "anisotropic_filtering",
            "join_ip", "defaults", "skip_intros", "fps_counter", "reload_blur", "bloom", "fog", "anti_aliasing",
            "hdr", "fmv_scale", "screen_filter", "invert_mouse", "hold_aim", "walk_speed", "left_stick_deadzone",
            "right_stick_deadzone", "fov", "draw_distance", "viewmodel_fov", "audio_output", "infinite_ammo",
            "rapid_fire", "no_recoil", "game_speed", "god_mode", "one_hit_kill", "infinite_grenades",
            "unlock_levels", "unlock_black_ops", "mouse_sensitivity", "touch_sensitivity",
            "left_stick_sensitivity", "right_stick_sensitivity", "language",
        )
    }
}
