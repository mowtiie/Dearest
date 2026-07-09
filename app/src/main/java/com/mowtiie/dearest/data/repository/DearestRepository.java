package com.mowtiie.dearest.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.mowtiie.dearest.data.dao.EntryDao;
import com.mowtiie.dearest.data.dao.NotebookDao;
import com.mowtiie.dearest.data.db.DearestDatabase;
import com.mowtiie.dearest.data.model.Entry;
import com.mowtiie.dearest.data.model.Notebook;
import com.mowtiie.dearest.security.KeyManager;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DearestRepository {

    public interface OperationCallback {
        void onComplete(boolean success, String errorMessage);
    }

    public interface ResultCallback<T> {
        void onResult(T value);
    }

    private static volatile DearestRepository instance;

    private final Context appContext;
    private final KeyManager keyManager;
    private final EntryDao entryDao = new EntryDao();
    private final NotebookDao notebookDao = new NotebookDao();
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final MutableLiveData<Object> refreshTrigger = new MutableLiveData<>();

    private volatile SQLiteDatabase db;
    private DearestDatabase dbHelper;

    private DearestRepository(Context context, KeyManager keyManager) {
        this.appContext = context.getApplicationContext();
        this.keyManager = keyManager;
        triggerRefresh();
    }

    public static DearestRepository getInstance(Context context, KeyManager keyManager) {
        if (instance == null) {
            synchronized (DearestRepository.class) {
                if (instance == null) {
                    instance = new DearestRepository(context, keyManager);
                }
            }
        }
        return instance;
    }

    public void open() {
        io.execute(() -> {
            if (db != null || !keyManager.isUnlocked()) return;
            dbHelper = new DearestDatabase(appContext, keyManager.getDatabaseKey());
            db = dbHelper.getWritableDatabase();
            triggerRefresh();
        });
    }

    public void close() {
        io.execute(() -> {
            if (db != null) {
                db.close();
                db = null;
            }
            dbHelper = null;
            triggerRefresh();
        });
    }

    public boolean isOpen() {
        return db != null;
    }

    public LiveData<List<Notebook>> observeNotebooks() {
        return Transformations.switchMap(refreshTrigger, ignored -> {
            MutableLiveData<List<Notebook>> result = new MutableLiveData<>();
            io.execute(() -> result.postValue(
                    db == null ? Collections.emptyList() : notebookDao.getAll(db)));
            return result;
        });
    }

    public LiveData<List<Entry>> observeEntries(String notebookId) {
        return Transformations.switchMap(refreshTrigger, ignored -> {
            MutableLiveData<List<Entry>> result = new MutableLiveData<>();
            io.execute(() -> result.postValue(
                    db == null ? Collections.emptyList() : entryDao.getByNotebook(db, notebookId)));
            return result;
        });
    }

    public LiveData<List<Entry>> observeAllEntries() {
        return Transformations.switchMap(refreshTrigger, ignored -> {
            MutableLiveData<List<Entry>> result = new MutableLiveData<>();
            io.execute(() -> result.postValue(
                    db == null ? Collections.emptyList() : entryDao.getAll(db)));
            return result;
        });
    }

    public LiveData<Entry> observeEntry(String entryId) {
        return Transformations.switchMap(refreshTrigger, ignored -> {
            MutableLiveData<Entry> result = new MutableLiveData<>();
            io.execute(() -> result.postValue(
                    db == null ? null : entryDao.getById(db, entryId)));
            return result;
        });
    }

    public LiveData<List<Entry>> observeSearch(String rawQuery) {
        return Transformations.switchMap(refreshTrigger, ignored -> {
            MutableLiveData<List<Entry>> result = new MutableLiveData<>();
            io.execute(() -> result.postValue(
                    db == null ? Collections.emptyList() : entryDao.search(db, rawQuery)));
            return result;
        });
    }

    public void getEntry(String entryId, ResultCallback<Entry> callback) {
        io.execute(() -> {
            Entry entry = (db == null) ? null : entryDao.getById(db, entryId);
            mainHandler.post(() -> callback.onResult(entry));
        });
    }

    public void countEntries(String notebookId, ResultCallback<Integer> callback) {
        io.execute(() -> {
            int count = (db == null) ? 0 : entryDao.countByNotebook(db, notebookId);
            mainHandler.post(() -> callback.onResult(count));
        });
    }

    public void saveEntry(Entry entry) {
        io.execute(() -> {
            if (db == null) return;
            if (entryDao.update(db, entry) == 0) {
                entryDao.insert(db, entry);
            }
            triggerRefresh();
        });
    }

    public void deleteEntry(String entryId) {
        io.execute(() -> {
            if (db == null) return;
            entryDao.delete(db, entryId);
            triggerRefresh();
        });
    }

    public void saveNotebook(Notebook notebook) {
        io.execute(() -> {
            if (db == null) return;
            if (notebookDao.update(db, notebook) == 0) {
                notebookDao.insert(db, notebook);
            }
            triggerRefresh();
        });
    }

    public void deleteNotebook(String notebookId, String moveEntriesToId, OperationCallback cb) {
        io.execute(() -> {
            boolean success = false;
            String error = null;
            try {
                if (db == null) {
                    error = "Database is locked";
                } else {
                    int entryCount = entryDao.countByNotebook(db, notebookId);
                    if (entryCount > 0 && moveEntriesToId == null) {
                        error = "Notebook still contains " + entryCount + " entr"
                                + (entryCount == 1 ? "y" : "ies");
                    } else {
                        if (entryCount > 0) {
                            entryDao.moveEntries(db, notebookId, moveEntriesToId);
                        }
                        notebookDao.delete(db, notebookId);
                        triggerRefresh();
                        success = true;
                    }
                }
            } catch (Exception e) {
                error = e.getMessage();
            }
            postResult(cb, success, error);
        });
    }

    private void triggerRefresh() {
        refreshTrigger.postValue(new Object());
    }

    private void postResult(OperationCallback cb, boolean success, String error) {
        if (cb != null) {
            mainHandler.post(() -> cb.onComplete(success, error));
        }
    }
}