package com.mowtiie.dearest.ui.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.mowtiie.dearest.DearestApp;
import com.mowtiie.dearest.data.repository.DearestRepository;
import com.mowtiie.dearest.security.KeyManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class DearestViewModel extends AndroidViewModel {

    protected final ExecutorService io = Executors.newSingleThreadExecutor();
    protected final Handler main = new Handler(Looper.getMainLooper());

    private final DearestRepository repository;

    protected DearestViewModel(@NonNull Application application) {
        this(application, null);
    }

    /** For tests: use the given repository instead of the application's. */
    protected DearestViewModel(@NonNull Application application, DearestRepository repository) {
        super(application);
        this.repository = repository;
    }

    protected DearestApp app() {
        return (DearestApp) getApplication();
    }

    protected KeyManager keyManager() {
        return app().keyManager();
    }

    protected DearestRepository repository() {
        return repository != null ? repository : app().repository();
    }

    @Override
    protected void onCleared() {
        io.shutdownNow();
    }
}
