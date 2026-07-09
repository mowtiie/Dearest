package com.mowtiie.dearest;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.mowtiie.dearest.data.db.DearestDatabase;
import com.mowtiie.dearest.data.repository.DearestRepository;
import com.mowtiie.dearest.security.BiometricGate;
import com.mowtiie.dearest.security.KeyManager;

public class DearestApp extends Application implements DefaultLifecycleObserver {

    public static final long LOCK_IMMEDIATELY = 0L;

    private static final String PREFS = "dearest_settings";
    private static final String KEY_LOCK_TIMEOUT_MS = "lock_timeout_ms";
    private static final long DEFAULT_TIMEOUT_MS  = 60_000L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final MutableLiveData<Boolean> locked = new MutableLiveData<>(true);
    private final Runnable lockRunnable = this::lockNow;

    private KeyManager keyManager;
    private DearestRepository repository;
    private BiometricGate biometricGate;
    private SharedPreferences settings;

    @Override
    public void onCreate() {
        super.onCreate();

        DearestDatabase.loadLibrary();

        settings   = getSharedPreferences(PREFS, MODE_PRIVATE);
        keyManager = new KeyManager(this);
        repository = DearestRepository.getInstance(this, keyManager);
        biometricGate = new BiometricGate(this, keyManager);

        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    public static DearestApp from(Context context) {
        return (DearestApp) context.getApplicationContext();
    }


    public KeyManager keyManager() {
        return keyManager;
    }

    public DearestRepository repository() {
        return repository;
    }

    public BiometricGate biometricGate() {
        return biometricGate;
    }

    public LiveData<Boolean> lockState() {
        return locked;
    }

    public void onUnlocked() {
        mainHandler.removeCallbacks(lockRunnable);
        repository.open();
        locked.setValue(false);
    }

    public void lockNow() {
        mainHandler.removeCallbacks(lockRunnable);
        repository.close();
        keyManager.lock();
        locked.setValue(true);
    }

    public long getLockTimeoutMs() {
        return settings.getLong(KEY_LOCK_TIMEOUT_MS, DEFAULT_TIMEOUT_MS);
    }

    public void setLockTimeoutMs(long timeoutMs) {
        settings.edit().putLong(KEY_LOCK_TIMEOUT_MS, timeoutMs).apply();
    }


    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        mainHandler.removeCallbacks(lockRunnable);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (!keyManager.isUnlocked()) return;

        long timeout = getLockTimeoutMs();
        if (timeout <= LOCK_IMMEDIATELY) {
            lockNow();
        } else {
            mainHandler.postDelayed(lockRunnable, timeout);
        }
    }
}
