package com.racas.black.preview

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.racas.black.controlcenter.BlackControlCenter
import com.racas.black.controlcenter.ControlCenterHost
import com.racas.black.controlcenter.GameSettings
import com.racas.black.controlcenter.GameStatus
import com.racas.black.controlcenter.InterfacePreferencesStore
import com.racas.black.controlcenter.SettingsStore
import com.racas.black.controlcenter.TomlSettingsStore
import com.racas.black.controlcenter.UpdateState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The installable UI preview: every screen and every setting runs for real, but the game, its files and the
 * renderer are not part of this APK. Settings are still written as TOML, in the preview's own folder.
 */
class PreviewActivity : ComponentActivity() {

    private lateinit var host: PreviewHost

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        host = PreviewHost(this)
        setContent {
            BlackControlCenter(host)
        }
    }

    override fun onDestroy() {
        host.close()
        super.onDestroy()
    }
}

private class PreviewHost(private val activity: ComponentActivity) : ControlCenterHost {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val store = TomlSettingsStore(
        File(activity.getExternalFilesDir(null) ?: activity.filesDir, "control-center"),
    )
    private val _game = MutableStateFlow<GameStatus>(
        GameStatus.Ready("/storage/emulated/0/Black — demo data", removable = false),
    )
    private val _update = MutableStateFlow<UpdateState>(UpdateState.Idle)

    override val appVersion: String = packageVersion(activity)
    override val isPreview: Boolean = true
    override val gameStatus: StateFlow<GameStatus> = _game.asStateFlow()
    override val updateState: StateFlow<UpdateState> = _update.asStateFlow()
    override val settingsStore: SettingsStore = store
    override val preferencesStore = InterfacePreferencesStore(activity)

    override fun refreshGameStatus() {
        // The preview never looks for game data: it always has demo content.
        _game.value = GameStatus.Ready("/storage/emulated/0/Black — demo data")
    }

    override fun requestStorageAccess() {
        toast("The preview needs no storage access. The full app asks for it on first launch.")
    }

    override fun importGame(uri: Uri) {
        toast("The preview uses demo settings. Import is available in the full app.")
    }

    override fun launchGame() {
        toast("This is the Control Center preview; the game itself is not included.")
    }

    override fun checkForUpdates() {
        if (_update.value is UpdateState.Checking) return
        _update.value = UpdateState.Checking
        scope.launch {
            delay(900)
            _update.value = UpdateState.Latest
        }
    }

    override fun openUrl(url: String) {
        runCatching {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure { toast("Nothing can open that link.") }
    }

    override fun shareSettings(settings: GameSettings) {
        val text = store.exportText(settings)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Black Control Center settings")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching {
            activity.startActivity(Intent.createChooser(intent, "Share settings"))
        }.onFailure { toast("No app to share the settings with.") }
    }

    override fun exportSettings(settings: GameSettings) {
        val text = store.exportText(settings)
        val name = "black-settings-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())}.toml"
        runCatching { writeDownload(name, text) }
            .onSuccess { toast("Exported $name to Downloads/Black") }
            .onFailure { toast("Export failed: ${it.message}") }
    }

    override fun setKeepScreenOn(enabled: Boolean) {
        if (enabled) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun toast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
    }

    fun close() {
        scope.cancel()
    }

    private fun writeDownload(name: String, text: String) {
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Black")
            }
            val uri = activity.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("could not create the file")
            activity.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                ?: error("could not open the file")
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Black")
            dir.mkdirs()
            File(dir, name).writeText(text)
        }
    }
}

private fun packageVersion(activity: ComponentActivity): String = runCatching {
    activity.packageManager.getPackageInfo(activity.packageName, 0).versionName
}.getOrNull() ?: "preview"
