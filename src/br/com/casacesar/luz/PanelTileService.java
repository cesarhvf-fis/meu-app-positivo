package br.com.casacesar.luz;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** Optional direct entry point when the manufacturer's long-press flow dismisses keyguard. */
public final class PanelTileService extends TileService {
    @Override public void onStartListening() {
        super.onStartListening(); Tile tile = getQsTile(); if (tile == null) return;
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_bulb)); tile.setLabel("Painel da luz"); tile.setState(Tile.STATE_INACTIVE);
        if (Build.VERSION.SDK_INT >= 29) tile.setSubtitle("Abrir controles"); tile.updateTile();
    }
    @Override public void onClick() {
        super.onClick();
        Intent intent = new Intent(this, MainActivity.class).putExtra("quickPanel", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (isLocked()) { startActivity(intent); return; }
        if (Build.VERSION.SDK_INT >= 34) startActivityAndCollapse(PendingIntent.getActivity(this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        else startActivityAndCollapse(intent);
    }
}
