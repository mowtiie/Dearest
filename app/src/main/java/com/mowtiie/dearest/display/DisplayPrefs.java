package com.mowtiie.dearest.display;

import android.content.Context;
import android.content.SharedPreferences;

public class DisplayPrefs {

    private static final String PREFS = "dearest_display";

    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_CONTRAST = "contrast";
    private static final String KEY_DYNAMIC_COLOR = "dynamic_color";
    private static final String KEY_PRIVACY_SCREEN = "privacy_screen";
    private static final String KEY_ALLOW_SCREENSHOTS = "allow_screenshots";

    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT  = "light";
    public static final String THEME_DARK   = "dark";

    public static final String CONTRAST_STANDARD = "standard";
    public static final String CONTRAST_MEDIUM   = "medium";
    public static final String CONTRAST_HIGH     = "high";

    private final SharedPreferences prefs;

    public DisplayPrefs(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String getThemeMode() {
        return prefs.getString(KEY_THEME_MODE, THEME_SYSTEM);
    }

    public void setThemeMode(String mode) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply();
    }

    public String getContrast() {
        return prefs.getString(KEY_CONTRAST, CONTRAST_STANDARD);
    }

    public void setContrast(String contrast) {
        prefs.edit().putString(KEY_CONTRAST, contrast).apply();
    }

    public boolean isDynamicColorEnabled() {
        return prefs.getBoolean(KEY_DYNAMIC_COLOR, true);
    }

    public void setDynamicColorEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply();
    }

    public boolean isPrivacyScreenEnabled() {
        return prefs.getBoolean(KEY_PRIVACY_SCREEN, true);
    }

    public void setPrivacyScreenEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_PRIVACY_SCREEN, enabled).apply();
    }

    public boolean isScreenshotsAllowed() {
        return prefs.getBoolean(KEY_ALLOW_SCREENSHOTS, false);
    }

    public void setScreenshotsAllowed(boolean allowed) {
        prefs.edit().putBoolean(KEY_ALLOW_SCREENSHOTS, allowed).apply();
    }
}