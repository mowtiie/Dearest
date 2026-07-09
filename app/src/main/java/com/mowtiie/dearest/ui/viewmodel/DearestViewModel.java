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

    protected DearestViewModel(@NonNull Application application) {
        super(application);
    }

    protected DearestApp app() {
        return (DearestApp) getApplication();
    }

    protected KeyManager keyManager() {
        return app().keyManager();
    }

    protected DearestRepository repository() {
        return app().repository();
    }

    @Override
    protected void onCleared() {
        io.shutdownNow();
    }
}
