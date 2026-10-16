package hk.kyo.healthykyofai;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.webkit.WebViewAssetLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Hosts the Healthy KyoFai web app (packaged from /www) in a WebView.
 * The page is served from https://appassets.androidplatform.net so it gets a stable,
 * secure origin: records in localStorage persist and the Anthropic API accepts the requests.
 */
public class MainActivity extends Activity {

    private static final String HOST = "appassets.androidplatform.net";
    private static final String HOME = "https://" + HOST + "/assets/index.html";
    private static final int REQ_FILE = 1001;

    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private Uri cameraUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web = new WebView(this);
        web.setBackgroundColor(0xFF15120F);
        setContentView(web);

        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setDatabaseEnabled(true);
        ws.setAllowFileAccess(false);
        ws.setAllowContentAccess(true);
        ws.setTextZoom(100);

        web.addJavascriptInterface(new Bridge(), "AndroidBridge");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if (HOST.equals(u.getHost())) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                } catch (ActivityNotFoundException ignored) {
                }
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                return openChooser(callback, params);
            }
        });

        web.loadUrl(HOME);
    }

    /* ---------- photo / file picking for <input type="file"> ---------- */

    private boolean openChooser(ValueCallback<Uri[]> callback, WebChromeClient.FileChooserParams params) {
        if (fileCallback != null) fileCallback.onReceiveValue(null);
        fileCallback = callback;
        cameraUri = null;

        boolean wantsImage = false;
        String[] types = params.getAcceptTypes();
        if (types != null) {
            for (String t : types) {
                if (t != null && t.startsWith("image")) wantsImage = true;
            }
        }

        Intent camera = null;
        if (wantsImage) {
            try {
                File dir = new File(getCacheDir(), "camera");
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
                File photo = File.createTempFile("meal_", ".jpg", dir);
                cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".files", photo);
                camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                camera.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
                camera.setClipData(ClipData.newRawUri("", cameraUri));
                camera.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (IOException e) {
                camera = null;
                cameraUri = null;
            }
        }

        Intent target;
        if (params.isCaptureEnabled() && camera != null) {
            target = camera;
        } else {
            Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
            pick.addCategory(Intent.CATEGORY_OPENABLE);
            pick.setType(wantsImage ? "image/*" : "*/*");
            target = Intent.createChooser(pick, wantsImage ? "揀相" : "揀檔案");
            if (camera != null) target.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{camera});
        }

        try {
            startActivityForResult(target, REQ_FILE);
            return true;
        } catch (ActivityNotFoundException e) {
            fileCallback = null;
            Toast.makeText(this, "搵唔到可以揀相嘅 app", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQ_FILE || fileCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        Uri[] result = null;
        if (resultCode == RESULT_OK) {
            Uri picked = data != null ? data.getData() : null;
            if (picked == null && data != null && data.getClipData() != null && data.getClipData().getItemCount() > 0) {
                Uri first = data.getClipData().getItemAt(0).getUri();
                if (first != null && !first.equals(cameraUri)) picked = first;
            }
            if (picked != null) {
                result = new Uri[]{picked};
            } else if (cameraUri != null) {
                result = new Uri[]{cameraUri};
            }
        }
        fileCallback.onReceiveValue(result);
        fileCallback = null;
    }

    /* ---------- saving Excel / backup files ---------- */

    private static String safeName(String name) {
        String n = name == null ? "" : name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return n.isEmpty() ? "healthykyofai.dat" : n;
    }

    private void share(byte[] bytes, String name, String mime) throws IOException {
        File dir = new File(getCacheDir(), "exports");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        File f = new File(dir, name);
        try (FileOutputStream os = new FileOutputStream(f)) {
            os.write(bytes);
        }
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", f);
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType(mime);
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.setClipData(ClipData.newRawUri(name, uri));
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        final Intent chooser = Intent.createChooser(send, "分享 " + name);
        runOnUiThread(() -> startActivity(chooser));
    }

    private class Bridge {
        /** Saves to Downloads/Healthy KyoFai (Android 10+); older phones get the share sheet. */
        @JavascriptInterface
        public String saveFile(String base64, String filename, String mime) {
            try {
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                final String name = safeName(filename);
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                    cv.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                    cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Healthy KyoFai");
                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                    if (uri == null) return "error:insert";
                    try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                        if (os == null) return "error:open";
                        os.write(bytes);
                    }
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "已儲存到 下載/Healthy KyoFai/" + name, Toast.LENGTH_LONG).show());
                    return "saved";
                }
                share(bytes, name, mime);
                return "shared";
            } catch (Exception e) {
                return "error:" + e.getMessage();
            }
        }

        @JavascriptInterface
        public String shareFile(String base64, String filename, String mime) {
            try {
                share(Base64.decode(base64, Base64.DEFAULT), safeName(filename), mime);
                return "shared";
            } catch (Exception e) {
                return "error:" + e.getMessage();
            }
        }
    }

    /* ---------- lifecycle ---------- */

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("(window.__skjBack && window.__skjBack()) ? '1' : '0'", value -> {
            if (!"\"1\"".equals(value)) finish();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @Override
    protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
