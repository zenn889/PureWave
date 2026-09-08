package com.zenn889.putar;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.getcapacitor.BridgeActivity;

/**
 * putar — music player.
 * Bridge tambahan agar <input type="file" accept="audio/*"> membuka picker
 * file asli Android (WebView bawaan Capacitor tidak menyediakannya).
 */
public class MainActivity extends BridgeActivity {

    private ValueCallback<Uri[]> fileCallback = null;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (bridge == null) return;
        WebView wv = bridge.getWebView();
        if (wv == null) return;
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
}
