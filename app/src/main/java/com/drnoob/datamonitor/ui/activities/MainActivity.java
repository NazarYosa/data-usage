package com.drnoob.datamonitor.ui.activities;

import static com.drnoob.datamonitor.core.Values.DATA_LIMIT;
import static com.drnoob.datamonitor.core.Values.DATA_RESET;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DAILY;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DATE;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_MONTHLY;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_NONE;
import static com.drnoob.datamonitor.core.Values.DATA_TYPE;
import static com.drnoob.datamonitor.core.Values.LOCK_SCREEN_REFRESH;
import static com.drnoob.datamonitor.core.Values.PREFS_NAME;
import static com.drnoob.datamonitor.core.Values.REMAINING_DATA_INFO;
import static com.drnoob.datamonitor.core.Values.REQUEST_READ_PHONE_STATE;
import static com.drnoob.datamonitor.core.Values.WIDGET_REFRESH_INTERVAL;
import static com.drnoob.datamonitor.core.Values.WIDGET_SHOW_WIFI;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.drnoob.datamonitor.Common;
import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.Widget.DataUsageWidget;
import com.drnoob.datamonitor.core.DeveloperInfo;
import com.drnoob.datamonitor.utils.ScreenMonitorService;

public class MainActivity extends Activity {

    private SharedPreferences prefs;

    private RadioGroup rgPlanCycle;
    private RadioButton rbDaily, rbMonthly, rbNone;
    private View layoutResetDate, layoutLimitContainer;
    private EditText etResetDate, etDataLimit;
    private Spinner spDataUnit, spRefreshInterval;
    private Switch switchShowRemaining, switchShowWifi, switchLockScreenRefresh;
    private LinearLayout cardPermissions;
    private Button btnGrantUsage, btnGrantPhone, btnGrantBattery, btnSave, btnRefreshNow, btnGuide;
    private TextView tvDeveloperCredit;

    private final int[] INTERVAL_VALUES = new int[]{60000, 300000, 900000, 1800000, 3600000};
    private final String[] INTERVAL_LABELS = new String[]{"1 min", "5 min", "15 min", "30 min", "1 hour"};
    private final String[] UNIT_LABELS = new String[]{"GB", "MB"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        initViews();
        setupSpinners();
        setupListeners();
        loadSettings();
        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
        triggerWidgetUpdate();
    }

    private void initViews() {
        rgPlanCycle = findViewById(R.id.rg_plan_cycle);
        rbDaily = findViewById(R.id.rb_daily);
        rbMonthly = findViewById(R.id.rb_monthly);
        rbNone = findViewById(R.id.rb_none);

        layoutResetDate = findViewById(R.id.layout_reset_date);
        layoutLimitContainer = findViewById(R.id.layout_limit_container);
        etResetDate = findViewById(R.id.et_reset_date);
        etDataLimit = findViewById(R.id.et_data_limit);

        spDataUnit = findViewById(R.id.sp_data_unit);
        spRefreshInterval = findViewById(R.id.sp_refresh_interval);

        switchShowRemaining = findViewById(R.id.switch_show_remaining);
        switchShowWifi = findViewById(R.id.switch_show_wifi);
        switchLockScreenRefresh = findViewById(R.id.switch_lock_screen_refresh);

        cardPermissions = findViewById(R.id.card_permissions);
        btnGrantUsage = findViewById(R.id.btn_grant_usage);
        btnGrantPhone = findViewById(R.id.btn_grant_phone);
        btnGrantBattery = findViewById(R.id.btn_grant_battery);

        btnSave = findViewById(R.id.btn_save);
        btnRefreshNow = findViewById(R.id.btn_refresh_now);
        btnGuide = findViewById(R.id.btn_guide);

        tvDeveloperCredit = findViewById(R.id.tv_developer_credit);
        if (tvDeveloperCredit != null) {
            String devName = DeveloperInfo.getDeveloperName();
            tvDeveloperCredit.setText(getString(R.string.developer_credit) != null
                    ? "Developed by " + devName : "Developed by " + devName);
        }
    }

    private void setupSpinners() {
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, UNIT_LABELS);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDataUnit.setAdapter(unitAdapter);

        ArrayAdapter<String> intervalAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, INTERVAL_LABELS);
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRefreshInterval.setAdapter(intervalAdapter);
    }

    private void setupListeners() {
        rgPlanCycle.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_monthly) {
                layoutResetDate.setVisibility(View.VISIBLE);
                layoutLimitContainer.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.rb_daily) {
                layoutResetDate.setVisibility(View.GONE);
                layoutLimitContainer.setVisibility(View.VISIBLE);
            } else {
                layoutResetDate.setVisibility(View.GONE);
                layoutLimitContainer.setVisibility(View.GONE);
            }
        });

        btnGrantUsage.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Cannot open Usage Access settings", Toast.LENGTH_SHORT).show();
            }
        });

        btnGrantPhone.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE},
                        REQUEST_READ_PHONE_STATE);
            }
        });

        btnGrantBattery.setOnClickListener(v -> {
            requestBatteryOptimizationExemption();
        });

        btnSave.setOnClickListener(v -> saveSettings());

        btnRefreshNow.setOnClickListener(v -> {
            triggerWidgetUpdate();
            Toast.makeText(this, R.string.label_widget_refreshed, Toast.LENGTH_SHORT).show();
        });

        btnGuide.setOnClickListener(v -> showWidgetGuide());
    }

    private void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    try {
                        Intent fallbackIntent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        startActivity(fallbackIntent);
                    } catch (Exception ex) {
                        Toast.makeText(this, "Cannot open battery settings", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(this, "Optimasi baterai sudah dinonaktifkan (Tanpa Batasan)", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadSettings() {
        String resetCycle = prefs.getString(DATA_RESET, DATA_RESET_DAILY);
        if (DATA_RESET_MONTHLY.equals(resetCycle)) {
            rbMonthly.setChecked(true);
            layoutResetDate.setVisibility(View.VISIBLE);
            layoutLimitContainer.setVisibility(View.VISIBLE);
        } else if (DATA_RESET_NONE.equals(resetCycle)) {
            rbNone.setChecked(true);
            layoutResetDate.setVisibility(View.GONE);
            layoutLimitContainer.setVisibility(View.GONE);
        } else {
            rbDaily.setChecked(true);
            layoutResetDate.setVisibility(View.GONE);
            layoutLimitContainer.setVisibility(View.VISIBLE);
        }

        int resetDate = prefs.getInt(DATA_RESET_DATE, 1);
        etResetDate.setText(String.valueOf(resetDate));

        float dataLimit = prefs.getFloat(DATA_LIMIT, -1f);
        int dataType = prefs.getInt(DATA_TYPE, 1); // 0 = MB, 1 = GB
        if (dataLimit > 0) {
            etDataLimit.setText(String.valueOf(dataLimit));
        } else {
            etDataLimit.setText("");
        }
        spDataUnit.setSelection(dataType == 0 ? 1 : 0);

        int interval = prefs.getInt(WIDGET_REFRESH_INTERVAL, 60000);
        int selectedIndex = 0;
        for (int i = 0; i < INTERVAL_VALUES.length; i++) {
            if (INTERVAL_VALUES[i] == interval) {
                selectedIndex = i;
                break;
            }
        }
        spRefreshInterval.setSelection(selectedIndex);

        switchShowRemaining.setChecked(prefs.getBoolean(REMAINING_DATA_INFO, true));
        switchShowWifi.setChecked(prefs.getBoolean(WIDGET_SHOW_WIFI, true));
        switchLockScreenRefresh.setChecked(prefs.getBoolean(LOCK_SCREEN_REFRESH, true));
    }

    private void saveSettings() {
        String cycle;
        if (rbMonthly.isChecked()) {
            cycle = DATA_RESET_MONTHLY;
            String dateStr = etResetDate.getText().toString().trim();
            if (!dateStr.isEmpty()) {
                try {
                    int d = Integer.parseInt(dateStr);
                    if (d < 1 || d > 31) {
                        Toast.makeText(this, R.string.msg_invalid_date, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    prefs.edit().putInt(DATA_RESET_DATE, d).apply();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, R.string.msg_invalid_date, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        } else if (rbNone.isChecked()) {
            cycle = DATA_RESET_NONE;
        } else {
            cycle = DATA_RESET_DAILY;
        }

        float dataLimit = -1f;
        String limitStr = etDataLimit.getText().toString().trim();
        if (!limitStr.isEmpty() && !rbNone.isChecked()) {
            try {
                dataLimit = Float.parseFloat(limitStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.msg_invalid_limit, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        int dataType = spDataUnit.getSelectedItemPosition() == 1 ? 0 : 1; // 0 = MB, 1 = GB
        int intervalMillis = INTERVAL_VALUES[spRefreshInterval.getSelectedItemPosition()];
        boolean lockRefresh = switchLockScreenRefresh.isChecked();

        prefs.edit()
                .putString(DATA_RESET, cycle)
                .putFloat(DATA_LIMIT, dataLimit)
                .putInt(DATA_TYPE, dataType)
                .putBoolean(REMAINING_DATA_INFO, switchShowRemaining.isChecked())
                .putBoolean(WIDGET_SHOW_WIFI, switchShowWifi.isChecked())
                .putBoolean(LOCK_SCREEN_REFRESH, lockRefresh)
                .putInt(WIDGET_REFRESH_INTERVAL, intervalMillis)
                .apply();

        if (lockRefresh) {
            ScreenMonitorService.start(this);
        } else {
            ScreenMonitorService.stop(this);
        }

        triggerWidgetUpdate();

        Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
    }

    private void triggerWidgetUpdate() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(this, DataUsageWidget.class));
        if (ids != null && ids.length > 0) {
            Intent updateIntent = new Intent(this, DataUsageWidget.class);
            updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            sendBroadcast(updateIntent);
        }
    }

    private void showWidgetGuide() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.layout_widget_setup, null);
        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void checkPermissions() {
        boolean usageGranted = Common.isUsageAccessGranted(this);
        boolean phoneGranted = Common.isReadPhoneStateGranted(this);

        boolean batteryExempt = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                batteryExempt = powerManager.isIgnoringBatteryOptimizations(getPackageName());
            }
        }

        if (usageGranted && phoneGranted && batteryExempt) {
            cardPermissions.setVisibility(View.GONE);
        } else {
            cardPermissions.setVisibility(View.VISIBLE);
            btnGrantUsage.setVisibility(usageGranted ? View.GONE : View.VISIBLE);
            btnGrantPhone.setVisibility(phoneGranted ? View.GONE : View.VISIBLE);
            btnGrantBattery.setVisibility(batteryExempt ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_PHONE_STATE) {
            checkPermissions();
            triggerWidgetUpdate();
        }
    }
}
