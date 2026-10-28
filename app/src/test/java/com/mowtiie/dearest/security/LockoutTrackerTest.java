package com.mowtiie.dearest.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class LockoutTrackerTest {

    private LockoutTracker tracker;

    @Before
    public void setUp() {
        tracker = new LockoutTracker(ApplicationProvider.getApplicationContext());
        tracker.reset();
    }

    @Test
    public void firstFiveFailuresAreFree() {
        for (int i = 0; i < 5; i++) {
            assertEquals(0L, tracker.recordFailure());
        }
        assertFalse(tracker.isLockedOut());
    }

    @Test
    public void delayEscalatesAfterFreeAttempts() {
        for (int i = 0; i < 5; i++) tracker.recordFailure();

        long first = tracker.recordFailure();
        assertTrue(first > 25_000L && first <= 30_000L);
        assertTrue(tracker.isLockedOut());

        long second = tracker.recordFailure();
        assertTrue(second > 55_000L && second <= 60_000L);
        assertEquals(7, tracker.failureCount());
    }

    @Test
    public void resetClearsFailuresAndLockout() {
        for (int i = 0; i < 7; i++) tracker.recordFailure();
        tracker.reset();
        assertEquals(0, tracker.failureCount());
        assertFalse(tracker.isLockedOut());
    }

    // ---- With the clocks driven by hand ------------------------------------------------------

    private long elapsed;
    private int boot;

    private LockoutTracker withClocks() {
        elapsed = 1_000_000L;
        boot = 7;
        android.content.SharedPreferences prefs = ApplicationProvider.getApplicationContext()
                .getSharedPreferences("lockout-test-" + System.nanoTime(),
                        android.content.Context.MODE_PRIVATE);
        return new LockoutTracker(prefs, () -> elapsed, () -> boot);
    }

    private static void failTimes(LockoutTracker t, int n) {
        for (int i = 0; i < n; i++) t.recordFailure();
    }

    @Test
    public void theWaitCountsDownOnTheElapsedClockAndEnds() {
        LockoutTracker t = withClocks();
        failTimes(t, 6);

        assertEquals(30_000L, t.remainingMs());
        elapsed += 12_000L;
        assertEquals(18_000L, t.remainingMs());
        elapsed += 18_000L;
        assertFalse(t.isLockedOut());
    }

    @Test
    public void aRebootRestartsTheFullWaitInsteadOfTrustingTheWallClock() {
        LockoutTracker t = withClocks();
        failTimes(t, 7);
        assertEquals(60_000L, t.remainingMs());
        elapsed += 50_000L;
        assertEquals(10_000L, t.remainingMs());

        boot++;
        elapsed = 500L; // the elapsed clock starts again after a reboot

        assertEquals(60_000L, t.remainingMs());
        elapsed += 20_000L;
        assertEquals(40_000L, t.remainingMs());
    }

    @Test
    public void aDeadlineFarBeyondTheDelayIsCutBackToTheDelay() {
        LockoutTracker t = withClocks();
        failTimes(t, 6);

        // The elapsed clock goes backwards without a reboot being seen, so the stored deadline
        // now looks very far away.
        elapsed -= 900_000L;

        assertEquals(30_000L, t.remainingMs());
    }

    @Test
    public void aFreeFailureLeavesNoLockout() {
        LockoutTracker t = withClocks();
        failTimes(t, 6);
        t.reset();

        failTimes(t, 3);

        assertFalse(t.isLockedOut());
        assertEquals(3, t.failureCount());
    }
}
