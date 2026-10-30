package com.android.settings;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.widget.WavyProgressView;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider;
import com.android.settingslib.widget.LayoutPreference;

import java.io.File;
import java.util.Locale;
import java.util.Scanner;

public class OosAboutPreference extends BasePreferenceController
        implements View.OnTouchListener, LifecycleObserver, OnStart, OnStop {

    private static final String TAG = "OosAboutPreference";
    private static final String KEY_DEVICE_NAME = "device_name";

    private final Context context;
    private final AboutPhoneData phoneData;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isLongPressDetected = false;
    private PreferenceScreen screen;
    private TextView deviceName;

    private final ContentObserver deviceNameObserver = new ContentObserver(handler) {
        @Override
        public void onChange(boolean selfChange) {
            updateDeviceName();
        }
    };

    public OosAboutPreference(Context context, String key) {
        super(context, key);
        this.context = context;
        this.phoneData = new AboutPhoneData(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        this.screen = screen;
        LayoutPreference preference = screen.findPreference(getPreferenceKey());
        if (preference != null) {
            onBindItems(preference.findViewById(R.id.oos_about_root));
        }
        Preference deviceNamePref = screen.findPreference(KEY_DEVICE_NAME);
        if (deviceNamePref != null) {
            deviceNamePref.setVisible(false);
        }
    }

    @Override
    public void onStart() {
        context.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.DEVICE_NAME), false, deviceNameObserver);
        updateDeviceName();
    }

    @Override
    public void onStop() {
        context.getContentResolver().unregisterContentObserver(deviceNameObserver);
    }

    private String orUnknown(String value) {
        return TextUtils.isEmpty(value) ? context.getString(R.string.device_info_default) : value;
    }

    private void onBindItems(View holder) {
        TextView osVersion = holder.findViewById(R.id.about_os_version);
        TextView model = holder.findViewById(R.id.device_model);
        TextView buildNumber = holder.findViewById(R.id.build_number);
        TextView soc = holder.findViewById(R.id.soc_about);
        TextView ram = holder.findViewById(R.id.ram_about);
        TextView battery = holder.findViewById(R.id.battery_about);
        TextView display = holder.findViewById(R.id.display_about);
        TextView storage = holder.findViewById(R.id.rom_about);
        WavyProgressView storageBar = holder.findViewById(R.id.rom_progress_bar);
        ImageView androidVersion = holder.findViewById(R.id.android_version);
        deviceName = holder.findViewById(R.id.device_name);
        applySettingsIconShape(holder);
        applyCompactAboutLayout(holder);

        osVersion.setText(getVersionLine());
        model.setText(getModelName());
        buildNumber.setText(Build.DISPLAY);
        soc.setText(orUnknown(phoneData.getSoc()));
        display.setText(orUnknown(phoneData.getDisplay()));
        if (!TextUtils.isEmpty(phoneData.getBattery())) {
            battery.setText(phoneData.getBattery());
        } else if (phoneData.getBatteryCapacity() > 0) {
            battery.setText(context.getString(R.string.descartes_about_battery_typ,
                    phoneData.getBatteryCapacity()));
        } else {
            battery.setText(orUnknown(null));
        }
        bindCameras(holder);
        updateDeviceName();

        holder.findViewById(R.id.about_update_pill).setOnClickListener(
                v -> startSafely(new Intent(Settings.ACTION_SYSTEM_UPDATE_SETTINGS)));
        holder.findViewById(R.id.about_storage_row).setOnClickListener(
                v -> startSafely(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)));
        holder.findViewById(R.id.about_device_name_row).setOnClickListener(v -> {
            Preference pref = screen != null ? screen.findPreference(KEY_DEVICE_NAME) : null;
            if (pref != null) {
                pref.performClick();
            }
        });

        new Thread(() -> {
            StorageManager sm = context.getSystemService(StorageManager.class);
            PrivateStorageInfo info = PrivateStorageInfo.getPrivateStorageInfo(
                    new StorageManagerVolumeProvider(sm));
            long used = info.totalBytes - info.freeBytes;
            int usedPercent = info.totalBytes > 0 ? (int) (used * 100 / info.totalBytes) : 0;
            int ramGb = getTotalRamGb();
            holder.post(() -> {
                storage.setText(String.format(Locale.getDefault(), "%s / %s",
                        Formatter.formatShortFileSize(context, used),
                        Formatter.formatShortFileSize(context, info.totalBytes)));
                animateProgressBar(storageBar, usedPercent);
                ram.setText(ramGb > 0
                        ? String.format(Locale.getDefault(), "%.2f GB", (float) ramGb)
                        : orUnknown(null));
            });
        }).start();

        final Intent platLogo = new Intent(Intent.ACTION_MAIN).setClassName(
                "android", com.android.internal.app.PlatLogoActivity.class.getName());
        androidVersion.setOnTouchListener(this);
        androidVersion.setOnClickListener(v -> startSafely(
                new Intent(context, com.android.settings.Settings.FirmwareVersionActivity.class)));
        androidVersion.setOnLongClickListener(v -> startSafely(platLogo));
    }

    private void applyCompactAboutLayout(View holder) {
        if (context.getResources().getConfiguration().screenWidthDp > 380) return;
        reflowSpecRow(holder, R.id.about_memory_battery_row);
        reflowSpecRow(holder, R.id.about_display_camera_row);
    }

    private void reflowSpecRow(View holder, int rowId) {
        android.widget.LinearLayout row = holder.findViewById(rowId);
        if (row == null) return;
        row.setOrientation(android.widget.LinearLayout.VERTICAL);
        int gap = (int) (8 * context.getResources().getDisplayMetrics().density + 0.5f);
        for (int i = 0; i < row.getChildCount(); i++) {
            View child = row.getChildAt(i);
            android.widget.LinearLayout.LayoutParams params =
                    (android.widget.LinearLayout.LayoutParams) child.getLayoutParams();
            if (child instanceof android.widget.Space) {
                params.width = android.widget.LinearLayout.LayoutParams.MATCH_PARENT;
                params.height = gap;
            } else {
                params.width = android.widget.LinearLayout.LayoutParams.MATCH_PARENT;
                params.height = android.widget.LinearLayout.LayoutParams.WRAP_CONTENT;
                params.weight = 0;
                int background;
                if (child.getId() == R.id.display_section &&
                        holder.findViewById(R.id.camera_section).getVisibility() == View.GONE) {
                    background = R.drawable.descartes_about_seg_bottom;
                } else if (child.getId() == R.id.display_section ||
                        child.getId() == R.id.camera_section) {
                    background = child.getId() == R.id.display_section
                            ? R.drawable.descartes_about_seg_top
                            : R.drawable.descartes_about_seg_bottom;
                } else {
                    background = child == row.getChildAt(0)
                            ? R.drawable.descartes_about_seg_top
                            : R.drawable.descartes_about_seg_bottom;
                }
                child.setBackgroundResource(background);
            }
            child.setLayoutParams(params);
        }
    }

    private void applySettingsIconShape(View holder) {
        int shape = Settings.System.getIntForUser(context.getContentResolver(),
                "settings_icon_corner_style", 2, UserHandle.USER_CURRENT);
        if (shape != 0) return;
        int[] shapeIds = {R.id.soc_shape, R.id.ram_shape, R.id.battery_shape,
                R.id.display_shape, R.id.camera_shape};
        for (int id : shapeIds) {
            ImageView image = holder.findViewById(id);
            if (image != null) image.setImageResource(R.drawable.circle);
        }
    }

    private String getVersionLine() {
        String version = SystemProperties.get("ro.descartes.version",
                SystemProperties.get("ro.crdroid.build.version"));
        String maintainer = AboutPhoneData.getProp("ro.descartes.maintainer");
        String tail = !TextUtils.isEmpty(maintainer) ? maintainer
                : SystemProperties.get("ro.descartes.buildtype", "UNOFFICIAL");
        return TextUtils.isEmpty(version) ? tail : version + " | " + tail;
    }

    private static String getModelName() {
        String marketName = SystemProperties.get("ro.product.marketname");
        return !TextUtils.isEmpty(marketName) ? marketName : Build.MODEL;
    }

    private void updateDeviceName() {
        if (deviceName == null) return;
        String name = Settings.Global.getString(context.getContentResolver(),
                Settings.Global.DEVICE_NAME);
        deviceName.setText(TextUtils.isEmpty(name) ? getModelName() : name);
    }

    private void bindCameras(View holder) {
        String front = phoneData.getFrontCameras();
        String rear = phoneData.getRearCameras();
        if (TextUtils.isEmpty(front) && TextUtils.isEmpty(rear)) {
            holder.findViewById(R.id.camera_section).setVisibility(View.GONE);
            holder.findViewById(R.id.camera_gap).setVisibility(View.GONE);
            holder.findViewById(R.id.display_section).setBackgroundResource(
                    R.drawable.descartes_about_seg_bottom);
            return;
        }
        TextView main = holder.findViewById(R.id.camera_rear);
        TextView detail = holder.findViewById(R.id.camera_front);
        if (TextUtils.isEmpty(rear)) {
            main.setText(context.getString(R.string.descartes_about_camera_front, front));
            detail.setVisibility(View.GONE);
            return;
        }
        main.setText(context.getString(R.string.descartes_about_camera_rear, rear));
        if (TextUtils.isEmpty(front)) {
            detail.setVisibility(View.GONE);
        } else {
            detail.setText(context.getString(R.string.descartes_about_camera_front, front));
        }
    }

    private boolean startSafely(Intent intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Unable to start " + intent, e);
            return false;
        }
    }

    private static int getTotalRamGb() {
        try (Scanner scanner = new Scanner(new File("/proc/meminfo"))) {
            while (scanner.hasNextLine()) {
                String[] vals = scanner.nextLine().split(":");
                if (vals.length > 1 && vals[0].trim().equals("MemTotal")) {
                    long kb = Long.parseLong(vals[1].trim().split("\\s+")[0]);
                    return (int) Math.ceil(kb / 1_000_000d);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading meminfo", e);
        }
        return 0;
    }

    private void animateProgressBar(WavyProgressView progressBar, int progress) {
        ObjectAnimator animator = ObjectAnimator.ofInt(progressBar, "progress", 0, progress);
        animator.setDuration(1000);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }

    @Override
    public boolean onTouch(View v, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isLongPressDetected = false;
                v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(200).start();
                handler.postDelayed(() -> {
                    isLongPressDetected = true;
                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    v.performLongClick();
                }, ViewConfiguration.getLongPressTimeout());
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isLongPressDetected) {
                    handler.removeCallbacksAndMessages(null);
                    isLongPressDetected = false;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacksAndMessages(null);
                ObjectAnimator scaleXUp = ObjectAnimator.ofFloat(v, "scaleX", 1.25f);
                ObjectAnimator scaleYUp = ObjectAnimator.ofFloat(v, "scaleY", 1.25f);
                scaleXUp.setDuration(200);
                scaleYUp.setDuration(200);
                scaleXUp.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        ObjectAnimator scaleXDown = ObjectAnimator.ofFloat(v, "scaleX", 1f);
                        ObjectAnimator scaleYDown = ObjectAnimator.ofFloat(v, "scaleY", 1f);
                        scaleXDown.setDuration(200);
                        scaleYDown.setDuration(200);
                        AnimatorSet bounceBack = new AnimatorSet();
                        bounceBack.play(scaleXDown).with(scaleYDown);
                        bounceBack.start();
                    }
                });
                AnimatorSet bounce = new AnimatorSet();
                bounce.play(scaleXUp).with(scaleYUp);
                bounce.start();
                if (!isLongPressDetected) {
                    v.performClick();
                }
                return true;
        }
        return false;
    }
}
