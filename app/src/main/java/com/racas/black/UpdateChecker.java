package com.racas.black;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONObject;

// Asks GitHub for the newest release of this app, at most every few hours and off the main thread, and hands it
// over when it is newer than the one installed and the player has not skipped it.
final class UpdateChecker {
    static final String REPO = "RanggaCasper/Black-Android";
    private static final String LATEST = "https://api.github.com/repos/" + REPO + "/releases/latest";
    private static final String TAG = "BlackUpdate";
    private static final long CHECK_EVERY_MS = 12L * 60 * 60 * 1000;
    private static final int TIMEOUT_MS = 8000;

    private static final String PREFS = "updates";
    private static final String LAST_CHECK = "last_check";
    private static final String SKIPPED = "skipped_tag";

    static final class Release {
        String tag;      // v1.2.3
        String name;     // Black for Android v1.2.3
        String page;     // the release on GitHub
        String apk;      // the APK's download, or null
        String changes;  // the "What's Changed" list as plain text, or ""
    }

    interface Listener {
        void onUpdate(Release r);
    }

    private UpdateChecker() {}

    static void check(Context context, Listener listener) {
        Context app = context.getApplicationContext();
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        if (now - prefs.getLong(LAST_CHECK, 0) < CHECK_EVERY_MS) return;
        String installed = installedVersion(app);
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                Release r = fetch();
                prefs.edit().putLong(LAST_CHECK, now).apply();
                if (r == null || r.tag.equals(prefs.getString(SKIPPED, null))) return;
                if (compare(r.tag, installed) <= 0) return;
                Log.i(TAG, "update " + r.tag + " over " + installed);
                main.post(() -> listener.onUpdate(r));
            } catch (Exception e) {
                Log.w(TAG, "no update check: " + e);  // offline, rate limited: try again next launch
            }
        }, "UpdateCheck").start();
    }

    static void skip(Context context, String tag) {
        context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(SKIPPED, tag).apply();
    }

    private static String installedVersion(Context app) {
        try {
            return app.getPackageManager().getPackageInfo(app.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "0";
        }
    }

    private static Release fetch() throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(LATEST).openConnection();
        c.setConnectTimeout(TIMEOUT_MS);
        c.setReadTimeout(TIMEOUT_MS);
        c.setRequestProperty("Accept", "application/vnd.github+json");
        c.setRequestProperty("User-Agent", "Black-Android");
        try {
            if (c.getResponseCode() != 200) return null;  // 404 when there is no release yet
            JSONObject j = new JSONObject(read(c.getInputStream()));
            Release r = new Release();
            r.tag = j.getString("tag_name");
            r.name = j.optString("name", r.tag);
            if (r.name.isEmpty()) r.name = r.tag;
            r.page = j.optString("html_url", "https://github.com/" + REPO + "/releases/latest");
            JSONArray assets = j.optJSONArray("assets");
            for (int i = 0; assets != null && i < assets.length(); i++) {
                JSONObject a = assets.getJSONObject(i);
                if (a.optString("name").toLowerCase().endsWith(".apk")) {
                    r.apk = a.optString("browser_download_url", null);
                    break;
                }
            }
            r.changes = changes(j.optString("body", ""));
            return r;
        } finally {
            c.disconnect();
        }
    }

    private static String read(InputStream in) throws Exception {
        try (in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            for (int n; (n = in.read(buf)) > 0;) out.write(buf, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    // GitHub's generated "What's Changed" list, without the authors' links: "* fix: x by @a in https://…" → "• fix: x"
    private static String changes(String body) {
        int at = body.indexOf("## What's Changed");
        if (at < 0) return "";
        StringBuilder sb = new StringBuilder();
        int lines = 0;
        for (String line : body.substring(at).split("\r?\n")) {
            line = line.trim();
            if (line.startsWith("**Full Changelog**") || (line.startsWith("## ") && sb.length() > 0)) break;
            if (!line.startsWith("* ") && !line.startsWith("- ")) continue;
            line = line.substring(2).replaceAll(" by @\\S+ in https://\\S+$", "").replaceAll(" in https://\\S+$", "");
            if (++lines > 10) {
                sb.append("• …\n");
                break;
            }
            sb.append("• ").append(line).append('\n');
        }
        return sb.toString().trim();
    }

    // dotted numbers, "v" and anything after "-" left out: v1.2.10 > 1.2.9, 1.0 == 1.0.0
    static int compare(String a, String b) {
        int[] x = parts(a), y = parts(b);
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length ? x[i] : 0, q = i < y.length ? y[i] : 0;
            if (p != q) return Integer.compare(p, q);
        }
        return 0;
    }

    private static int[] parts(String v) {
        if (v == null) return new int[0];
        v = v.trim();
        if (v.startsWith("v") || v.startsWith("V")) v = v.substring(1);
        int dash = v.indexOf('-');
        if (dash >= 0) v = v.substring(0, dash);
        String[] s = v.split("\\.");
        int[] n = new int[s.length];
        for (int i = 0; i < s.length; i++) {
            try {
                n[i] = Integer.parseInt(s[i].replaceAll("\\D.*", ""));
            } catch (NumberFormatException e) {
                n[i] = 0;
            }
        }
        return n;
    }
}
