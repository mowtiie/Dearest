package com.mowtiie.dearest.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class KeyManagerTest {

    private KeyManager keys;

    @Before
    public void setUp() {
        keys = new KeyManager(ApplicationProvider.getApplicationContext());
    }

    private static char[] pw(String s) {
        return s.toCharArray();
    }

    @Test
    public void setupUnlocksAndWipesPassphrase() {
        char[] passphrase = pw("first passphrase");
        assertFalse(keys.isInitialized());
        keys.setup(passphrase);
        assertTrue(keys.isInitialized());
        assertTrue(keys.isUnlocked());
        assertArrayEquals(new char[passphrase.length], passphrase);
    }

    @Test
    public void lockZeroesKeyAndUnlockRestoresSameKey() {
        keys.setup(pw("first passphrase"));
        byte[] original = keys.getDatabaseKey();
        keys.lock();
        assertFalse(keys.isUnlocked());

        assertTrue(keys.unlock(pw("first passphrase")));
        assertArrayEquals(original, keys.getDatabaseKey());
    }

    @Test
    public void wrongPassphraseDoesNotUnlock() {
        keys.setup(pw("first passphrase"));
        keys.lock();
        assertFalse(keys.unlock(pw("not the passphrase")));
        assertFalse(keys.isUnlocked());
    }

    @Test(expected = KeyManagerException.class)
    public void lockedManagerRefusesToReturnKey() {
        keys.setup(pw("first passphrase"));
        keys.lock();
        keys.getDatabaseKey();
    }

    @Test
    public void changePassphraseRewrapsSameDek() {
        keys.setup(pw("first passphrase"));
        byte[] dek = keys.getDatabaseKey();

        assertTrue(keys.changePassphrase(pw("first passphrase"), pw("second passphrase")));
        keys.lock();

        assertFalse(keys.unlock(pw("first passphrase")));
        assertTrue(keys.unlock(pw("second passphrase")));
        assertArrayEquals("database key must not change with the passphrase", dek, keys.getDatabaseKey());
    }

    @Test
    public void changePassphraseRejectsWrongCurrentPassphrase() {
        keys.setup(pw("first passphrase"));
        assertFalse(keys.changePassphrase(pw("bad guess"), pw("second passphrase")));
        keys.lock();
        assertTrue(keys.unlock(pw("first passphrase")));
    }

    @Test
    public void everyVaultGetsAFreshKey() {
        keys.setup(pw("first passphrase"));
        byte[] first = keys.getDatabaseKey();
        keys.resetVault();
        keys.setup(pw("first passphrase"));
        assertFalse(Arrays.equals(first, keys.getDatabaseKey()));
    }

    @Test(expected = KeyManagerException.class)
    public void setupTwiceIsRejected() {
        keys.setup(pw("first passphrase"));
        keys.setup(pw("another passphrase"));
    }
}
