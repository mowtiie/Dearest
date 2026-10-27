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
}
