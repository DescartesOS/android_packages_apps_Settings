package com.android.settings;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Size;
import android.view.Display;

import java.util.ArrayList;
import java.util.List;

public class AboutPhoneData {
    private final Context context;
    private final String soc;
    private final int batteryCapacity;
    private final String battery;
    private final String display;
    private final String frontCameras;
    private final String rearCameras;

    private static final String PROP_SOC = "ro.descartes.soc";
    private static final String PROP_BATTERY = "ro.descartes.battery";
    private static final String PROP_DISPLAY = "ro.descartes.display";
    private static final String PROP_CAMERA_FRONT = "ro.descartes.camera.front";
    private static final String PROP_CAMERA_REAR = "ro.descartes.camera.rear";

    public AboutPhoneData(Context context) {
        this.context = context.getApplicationContext();
        this.soc = detectSoC();
        this.batteryCapacity = detectBatteryCapacity();
        this.battery = SystemProperties.get(PROP_BATTERY);
        this.display = detectDisplay();
        String front = SystemProperties.get(PROP_CAMERA_FRONT);
        String rear = SystemProperties.get(PROP_CAMERA_REAR);
        if (TextUtils.isEmpty(front) && TextUtils.isEmpty(rear)) {
            front = detectCameras(CameraCharacteristics.LENS_FACING_FRONT);
            rear = detectCameras(CameraCharacteristics.LENS_FACING_BACK);
        }
        this.frontCameras = front;
        this.rearCameras = rear;
    }

    private static String firstProp(String... keys) {
        for (String key : keys) {
            String value = SystemProperties.get(key);
            if (!TextUtils.isEmpty(value)) return value;
        }
        return null;
    }

    private String detectSoC() {
        String prop = SystemProperties.get(PROP_SOC);
        if (!TextUtils.isEmpty(prop)) return prop;
        String manufacturer = firstProp("ro.soc.manufacturer");
        String model = firstProp("ro.soc.model", "ro.board.platform",
                "ro.hardware.chipname", "ro.vendor.qti.soc_name");
        if (model == null) return null;
        return manufacturer != null ? manufacturer + " " + model : model;
    }

    private int detectBatteryCapacity() {
        try {
            double capacity = new com.android.internal.os.PowerProfile(context)
                    .getBatteryCapacity();
            if (capacity > 1000) return (int) Math.round(capacity);
        } catch (Exception ignored) {
        }
        return 0;
    }

    private String detectDisplay() {
        String prop = SystemProperties.get(PROP_DISPLAY);
        if (!TextUtils.isEmpty(prop)) return prop;
        DisplayManager dm = context.getSystemService(DisplayManager.class);
        Display display = dm != null ? dm.getDisplay(Display.DEFAULT_DISPLAY) : null;
        if (display == null) return null;
        int width = 0, height = 0;
        float refreshRate = 0f;
        for (Display.Mode mode : display.getSupportedModes()) {
            if (mode.getPhysicalWidth() * mode.getPhysicalHeight() > width * height) {
                width = mode.getPhysicalWidth();
                height = mode.getPhysicalHeight();
            }
            refreshRate = Math.max(refreshRate, mode.getRefreshRate());
        }
        if (width <= 0 || height <= 0) return null;
        return Math.min(width, height) + " x " + Math.max(width, height)
                + ", " + Math.round(refreshRate) + " Hz";
    }

    private String detectCameras(int facing) {
        CameraManager cm = context.getSystemService(CameraManager.class);
        if (cm == null) return null;
        List<String> megapixels = new ArrayList<>();
        try {
            for (String id : cm.getCameraIdList()) {
                CameraCharacteristics chars = cm.getCameraCharacteristics(id);
                Integer lensFacing = chars.get(CameraCharacteristics.LENS_FACING);
                Size size = chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
                if (lensFacing == null || lensFacing != facing || size == null) continue;
                long mp = Math.round(size.getWidth() * (double) size.getHeight() / 1_000_000d);
                if (mp > 0) megapixels.add(mp + " MP");
            }
        } catch (Exception ignored) {
        }
        return megapixels.isEmpty() ? null : TextUtils.join(" + ", megapixels);
    }

    public String getSoc() {
        return soc;
    }

    public int getBatteryCapacity() {
        return batteryCapacity;
    }

    public String getBattery() {
        return battery;
    }

    public String getDisplay() {
        return display;
    }

    public String getFrontCameras() {
        return frontCameras;
    }

    public String getRearCameras() {
        return rearCameras;
    }
}
