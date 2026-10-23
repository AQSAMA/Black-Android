package com.racas.black.preview

import android.Manifest
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.provider.MediaStore
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The installable UI preview: every screen and every setting runs for real, but the game, its renderer and its saves
 * are not part of this APK. The game files are found for real in the folders the game reads them from, so the setup
 * screens show whether the game is there; the preview cannot copy files or start the game.
 */
class PreviewActivity : ComponentActivity() {

    private lateinit var host: PreviewHost
    private var askedStorage = false  // the explanation is shown once a launch, not on every return to the screen

    private val storagePermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        if (::host.isInitialized) host.refreshGameStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        host = PreviewHost(this)
        host.onGameStatus = ::onGameStatus
        setContent {
            BlackControlCenter(host)
        }
        host.refreshGameStatus()
    }

    override fun onResume() {
        super.onResume()
        // the all-files-access page is a screen of its own: coming back from it means the folders are looked in again
        if (::host.isInitialized) host.refreshGameStatus()
    }

    override fun onDestroy() {
        host.close()
        super.onDestroy()
    }

    internal fun askForStoragePermission() {
        storagePermission.launch(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
        )
    }

    // the first real answer from the scan: without the access the game's own folder cannot be seen at all, so say why
    // before the player is left on a screen about missing files
    private fun onGameStatus(status: GameStatus) {
        if (askedStorage || status is GameStatus.Checking) return
        askedStorage = true
        if (status !is GameStatus.Missing || status.storageGranted || isFinishing || isDestroyed) return
        AlertDialog.Builder(this)
            .setTitle("Storage access needed")
            .setMessage(
                if (Build.VERSION.SDK_INT >= 30) {
                    "Black reads the game from the Black folder on your phone's storage:\n\n" +
                        "/storage/emulated/0/Black\n\n" +
                        "On the next screen, turn on \"Allow access to manage all files\" for this preview, then " +
                        "come back. The preview cannot see your game files without it.\n\n" +
                        "You can also pick the game's folder from the Home screen: the preview reads it through " +
                        "the picker without this access."
                } else {
                    "Black reads the game from the Black folder on your phone's storage:\n\n" +
                        "/storage/emulated/0/Black\n\n" +
                        "On the next screen, allow this preview to access your storage, then come back. The preview " +
                        "cannot see your game files without it.\n\n" +
                        "You can also pick the game's folder from the Home screen: the preview reads it through " +
                        "the picker without this access."
                },
            )
            .setPositiveButton("Allow access") { _, _ -> host.requestStorageAccess() }
            .setNegativeButton("Not now", null)
            .show()
    }
}

private class PreviewHost(private val activity: PreviewActivity) : ControlCenterHost {

    // called on the main thread with every answer, so the activity can explain the storage access on the first one
    var onGameStatus: ((GameStatus) -> Unit)? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val store = TomlSettingsStore(
        File(activity.getExternalFilesDir(null) ?: activity.filesDir, "control-center"),
    )
    private val prefs = activity.getSharedPreferences("preview-game", Context.MODE_PRIVATE)
    private val _game = MutableStateFlow<GameStatus>(GameStatus.Checking)
    private val _update = MutableStateFlow<UpdateState>(UpdateState.Idle)
    private var scan: Job? = null

    // the folder the player picked with the folder picker: it says where the game is better than the default folders
    // do, and read through the picker's own grant it works without all files access
    @Volatile
    private var pickedTree: Uri? = prefs.getString(PICKED_TREE, null)?.let(Uri::parse)

    override val appVersion: String = packageVersion(activity)
    override val isPreview: Boolean = true
    override val gameStatus: StateFlow<GameStatus> = _game.asStateFlow()
    override val updateState: StateFlow<UpdateState> = _update.asStateFlow()
    override val settingsStore: SettingsStore = store
    override val preferencesStore = InterfacePreferencesStore(activity)

    override fun refreshGameStatus() {
        if (_game.value is GameStatus.Importing) return
        scan?.cancel()
        scan = scope.launch {
            val status = withContext(Dispatchers.IO) { scanGame() }
            _game.value = status
            onGameStatus?.invoke(status)
        }
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
        if (!DocumentsContract.isTreeUri(uri)) {
            // the file picker's .iso: the preview reads folders and copies nothing, so say that instead of changing the
            // status the screen already shows
            toast(
                "The preview reads the game's folder instead of copying files. Pick the folder that holds " +
                    "default.xbe and its folders, or the disc image.",
            )
            return
        }
        runCatching {
            activity.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        pickedTree = uri
        _game.value = GameStatus.Importing(displayName(uri) ?: "game folder", 0f)
        scan?.cancel()
        scope.launch {
            when (val picked = withContext(Dispatchers.IO) { inspectTree(uri) }) {
                is TreeContents.Game -> {
                    remember(uri)
                    _game.value = GameStatus.Ready(picked.path)
                }
                TreeContents.NoGame -> {
                    // the folder stays the one to look in for this run, so a rescan says why again instead of flashing
                    // the answer away; it is not saved, a wrong pick must not outlive the app
                    prefs.edit().remove(PICKED_TREE).apply()
                    _game.value = GameStatus.Error(
                        "That folder has no game files. Pick the folder that holds default.xbe and its folders, " +
                            "or the disc image.",
                    )
                }
                TreeContents.NoAccess -> {
                    pickedTree = null
                    prefs.edit().remove(PICKED_TREE).apply()
                    _game.value = GameStatus.Error("The folder cannot be read. Pick it again and allow access.")
                }
            }
        }
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
        scan?.cancel()
        scope.cancel()
    }

    // the picked folder for the next run once the game was found in it, and the one before let go
    private fun remember(tree: Uri) {
        val saved = prefs.getString(PICKED_TREE, null)
        if (saved != null && saved != tree.toString()) {
            runCatching {
                activity.contentResolver.releasePersistableUriPermission(
                    Uri.parse(saved),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
        pickedTree = tree
        prefs.edit().putString(PICKED_TREE, tree.toString()).apply()
    }

    // the folders the game itself reads, in native/game_files.cpp's order: the app's own assets, the phone's Black
    // folder, then the Black folder at the top of each card. Read without all files access they just look empty.
    private fun scanGame(): GameStatus {
        val tree = pickedTree
        if (tree != null) {
            when (val picked = inspectTree(tree)) {
                is TreeContents.Game -> return GameStatus.Ready(picked.path)
                TreeContents.NoAccess -> {
                    pickedTree = null
                    prefs.edit().remove(PICKED_TREE).apply()
                }
                TreeContents.NoGame -> {
                    // the picked folder is where the player said the game is: when the files are gone from it that is
                    // what the screen says, unless one of the default folders has the game after all
                    val found = findKnownGame()
                    if (found != null) {
                        pickedTree = null
                        prefs.edit().remove(PICKED_TREE).apply()
                        return GameStatus.Ready(found.absolutePath)
                    }
                    return GameStatus.Error(
                        "The folder you picked has no game files. Pick the folder that holds default.xbe and its " +
                            "folders, or the disc image.",
                    )
                }
            }
        }
        findKnownGame()?.let { return GameStatus.Ready(it.absolutePath) }
        return GameStatus.Missing(hasStorageAccess())
    }

    private fun findKnownGame(): File? {
        for (root in gameFolders()) {
            if (hasGame(root)) return root
            val assets = File(root, "assets")
            if (hasGame(assets)) return assets
        }
        return null
    }

    private fun gameFolders(): List<File> {
        val out = mutableListOf<File>()
        activity.getExternalFilesDir(null)?.let { out += File(it, "assets") }
        out += File(Environment.getExternalStorageDirectory(), "Black")
        if (Build.VERSION.SDK_INT >= 30) {
            activity.getSystemService(StorageManager::class.java)?.storageVolumes?.forEach { volume ->
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

    // what the picked folder holds, read through the picker's grant: the game's default.xbe or disc image in it, or in
    // its assets folder, which are the folders the game itself looks in
    private fun inspectTree(tree: Uri): TreeContents = try {
        val root = DocumentsContract.getTreeDocumentId(tree)
        val contents = children(tree, root)
        if (holdsGame(contents)) {
            TreeContents.Game(pathOf(tree))
        } else {
            val assets = contents.firstOrNull { it.directory && it.name.equals("assets", ignoreCase = true) }
            if (assets != null && holdsGame(children(tree, assets.id))) {
                TreeContents.Game(pathOf(tree) + "/assets")
            } else {
                TreeContents.NoGame
            }
        }
    } catch (e: SecurityException) {
        TreeContents.NoAccess
    } catch (e: Exception) {
        TreeContents.NoAccess
    }

    private fun holdsGame(contents: List<Doc>): Boolean = contents.any {
        !it.directory &&
            (it.name.equals("default.xbe", ignoreCase = true) || it.name.endsWith(".iso", ignoreCase = true))
    }

    private fun children(tree: Uri, parentId: String): List<Doc> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
        val out = ArrayList<Doc>()
        activity.contentResolver.query(
            uri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
            ),
            null,
            null,
            null,
        )?.use { cursor: Cursor ->
            while (cursor.moveToNext()) {
                out += Doc(
                    id = cursor.getString(0),
                    name = cursor.getString(1) ?: "",
                    directory = cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR,
                    size = if (cursor.isNull(3)) -1 else cursor.getLong(3),
                )
            }
        }
        return out
    }

    // the game's own path for the picked folder when the picker shows the shared storage: the doc id names the volume
    // and the folder ("primary:Black"), so it says /storage/emulated/0/Black without looking at the disk
    private fun pathOf(tree: Uri): String {
        treeFolder(tree)?.let { return it.absolutePath }
        return displayName(tree) ?: "picked folder"
    }

    private fun treeFolder(tree: Uri): File? {
        val id = runCatching { DocumentsContract.getTreeDocumentId(tree) }.getOrNull() ?: return null
        val colon = id.indexOf(':')
        if (colon < 0) return null
        val volume = id.substring(0, colon)
        val path = id.substring(colon + 1)
        if (volume == "raw") return if (path.startsWith("/")) File(path) else null
        val root = when (volume) {
            "primary", "home" -> Environment.getExternalStorageDirectory()
            else -> File("/storage", volume)
        }
        return if (path.isEmpty()) root else File(root, path)
    }

    private fun displayName(tree: Uri): String? = try {
        val doc = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        activity.contentResolver.query(
            doc,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor -> if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getString(0) else null }
    } catch (e: Exception) {
        null
    }

    private sealed interface TreeContents {
        data class Game(val path: String) : TreeContents
        data object NoGame : TreeContents
        data object NoAccess : TreeContents
    }

    private data class Doc(val id: String, val name: String, val directory: Boolean, val size: Long)

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

private const val PICKED_TREE = "picked-tree"

private fun packageVersion(activity: ComponentActivity): String = runCatching {
    activity.packageManager.getPackageInfo(activity.packageName, 0).versionName
}.getOrNull() ?: "preview"
