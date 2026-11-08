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
        if (intent == null) return;
        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            Log.d(TAG, "Device started or package replaced (" + action + "), updating widget...");
            try {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                ComponentName cn = new ComponentName(context, DataUsageWidget.class);
                int[] ids = appWidgetManager.getAppWidgetIds(cn);

                if (ids != null && ids.length > 0) {
                    DataUsageWidget widget = new DataUsageWidget();
                    widget.onUpdate(context, appWidgetManager, ids);
                }
                DataUsageWidget.setRepeating(context);
            } catch (Exception e) {
                Log.e(TAG, "Error updating widget on boot/replace: " + e.getMessage());
            }
        }
    }
}
