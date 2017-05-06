package com.example.videomaps;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.ImageButton;
import android.widget.RemoteViews;

/**
 * Implementation of App Widget functionality.
 */
public class RecordWidget extends AppWidgetProvider {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent.getAction().equals("RecordStart")) {

            Intent intent_ = new Intent(context, RecordWidget.class);
            ComponentName cn = new ComponentName("com.example.videomaps", "com.example.videomaps.RecordActivity");
            intent_.setComponent(cn);
            intent_.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent_);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        final Intent intent = new Intent(context, RecordWidget.class);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        intent.setAction("RecordStart");

        //PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.record_widget);
        ComponentName watchWidget = new ComponentName(context, RecordWidget.class);
        views.setOnClickPendingIntent(R.id.record_widget, getPendingSelfIntent(context, "RecordStart"));
        appWidgetManager.updateAppWidget(watchWidget, views);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the last widget is enabled
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }


    protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }
}

