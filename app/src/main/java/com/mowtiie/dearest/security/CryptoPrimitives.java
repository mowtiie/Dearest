package com.mowtiie.dearest.security;

import android.util.Base64;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoPrimitives {

    static final int DEK_LENGTH_BYTES  = 32;
    static final int SALT_LENGTH_BYTES = 16;

    private static final int KEK_LENGTH_BITS  = 256;
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_IV_BYTES     = 12;
    private static final int GCM_TAG_BITS     = 128;

    private static final SecureRandom RANDOM = new SecureRandom();

    private CryptoPrimitives() {}

    static byte[] randomBytes(int length) {
        byte[] out = new byte[length];
        RANDOM.nextBytes(out);
        return out;
    }

    static byte[] deriveKek(char[] passphrase, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(passphrase, salt, iterations, KEK_LENGTH_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new KeyManagerException("Key derivation failed", e);
        } finally {
            spec.clearPassword();
        }
    }

    static byte[] aesGcmEncrypt(byte[] key, byte[] plaintext) {
        try {
            byte[] iv = randomBytes(GCM_IV_BYTES);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);

            byte[] out = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ciphertext, 0, out, iv.length, ciphertext.length);
            return out;
        } catch (GeneralSecurityException e) {
            throw new KeyManagerException("Encryption failed", e);
        }
    }

    static byte[] aesGcmDecrypt(byte[] key, byte[] ivAndCiphertext) throws GeneralSecurityException {
        byte[] iv = Arrays.copyOfRange(ivAndCiphertext, 0, GCM_IV_BYTES);
        byte[] ciphertext = Arrays.copyOfRange(ivAndCiphertext, GCM_IV_BYTES, ivAndCiphertext.length);

        Cipher cipher = Cipher.getInstance(AES_GCM);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
        return cipher.doFinal(ciphertext);
    }

    static String toBase64(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    static byte[] fromBase64(String data) {
        return Base64.decode(data, Base64.NO_WRAP);
    }

    static void zero(byte[] data) {
        if (data != null) Arrays.fill(data, (byte) 0);
    }

    static void zero(char[] data) {
        if (data != null) Arrays.fill(data, '\0');
    }
}
