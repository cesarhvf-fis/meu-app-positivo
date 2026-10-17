package br.com.casacesar.luz;

import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;

public final class LampWidgetProvider extends AppWidgetProvider {
    private static final String REFRESH = "br.com.casacesar.luz.REFRESH_WIDGET";
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        renderAll(context); WidgetRefreshJob.schedule(context);
    }
    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (REFRESH.equals(intent.getAction())) { WidgetRefreshJob.schedule(context); }
    }
    static void renderAll(Context context) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, LampWidgetProvider.class)); if (ids.length == 0) return;
            CasaApp app = (CasaApp) context.getApplicationContext(); WidgetState state = WidgetState.from(app.snapshot(), app.preferred());
            for (int id : ids) {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_lamp);
                views.setTextViewText(R.id.widget_status, state.status);
                views.setImageViewResource(R.id.widget_icon, state.lit ? R.drawable.widget_bulb_on : R.drawable.widget_bulb_off);
                Intent panel = new Intent(context, MainActivity.class).putExtra("quickPanel", true).setData(Uri.parse("casa-lampada://widget/panel/" + id)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ActivityOptions options = ActivityOptions.makeBasic();
                // Explicit, immutable delegation for a user tap on this widget, not an automatic launch.
                if (Build.VERSION.SDK_INT >= 36) options.setPendingIntentCreatorBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS);
                else if (Build.VERSION.SDK_INT >= 35) options.setPendingIntentCreatorBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
                PendingIntent open = PendingIntent.getActivity(context, id, panel, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE, options.toBundle());
                views.setOnClickPendingIntent(R.id.widget_root, open);
                Intent refresh = new Intent(context, LampWidgetProvider.class).setAction(REFRESH).setData(Uri.parse("casa-lampada://widget/refresh/" + id)).addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                views.setOnClickPendingIntent(R.id.widget_refresh, PendingIntent.getBroadcast(context, id, refresh, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
                manager.updateAppWidget(id, views);
            }
        } catch (RuntimeException unsupportedHost) { }
    }
}
