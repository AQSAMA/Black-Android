package com.racas.black;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;

import java.io.File;

import org.libsdl.app.SDLActivity;

// SDL's activity running libmain.so. The game goes in the Black folder on the phone's storage
// (/storage/emulated/0/Black). Reading it needs all files access, a special access Android grants on a settings page
// of its own, never among the app's ordinary permissions. This asks for it and says where the game goes, each time
// the app comes back without them; the game itself waits and starts as soon as it finds the files.
// At launch it also asks GitHub for a newer release (UpdateChecker) and tells of it once nothing else is asked.
public class BlackActivity extends SDLActivity {
    static final String SHARED = "Black";

    private AlertDialog dialog;  // the one up, at most one at a time
    private boolean notNow;      // the player turned the access down this run
    private boolean resumed;
    private UpdateChecker.Release update;  // a newer release not shown yet
    private final GameImporter importer = new GameImporter(this);
    private boolean importing;  // the file picker or the copy is up: nothing else is asked meanwhile

    @Override
    protected String[] getLibraries() {
        return new String[] {"SDL3", "main"};
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        File own = getExternalFilesDir(null);
        if (own != null) new File(own, "assets").mkdirs();
        UpdateChecker.check(this, r -> {
            update = r;
            tellOfUpdate();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        askForGame();
        tellOfUpdate();
    }

    @Override
    protected void onPause() {
        resumed = false;
        super.onPause();
    }

    private void askForGame() {
        if (importing || (dialog != null && dialog.isShowing())) return;
        File own = getExternalFilesDir(null);
        if (own != null && hasGame(new File(own, "assets"))) return;
        if (!Environment.isExternalStorageManager()) {
            if (!notNow) askAllFilesAccess();
            return;
        }
        File shared = new File(Environment.getExternalStorageDirectory(), SHARED);
        shared.mkdirs();
        if (!hasGame(shared) && !hasGame(new File(shared, "assets"))) tellWhereTheGameGoes();
    }

    // the newer release, once the app is in front with no other dialog up; shown once a run
    private void tellOfUpdate() {
        if (update == null || importing || !resumed || isFinishing() || (dialog != null && dialog.isShowing())) return;
        UpdateChecker.Release r = update;
        update = null;
        String installed = "";
        try {
            installed = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            // only for the message
        }
        StringBuilder msg = new StringBuilder(r.name + " is available");
        if (!installed.isEmpty()) msg.append(" (you have ").append(installed).append(")");
        msg.append('.');
        if (!r.changes.isEmpty()) msg.append("\n\nWhat's new:\n").append(r.changes);
        msg.append("\n\nInstall it over this one: your settings and saves stay.");
        dialog = new AlertDialog.Builder(this)
                .setTitle("Update available")
                .setMessage(msg)
                .setPositiveButton("Download", (d, w) -> open(r.apk != null ? r.apk : r.page))
                .setNeutralButton("Skip this version", (d, w) -> UpdateChecker.skip(this, r.tag))
                .setNegativeButton("Later", null)
                .show();
    }

    private void open(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            // no browser: nothing to open it with
        }
    }

    // default.xbe or a disc image in the folder
    private static boolean hasGame(File dir) {
        if (new File(dir, "default.xbe").exists()) return true;
        File[] files = dir.listFiles();
        if (files == null) return false;
        for (File f : files)
            if (f.getName().toLowerCase().endsWith(".iso")) return true;
        return false;
    }

    private void askAllFilesAccess() {
        dialog = new AlertDialog.Builder(this)
                .setTitle("Storage access")
                .setMessage("Black reads the game from the Black folder on your phone's storage:\n\n"
                        + "/storage/emulated/0/" + SHARED + "\n\n"
                        + "On the next screen turn on \"Allow access to manage all files\" for Black, then come back.")
                .setCancelable(false)
                .setPositiveButton("Allow access", (d, w) -> openAccessSettings())
                .setNegativeButton("Not now", (d, w) -> notNow = true)
                .setNeutralButton("Import game", (d, w) -> startImport())
                .setOnDismissListener(d -> tellOfUpdate())
                .show();
    }

    private void tellWhereTheGameGoes() {
        dialog = new AlertDialog.Builder(this)
                .setTitle("Game files")
                .setMessage("Copy the game into the Black folder on your phone's storage:\n\n"
                        + "/storage/emulated/0/" + SHARED + "\n\n"
                        + "Its disc image (.iso), or default.xbe with its folders. The game starts as soon as they "
                        + "are there.\n\n"
                        + "Or import the disc image from your Downloads, a cloud drive or a USB stick.")
                .setPositiveButton("OK", null)
                .setNeutralButton("Import game", (d, w) -> startImport())
                .setOnDismissListener(d -> tellOfUpdate())
                .show();
    }

    // the disc image the player picks, copied where the game looks: the Black folder with all files access, else the
    // app's own assets folder, which needs none
    private void startImport() {
        importing = importer.pick();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != GameImporter.REQUEST) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        Uri uri = resultCode == RESULT_OK && data != null ? data.getData() : null;
        if (uri == null) {
            importing = false;  // the picker was closed: onResume asks again
            return;
        }
        File dir = Environment.isExternalStorageManager()
                ? new File(Environment.getExternalStorageDirectory(), SHARED)
                : new File(getExternalFilesDir(null), "assets");
        importer.copy(uri, dir, ok -> {
            importing = false;
            if (!ok) askForGame();
            tellOfUpdate();
        });
    }

    // the app's own page of the access, or the list of every app's where a phone has no such page
    private void openAccessSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
        } catch (ActivityNotFoundException e) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            } catch (ActivityNotFoundException e2) {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName())));
            }
        }
    }
}
