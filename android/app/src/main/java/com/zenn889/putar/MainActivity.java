package com.zenn889.putar;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.getcapacitor.BridgeActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * putar — music player.
 * 1. Bridge file picker asli Android utk <input type="file" accept="audio/*">.
 * 2. Bridge "PutarNative.scan()" — pindai seluruh audio MediaStore lalu kirim
 *    hasilnya ke window.__putarMusicScan(json). Pemutaran lewat MusicServer
 *    lokal (http://127.0.0.1:port/s?id=..&m=..).
 */
public class MainActivity extends BridgeActivity {

    private ValueCallback<Uri[]> fileCallback = null;
    private final AtomicBoolean scanning = new AtomicBoolean(false);

    private final ActivityResultLauncher<Intent> pickAudio =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            ValueCallback<Uri[]> cb = fileCallback;
            fileCallback = null;
            if (cb == null) return;
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri[] uris;
                ClipData clip = result.getData().getClipData();
                if (clip != null) {
                    uris = new Uri[clip.getItemCount()];
                    for (int i = 0; i < clip.getItemCount(); i++) {
                        uris[i] = clip.getItemAt(i).getUri();
                    }
                } else {
                    uris = new Uri[] { result.getData().getData() };
                }
                cb.onReceiveValue(uris);
            } else {
                cb.onReceiveValue(null);
            }
        });

    private final ActivityResultLauncher<String> permissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                runScanAsync();
            } else {
                scanning.set(false);
                notifyJs("{ok:false,error:\"permission\"}");
            }
        });

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MusicServer.get(this);
        if (bridge == null) return;
        WebView wv = bridge.getWebView();
        if (wv == null) return;
        wv.addJavascriptInterface(this, "PutarNative");
        android.webkit.WebSettings ws = wv.getSettings();
        ws.setSupportZoom(false);
        ws.setBuiltInZoomControls(false);
        ws.setDisplayZoomControls(false);
        ws.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }
                fileCallback = filePathCallback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                pickAudio.launch(intent);
                return true;
            }
        });
    }

    /* ============ dipanggil JS: window.PutarNative.scan() ============ */
    @JavascriptInterface
    public void scan() {
        if (!scanning.compareAndSet(false, true)) {
            notifyJs("{ok:false,error:\"busy\"}");
            return;
        }
        String perm = Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_AUDIO
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            runOnUiThread(() -> permissionLauncher.launch(perm));
        } else {
            runScanAsync();
        }
    }

    /** Port server streaming lokal — dipakai JS utk membangun ulang URL lagu setelah restart. */
    @JavascriptInterface
    public int port() {
        return MusicServer.get(this).getPort();
    }

    /** Cek cepat apakah izin akses audio sudah diberikan (tanpa memunculkan dialog). */
    @JavascriptInterface
    public boolean hasPerm() {
        String perm = Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_AUDIO
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        return ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED;
    }

    /** Bisa dijalankan dari thread mana pun; query MediaStore di thread ini (bukan UI). */
    private void runScanAsync() {
        new Thread(() -> {
            try {
                JSONArray songs = new JSONArray();
                Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                String[] projection = {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.MIME_TYPE
                };
                String selection = MediaStore.Audio.Media.DURATION + " > 3000"; // buang bunyi < 3 dtk
                String order = MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC";
                boolean wantHiRes = Build.VERSION.SDK_INT >= 29;
                String[] proj = wantHiRes
                        ? new String[] {
                            MediaStore.Audio.Media._ID,
                            MediaStore.Audio.Media.TITLE,
                            MediaStore.Audio.Media.ARTIST,
                            MediaStore.Audio.Media.DURATION,
                            MediaStore.Audio.Media.MIME_TYPE,
                            "sample_rate"
                        }
                        : projection;
                try {
                    queryAndEmit(collection, proj, selection, order, songs);
                } catch (IllegalArgumentException ex) {
                    // kolom sample_rate ternyata tak tersedia di perangkat ini — query tanpa kolom itu
                    if (wantHiRes) {
                        songs = new JSONArray();
                        queryAndEmit(collection, projection, selection, order, songs);
                    } else {
                        throw ex;
                    }
                }
                JSONObject out = new JSONObject();
                out.put("ok", true);
                out.put("port", MusicServer.get(this).getPort());
                out.put("songs", songs);
                notifyJs(out.toString());
            } catch (Exception e) {
                notifyJs("{ok:false,error:\"scan\"}");
            } finally {
                scanning.set(false);
            }
        }, "putar-scan").start();
    }

    private void queryAndEmit(Uri collection, String[] projection, String selection, String order,
                              JSONArray songs) throws Exception {
        try (Cursor c = getContentResolver().query(collection, projection, selection, null, order)) {
            if (c == null) return;
            int iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int iMime = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE);
            int iSr = c.getColumnIndex("sample_rate");
            while (c.moveToNext()) {
                JSONObject o = new JSONObject();
                o.put("i", c.getLong(iId));
                o.put("t", c.getString(iTitle) != null ? c.getString(iTitle) : "Tanpa judul");
                String artist = c.getString(iArtist);
                o.put("a", artist == null || artist.isEmpty() || "<unknown>".equals(artist)
                        ? "" : artist);
                o.put("d", c.getLong(iDur));
                String mime = c.getString(iMime);
                o.put("m", mime != null && !mime.isEmpty() ? mime : "audio/mpeg");
                if (iSr >= 0) {
                    long sr = c.getLong(iSr);
                    if (sr > 48000) o.put("sr", sr); // flag hi-res saja (>48kHz)
                }
                songs.put(o);
            }
        }
    }

    /** Kirim hasil ke JS (harus dari UI thread). */
    private void notifyJs(String json) {
        runOnUiThread(() -> {
            WebView wv = bridge != null ? bridge.getWebView() : null;
            if (wv != null) {
                wv.evaluateJavascript(
                    "window.__putarMusicScan && window.__putarMusicScan(" + json + ")", null);
            }
        });
    }
}
