package br.com.casacesar.luz;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;

public final class LampTileService extends TileService {
    private static final AtomicBoolean commandRunning = new AtomicBoolean(false);
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean listening;
    private CasaApp app() { return (CasaApp) getApplication(); }
    @Override public void onStartListening() {
        super.onStartListening(); listening = true; update(true);
        app().worker.execute(() -> { try { app().readPreferred(); } catch (Exception e) { app().sessionFailure(e); } ui.post(() -> update(false)); });
    }
    @Override public void onStopListening() { listening = false; super.onStopListening(); }
    @Override public void onClick() {
        super.onClick();
        toggleLight();
    }
    private void toggleLight() {
        if (!app().snapshot().optBoolean("loggedIn") || app().preferred().isEmpty()) { openPanel(); return; }
        if (!commandRunning.compareAndSet(false, true)) return;
        update(false);
        app().worker.execute(() -> {
            String error = null;
            try { app().togglePreferred(); } catch (Exception e) { app().sessionFailure(e); error = CasaApp.message(e); }
            finally { commandRunning.set(false); }
            final String text = error;
            ui.post(() -> { update(false); if (text != null) Toast.makeText(this, text, Toast.LENGTH_LONG).show(); });
            app().notifyTile();
        });
    }
    private void openPanel() {
        Intent intent = new Intent(this, MainActivity.class).putExtra("quickPanel", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (isLocked()) { startActivity(intent); return; }
        if (Build.VERSION.SDK_INT >= 34) startActivityAndCollapse(PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        else startActivityAndCollapse(intent);
    }
    private void update(boolean checking) {
        if (!listening) return;
        try {
            Tile tile = getQsTile(); if (tile == null) return;
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_bulb)); tile.setLabel("Lâmpada");
            JSONObject snapshot = app().snapshot(), d = CloudBackend.find(snapshot.optJSONArray("devices") == null ? new org.json.JSONArray() : snapshot.optJSONArray("devices"), app().preferred());
            boolean loggedIn = snapshot.optBoolean("loggedIn"), on = false;
            String subtitle = !loggedIn ? "Entrar na conta" : d == null ? "Escolher lâmpada" : d.optString("name", "Lâmpada");
            if (d != null) {
                JSONObject c = CloudBackend.control(d, "switch_led|switch|switch_1");
                on = c != null && d.getJSONObject("dps").optBoolean(c.getString("id"));
                if (d.opt("online") == Boolean.FALSE) subtitle = "Offline";
                else if (checking) subtitle = "Verificando…";
                else subtitle = on ? "Ligada" : "Desligada";
            }
            if (commandRunning.get()) subtitle = "Aplicando…";
            tile.setState(commandRunning.get() ? Tile.STATE_UNAVAILABLE : on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            if (Build.VERSION.SDK_INT >= 29) tile.setSubtitle(subtitle);
            if (Build.VERSION.SDK_INT >= 30) tile.setStateDescription(subtitle);
            tile.setContentDescription("Lâmpada. " + subtitle + ". Segure para abrir os controles."); tile.updateTile();
        } catch (Exception ignored) { }
    }
}
