package com.mowtiie.dearest.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.mowtiie.dearest.data.dao.EntryDao;
import com.mowtiie.dearest.data.dao.NotebookDao;
import com.mowtiie.dearest.data.dao.TagDao;
import com.mowtiie.dearest.data.db.DatabaseContract;
import com.mowtiie.dearest.data.db.DearestDatabase;
import com.mowtiie.dearest.data.model.Entry;
import com.mowtiie.dearest.data.model.Notebook;
import com.mowtiie.dearest.data.model.Tag;
import com.mowtiie.dearest.security.KeyManager;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
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
    private final TagDao tagDao = new TagDao();
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

    public LiveData<List<Tag>> observeTags() {
        return Transformations.switchMap(refreshTrigger, ignored -> {
            MutableLiveData<List<Tag>> result = new MutableLiveData<>();
            io.execute(() -> result.postValue(
                    db == null ? Collections.emptyList() : tagDao.getAll(db)));
            return result;
        });
    }

    public LiveData<Map<String, Set<String>>> observeEntryTagLinks() {
        return Transformations.switchMap(refreshTrigger, ignored -> {
            MutableLiveData<Map<String, Set<String>>> result = new MutableLiveData<>();
            io.execute(() -> result.postValue(
                    db == null ? Collections.emptyMap() : tagDao.getAllEntryTagLinks(db)));
            return result;
        });
    }

    public void getEntry(String entryId, ResultCallback<Entry> callback) {
        io.execute(() -> {
            Entry entry = (db == null) ? null : entryDao.getById(db, entryId);
            mainHandler.post(() -> callback.onResult(entry));
        });
    }

    public void getTagsForEntry(String entryId, ResultCallback<List<Tag>> callback) {
        io.execute(() -> {
            List<Tag> tags = (db == null) ? Collections.emptyList() : tagDao.getTagsForEntry(db, entryId);
            mainHandler.post(() -> callback.onResult(tags));
        });
    }

    public void countEntries(String notebookId, ResultCallback<Integer> callback) {
        io.execute(() -> {
            int count = (db == null) ? 0 : entryDao.countByNotebook(db, notebookId);
            mainHandler.post(() -> callback.onResult(count));
        });
    }

    public void loadAll(BiConsumer<List<Notebook>, List<Entry>> callback) {
        io.execute(() -> {
            List<Notebook> notebooks = (db == null) ? Collections.emptyList() : notebookDao.getAll(db);
            List<Entry> entries = (db == null) ? Collections.emptyList() : entryDao.getAll(db);
            mainHandler.post(() -> callback.accept(notebooks, entries));
        });
    }

    public void importAll(List<Notebook> notebooks, List<Entry> entries,
                          boolean replaceAll, OperationCallback cb) {
        io.execute(() -> {
            if (db == null) {
                postResult(cb, false, "Database is locked");
                return;
            }
            boolean ok = false;
            String error = null;
            try {
                db.beginTransaction();
                try {
                    if (replaceAll) {
                        db.delete(DatabaseContract.Entries.TABLE, null, null);
                        db.delete(DatabaseContract.Notebooks.TABLE, null, null);
                    }
                    for (Notebook n : notebooks) {
                        if (notebookDao.getById(db, n.getId()) == null) {
                            notebookDao.insert(db, n);
                        } else {
                            notebookDao.update(db, n);
                        }
                    }
                    for (Entry e : entries) {
                        Entry existing = entryDao.getById(db, e.getId());
                        if (existing == null) {
                            entryDao.insert(db, e);
                        } else if (e.getUpdatedAt() > existing.getUpdatedAt()) {
                            entryDao.update(db, e);
                        }
                    }
                    db.setTransactionSuccessful();
                    ok = true;
                } finally {
                    db.endTransaction();
                }
                triggerRefresh();
            } catch (Exception e) {
                error = e.getMessage();
            }
            postResult(cb, ok, error);
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

    public void saveEntryWithTags(Entry entry, List<String> tagNames, OperationCallback cb) {
        io.execute(() -> {
            if (db == null) {
                postResult(cb, false, "Database is locked");
                return;
            }
            boolean ok = false;
            String error = null;
            try {
                db.beginTransaction();
                try {
                    if (entryDao.update(db, entry) == 0) {
                        entryDao.insert(db, entry);
                    }
                    List<String> tagIds = new java.util.ArrayList<>();
                    for (String name : tagNames) {
                        String trimmed = name == null ? "" : name.trim();
                        if (trimmed.isEmpty()) continue;
                        tagIds.add(tagDao.findOrCreateByName(db, trimmed).getId());
                    }
                    tagDao.setTagsForEntry(db, entry.getId(), tagIds);
                    db.setTransactionSuccessful();
                    ok = true;
                } finally {
                    db.endTransaction();
                }
                triggerRefresh();
            } catch (Exception e) {
                error = e.getMessage();
            }
            postResult(cb, ok, error);
        });
    }

    public void renameTag(String tagId, String newName, OperationCallback cb) {
        io.execute(() -> {
            boolean ok = false;
            String error = null;
            if (db == null) {
                error = "Database is locked";
            } else {
                try {
                    tagDao.update(db, new Tag(tagId, newName.trim(), 0));
                    triggerRefresh();
                    ok = true;
                } catch (Exception e) {
                    error = "That tag name is already in use";
                }
            }
            postResult(cb, ok, error);
        });
    }

    public void deleteTag(String tagId) {
        io.execute(() -> {
            if (db == null) return;
            tagDao.delete(db, tagId);
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