package com.racas.black;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

// Copies the game the player picks with Android's pickers into the folder the game is read from: a disc image (.iso)
// from the file picker, or a whole game folder (default.xbe with its data folders) from the folder picker. The copy is
// written as .part and renamed at the end: the game looks for any .iso there every second and would open a half
// written one, and default.xbe is copied last so the game cannot start on a half copied folder.
final class GameImporter {
    static final int REQUEST = 0x8A11;
    private static final String TAG = "BlackImport";
    private static final byte[] MAGIC = "MICROSOFT*XBOX*MEDIA".getBytes();
    // where the volume descriptor sits: the game partition alone, redump, XGD3, XGD1 video offsets
    private static final long[] PARTITIONS = {0, 0x18300000L, 0xFD90000L, 0x2080000L};
    private static final int BUFFER = 4 << 20;

    private final Activity activity;
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean cancelled;

    GameImporter(Activity activity) {
        this.activity = activity;
    }

    // the picker; any file type, since phones rarely know a type for .iso
    boolean pick() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        try {
            activity.startActivityForResult(i, REQUEST);
            return true;
        } catch (ActivityNotFoundException e) {
            message("Import game", "This phone has no file picker to choose the disc image with.", null);
            return false;
        }
    }

    // the picked file into dir; done runs on the main thread with true once the image is in place
    void copy(Uri uri, File dir, Consumer<Boolean> done) {
        String name = displayName(uri);
        long size = size(uri);
        if (name == null || !name.toLowerCase().endsWith(".iso")) {
            message("Import game", "Choose the game's disc image, a file ending in .iso.", () -> done.accept(false));
            return;
        }
        dir.mkdirs();
        if (size > 0 && dir.getUsableSpace() < size + (64L << 20)) {
            message("Not enough space", String.format("The disc image needs %.1f GB free, there are %.1f GB.",
                    size / 1e9, dir.getUsableSpace() / 1e9), () -> done.accept(false));
            return;
        }

        ProgressBar bar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(1000);
        bar.setIndeterminate(size <= 0);
        TextView text = new TextView(activity);
        text.setText("Starting…");
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * activity.getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(bar);
        box.addView(text);
        cancelled = false;
        AlertDialog progress = new AlertDialog.Builder(activity)
                .setTitle("Importing " + name)
                .setView(box)
                .setCancelable(false)
                .setNegativeButton("Cancel", (d, w) -> cancelled = true)
                .show();
        progress.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        File part = new File(dir, name + ".part");
        File iso = new File(dir, name);
        new Thread(() -> {
            String error = null;
            try (ParcelFileDescriptor pfd = activity.getContentResolver().openFileDescriptor(uri, "r");
                 FileInputStream in = new FileInputStream(pfd.getFileDescriptor());
                 FileChannel src = in.getChannel()) {
                if (isXboxImage(src) == Boolean.FALSE) {
                    error = name + " is not an Xbox disc image.";
                } else {
                    try (FileOutputStream out = new FileOutputStream(part); FileChannel dst = out.getChannel()) {
                        ByteBuffer buf = ByteBuffer.allocateDirect(BUFFER);
                        long copied = 0, shown = 0;
                        while (!cancelled && src.read(buf) > 0) {
                            buf.flip();
                            while (buf.hasRemaining()) copied += dst.write(buf);
                            buf.clear();
                            if (copied - shown >= (16L << 20)) {
                                shown = copied;
                                long c = copied;
                                main.post(() -> show(bar, text, c, size));
                            }
                        }
                        out.getFD().sync();
                        if (!cancelled && size > 0 && copied != size) error = "The copy stopped early.";
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "import failed", e);
                error = "The copy failed: " + e.getMessage();
            }
            boolean ok = error == null && !cancelled && part.renameTo(iso);
            if (!ok) {
                part.delete();
                if (error == null && !cancelled) error = "The image could not be put in place.";
            }
            String why = error;
            main.post(() -> {
                progress.dismiss();
                if (ok) {
                    Log.i(TAG, "imported " + iso);
                    message("Game imported", name + " is in place. The game starts in a moment.", () -> done.accept(true));
                } else if (why != null) {
                    message("Import failed", why, () -> done.accept(false));
                } else {
                    done.accept(false);  // cancelled
                }
            });
        }, "GameImport").start();
    }

    // The folder picker (ACTION_OPEN_DOCUMENT_TREE) hands back a tree uri, not a path: DocumentsContract walks it, so
    // the chosen folder is read the way the picker showed it, with no storage permission needed for the read.
    static boolean isTree(Uri uri) {
        return uri != null && DocumentsContract.isTreeUri(uri);
    }

    // The shared-storage folder a tree uri names, when it is one: /storage/emulated/0/Black, a Black folder on a card
    // and the app's own folders are. null for a folder another provider holds (Downloads, a cloud drive): it can still
    // be copied from, only comparing it with the game's own folders by path is impossible then.
    static File treeFolder(Uri tree) {
        if (!isTree(tree)) return null;
        String id;
        try {
            id = DocumentsContract.getTreeDocumentId(tree);
        } catch (Exception e) {
            return null;
        }
        if (id == null) return null;
        int colon = id.indexOf(':');
        if (colon < 0) return null;
        String volume = id.substring(0, colon);
        String path = id.substring(colon + 1);
        if ("raw".equals(volume)) return path.startsWith("/") ? new File(path) : null;
        File root;
        if ("primary".equals(volume) || "home".equals(volume)) {
            root = Environment.getExternalStorageDirectory();
        } else {
            root = new File("/storage", volume);
        }
        return path.isEmpty() ? root : new File(root, path);
    }

    // the folder's name as the picker shows it, for the progress dialog
    String treeName(Uri tree) {
        try {
            Uri doc = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
            try (Cursor c = activity.getContentResolver().query(doc,
                    new String[] {DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null)) {
                if (c != null && c.moveToFirst() && !c.isNull(0)) return c.getString(0);
            }
        } catch (Exception e) {
            // fall back on the folder's own name below
        }
        File folder = treeFolder(tree);
        return folder != null && folder.getName() != null && !folder.getName().isEmpty()
                ? folder.getName() : "game folder";
    }

    // Copies the game files in a folder the player picked into dir: the extracted game (default.xbe with its data
    // folders) or the disc image. Only the game's files are taken, so a picker left on Downloads does not copy the
    // whole of it. Each file is written as .part and renamed at the end, as the image copy is.
    void copyTree(Uri tree, File dir, Consumer<Boolean> done) {
        String name = treeName(tree);
        ProgressBar bar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(1000);
        bar.setIndeterminate(true);
        TextView text = new TextView(activity);
        text.setText("Looking for the game files…");
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * activity.getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(bar);
        box.addView(text);
        cancelled = false;
        AlertDialog progress = new AlertDialog.Builder(activity)
                .setTitle("Importing " + name)
                .setView(box)
                .setCancelable(false)
                .setNegativeButton("Cancel", (d, w) -> cancelled = true)
                .show();
        progress.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        new Thread(() -> {
            String error = null;
            boolean ok = false;
            try {
                String[] why = new String[1];
                List<Copy> plan = plan(tree, DocumentsContract.getTreeDocumentId(tree), why);
                if (plan == null) {
                    error = why[0];
                } else if (plan.isEmpty()) {
                    error = "The folder has no game files to copy.";
                } else {
                    long total = 0;
                    for (Copy c : plan) if (c.size > 0) total += c.size;
                    // the target may not exist yet on a fresh install, and a missing path reports no free space
                    boolean made = dir.isDirectory() || dir.mkdirs();
                    long free = made ? dir.getUsableSpace() : 0;
                    if (!made) {
                        error = "The game folder could not be made there.";
                    } else if (total > 0 && free > 0 && free < total + (64L << 20)) {
                        error = String.format("The game files need %.1f GB free, there are %.1f GB.",
                                total / 1e9, free / 1e9);
                    } else {
                        long known = total;
                        main.post(() -> {
                            bar.setIndeterminate(known <= 0);
                            if (known > 0) bar.setProgress(0);
                            text.setText(known > 0 ? String.format("0.00 of %.2f GB", known / 1e9) : "0.00 GB");
                        });
                        // every file is copied as .part first and given its name only once the whole folder is in: the
                        // game looks for default.xbe every second, and a half copied folder must not start
                        List<File[]> staged = new ArrayList<>();
                        long copied = 0;
                        for (Copy c : plan) {
                            if (cancelled) break;
                            File out = new File(dir, c.rel);
                            File parent = out.getParentFile();
                            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                                error = "The game folder could not be made there.";
                                break;
                            }
                            File part = new File(parent, out.getName() + ".part");
                            long n = copyDoc(tree, c.id, part, bar, text, copied, known);
                            if (n < 0) {
                                part.delete();
                                if (!cancelled) error = "The copy failed at " + c.rel + ".";
                                break;
                            }
                            staged.add(new File[] {part, out});
                            copied += n;
                            long now = copied;
                            main.post(() -> show(bar, text, now, known));
                        }
                        // all files copied: the names go on now, default.xbe last as the plan lists it
                        ok = error == null && !cancelled;
                        if (ok) {
                            for (File[] s : staged) {
                                if (!s[0].renameTo(s[1]) && !(s[1].delete() && s[0].renameTo(s[1]))) {
                                    error = "The files could not be put in place. Free some space and try again.";
                                    ok = false;
                                    break;
                                }
                            }
                        }
                        if (!ok) for (File[] s : staged) s[0].delete();
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "folder import failed", e);
                error = "The copy failed: " + e.getMessage();
                ok = false;
            }
            String reason = error;
            boolean imported = ok && reason == null;
            main.post(() -> {
                progress.dismiss();
                if (imported) {
                    Log.i(TAG, "imported from " + tree);
                    message("Game imported", "The game files are in place. The game starts in a moment.",
                            () -> done.accept(true));
                } else if (reason != null) {
                    message("Import failed", reason, () -> done.accept(false));
                } else {
                    done.accept(false);  // cancelled
                }
            });
        }, "GameImport").start();
    }

    // the files to copy from the picked folder. null with error filled in when it holds no game at all.
    private List<Copy> plan(Uri tree, String rootId, String[] error) {
        List<Doc> root = children(tree, rootId);
        List<Copy> out = gamePlan(tree, root);
        if (out != null) return out;
        // the picker can be left on the folder above the game's: one subfolder holding the game files is it. more than
        // one is not guessed at, the player is asked to open the game's folder itself. only the first few folders are
        // looked in: a picker left on a folder of hundreds is not the game's, and the player is asked to open it.
        List<Doc> found = new ArrayList<>();
        int checked = 0;
        for (Doc d : root) {
            if (checked >= 32) break;  // a picker left on a folder of hundreds is not the game's
            if (!d.isDir() || d.name == null) continue;
            checked++;
            if (gamePlan(tree, children(tree, d.id)) != null) found.add(d);
        }
        if (found.size() == 1) {
            out = gamePlan(tree, children(tree, found.get(0).id));
            if (out != null) return out;
        }
        error[0] = found.isEmpty()
                ? "That folder has no game files. Pick the folder that holds default.xbe and its folders, or the "
                        + "disc image."
                : "That folder holds more than one game folder. Pick the one with the game files inside.";
        return null;
    }

    // the game folder's contents as copies: default.xbe with everything beside it, the xbe last so the game cannot
    // start on half a copy; or the disc image(s). null when the folder holds neither.
    private List<Copy> gamePlan(Uri tree, List<Doc> contents) {
        Doc xbe = null;
        List<Doc> images = new ArrayList<>();
        for (Doc d : contents) {
            if (d.isDir()) continue;
            if ("default.xbe".equals(d.lower())) xbe = d;
            else if (d.lower().endsWith(".iso")) images.add(d);
        }
        List<Copy> out = new ArrayList<>();
        if (xbe != null) {
            for (Doc d : contents) {
                if (d == xbe || d.name == null) continue;
                if (d.isDir()) collect(tree, d, fileName(d.name), 0, out);
                // the image is not needed beside the extracted files, and copying it again would fill the storage
                else if (d.lower().endsWith(".iso")) continue;
                else out.add(new Copy(d.id, fileName(d.name), d.size));
            }
            out.add(new Copy(xbe.id, fileName(xbe.name), xbe.size));
            return out;
        }
        if (!images.isEmpty()) {
            for (Doc d : images) if (d.name != null) out.add(new Copy(d.id, fileName(d.name), d.size));
            return out;
        }
        return null;
    }

    // every file under dir, its path kept relative to the game folder
    private void collect(Uri tree, Doc dir, String rel, int depth, List<Copy> out) {
        if (depth > 16) return;  // deeper than any game folder: not copied rather than walked forever
        for (Doc child : children(tree, dir.id)) {
            if (child.name == null) continue;
            String childRel = rel + "/" + fileName(child.name);
            if (child.isDir()) collect(tree, child, childRel, depth + 1, out);
            else out.add(new Copy(child.id, childRel, child.size));
        }
    }

    // one file of the picked folder into part; its bytes copied, -1 when it cannot be read
    private long copyDoc(Uri tree, String docId, File part, ProgressBar bar, TextView text, long before, long total) {
        Uri uri = DocumentsContract.buildDocumentUriUsingTree(tree, docId);
        try (ParcelFileDescriptor pfd = activity.getContentResolver().openFileDescriptor(uri, "r")) {
            if (pfd == null) return -1;
            long copied = 0, shown = 0;
            try (FileInputStream in = new FileInputStream(pfd.getFileDescriptor());
                 FileChannel src = in.getChannel();
                 FileOutputStream out = new FileOutputStream(part);
                 FileChannel dst = out.getChannel()) {
                ByteBuffer buf = ByteBuffer.allocateDirect(BUFFER);
                while (!cancelled && src.read(buf) > 0) {
                    buf.flip();
                    while (buf.hasRemaining()) copied += dst.write(buf);
                    buf.clear();
                    if (copied - shown >= (16L << 20)) {
                        shown = copied;
                        long now = before + copied;
                        main.post(() -> show(bar, text, now, total));
                    }
                }
                out.getFD().sync();
            }
            return cancelled ? -1 : copied;
        } catch (Exception e) {
            Log.w(TAG, "copy failed: " + docId, e);
            return -1;
        }
    }

    // the children of a folder in the picked tree; an unreadable folder looks empty
    private List<Doc> children(Uri tree, String parentId) {
        List<Doc> out = new ArrayList<>();
        Uri uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId);
        try (Cursor c = activity.getContentResolver().query(uri, new String[] {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE}, null, null, null)) {
            while (c != null && c.moveToNext()) {
                out.add(new Doc(c.getString(0), c.getString(1), c.getString(2),
                        c.isNull(3) ? -1 : c.getLong(3)));
            }
        } catch (Exception e) {
            Log.w(TAG, "cannot list a folder: " + parentId, e);
        }
        return out;
    }

    // a name the file can be made with: providers can send odd names, and a path walking up out of the game folder
    // would put the game's files somewhere else
    private static String fileName(String name) {
        if (name == null || name.isEmpty()) return "file";
        String out = name.replace('/', '_').replace('\\', '_');
        return ".".equals(out) || "..".equals(out) ? "_" + out : out;
    }

    private static boolean sameFile(File a, File b) {
        try {
            return a.getCanonicalPath().equals(b.getCanonicalPath());
        } catch (IOException e) {
            return a.getAbsolutePath().equals(b.getAbsolutePath());
        }
    }

    private static final class Doc {
        final String id, name, mime;
        final long size;

        Doc(String id, String name, String mime, long size) {
            this.id = id;
            this.name = name;
            this.mime = mime;
            this.size = size;
        }

        boolean isDir() {
            return DocumentsContract.Document.MIME_TYPE_DIR.equals(mime);
        }

        String lower() {
            return name == null ? "" : name.toLowerCase(Locale.ROOT);
        }
    }

    private static final class Copy {
        final String id, rel;
        final long size;

        Copy(String id, String rel, long size) {
            this.id = id;
            this.rel = rel;
            this.size = size;
        }
    }

    private static void show(ProgressBar bar, TextView text, long copied, long size) {
        if (size > 0) {
            bar.setProgress((int) (copied * 1000 / size));
            text.setText(String.format("%.2f of %.2f GB", copied / 1e9, size / 1e9));
        } else {
            text.setText(String.format("%.2f GB", copied / 1e9));
        }
    }

    // null when the file cannot be read out of order (some cloud drives stream it): copied unchecked then, the game
    // tells of a wrong image itself. Positional reads leave the channel at the start for the copy.
    private static Boolean isXboxImage(FileChannel ch) {
        ByteBuffer b = ByteBuffer.allocate(MAGIC.length);
        try {
            for (long part : PARTITIONS) {
                b.clear();
                if (ch.read(b, part + 32 * 2048) == MAGIC.length && Arrays.equals(b.array(), MAGIC)) return true;
            }
            return false;
        } catch (Exception e) {
            Log.i(TAG, "image not checked, no seeking: " + e);
            return null;
        }
    }

    private String displayName(Uri uri) {
        try (Cursor c = activity.getContentResolver().query(uri, new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst() && !c.isNull(0)) return new File(c.getString(0)).getName();
        } catch (Exception e) {
            // fall back on the path
        }
        String last = uri.getLastPathSegment();
        return last == null ? null : new File(last).getName();
    }

    private long size(Uri uri) {
        try (Cursor c = activity.getContentResolver().query(uri, new String[] {OpenableColumns.SIZE}, null, null, null)) {
            if (c != null && c.moveToFirst() && !c.isNull(0)) return c.getLong(0);
        } catch (Exception e) {
            // unknown: the progress has no end then
        }
        return -1;
    }

    private void message(String title, String text, Runnable after) {
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(text)
                .setPositiveButton("OK", null)
                .setOnDismissListener(d -> {
                    if (after != null) after.run();
                })
                .show();
    }
}
