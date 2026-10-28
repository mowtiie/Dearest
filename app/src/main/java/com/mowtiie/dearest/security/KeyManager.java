package com.mowtiie.dearest.security;

import android.content.Context;

import java.security.GeneralSecurityException;

public final class KeyManager {

    private static final int PBKDF2_ITERATIONS = 600_000;

    private final KeyStorage storage;

    private byte[] dek;

    public KeyManager(Context context) {
        this.storage = new KeyStorage(context);
    }

    public boolean isInitialized() {
        return storage.isInitialized();
    }

    public boolean isUnlocked() {
        return dek != null;
    }

    public void setup(char[] passphrase) {
        if (isInitialized()) {
            CryptoPrimitives.zero(passphrase);
            throw new KeyManagerException("Vault already initialised");
        }
        byte[] salt   = CryptoPrimitives.randomBytes(CryptoPrimitives.SALT_LENGTH_BYTES);
        byte[] newDek = CryptoPrimitives.randomBytes(CryptoPrimitives.DEK_LENGTH_BYTES);
        byte[] kek = null;
        boolean adopted = false;
        try {
            kek = CryptoPrimitives.deriveKek(passphrase, salt, PBKDF2_ITERATIONS);
            byte[] wrapped = CryptoPrimitives.aesGcmEncrypt(kek, newDek);
            storage.saveVault(salt, PBKDF2_ITERATIONS, wrapped);
            replaceDek(newDek);
            adopted = true;
        } finally {
            CryptoPrimitives.zero(kek);
            CryptoPrimitives.zero(passphrase);
            if (!adopted) CryptoPrimitives.zero(newDek);
        }
    }

    public boolean unlock(char[] passphrase) {
        if (!isInitialized()) {
            CryptoPrimitives.zero(passphrase);
            throw new KeyManagerException("Vault not initialised");
        }
        byte[] salt       = storage.getSalt();
        int    iterations = storage.getIterations();
        byte[] wrapped    = storage.getWrappedDek();
        byte[] kek = null;
        try {
            kek = CryptoPrimitives.deriveKek(passphrase, salt, iterations);
            replaceDek(CryptoPrimitives.aesGcmDecrypt(kek, wrapped));
            return true;
        } catch (GeneralSecurityException wrongPassphrase) {
            replaceDek(null);
            return false;
        } finally {
            CryptoPrimitives.zero(kek);
            CryptoPrimitives.zero(passphrase);
        }
    }

    public boolean changePassphrase(char[] currentPassphrase, char[] newPassphrase) {
        if (!isInitialized()) {
            CryptoPrimitives.zero(currentPassphrase);
            CryptoPrimitives.zero(newPassphrase);
            throw new KeyManagerException("Vault not initialised");
        }
        byte[] salt       = storage.getSalt();
        int    iterations = storage.getIterations();
        byte[] wrapped    = storage.getWrappedDek();

        byte[] oldKek = null;
        byte[] currentDek;
        try {
            oldKek = CryptoPrimitives.deriveKek(currentPassphrase, salt, iterations);
            currentDek = CryptoPrimitives.aesGcmDecrypt(oldKek, wrapped);
        } catch (GeneralSecurityException wrongPassphrase) {
            CryptoPrimitives.zero(newPassphrase);
            return false;
        } finally {
            CryptoPrimitives.zero(oldKek);
            CryptoPrimitives.zero(currentPassphrase);
        }

        byte[] newSalt = CryptoPrimitives.randomBytes(CryptoPrimitives.SALT_LENGTH_BYTES);
        byte[] newKek = null;
        boolean adopted = false;
        try {
            newKek = CryptoPrimitives.deriveKek(newPassphrase, newSalt, PBKDF2_ITERATIONS);
            byte[] newWrapped = CryptoPrimitives.aesGcmEncrypt(newKek, currentDek);
            storage.saveVault(newSalt, PBKDF2_ITERATIONS, newWrapped);
            replaceDek(currentDek);
            adopted = true;
            return true;
        } finally {
            CryptoPrimitives.zero(newKek);
            CryptoPrimitives.zero(newPassphrase);
            if (!adopted) CryptoPrimitives.zero(currentDek);
        }
    }

    public byte[] getDatabaseKey() {
        if (dek == null) {
            throw new KeyManagerException("Locked — unlock before requesting the key");
        }
        return dek.clone();
    }

    void unlockWithDek(byte[] dek) {
        replaceDek(dek.clone());
    }

    public void lock() {
        replaceDek(null);
    }

    public void resetVault() {
        lock();
        storage.clear();
    }

    /** Swaps in a new key (or none), wiping the one it replaces. */
    private void replaceDek(byte[] next) {
        byte[] previous = dek;
        dek = next;
        if (previous != null && previous != next) CryptoPrimitives.zero(previous);
    }
}
