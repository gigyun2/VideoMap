package com.example.videomaps;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * Implementation of App Widget functionality.
 */
public class RecordWidget extends AppWidgetProvider {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent.getAction().equals("Start Recording")) {

            Intent actRecord = new Intent();
//            actRecord.putExtra("latitude", lat);
//            actRecord.putExtra("longitude", lng);
//            actRecord.setClass(MainActivity.this, RecordActivity.class);
//            startActivity(actRecord);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        //retrieve a ref to the manager so we can pass a view update

        Intent i = new Intent();
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.setClassName("com.example.videomaps", "com.example.videomaps.RecordActivity");
        PendingIntent myPI = PendingIntent.getService(context, 0, i, 0);
        //intent to start service

        // Get the layout for the App Widget
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.record_widget);

        //attach the click listener for the service start command intent
        views.setOnClickPendingIntent(R.id.record_widget, myPI);

        //define the componenet for self
        ComponentName comp = new ComponentName(context.getPackageName(), RecordWidget.class.getName());

        //tell the manager to update all instances of the toggle widget with the click listener
        mgr.updateAppWidget(comp, views);

        // There may be multiple widgets active, so update all of them
//        for (int appWidgetId : appWidgetIds) {
//            //updateAppWidget(context, appWidgetManager, appWidgetId);
//        }
//        Intent intent = new Intent(context, RecordWidget.class);
//        intent.setAction("Start Recording");
//        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
//        // Get the layout for the App Widget and attach an on-click listener to the button
//        RemoteViews views = new RemoteViews(context.getPackageName(),R.layout.record_widget);
//        views.setOnClickPendingIntent(R.id.record_widget, pendingIntent);
    }

    @Override
    public void onEnabled(Context context) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        //retrieve a ref to the manager so we can pass a view update

        Intent i = new Intent();
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.setClassName("com.example.videomaps", "com.example.videomaps.RecordActivity");
        PendingIntent myPI = PendingIntent.getService(context, 0, i, 0);
        //intent to start service

        // Get the layout for the App Widget
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.record_widget);

        //attach the click listener for the service start command intent
        views.setOnClickPendingIntent(R.id.record_widget, myPI);

        //define the componenet for self
        ComponentName comp = new ComponentName(context.getPackageName(), RecordWidget.class.getName());

        //tell the manager to update all instances of the toggle widget with the click listener
        mgr.updateAppWidget(comp, views);
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

//    @Override
//    public void onClickPendingIntent() {
//
//    }
}

