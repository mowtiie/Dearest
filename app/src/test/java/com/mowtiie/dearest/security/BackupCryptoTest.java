package com.mowtiie.dearest.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;

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
}
