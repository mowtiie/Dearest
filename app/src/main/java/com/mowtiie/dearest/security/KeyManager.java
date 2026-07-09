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
            throw new KeyManagerException("Vault already initialised");
        }
        byte[] salt   = CryptoPrimitives.randomBytes(CryptoPrimitives.SALT_LENGTH_BYTES);
        byte[] newDek = CryptoPrimitives.randomBytes(CryptoPrimitives.DEK_LENGTH_BYTES);
        byte[] kek    = CryptoPrimitives.deriveKek(passphrase, salt, PBKDF2_ITERATIONS);
        try {
            byte[] wrapped = CryptoPrimitives.aesGcmEncrypt(kek, newDek);
            storage.saveVault(salt, PBKDF2_ITERATIONS, wrapped);
            this.dek = newDek;
        } finally {
            CryptoPrimitives.zero(kek);
            CryptoPrimitives.zero(passphrase);
        }
    }

    public boolean unlock(char[] passphrase) {
        if (!isInitialized()) {
            throw new KeyManagerException("Vault not initialised");
        }
        byte[] salt       = storage.getSalt();
        int    iterations = storage.getIterations();
        byte[] wrapped    = storage.getWrappedDek();
        byte[] kek        = CryptoPrimitives.deriveKek(passphrase, salt, iterations);
        try {
            this.dek = CryptoPrimitives.aesGcmDecrypt(kek, wrapped);
            return true;
        } catch (GeneralSecurityException wrongPassphrase) {
            this.dek = null;
            return false;
        } finally {
            CryptoPrimitives.zero(kek);
            CryptoPrimitives.zero(passphrase);
        }
    }

    public boolean changePassphrase(char[] currentPassphrase, char[] newPassphrase) {
        if (!isInitialized()) {
            throw new KeyManagerException("Vault not initialised");
        }
        byte[] salt       = storage.getSalt();
        int    iterations = storage.getIterations();
        byte[] wrapped    = storage.getWrappedDek();

        byte[] oldKek = CryptoPrimitives.deriveKek(currentPassphrase, salt, iterations);
        byte[] currentDek;
        try {
            currentDek = CryptoPrimitives.aesGcmDecrypt(oldKek, wrapped);
        } catch (GeneralSecurityException wrongPassphrase) {
            return false;
        } finally {
            CryptoPrimitives.zero(oldKek);
            CryptoPrimitives.zero(currentPassphrase);
        }

        byte[] newSalt = CryptoPrimitives.randomBytes(CryptoPrimitives.SALT_LENGTH_BYTES);
        byte[] newKek  = CryptoPrimitives.deriveKek(newPassphrase, newSalt, PBKDF2_ITERATIONS);
        try {
            byte[] newWrapped = CryptoPrimitives.aesGcmEncrypt(newKek, currentDek);
            storage.saveVault(newSalt, PBKDF2_ITERATIONS, newWrapped);
            this.dek = currentDek;
            return true;
        } finally {
            CryptoPrimitives.zero(newKek);
            CryptoPrimitives.zero(newPassphrase);
        }
    }

    public byte[] getDatabaseKey() {
        if (dek == null) {
            throw new KeyManagerException("Locked — unlock before requesting the key");
        }
        return dek.clone();
    }

    public void lock() {
        CryptoPrimitives.zero(dek);
        dek = null;
    }

    public void resetVault() {
        lock();
        storage.clear();
    }
}
