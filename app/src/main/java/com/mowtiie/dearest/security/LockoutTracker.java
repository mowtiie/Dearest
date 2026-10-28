package com.mowtiie.dearest.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings;

import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

/**
 * Slows down passphrase guessing on the device. The wait is measured on the monotonic
 * elapsed-time clock, so setting the date back does not shorten it. That clock restarts when the
 * device reboots, so a reboot restarts the full wait rather than trusting the wall clock.
 */
public final class LockoutTracker {

    private static final String PREFS = "dearest_lockout";
    private static final String KEY_FAILURES = "failures";
    private static final String KEY_UNTIL = "locked_until_elapsed";
    private static final String KEY_DELAY = "lock_delay_ms";
    private static final String KEY_BOOT = "boot_count";

    private static final int  FREE_ATTEMPTS = 5;
    private static final long STEP_MS = 30_000L;

    private final SharedPreferences prefs;
    private final LongSupplier elapsedClock;
    private final IntSupplier bootCount;

    public LockoutTracker(Context context) {
        this(context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE),
                SystemClock::elapsedRealtime,
                () -> Settings.Global.getInt(context.getApplicationContext().getContentResolver(),
                        Settings.Global.BOOT_COUNT, -1));
    }

    /** For tests, which drive the clocks by hand. */
    LockoutTracker(SharedPreferences prefs, LongSupplier elapsedClock, IntSupplier bootCount) {
        this.prefs = prefs;
        this.elapsedClock = elapsedClock;
        this.bootCount = bootCount;
    }

    public boolean isLockedOut() {
        return remainingMs() > 0L;
    }

    public long remainingMs() {
        long until = prefs.getLong(KEY_UNTIL, 0L);
        if (until == 0L) return 0L;
        long delay = prefs.getLong(KEY_DELAY, 0L);
        long now = elapsedClock.getAsLong();
        int boot = bootCount.getAsInt();
        if (prefs.getInt(KEY_BOOT, boot) != boot) {
            // The device rebooted, so the stored deadline is on a clock that no longer exists.
            until = now + delay;
            prefs.edit().putLong(KEY_UNTIL, until).putInt(KEY_BOOT, boot).apply();
        }
        // Never longer than the delay that was set, whatever the stored deadline says.
        return Math.max(0L, Math.min(until - now, delay));
    }

    public int failureCount() {
        return prefs.getInt(KEY_FAILURES, 0);
    }

    public long recordFailure() {
        int failures = failureCount() + 1;
        long delay = 0L;
        if (failures > FREE_ATTEMPTS) {
            delay = STEP_MS * (failures - FREE_ATTEMPTS);
        }
        SharedPreferences.Editor edit = prefs.edit().putInt(KEY_FAILURES, failures);
        if (delay > 0L) {
            edit.putLong(KEY_UNTIL, elapsedClock.getAsLong() + delay)
                    .putLong(KEY_DELAY, delay)
                    .putInt(KEY_BOOT, bootCount.getAsInt());
        } else {
            edit.remove(KEY_UNTIL).remove(KEY_DELAY).remove(KEY_BOOT);
        }
        edit.apply();
        return delay;
    }

    public void reset() {
        prefs.edit().remove(KEY_FAILURES).remove(KEY_UNTIL).remove(KEY_DELAY).remove(KEY_BOOT).apply();
    }
}
