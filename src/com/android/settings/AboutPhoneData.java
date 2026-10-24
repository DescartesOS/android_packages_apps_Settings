package com.android.settings;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.SystemProperties;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class AboutPhoneData {
    private final Context context;
    private String codename;
    private String soc;
    private String battery;
    private String display;
    private String camera;

    private static final String PROP_CODENAME = "ro.infinity.codename";
    private static final String PROP_SOC = "ro.infinity.soc";
    private static final String PROP_BATTERY = "ro.infinity.battery";
    private static final String PROP_DISPLAY = "ro.infinity.display";
    private static final String PROP_CAMERA = "ro.infinity.camera";

    public AboutPhoneData(Context context) {
        this.context = context.getApplicationContext();
        initializeData();
    }

    private void initializeData() {
        this.codename = getSystemPropertyStrict(PROP_CODENAME);
        this.soc = detectSoC();
        this.battery = detectBattery();
        this.display = detectDisplay();
        this.camera = detectCamera();
    }

    private String getSystemPropertyStrict(String key) {
        String value = SystemProperties.get(key);
        return (value != null && !value.isEmpty()) ? value : "Unknown";
    }

    private String detectSoC() {
        String prop = SystemProperties.get(PROP_SOC);
        if (prop != null && !prop.isEmpty()) return prop;
        prop = SystemProperties.get("ro.soc.model");
        if (prop != null && !prop.isEmpty()) return prop;
        prop = SystemProperties.get("ro.board.platform");
        if (prop != null && !prop.isEmpty()) return prop;
        prop = SystemProperties.get("ro.hardware.chipname");
        if (prop != null && !prop.isEmpty()) return prop;
        prop = SystemProperties.get("ro.vendor.qti.soc_name");
        return (prop != null && !prop.isEmpty()) ? prop : "Unknown";
    }

    private String detectBattery() {
        try {
            com.android.internal.os.PowerProfile powerProfile = 
                new com.android.internal.os.PowerProfile(context);
            double capacity = powerProfile.getBatteryCapacity();
            if (capacity > 1000) {
                return Math.round(capacity) + " mAh";
            }
        } catch (Exception ignored) {}

        String prop = SystemProperties.get(PROP_BATTERY);
        if (prop != null && !prop.isEmpty()) return prop;
        
        return "Unknown";
    }

    private String detectDisplay() {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            Display display = wm.getDefaultDisplay();
            if (display != null) {
            DisplayMetrics metrics = new DisplayMetrics();
            display.getRealMetrics(metrics);
            int width = metrics.widthPixels;
            int height = metrics.heightPixels;
            float maxRefreshRate = getMaxRefreshRate();
            if (width > 0 && height > 0) {
            return width + " x " + height + ", " + Math.round(maxRefreshRate) + " Hz";
            }
          }
        }

        String prop = SystemProperties.get(PROP_DISPLAY);
        if (prop != null && !prop.isEmpty()) return prop;
        
        return "Unknown";
    }

    private float getMaxRefreshRate() {
        DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        if (dm == null) return 60f;
        Display display = dm.getDisplay(Display.DEFAULT_DISPLAY);
        if (display == null) return 60f;
        
        float maxRefreshRate = 60f;
        for (Display.Mode mode : display.getSupportedModes()) {
            if (mode.getRefreshRate() > maxRefreshRate) {
                maxRefreshRate = mode.getRefreshRate();
            }
        }
        return maxRefreshRate;
    }

    private String detectCamera() {
        String prop = SystemProperties.get(PROP_CAMERA);
        if (prop != null && !prop.isEmpty()) return prop;

        return "Multi-Lens Module";
    }

    public String getCodename() {
        return codename;
    }

    public String getSoc() {
        return soc;
    }

    public String getBattery() {
        return battery;
    }

    public String getDisplay() {
        return display;
    }

    public String getCamera() {
        return camera;
    }
}