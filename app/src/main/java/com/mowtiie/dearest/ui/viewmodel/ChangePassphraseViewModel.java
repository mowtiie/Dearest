package com.mowtiie.dearest.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Arrays;

public class ChangePassphraseViewModel extends DearestViewModel {

    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> success = new MutableLiveData<>(false);

    public ChangePassphraseViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Boolean> busy()    { return busy; }
    public LiveData<String>  error()   { return error; }
    public LiveData<Boolean> success() { return success; }

    public void change(char[] current, char[] newPassphrase, char[] confirmation) {
        error.setValue(null);

        if (!Arrays.equals(newPassphrase, confirmation)) {
            wipe(current); wipe(newPassphrase); wipe(confirmation);
            error.setValue("The new passphrases don't match.");
            return;
        }
        if (newPassphrase.length < UnlockViewModel.MIN_PASSPHRASE_LENGTH) {
            wipe(current); wipe(newPassphrase); wipe(confirmation);
            error.setValue("Use at least " + UnlockViewModel.MIN_PASSPHRASE_LENGTH + " characters.");
            return;
        }
        wipe(confirmation);

        busy.setValue(true);
        io.execute(() -> {
            boolean ok = keyManager().changePassphrase(current, newPassphrase);
            main.post(() -> {
                busy.setValue(false);
                if (ok) {
                    success.setValue(true);
                } else {
                    error.setValue("Current passphrase is incorrect.");
                }
            });
        });
    }

    private static void wipe(@Nullable char[] array) {
        if (array != null) Arrays.fill(array, '\0');
    }
}