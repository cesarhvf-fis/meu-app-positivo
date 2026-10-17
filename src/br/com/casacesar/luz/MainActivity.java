package br.com.casacesar.luz;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.view.WindowManager;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private WebView web;
    private String pendingPhoto;
    private boolean destroyed;
    private boolean backPending;
    private CasaApp app() { return (CasaApp) getApplication(); }
    @Override public void onCreate(Bundle saved) {
        boolean quickPanel = this instanceof PanelActivity
                || android.service.quicksettings.TileService.ACTION_QS_TILE_PREFERENCES.equals(getIntent().getAction())
                || getIntent().getBooleanExtra("quickPanel", false);
        if (quickPanel) setTheme(R.style.Theme_Casa_Panel);
        super.onCreate(saved);
        if (Build.VERSION.SDK_INT >= 33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::handleAppBack);
        if (Build.VERSION.SDK_INT >= 27) setShowWhenLocked(true);
        else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        if (quickPanel) {
            GradientDrawable background = new GradientDrawable(); background.setColor(Color.argb(150, 24, 27, 34)); background.setCornerRadius(dp(24)); getWindow().setBackgroundDrawable(background);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND); getWindow().setDimAmount(0.20f);
            try { if (Build.VERSION.SDK_INT >= 31) getWindow().setBackgroundBlurRadius(dp(24)); } catch (RuntimeException unsupportedBlur) { }
        }
        LinearLayout root = new LinearLayout(this); root.setBackgroundColor(Color.TRANSPARENT);
        if (quickPanel) {
            GradientDrawable outline = new GradientDrawable(); outline.setColor(Color.TRANSPARENT); outline.setCornerRadius(dp(24));
            root.setBackground(outline); root.setClipToOutline(true);
        }
        // Honor Android 15/16 edge-to-edge insets and keep the keyboard from covering inputs.
        root.setOnApplyWindowInsetsListener((v, insets) -> { if (!quickPanel) v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()); return insets; });
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        web = new WebView(this); web.setBackgroundColor(Color.TRANSPARENT); web.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        web.getSettings().setJavaScriptEnabled(true); web.getSettings().setDomStorageEnabled(false); web.getSettings().setAllowFileAccess(false); web.getSettings().setAllowContentAccess(false);
        web.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW); web.getSettings().setSaveFormData(false); web.getSettings().setSupportMultipleWindows(false);
        web.addJavascriptInterface(new Bridge(), "CasaNative");
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return true; }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) { return resource(request.getUrl()); }
        });
        root.addView(web, new LinearLayout.LayoutParams(-1, -1)); setContentView(root);
        if (quickPanel) {
            int width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(24), dp(420));
            int height = Math.min(getResources().getDisplayMetrics().heightPixels - dp(90), dp(720));
            getWindow().setLayout(width, height);
            setFinishOnTouchOutside(true);
        }
        web.loadUrl("https://app.local/index.html" + (quickPanel ? "?panel=1" : ""));
    }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private WebResourceResponse resource(Uri uri) {
        try {
            if (!"https".equals(uri.getScheme()) || !"app.local".equals(uri.getHost())) return blocked();
            String path = uri.getPath(); InputStream in; String mime;
            if (path != null && path.matches("/icons/[a-f0-9]{32}\\.png")) { in = new FileInputStream(new File(new File(getFilesDir(), "icons"), path.substring(7))); mime = "image/png"; }
            else if (path != null && path.matches("/(index\\.html|app\\.js|features\\.js|bridge\\.js|style\\.css|panel-glass\\.css|positivo\\.png)")) { in = getAssets().open(path.substring(1)); mime = path.endsWith(".html") ? "text/html" : path.endsWith(".js") ? "text/javascript" : path.endsWith(".css") ? "text/css" : "image/png"; }
            else return blocked();
            Map<String, String> headers = new HashMap<>(); headers.put("Cache-Control", "no-store"); headers.put("X-Content-Type-Options", "nosniff");
            headers.put("Content-Security-Policy", "default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'none'; frame-src 'none'; object-src 'none'; base-uri 'none'");
            return new WebResourceResponse(mime, "UTF-8", 200, "OK", headers, in);
        } catch (Exception e) { return blocked(); }
    }
    private WebResourceResponse blocked() { return new WebResourceResponse("text/plain", "UTF-8", 403, "Forbidden", new HashMap<>(), new ByteArrayInputStream(new byte[0])); }
    private final class Bridge {
        @JavascriptInterface public void call(String id, String route, String raw) {
            if (id == null || !id.matches("[A-Za-z0-9_-]{1,64}") || raw == null || raw.length() > 16000 || route == null) return;
            if (route.equals("choose-photo")) { runOnUiThread(() -> choosePhoto(id)); return; }
            if (route.equals("add-tile")) { runOnUiThread(() -> addTile(id)); return; }
            if (route.equals("add-panel-tile")) { runOnUiThread(() -> addTile(id, true)); return; }
            app().worker.execute(() -> {
                try { JSONObject input = raw.equals("null") ? new JSONObject() : new JSONObject(raw); Object result = app().dispatch(route, input); reply(id, true, result); if (route.equals("preferred") || route.equals("lamp") || route.equals("toggle") || route.equals("logout")) app().notifyTile(); }
                catch (Exception e) { app().sessionFailure(e); try { reply(id, false, new JSONObject().put("error", CasaApp.message(e))); } catch (Exception ignored) { } }
            });
        }
    }
    private void reply(String id, boolean ok, Object value) {
        final String js = "window.CasaReply(" + JSONObject.quote(id) + "," + ok + "," + String.valueOf(value) + ");";
        runOnUiThread(() -> { if (!destroyed && web != null) web.evaluateJavascript(js, null); });
    }
    private void addTile(String id) {
        addTile(id, false);
    }
    private void addTile(String id, boolean panel) {
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                getSystemService(StatusBarManager.class).requestAddTileService(new ComponentName(this, panel ? PanelTileService.class : LampTileService.class), panel ? "Painel da luz" : "Lâmpada", Icon.createWithResource(this, R.drawable.ic_bulb), getMainExecutor(), result -> {
                    String message = result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ? "Botão adicionado aos Ajustes rápidos." : result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ? "O botão já está adicionado." : "O botão não foi adicionado. Você pode adicioná-lo pelo editor dos Ajustes rápidos.";
                    try { reply(id, true, new JSONObject().put("message", message)); } catch (Exception ignored) { }
                });
            } catch (Exception e) { try { reply(id, false, new JSONObject().put("error", "Abra o editor dos Ajustes rápidos e adicione Lâmpada.")); } catch (Exception ignored) { } }
        } else {
            new AlertDialog.Builder(this).setTitle("Adicionar o botão Lâmpada").setMessage("Abra os Ajustes rápidos, toque em editar e arraste Lâmpada para seus botões ativos.").setPositiveButton("Entendi", null).show();
            try { reply(id, true, new JSONObject().put("message", "Adicione Lâmpada pelo editor dos Ajustes rápidos.")); } catch (Exception ignored) { }
        }
    }
    private void choosePhoto(String id) {
        if (pendingPhoto != null) { try { reply(id, false, new JSONObject().put("error", "Já há uma escolha de foto em andamento.")); } catch (Exception ignored) { } return; }
        pendingPhoto = id;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
        try { startActivityForResult(intent, 20); } catch (Exception e) { pendingPhoto = null; try { reply(id, false, new JSONObject().put("error", "Não foi possível abrir o seletor de fotos.")); } catch (Exception ignored) { } }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data); if (request != 20 || pendingPhoto == null) return;
        String id = pendingPhoto; pendingPhoto = null;
        if (result != RESULT_OK || data == null || data.getData() == null) { try { reply(id, false, new JSONObject().put("error", "Seleção cancelada.")); } catch (Exception ignored) { } return; }
        Uri uri = data.getData();
        app().worker.execute(() -> {
            try {
                BitmapFactory.Options size = new BitmapFactory.Options(); size.inJustDecodeBounds = true;
                try (InputStream in = getContentResolver().openInputStream(uri)) { BitmapFactory.decodeStream(in, null, size); }
                if (size.outWidth <= 0 || size.outHeight <= 0 || size.outWidth > 8192 || size.outHeight > 8192 || (long) size.outWidth * size.outHeight > 20000000L) throw new Exception("Escolha uma imagem menor.");
                BitmapFactory.Options options = new BitmapFactory.Options(); int sample = 1; while (Math.max(size.outWidth, size.outHeight) / (sample * 2) >= 128) sample *= 2; options.inSampleSize = sample;
                Bitmap source; try (InputStream in = getContentResolver().openInputStream(uri)) { source = BitmapFactory.decodeStream(in, null, options); }
                if (source == null) throw new Exception("Formato de imagem não reconhecido."); Bitmap icon = Bitmap.createScaledBitmap(source, 128, 128, true);
                String image = UUID.randomUUID().toString().replace("-", "") + ".png"; File dir = new File(getFilesDir(), "icons"); if (!dir.exists() && !dir.mkdirs()) throw new Exception("Não foi possível guardar a imagem.");
                try (java.io.FileOutputStream out = new java.io.FileOutputStream(new File(dir, image))) { if (!icon.compress(Bitmap.CompressFormat.PNG, 100, out)) throw new Exception("Não foi possível guardar a imagem."); }
                if (source != icon) source.recycle(); icon.recycle(); reply(id, true, new JSONObject().put("image", image));
            } catch (Exception e) { try { reply(id, false, new JSONObject().put("error", CasaApp.message(e))); } catch (Exception ignored) { } }
        });
    }
    private void leaveActivity() {
        boolean panel=this instanceof PanelActivity || getIntent().getBooleanExtra("quickPanel",false)
                || android.service.quicksettings.TileService.ACTION_QS_TILE_PREFERENCES.equals(getIntent().getAction());
        if(panel)finish();else if(!moveTaskToBack(true))finish();
    }
    private void handleAppBack() {
        if(backPending||destroyed)return;
        if(web==null){leaveActivity();return;}
        backPending=true;
        web.evaluateJavascript("window.CasaBack ? window.CasaBack() : false", result->{backPending=false;if(!destroyed&&!"true".equals(result))leaveActivity();});
    }
    @Override public void onBackPressed() { handleAppBack(); }
    @Override protected void onPause() { if (web != null) web.evaluateJavascript("window.CasaPause&&window.CasaPause();", null); super.onPause(); }
    @Override protected void onResume() { super.onResume(); if (web != null) web.evaluateJavascript("window.CasaResume&&window.CasaResume();", null); }
    @Override protected void onDestroy() { destroyed = true; if (web != null) { web.removeJavascriptInterface("CasaNative"); web.stopLoading(); web.destroy(); web = null; } super.onDestroy(); }
}
