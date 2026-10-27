package com.mowtiie.dearest.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.security.GeneralSecurityException;
import java.util.Arrays;

public class CryptoPrimitivesTest {

    private static final int ITERATIONS = 1_000;

    @Test
    public void aesGcmRoundTrips() throws Exception {
        byte[] key = CryptoPrimitives.randomBytes(32);
        byte[] plaintext = "dear diary".getBytes();
        byte[] sealed = CryptoPrimitives.aesGcmEncrypt(key, plaintext);
        assertArrayEquals(plaintext, CryptoPrimitives.aesGcmDecrypt(key, sealed));
    }

    @Test
    public void aesGcmUsesFreshIvEveryTime() {
        byte[] key = CryptoPrimitives.randomBytes(32);
        byte[] plaintext = new byte[16];
        assertFalse(Arrays.equals(
                CryptoPrimitives.aesGcmEncrypt(key, plaintext),
                CryptoPrimitives.aesGcmEncrypt(key, plaintext)));
    }

    @Test(expected = GeneralSecurityException.class)
    public void aesGcmRejectsWrongKey() throws Exception {
        byte[] sealed = CryptoPrimitives.aesGcmEncrypt(CryptoPrimitives.randomBytes(32), new byte[32]);
        CryptoPrimitives.aesGcmDecrypt(CryptoPrimitives.randomBytes(32), sealed);
    }

    @Test(expected = GeneralSecurityException.class)
    public void aesGcmRejectsTamperedCiphertext() throws Exception {
        byte[] key = CryptoPrimitives.randomBytes(32);
        byte[] sealed = CryptoPrimitives.aesGcmEncrypt(key, new byte[32]);
        sealed[sealed.length - 1] ^= 1;
        CryptoPrimitives.aesGcmDecrypt(key, sealed);
    }

    @Test
    public void kekDerivationIsDeterministicAndSaltDependent() {
        byte[] salt = CryptoPrimitives.randomBytes(CryptoPrimitives.SALT_LENGTH_BYTES);
        byte[] a = CryptoPrimitives.deriveKek("correct horse".toCharArray(), salt, ITERATIONS);
        byte[] b = CryptoPrimitives.deriveKek("correct horse".toCharArray(), salt, ITERATIONS);
        byte[] otherSalt = CryptoPrimitives.deriveKek("correct horse".toCharArray(),
                CryptoPrimitives.randomBytes(CryptoPrimitives.SALT_LENGTH_BYTES), ITERATIONS);
        assertEquals(32, a.length);
        assertArrayEquals(a, b);
        assertFalse(Arrays.equals(a, otherSalt));
    }

    @Test
    public void zeroWipesBuffers() {
        byte[] bytes = {1, 2, 3};
        char[] chars = {'a', 'b'};
        CryptoPrimitives.zero(bytes);
        CryptoPrimitives.zero(chars);
        assertArrayEquals(new byte[3], bytes);
        assertArrayEquals(new char[2], chars);
    }
}
