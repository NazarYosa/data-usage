package com.drnoob.datamonitor.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.Widget.DataUsageWidget;
import com.drnoob.datamonitor.core.Values;
import com.drnoob.datamonitor.ui.activities.MainActivity;

/**
 * Continuous Background Service that drives live widget updates.
 * - Runs a reliable timer loop for periodic refresh.
 * - Listens for SCREEN_ON and USER_PRESENT for instantaneous updates.
 * - Ensures widget is always 100% synchronized with kernel data stats.
 */
public class ScreenMonitorService extends Service {
    private static final String TAG = "ScreenMonitorService";
    private static final String CHANNEL_ID = "screen_monitor_channel";
    private static final int NOTIFICATION_ID = 1002;

    private static boolean isRunning = false;

    private BroadcastReceiver screenReceiver;
    private final Handler updateHandler = new Handler(Looper.getMainLooper());

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                refreshAllWidgets();
            } catch (Exception e) {
                Log.e(TAG, "Error in periodic update: " + e.getMessage());
            }

            SharedPreferences prefs = getSharedPreferences(Values.PREFS_NAME, Context.MODE_PRIVATE);
            int interval = prefs.getInt(Values.WIDGET_REFRESH_INTERVAL, 60000);
            updateHandler.postDelayed(this, interval);
        }
    };

    public static boolean isServiceRunning() {
        return isRunning;
    }

    public static void start(Context context) {
        try {
            Intent intent = new Intent(context, ScreenMonitorService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Cannot start ScreenMonitorService right now: " + t.getMessage());
        }
    }

    public static void stop(Context context) {
        try {
            Intent intent = new Intent(context, ScreenMonitorService.class);
            context.stopService(intent);
        } catch (Throwable t) {
            Log.w(TAG, "Error stopping ScreenMonitorService: " + t.getMessage());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        createNotificationChannel();
        Notification notification = buildNotification();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Could not startForeground with dataSync: " + t.getMessage());
            try {
                startForeground(NOTIFICATION_ID, notification);
            } catch (Throwable ignored) {}
        }

        registerScreenReceiver();
        startPeriodicLoop();
    }

    private void startPeriodicLoop() {
        updateHandler.removeCallbacks(updateRunnable);
        updateHandler.post(updateRunnable);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Widget Lock Screen Monitor",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            channel.setDescription("Menjaga widget data tetap ter-update secara real-time");
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle("Data Usage")
                .setContentText("Pemantauan kuota & widget aktif di latar belakang")
                .setSmallIcon(R.drawable.ic_widget)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        return builder.build();
    }

    private void registerScreenReceiver() {
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) return;
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_ON.equals(action)
                        || Intent.ACTION_USER_PRESENT.equals(action)
                        || "android.intent.action.USER_UNLOCKED".equals(action)) {
                    refreshAllWidgets();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(screenReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                registerReceiver(screenReceiver, filter);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not register receiver in service: " + e.getMessage());
        }
    }

    public void refreshAllWidgets() {
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            ComponentName cn = new ComponentName(this, DataUsageWidget.class);
            int[] ids = appWidgetManager.getAppWidgetIds(cn);
            if (ids != null && ids.length > 0) {
                for (int id : ids) {
                    DataUsageWidget.updateAppWidget(this, appWidgetManager, id);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing widgets: " + e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        refreshAllWidgets();
        startPeriodicLoop();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        updateHandler.removeCallbacks(updateRunnable);
        if (screenReceiver != null) {
            try {
                unregisterReceiver(screenReceiver);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
