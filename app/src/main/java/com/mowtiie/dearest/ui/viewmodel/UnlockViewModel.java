package com.mowtiie.dearest.ui.viewmodel;


import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mowtiie.dearest.security.LockoutTracker;

import java.util.Arrays;

public class UnlockViewModel extends DearestViewModel {

    public enum Mode { SETUP, UNLOCK }

    public static final int MIN_PASSPHRASE_LENGTH = 8;

    private final MutableLiveData<Mode> mode = new MutableLiveData<>();
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Long> lockoutRemainingMs = new MutableLiveData<>(0L);

    private final LockoutTracker lockout;

    public UnlockViewModel(@NonNull Application application) {
        super(application);
        lockout = new LockoutTracker(application);
        mode.setValue(keyManager().isInitialized() ? Mode.UNLOCK : Mode.SETUP);
        lockoutRemainingMs.setValue(lockout.remainingMs());
    }

    public LiveData<Mode> mode() { return mode; }
    public LiveData<Boolean> busy() { return busy; }
    public LiveData<String> error() { return error; }
    public LiveData<Long> lockoutRemainingMs() { return lockoutRemainingMs; }

    public void setup(char[] passphrase, char[] confirmation) {
        error.setValue(null);

        if (!Arrays.equals(passphrase, confirmation)) {
            wipe(passphrase);
            wipe(confirmation);
            error.setValue("Those don't match. Try again.");
            return;
        }
        if (passphrase.length < MIN_PASSPHRASE_LENGTH) {
            wipe(passphrase);
            wipe(confirmation);
            error.setValue("Use at least " + MIN_PASSPHRASE_LENGTH + " characters.");
            return;
        }
        wipe(confirmation);

        busy.setValue(true);
        io.execute(() -> {
            try {
                keyManager().setup(passphrase);
                main.post(() -> {
                    busy.setValue(false);
                    app().onUnlocked();
                });
            } catch (Exception e) {
                main.post(() -> {
                    busy.setValue(false);
                    error.setValue("Setup failed. Please try again.");
                });
            }
        });
    }

    public void unlock(char[] passphrase) {
        error.setValue(null);

        if (lockout.isLockedOut()) {
            wipe(passphrase);
            lockoutRemainingMs.setValue(lockout.remainingMs());
            error.setValue("Too many attempts. Please wait.");
            return;
        }

        busy.setValue(true);
        io.execute(() -> {
            boolean ok = keyManager().unlock(passphrase);
            main.post(() -> {
                busy.setValue(false);
                if (ok) {
                    lockout.reset();
                    lockoutRemainingMs.setValue(0L);
                    app().onUnlocked();
                } else {
                    long wait = lockout.recordFailure();
                    lockoutRemainingMs.setValue(wait);
                    error.setValue(wait > 0L
                            ? "Incorrect passphrase. Too many attempts — please wait."
                            : "Incorrect passphrase.");
                }
            });
        });
    }

    private static void wipe(char[] array) {
        if (array != null) Arrays.fill(array, '\0');
    }
}
