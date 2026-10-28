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

    // ---- Key arrays are wiped, not just dropped ----------------------------------------------

    /** The manager's own key array (not a clone), to check what happens to it. */
    private byte[] heldKey() throws Exception {
        java.lang.reflect.Field field = KeyManager.class.getDeclaredField("dek");
        field.setAccessible(true);
        return (byte[]) field.get(keys);
    }

    private static void assertWiped(byte[] array) {
        assertArrayEquals(new byte[array.length], array);
    }

    @Test
    public void lockWipesTheHeldKeyArray() throws Exception {
        keys.setup(pw("first passphrase"));
        byte[] held = heldKey();

        keys.lock();

        assertWiped(held);
    }

    @Test
    public void changingThePassphraseWipesTheReplacedKeyAndKeepsTheKey() throws Exception {
        keys.setup(pw("first passphrase"));
        byte[] before = heldKey();
        byte[] keyValue = keys.getDatabaseKey();

        assertTrue(keys.changePassphrase(pw("first passphrase"), pw("second passphrase")));

        assertWiped(before);
        assertArrayEquals(keyValue, keys.getDatabaseKey());
    }

    @Test
    public void aWrongCurrentPassphraseWipesBothPassphrasesAndLeavesTheKeyAlone() throws Exception {
        keys.setup(pw("first passphrase"));
        byte[] held = heldKey();
        byte[] keyValue = keys.getDatabaseKey();
        char[] current = pw("not the passphrase");
        char[] next = pw("brand new passphrase");

        assertFalse(keys.changePassphrase(current, next));

        assertArrayEquals(new char[current.length], current);
        assertArrayEquals(new char[next.length], next);
        assertArrayEquals(keyValue, held);
    }

    @Test
    public void aFailedUnlockWhileUnlockedWipesTheHeldKey() throws Exception {
        keys.setup(pw("first passphrase"));
        byte[] held = heldKey();

        assertFalse(keys.unlock(pw("not the passphrase")));

        assertWiped(held);
        assertFalse(keys.isUnlocked());
    }

    @Test
    public void unlockingWithABiometricKeyWipesTheOneItReplaces() throws Exception {
        keys.setup(pw("first passphrase"));
        byte[] held = heldKey();

        keys.unlockWithDek(new byte[32]);

        assertWiped(held);
        assertTrue(keys.isUnlocked());
    }

    @Test
    public void aRejectedSecondSetupStillWipesThePassphrase() {
        keys.setup(pw("first passphrase"));
        char[] again = pw("another passphrase");

        try {
            keys.setup(again);
            org.junit.Assert.fail("expected the second setup to be rejected");
        } catch (KeyManagerException expected) {
            // rejected
        }

        assertArrayEquals(new char[again.length], again);
    }
}
