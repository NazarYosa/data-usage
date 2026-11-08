package com.drnoob.datamonitor.Widget;

import static com.drnoob.datamonitor.core.Values.DATA_LIMIT;
import static com.drnoob.datamonitor.core.Values.DATA_RESET;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DAILY;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DATE;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_MONTHLY;
import static com.drnoob.datamonitor.core.Values.DATA_TYPE;
import static com.drnoob.datamonitor.core.Values.PREFS_NAME;
import static com.drnoob.datamonitor.core.Values.REMAINING_DATA_INFO;
import static com.drnoob.datamonitor.core.Values.SESSION_MONTHLY;
import static com.drnoob.datamonitor.core.Values.SESSION_TODAY;
import static com.drnoob.datamonitor.core.Values.WIDGET_REFRESH_INTERVAL;
import static com.drnoob.datamonitor.core.Values.WIDGET_SHOW_WIFI;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.formatBytes;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.formatData;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceMobileDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceWifiDataUsage;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.drnoob.datamonitor.Common;
import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.ui.activities.MainActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DataUsageWidget extends AppWidgetProvider {
    private static final String TAG = "DataUsageWidget";
    public static final String ACTION_MANUAL_REFRESH = "com.drnoob.datamonitor.ACTION_MANUAL_REFRESH";

    private static boolean isReceiverRunning = false;
    private boolean manualRefresh = false;

    public boolean isManualRefresh() {
        return manualRefresh;
    }

    public void setManualRefresh(boolean manualRefresh) {
        this.manualRefresh = manualRefresh;
    }

    public static String[] splitValueAndUnit(String formatted) {
        if (formatted == null || formatted.isEmpty()) return new String[]{"0.0", "MB"};
        int space = formatted.indexOf(' ');
        if (space == -1) return new String[]{formatted, ""};
        return new String[]{formatted.substring(0, space), formatted.substring(space + 1)};
    }

    public void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Long[] mobile = new Long[]{0L, 0L, 0L};
        Long[] wifi = new Long[]{0L, 0L, 0L};
        String mobileData = "0.0 MB";
        String wifiData = "0.0 MB";

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int date = prefs.getInt(DATA_RESET_DATE, 1);
        String resetCycle = prefs.getString(DATA_RESET, DATA_RESET_DAILY);

        // Update timestamp: jam, menit, detik (HH:mm:ss)
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String currentTime = timeFormat.format(new Date());

        boolean usageGranted = Common.isUsageAccessGranted(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.data_usage_widget);

        // Set Last Update Time
        views.setTextViewText(R.id.widget_last_update, currentTime);

        if (!usageGranted) {
            views.setTextViewText(R.id.widget_mobile_data_used, "Izin");
            views.setTextViewText(R.id.widget_mobile_data_unit, "butuh");
            views.setTextViewText(R.id.widget_wifi_used, "Izin");
            views.setTextViewText(R.id.widget_wifi_unit, "butuh");
            views.setTextViewText(R.id.widget_data_usage_remaining, "Ketuk untuk buka pengaturan & beri izin");
            views.setViewVisibility(R.id.widget_data_usage_remaining, View.VISIBLE);
        } else {
            try {
                if (DATA_RESET_MONTHLY.equals(resetCycle)) {
                    mobile = getDeviceMobileDataUsage(context, SESSION_MONTHLY, date);
                } else {
                    mobile = getDeviceMobileDataUsage(context, SESSION_TODAY, 1);
                }

                mobileData = formatData(mobile[0], mobile[1])[2];

                wifi = getDeviceWifiDataUsage(context, SESSION_TODAY);
                wifiData = formatData(wifi[0], wifi[1])[2];
            } catch (Exception e) {
                Log.e(TAG, "Error fetching usage data for widget: " + e.getMessage());
            }

            String[] mobileParts = splitValueAndUnit(mobileData);
            views.setTextViewText(R.id.widget_mobile_data_used, mobileParts[0]);
            views.setTextViewText(R.id.widget_mobile_data_unit, mobileParts[1]);

            String[] wifiParts = splitValueAndUnit(wifiData);
            views.setTextViewText(R.id.widget_wifi_used, wifiParts[0]);
            views.setTextViewText(R.id.widget_wifi_unit, wifiParts[1]);
        }

        if (isManualRefresh()) {
            views.setViewVisibility(R.id.widget_update, View.INVISIBLE);
            views.setViewVisibility(R.id.widget_update_progress, View.VISIBLE);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

        boolean showRemaining = prefs.getBoolean(REMAINING_DATA_INFO, true);
        boolean showWifiUsage = prefs.getBoolean(WIDGET_SHOW_WIFI, true);
        float dataLimit = prefs.getFloat(DATA_LIMIT, -1f);
        int dataType = prefs.getInt(DATA_TYPE, 1); // 0 = MB, 1 = GB

        if (!showWifiUsage) {
            views.setViewVisibility(R.id.layout_wifi, View.GONE);
            views.setViewVisibility(R.id.divider_widget, View.GONE);
        } else {
            views.setViewVisibility(R.id.layout_wifi, View.VISIBLE);
            views.setViewVisibility(R.id.divider_widget, View.VISIBLE);
        }

        if (usageGranted) {
            if (showRemaining && dataLimit > 0) {
                long limitBytes;
                if (dataType == 0) {
                    limitBytes = (long) (dataLimit * 1024L * 1024L);
                } else {
                    limitBytes = (long) (dataLimit * 1024L * 1024L * 1024L);
                }

                long totalUsedBytes = mobile[2];
                if (limitBytes >= totalUsedBytes) {
                    long remaining = limitBytes - totalUsedBytes;
                    String remainingData = formatBytes(remaining);
                    views.setTextViewText(R.id.widget_data_usage_remaining,
                            context.getString(R.string.label_data_remaining, remainingData));
                } else {
                    long excess = totalUsedBytes - limitBytes;
                    String excessData = formatBytes(excess);
                    views.setTextViewText(R.id.widget_data_usage_remaining,
                            context.getString(R.string.label_data_remaining_used_excess, excessData));
                }
                views.setViewVisibility(R.id.widget_data_usage_remaining, View.VISIBLE);
            } else {
                views.setTextViewText(R.id.widget_data_usage_remaining, "");
                views.setViewVisibility(R.id.widget_data_usage_remaining, View.GONE);
            }
        }

        // Setup manual refresh pending intent
        Intent refreshIntent = new Intent(context, DataUsageWidget.class);
        refreshIntent.setAction(ACTION_MANUAL_REFRESH);
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_update, refreshPendingIntent);

        // Click on root opens MainActivity (Settings)
        Intent appIntent = new Intent(context, MainActivity.class);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent appPendingIntent = PendingIntent.getActivity(
                context, 0, appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, appPendingIntent);
        views.setOnClickPendingIntent(R.id.layout_data, appPendingIntent);
        views.setOnClickPendingIntent(R.id.layout_wifi, appPendingIntent);

        if (isManualRefresh()) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                views.setViewVisibility(R.id.widget_update, View.VISIBLE);
                views.setViewVisibility(R.id.widget_update_progress, View.GONE);
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }, 750);
        } else {
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();

        if (ACTION_MANUAL_REFRESH.equals(action)) {
            setManualRefresh(true);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, DataUsageWidget.class));
            if (ids != null && ids.length > 0) {
                onUpdate(context, appWidgetManager, ids);
            }
            return;
        }

        setManualRefresh(false);
        super.onReceive(context, intent);

        if (Intent.ACTION_USER_PRESENT.equals(action)
                || Intent.ACTION_SCREEN_ON.equals(action)
                || Intent.ACTION_USER_UNLOCKED.equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, DataUsageWidget.class));
            if (ids != null && ids.length > 0) {
                onUpdate(context, appWidgetManager, ids);
            }
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            try {
                updateAppWidget(context, appWidgetManager, appWidgetId);
            } catch (Exception e) {
                Log.e(TAG, "Error in onUpdate: " + e.getMessage());
            }
        }

        if (!isReceiverRunning) {
            startReceiver(context);
            isReceiverRunning = true;
        }

        setRepeating(context);
    }

    private static void setRepeating(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, DataUsageWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(new ComponentName(context, DataUsageWidget.class));
        if (ids == null || ids.length == 0) return;
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        int elapsedTime = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(WIDGET_REFRESH_INTERVAL, 60000);

        long triggerAtMillis = System.currentTimeMillis() + elapsedTime;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not schedule alarm: " + e.getMessage());
        }
    }

    private void startReceiver(Context context) {
        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_SCREEN_ON);
            intentFilter.addAction(Intent.ACTION_USER_PRESENT);
            intentFilter.setPriority(100);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.getApplicationContext().registerReceiver(
                        new DataUsageWidget(),
                        intentFilter,
                        Context.RECEIVER_EXPORTED);
            } else {
                context.getApplicationContext().registerReceiver(new DataUsageWidget(), intentFilter);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not register receiver: " + e.getMessage());
        }
    }
}
