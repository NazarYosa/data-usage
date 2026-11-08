package com.drnoob.datamonitor.utils;

import static com.drnoob.datamonitor.core.Values.DATA_RESET_DATE;
import static com.drnoob.datamonitor.core.Values.PREFS_NAME;
import static com.drnoob.datamonitor.core.Values.SESSION_MONTHLY;
import static com.drnoob.datamonitor.core.Values.SESSION_TODAY;

import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Calendar;
import java.util.Locale;

public class NetworkStatsHelper {
    private static final String TAG = "NetworkStatsHelper";

    public static String getSubscriberId(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            try {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (telephonyManager != null) {
                    return telephonyManager.getSubscriberId();
                }
            } catch (Exception e) {
                Log.w(TAG, "Cannot get subscriberId: " + e.getMessage());
            }
        }
        return null;
    }

    public static Long[] getDeviceWifiDataUsage(Context context, int session) {
        try {
            Long[] period = getTimePeriod(context, session, 1);
            NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
            if (networkStatsManager == null) {
                return new Long[]{0L, 0L, 0L};
            }

            NetworkStats.Bucket bucket = networkStatsManager.querySummaryForDevice(
                    ConnectivityManager.TYPE_WIFI,
                    null,
                    period[0],
                    period[1]);

            if (bucket != null) {
                long rx = bucket.getRxBytes();
                long tx = bucket.getTxBytes();
                return new Long[]{tx, rx, tx + rx};
            }
            return new Long[]{0L, 0L, 0L};
        } catch (Exception e) {
            Log.w(TAG, "Error getting Wifi data usage: " + e.getMessage());
            return new Long[]{0L, 0L, 0L};
        }
    }

    public static Long[] getDeviceMobileDataUsage(Context context, int session, int startDate) {
        try {
            Long[] period = getTimePeriod(context, session, startDate);
            NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
            if (networkStatsManager == null) {
                return new Long[]{0L, 0L, 0L};
            }

            NetworkStats.Bucket bucket = networkStatsManager.querySummaryForDevice(
                    ConnectivityManager.TYPE_MOBILE,
                    getSubscriberId(context),
                    period[0],
                    period[1]);

            if (bucket != null) {
                long rx = bucket.getRxBytes();
                long tx = bucket.getTxBytes();
                return new Long[]{tx, rx, tx + rx};
            }
            return new Long[]{0L, 0L, 0L};
        } catch (Exception e) {
            Log.w(TAG, "Error getting Mobile data usage: " + e.getMessage());
            return new Long[]{0L, 0L, 0L};
        }
    }

    public static String formatBytes(long bytes) {
        if (bytes <= 0) return "0.00 MB";
        float mb = bytes / (1024f * 1024f);
        if (mb >= 1024f) {
            return String.format(Locale.US, "%.2f GB", mb / 1024f);
        } else if (mb < 0.01f) {
            float kb = bytes / 1024f;
            return String.format(Locale.US, "%.1f KB", kb);
        } else {
            return String.format(Locale.US, "%.2f MB", mb);
        }
    }

    public static String[] formatData(Long sent, Long received) {
        if (sent == null) sent = 0L;
        if (received == null) received = 0L;
        long total = sent + received;

        return new String[]{formatBytes(sent), formatBytes(received), formatBytes(total)};
    }

    public static Long[] getTimePeriod(Context context, int session, int startDate) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        long resetTimeMillis;
        long endTimeMillis;

        if (session == SESSION_MONTHLY) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            int monthlyResetDate = prefs.getInt(DATA_RESET_DATE, 1);
            int today = cal.get(Calendar.DAY_OF_MONTH);
            if (monthlyResetDate > today) {
                cal.add(Calendar.MONTH, -1);
            }
            int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            cal.set(Calendar.DAY_OF_MONTH, Math.min(monthlyResetDate, maxDay));
            resetTimeMillis = cal.getTimeInMillis();

            Calendar endCal = (Calendar) cal.clone();
            endCal.add(Calendar.MONTH, 1);
            endTimeMillis = endCal.getTimeInMillis();
        } else {
            // SESSION_TODAY: Start of today 00:00:00 to end of today / tomorrow 00:00:00
            resetTimeMillis = cal.getTimeInMillis();
            Calendar endCal = (Calendar) cal.clone();
            endCal.add(Calendar.DAY_OF_MONTH, 1);
            endTimeMillis = endCal.getTimeInMillis();
        }

        return new Long[]{resetTimeMillis, endTimeMillis};
    }
}
