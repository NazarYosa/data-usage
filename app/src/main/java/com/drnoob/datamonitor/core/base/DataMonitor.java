package com.drnoob.datamonitor.core.base;

import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.drnoob.datamonitor.Widget.DataUsageWidget;
import com.drnoob.datamonitor.core.Values;
import com.drnoob.datamonitor.utils.ScreenMonitorService;

public class DataMonitor extends Application {
    private static final String TAG = "DataMonitorApp";

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_ON.equals(action)
                    || Intent.ACTION_USER_PRESENT.equals(action)
                    || "android.intent.action.USER_UNLOCKED".equals(action)) {
                try {
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                    int[] ids = appWidgetManager.getAppWidgetIds(
                            new ComponentName(context, DataUsageWidget.class));
                    if (ids != null && ids.length > 0) {
                        DataUsageWidget widget = new DataUsageWidget();
                        widget.onUpdate(context, appWidgetManager, ids);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error refreshing widget on screen event: " + e.getMessage());
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        registerScreenListener();

        // Check if lock screen service should be running
        SharedPreferences prefs = getSharedPreferences(Values.PREFS_NAME, Context.MODE_PRIVATE);
        boolean lockScreenRefresh = prefs.getBoolean(Values.LOCK_SCREEN_REFRESH, true);
        if (lockScreenRefresh) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(this, DataUsageWidget.class));
            if (ids != null && ids.length > 0) {
                ScreenMonitorService.start(this);
            }
        }
    }

    private void registerScreenListener() {
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            filter.setPriority(100);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(screenReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                registerReceiver(screenReceiver, filter);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not register screen receiver in app: " + e.getMessage());
        }
    }
}
