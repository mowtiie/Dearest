package com.mowtiie.dearest.security;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.GeneralSecurityException;

public final class BackupCrypto {

    private static final String FORMAT = "dearest-backup";
    private static final int ITERATIONS = 600_000;

    private BackupCrypto() {}

    public static String encrypt(byte[] plaintext, char[] password) {
        byte[] salt = CryptoPrimitives.randomBytes(CryptoPrimitives.SALT_LENGTH_BYTES);
        byte[] key = CryptoPrimitives.deriveKek(password, salt, ITERATIONS);
        try {
            byte[] payload = CryptoPrimitives.aesGcmEncrypt(key, plaintext);
            JSONObject env = new JSONObject();
            env.put("format", FORMAT);
            env.put("version", 1);
            env.put("kdf", "PBKDF2WithHmacSHA256");
            env.put("iterations", ITERATIONS);
            env.put("salt", CryptoPrimitives.toBase64(salt));
            env.put("payload", CryptoPrimitives.toBase64(payload));
            return env.toString();
        } catch (JSONException e) {
            throw new KeyManagerException("Backup encoding failed", e);
        } finally {
            CryptoPrimitives.zero(key);
            CryptoPrimitives.zero(password);
        }
    }

    public static byte[] decrypt(String envelope, char[] password) throws GeneralSecurityException {
        byte[] key = null;
        try {
            JSONObject env = new JSONObject(envelope);
            int iterations = env.getInt("iterations");
            byte[] salt = CryptoPrimitives.fromBase64(env.getString("salt"));
            byte[] payload = CryptoPrimitives.fromBase64(env.getString("payload"));
            key = CryptoPrimitives.deriveKek(password, salt, iterations);
            return CryptoPrimitives.aesGcmDecrypt(key, payload);
        } catch (JSONException e) {
            throw new GeneralSecurityException("Not a valid Dearest backup file", e);
        } finally {
            CryptoPrimitives.zero(key);
            CryptoPrimitives.zero(password);
        }
    }
}
