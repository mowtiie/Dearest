package com.mowtiie.dearest.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.util.Base64;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

@RunWith(RobolectricTestRunner.class)
public class BackupCryptoTest {

    @Test
    public void roundTripsAndWipesPasswords() throws Exception {
        byte[] data = "{\"entries\":[]}".getBytes(StandardCharsets.UTF_8);
        char[] password = "hunter22hunter".toCharArray();
        String envelope = BackupCrypto.encrypt(data, password);
        assertArrayEquals(new char[password.length], password);

        assertArrayEquals(data, BackupCrypto.decrypt(envelope, "hunter22hunter".toCharArray()));
    }

    @Test
    public void envelopeDoesNotContainPlaintext() {
        String envelope = BackupCrypto.encrypt("secret diary text".getBytes(StandardCharsets.UTF_8),
                "pw-pw-pw-pw".toCharArray());
        assertFalse(envelope.contains("secret diary text"));
    }

    @Test(expected = GeneralSecurityException.class)
    public void wrongPasswordFails() throws Exception {
        String envelope = BackupCrypto.encrypt(new byte[8], "right-password".toCharArray());
        BackupCrypto.decrypt(envelope, "wrong-password".toCharArray());
    }

    @Test(expected = GeneralSecurityException.class)
    public void tamperedPayloadFails() throws Exception {
        String envelope = BackupCrypto.encrypt(new byte[64], "right-password".toCharArray());
        JSONObject env = new JSONObject(envelope);
        byte[] payload = Base64.decode(env.getString("payload"), Base64.NO_WRAP);
        payload[payload.length - 1] ^= 1;
        env.put("payload", Base64.encodeToString(payload, Base64.NO_WRAP));
        BackupCrypto.decrypt(env.toString(), "right-password".toCharArray());
    }

    @Test(expected = GeneralSecurityException.class)
    public void garbageIsRejectedAsNotABackup() throws Exception {
        BackupCrypto.decrypt("not json at all", "whatever-pw".toCharArray());
    }

    private static String envelopeWith(String field, Object value) throws Exception {
        JSONObject env = new JSONObject(
                BackupCrypto.encrypt(new byte[32], "right-password".toCharArray()));
        env.put(field, value);
        return env.toString();
    }

    private static void assertRejected(String envelope) {
        long start = System.nanoTime();
        try {
            BackupCrypto.decrypt(envelope, "right-password".toCharArray());
            fail("expected the file to be rejected");
        } catch (GeneralSecurityException expected) {
            // rejected
        }
        // Rejected up front, not after deriving a key with a hostile iteration count.
        assertTrue((System.nanoTime() - start) < 5_000_000_000L);
    }

    @Test
    public void absurdIterationCountsAreRejectedWithoutDerivingAKey() throws Exception {
        assertRejected(envelopeWith("iterations", Integer.MAX_VALUE));
        assertRejected(envelopeWith("iterations", 50_000_000));
    }

    @Test
    public void tooFewOrNonsenseIterationCountsAreRejected() throws Exception {
        assertRejected(envelopeWith("iterations", 0));
        assertRejected(envelopeWith("iterations", -5));
        assertRejected(envelopeWith("iterations", 1_000));
    }

    @Test
    public void badSaltsAndPayloadsAreRejected() throws Exception {
        assertRejected(envelopeWith("salt", ""));
        assertRejected(envelopeWith("salt", Base64.encodeToString(new byte[200], Base64.NO_WRAP)));
        assertRejected(envelopeWith("payload", Base64.encodeToString(new byte[5], Base64.NO_WRAP)));
        assertRejected(envelopeWith("payload", "***not base64***"));
    }
}
