package com.mowtiie.dearest;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.mowtiie.dearest.backup.BackupManager;
import com.mowtiie.dearest.crash.CrashHandler;
import com.mowtiie.dearest.data.db.DearestDatabase;
import com.mowtiie.dearest.data.repository.DearestRepository;
import com.mowtiie.dearest.display.DisplayPrefs;
import com.mowtiie.dearest.display.PrivacyScreenGuard;
import com.mowtiie.dearest.notification.ReminderScheduler;
import com.mowtiie.dearest.security.BiometricGate;
import com.mowtiie.dearest.security.KeyManager;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;

public class DearestApp extends Application implements DefaultLifecycleObserver {

    public static final long LOCK_IMMEDIATELY = 0L;

    private static final String PREFS = "dearest_settings";
    private static final String KEY_LOCK_TIMEOUT_MS = "lock_timeout_ms";
    private static final long   DEFAULT_TIMEOUT_MS  = 60_000L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final MutableLiveData<Boolean> locked = new MutableLiveData<>(true);
    private final Runnable lockRunnable = this::lockNow;
    private boolean autoLockSuppressed;

    private KeyManager keyManager;
    private DearestRepository repository;
    private BiometricGate biometricGate;
    private BackupManager backupManager;
    private DisplayPrefs displayPrefs;
    private SharedPreferences settings;

    @Override
    public void onCreate() {
        super.onCreate();

        CrashHandler.install(this);

        displayPrefs = new DisplayPrefs(this);

        applyThemeMode(displayPrefs.getThemeMode());
        DearestDatabase.loadLibrary();
        ReminderScheduler.ensureChannel(this);

        settings   = getSharedPreferences(PREFS, MODE_PRIVATE);
        keyManager = new KeyManager(this);
        repository = DearestRepository.getInstance(this, keyManager);
        biometricGate = new BiometricGate(this, keyManager);
        backupManager = new BackupManager(this, repository);

        DynamicColors.applyToActivitiesIfAvailable(this,
                new DynamicColorsOptions.Builder()
                        .setPrecondition((activity, provider) -> displayPrefs.isDynamicColorEnabled())
                        .build());

        PrivacyScreenGuard.install(this, displayPrefs);

        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    private static void applyThemeMode(String mode) {
        int nightMode;
        switch (mode) {
            case DisplayPrefs.THEME_LIGHT: nightMode = AppCompatDelegate.MODE_NIGHT_NO; break;
            case DisplayPrefs.THEME_DARK:  nightMode = AppCompatDelegate.MODE_NIGHT_YES; break;
            default:                       nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM; break;
        }
        AppCompatDelegate.setDefaultNightMode(nightMode);
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

    public BackupManager backupManager() {
        return backupManager;
    }

    public DisplayPrefs displayPrefs() {
        return displayPrefs;
    }

    public void setThemeMode(String mode) {
        displayPrefs.setThemeMode(mode);
        applyThemeMode(mode);
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

    public void setAutoLockSuppressed(boolean suppressed) {
        this.autoLockSuppressed = suppressed;
        if (!suppressed && !ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            onStop(ProcessLifecycleOwner.get());
        }
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        mainHandler.removeCallbacks(lockRunnable);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (!keyManager.isUnlocked()) return;
        if (autoLockSuppressed) return;

        long timeout = getLockTimeoutMs();
        if (timeout <= LOCK_IMMEDIATELY) {
            lockNow();
        } else {
            mainHandler.postDelayed(lockRunnable, timeout);
        }
    }
}