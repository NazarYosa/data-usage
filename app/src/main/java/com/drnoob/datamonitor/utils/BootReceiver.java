package com.drnoob.datamonitor.utils;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.drnoob.datamonitor.Widget.DataUsageWidget;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed, updating widget...");
            try {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, DataUsageWidget.class));
                if (ids != null && ids.length > 0) {
                    Intent updateIntent = new Intent(context, DataUsageWidget.class);
                    updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                    context.sendBroadcast(updateIntent);

                    android.content.SharedPreferences prefs = context.getSharedPreferences(
                            com.drnoob.datamonitor.core.Values.PREFS_NAME, Context.MODE_PRIVATE);
                    boolean lockScreenRefresh = prefs.getBoolean(
                            com.drnoob.datamonitor.core.Values.LOCK_SCREEN_REFRESH, true);
                    if (lockScreenRefresh) {
                        ScreenMonitorService.start(context);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating widget on boot: " + e.getMessage());
            }
        }
    }
}

