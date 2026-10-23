package com.racas.black

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.racas.black.controlcenter.BlackControlCenter
import com.racas.black.controlcenter.ControlCenterHost
import com.racas.black.controlcenter.GameSettings
import com.racas.black.controlcenter.GameStatus
import com.racas.black.controlcenter.InterfacePreferencesStore
import com.racas.black.controlcenter.SettingsStore
import com.racas.black.controlcenter.TomlSettingsStore
import com.racas.black.controlcenter.UpdateState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The Compose front end over a running game. It reads and writes the same black.toml, layout.toml and android.toml
 * the renderer uses, touching .settings-changed after every save; the native loop reloads them without a restart.
 */
class SettingsActivity : ComponentActivity() {

    private lateinit var host: AppHost

    private val storagePermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (::host.isInitialized) host.refreshGameStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        host = AppHost(this)
        setContent {
            BlackControlCenter(host)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::host.isInitialized) host.refreshGameStatus()
    }

    internal fun askForStoragePermission() {
        storagePermission.launch(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
        )
    }
}

private class AppHost(private val activity: SettingsActivity) : ControlCenterHost {

    private val _game = MutableStateFlow<GameStatus>(GameStatus.Checking)
    private val _update = MutableStateFlow<UpdateState>(UpdateState.Idle)
    private val importer = GameImporter(activity)

    override val appVersion: String = runCatching {
        activity.packageManager.getPackageInfo(activity.packageName, 0).versionName
    }.getOrNull() ?: "1.0"

    override val isPreview: Boolean = false
    override val gameStatus: StateFlow<GameStatus> = _game.asStateFlow()
    override val updateState: StateFlow<UpdateState> = _update.asStateFlow()

    // native app::ExeDir() is the app-specific external folder, where the renderer keeps its settings
    override val settingsStore: SettingsStore = TomlSettingsStore(
        activity.getExternalFilesDir(null) ?: activity.filesDir,
    )
    override val preferencesStore = InterfacePreferencesStore(activity)

    init {
        refreshGameStatus()
    }

    override fun refreshGameStatus() {
        if (_game.value is GameStatus.Importing) return
        _game.value = scanGame()
    }

    override fun requestStorageAccess() {
        if (hasStorageAccess()) {
            refreshGameStatus()
            return
        }
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                activity.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:${activity.packageName}"),
                    ),
                )
            } catch (e: ActivityNotFoundException) {
                try {
                    activity.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                } catch (e2: ActivityNotFoundException) {
                    activity.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${activity.packageName}"),
                        ),
                    )
                }
            }
        } else {
            activity.askForStoragePermission()
        }
    }

    override fun importGame(uri: Uri) {
        val name = displayName(uri)
        _game.value = GameStatus.Importing(name, 0f)
        val target = if (hasStorageAccess()) {
            File(Environment.getExternalStorageDirectory(), "Black")
        } else {
            File(activity.getExternalFilesDir(null), "assets")
        }
        importer.copy(uri, target) { _ ->
            _game.value = scanGame()
        }
    }

    override fun launchGame() {
        val intent = Intent(activity, BlackActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        activity.startActivity(intent)
        activity.finish()
    }

    override fun checkForUpdates() {
        if (_update.value is UpdateState.Checking) return
        _update.value = UpdateState.Checking
        UpdateChecker.check(activity, true) { release, failed ->
            _update.value = when {
                release != null -> UpdateState.Available(
                    tag = release.tag,
                    name = release.name,
                    changes = release.changes,
                    page = release.page,
                    apk = release.apk,
                )
                failed -> UpdateState.Failed("GitHub could not be reached. Check the connection and try again.")
                else -> UpdateState.Latest
            }
        }
    }

    override fun openUrl(url: String) {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            toast("Nothing on this phone can open that link.")
        }
    }

    override fun shareSettings(settings: GameSettings) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Black Android settings")
            putExtra(Intent.EXTRA_TEXT, settingsStore.exportText(settings))
        }
        try {
            activity.startActivity(Intent.createChooser(intent, "Share settings"))
        } catch (e: ActivityNotFoundException) {
            toast("No app can share the settings.")
        }
    }

    override fun exportSettings(settings: GameSettings) {
        val text = settingsStore.exportText(settings)
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

    private fun scanGame(): GameStatus {
        for (root in gameFolders()) {
            if (hasGame(root)) return GameStatus.Ready(root.absolutePath)
            if (hasGame(File(root, "assets"))) return GameStatus.Ready(File(root, "assets").absolutePath)
        }
        return GameStatus.Missing(hasStorageAccess())
    }

    private fun gameFolders(): List<File> {
        val out = mutableListOf<File>()
        activity.getExternalFilesDir(null)?.let { out += File(it, "assets") }
        out += File(Environment.getExternalStorageDirectory(), "Black")
        if (Build.VERSION.SDK_INT >= 30) {
            val manager = activity.getSystemService(StorageManager::class.java)
            manager?.storageVolumes?.forEach { volume ->
                if (!volume.isPrimary && Environment.MEDIA_MOUNTED == volume.state) {
                    volume.directory?.let { out += File(it, "Black") }
                }
            }
        } else {
            // Android 10: each card's root above the app's own folder on it
            activity.getExternalFilesDirs(null).drop(1).forEach { own ->
                var root: File? = own
                repeat(4) { root = root?.parentFile }
                root?.let { out += File(it, "Black") }
            }
        }
        return out
    }

    private fun hasGame(dir: File): Boolean {
        if (File(dir, "default.xbe").isFile) return true
        val files = dir.listFiles() ?: return false
        return files.any { it.isFile && it.name.endsWith(".iso", ignoreCase = true) }
    }

    private fun hasStorageAccess(): Boolean {
        if (Build.VERSION.SDK_INT >= 30) return Environment.isExternalStorageManager()
        return activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED &&
            activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun displayName(uri: Uri): String {
        try {
            activity.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor: Cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) return File(cursor.getString(0)).name
            }
        } catch (e: Exception) {
            // fall back on the path
        }
        return uri.lastPathSegment?.let { File(it).name } ?: "disc image"
    }

    private fun writeDownload(name: String, text: String) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Black")
        }
        val uri = activity.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("could not create the file")
        activity.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
            ?: error("could not open the file")
    }
}
