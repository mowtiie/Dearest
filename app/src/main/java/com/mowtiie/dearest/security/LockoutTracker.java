package com.mowtiie.dearest.security;

import android.content.Context;
import android.content.SharedPreferences;

public final class LockoutTracker {

    private static final String PREFS = "dearest_lockout";
    private static final String KEY_FAILURES = "failures";
    private static final String KEY_UNTIL = "locked_until";

    private static final int  FREE_ATTEMPTS = 5;
    private static final long STEP_MS = 30_000L;

    private final SharedPreferences prefs;

    public LockoutTracker(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isLockedOut() {
        return remainingMs() > 0L;
    }

    public long remainingMs() {
        long until = prefs.getLong(KEY_UNTIL, 0L);
        return Math.max(0L, until - System.currentTimeMillis());
    }

    public int failureCount() {
        return prefs.getInt(KEY_FAILURES, 0);
    }

    public long recordFailure() {
        int failures = failureCount() + 1;
        long lockUntil = 0L;
        if (failures > FREE_ATTEMPTS) {
            long delay = STEP_MS * (failures - FREE_ATTEMPTS);
            lockUntil = System.currentTimeMillis() + delay;
        }
        prefs.edit()
                .putInt(KEY_FAILURES, failures)
                .putLong(KEY_UNTIL, lockUntil)
                .apply();
        return Math.max(0L, lockUntil - System.currentTimeMillis());
    }

    public void reset() {
        prefs.edit().remove(KEY_FAILURES).remove(KEY_UNTIL).apply();
    }
}
