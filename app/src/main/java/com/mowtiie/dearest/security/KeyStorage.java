package com.mowtiie.dearest.security;

import android.content.Context;
import android.content.SharedPreferences;

final class KeyStorage {

    private static final String PREFS_NAME = "dearest_keys";
    private static final String KEY_SALT = "kdf_salt";
    private static final String KEY_ITERATIONS = "kdf_iterations";
    private static final String KEY_WRAPPED_DEK = "wrapped_dek";

    private final SharedPreferences prefs;

    KeyStorage(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    boolean isInitialized() {
        return prefs.contains(KEY_WRAPPED_DEK);
    }

    void saveVault(byte[] salt, int iterations, byte[] wrappedDek) {
        prefs.edit()
                .putString(KEY_SALT, CryptoPrimitives.toBase64(salt))
                .putInt(KEY_ITERATIONS, iterations)
                .putString(KEY_WRAPPED_DEK, CryptoPrimitives.toBase64(wrappedDek))
                .apply();
    }

    byte[] getSalt() {
        return CryptoPrimitives.fromBase64(require(KEY_SALT));
    }

    int getIterations() {
        int iterations = prefs.getInt(KEY_ITERATIONS, 0);
        if (iterations <= 0) {
            throw new KeyManagerException("Stored iteration count is missing or invalid");
        }
        return iterations;
    }

    byte[] getWrappedDek() {
        return CryptoPrimitives.fromBase64(require(KEY_WRAPPED_DEK));
    }

    void clear() {
        prefs.edit().clear().apply();
    }

    private String require(String key) {
        String value = prefs.getString(key, null);
        if (value == null) {
            throw new KeyManagerException("Missing key material: " + key);
        }
        return value;
    }
}
