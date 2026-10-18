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
public class BlackActivity extends SDLActivity {
    static final String SHARED = "Black";

    private AlertDialog dialog;  // the one up, at most one at a time
    private boolean notNow;      // the player turned the access down this run

    @Override
    protected String[] getLibraries() {
        return new String[] {"SDL3", "main"};
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        File own = getExternalFilesDir(null);
        if (own != null) new File(own, "assets").mkdirs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dialog != null && dialog.isShowing()) return;
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
                .show();
    }

    private void tellWhereTheGameGoes() {
        dialog = new AlertDialog.Builder(this)
                .setTitle("Game files")
                .setMessage("Copy the game into the Black folder on your phone's storage:\n\n"
                        + "/storage/emulated/0/" + SHARED + "\n\n"
                        + "Its disc image (.iso), or default.xbe with its folders. The game starts as soon as they "
                        + "are there.")
                .setPositiveButton("OK", null)
                .show();
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
