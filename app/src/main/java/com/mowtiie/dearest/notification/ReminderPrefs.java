package com.mowtiie.dearest.notification;

import android.content.Context;
import android.content.SharedPreferences;

public class ReminderPrefs {

    private static final String PREFS = "dearest_reminder";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_HOUR = "hour";
    private static final String KEY_MINUTE = "minute";
    private static final String KEY_PROMPTED_EXACT_ALARM = "prompted_exact_alarm";

    private static final int DEFAULT_HOUR   = 21;
    private static final int DEFAULT_MINUTE = 0;

    private final SharedPreferences prefs;

    public ReminderPrefs(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() {
        return prefs.getBoolean(KEY_ENABLED, false);
    }

    public void setEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public int getHour() {
        return prefs.getInt(KEY_HOUR, DEFAULT_HOUR);
    }

    public int getMinute() {
        return prefs.getInt(KEY_MINUTE, DEFAULT_MINUTE);
    }

    public void setTime(int hour, int minute) {
        prefs.edit().putInt(KEY_HOUR, hour).putInt(KEY_MINUTE, minute).apply();
    }

    public boolean hasPromptedExactAlarm() {
        return prefs.getBoolean(KEY_PROMPTED_EXACT_ALARM, false);
    }

    public void setPromptedExactAlarm(boolean prompted) {
        prefs.edit().putBoolean(KEY_PROMPTED_EXACT_ALARM, prompted).apply();
    }
}