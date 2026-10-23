package com.racas.black.controlcenter

import android.net.Uri
import kotlinx.coroutines.flow.StateFlow

sealed interface GameStatus {
    data object Checking : GameStatus
    data class Ready(val path: String, val removable: Boolean = false) : GameStatus
    data class Missing(val storageGranted: Boolean) : GameStatus
    data class Importing(val name: String, val progress: Float) : GameStatus
    data class Error(val message: String) : GameStatus
}

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object Latest : UpdateState
    data class Available(
        val tag: String,
        val name: String,
        val changes: String,
        val page: String,
        val apk: String?,
    ) : UpdateState

    data class Failed(val message: String) : UpdateState
}

/**
 * The real app and the preview APK implement this. Keeping it in the UI module means the same screens run against the
 * game or against demo data, and the preview never needs libmain.so.
 */
interface ControlCenterHost {
    val appVersion: String
    val isPreview: Boolean
    val gameStatus: StateFlow<GameStatus>
    val updateState: StateFlow<UpdateState>
    val settingsStore: SettingsStore
    val preferencesStore: InterfacePreferencesStore

    fun refreshGameStatus()
    fun requestStorageAccess()
    fun importGame(uri: Uri)
    fun launchGame()
    fun checkForUpdates()
    fun openUrl(url: String)
    fun shareSettings(settings: GameSettings)
    fun exportSettings(settings: GameSettings)
    fun setKeepScreenOn(enabled: Boolean)
    fun toast(message: String)
}
