package com.mowtiie.dearest.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import androidx.biometric.BiometricManager;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class BiometricGate {

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "dearest_biometric_key";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;

    private static final String PREFS = "dearest_biometric";
    private static final String KEY_WRAPPED = "wrapped_dek";
    private static final String KEY_IV = "iv";

    private final KeyManager keyManager;
    private final SharedPreferences prefs;

    public BiometricGate(Context context, KeyManager keyManager) {
        this.keyManager = keyManager;
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isAvailable(Context context) {
        int result = BiometricManager.from(context)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        return result == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public boolean isEnrolled() {
        return prefs.contains(KEY_WRAPPED) && prefs.contains(KEY_IV);
    }

    public Cipher getEnrollCipher() throws GeneralSecurityException, IOException {
        deleteKey();
        SecretKey key = generateKey();
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher;
    }

    public void completeEnroll(Cipher authenticatedCipher) throws GeneralSecurityException {
        if (!keyManager.isUnlocked()) {
            throw new IllegalStateException("Unlock before enabling biometrics");
        }
        byte[] dek = keyManager.getDatabaseKey();
        try {
            byte[] wrapped = authenticatedCipher.doFinal(dek);
            prefs.edit()
                    .putString(KEY_WRAPPED, toB64(wrapped))
                    .putString(KEY_IV, toB64(authenticatedCipher.getIV()))
                    .apply();
        } finally {
            Arrays.fill(dek, (byte) 0);
        }
    }

    public Cipher getUnlockCipher() throws GeneralSecurityException, IOException {
        SecretKey key = getKey();
        if (key == null) throw new IllegalStateException("Biometric key missing");
        byte[] iv = fromB64(prefs.getString(KEY_IV, null));
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
        return cipher;
    }

    public void completeUnlock(Cipher authenticatedCipher) throws GeneralSecurityException {
        byte[] wrapped = fromB64(prefs.getString(KEY_WRAPPED, null));
        byte[] dek = authenticatedCipher.doFinal(wrapped);
        try {
            keyManager.unlockWithDek(dek);
        } finally {
            Arrays.fill(dek, (byte) 0);
        }
    }

    public void disable() {
        prefs.edit().remove(KEY_WRAPPED).remove(KEY_IV).apply();
        try {
            deleteKey();
        } catch (Exception ignored) {
        }
    }

    private SecretKey generateKey() throws GeneralSecurityException {
        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG);
        } else {
            builder.setUserAuthenticationValidityDurationSeconds(-1);
        }

        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        generator.init(builder.build());
        return generator.generateKey();
    }

    private SecretKey getKey() throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
        ks.load(null);
        return (SecretKey) ks.getKey(KEY_ALIAS, null);
    }

    private void deleteKey() throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
        ks.load(null);
        if (ks.containsAlias(KEY_ALIAS)) ks.deleteEntry(KEY_ALIAS);
    }

    private static String toB64(byte[] b) { return Base64.encodeToString(b, Base64.NO_WRAP); }
    private static byte[] fromB64(String s) { return Base64.decode(s, Base64.NO_WRAP); }
}
