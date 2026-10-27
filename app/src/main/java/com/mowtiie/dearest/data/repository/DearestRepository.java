package com.mowtiie.dearest.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.backup.BackupData;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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

    // ---- Reads -------------------------------------------------------------------------------

    public LiveData<List<Notebook>> observeNotebooks() {
        return observe(Collections.emptyList(), notebookDao::getAll);
    }

    public LiveData<List<Entry>> observeEntries(String notebookId) {
        return observe(Collections.emptyList(), d -> entryDao.getByNotebook(d, notebookId));
    }

    public LiveData<List<Entry>> observeAllEntries() {
        return observe(Collections.emptyList(), entryDao::getAll);
    }

    public LiveData<Entry> observeEntry(String entryId) {
        return observe(null, d -> entryDao.getById(d, entryId));
    }

    public LiveData<List<Entry>> observeSearch(String rawQuery) {
        return observe(Collections.emptyList(), d -> entryDao.search(d, rawQuery));
    }

    public LiveData<List<Tag>> observeTags() {
        return observe(Collections.emptyList(), tagDao::getAll);
    }

    public LiveData<Map<String, Set<String>>> observeEntryTagLinks() {
        return observe(Collections.emptyMap(), tagDao::getAllEntryTagLinks);
    }

    public void getEntry(String entryId, ResultCallback<Entry> callback) {
        read(null, d -> entryDao.getById(d, entryId), callback);
    }

    public void getTagsForEntry(String entryId, ResultCallback<List<Tag>> callback) {
        read(Collections.emptyList(), d -> tagDao.getTagsForEntry(d, entryId), callback);
    }

    public void countEntries(String notebookId, ResultCallback<Integer> callback) {
        read(0, d -> entryDao.countByNotebook(d, notebookId), callback);
    }

    /** Everything, with full entry bodies, for backup and export. */
    public void loadAll(ResultCallback<BackupData> callback) {
        io.execute(() -> {
            SQLiteDatabase d = db;
            BackupData data = (d == null)
                    ? new BackupData(Collections.emptyList(), Collections.emptyList(),
                            Collections.emptyList(), Collections.emptyMap())
                    : new BackupData(notebookDao.getAll(d), entryDao.getAllFull(d),
                            tagDao.getAll(d), tagDao.getAllEntryTagLinks(d));
            mainHandler.post(() -> callback.onResult(data));
        });
    }

    // ---- Writes ------------------------------------------------------------------------------

    public void importAll(BackupData data, boolean replaceAll, OperationCallback cb) {
        inTransaction(cb, d -> {
            if (replaceAll) {
                // Entry-tag links go with the entries and tags via ON DELETE CASCADE.
                d.delete(DatabaseContract.Entries.TABLE, null, null);
                d.delete(DatabaseContract.Notebooks.TABLE, null, null);
                d.delete(DatabaseContract.Tags.TABLE, null, null);
            }
            for (Notebook n : data.notebooks) {
                if (notebookDao.getById(d, n.getId()) == null) {
                    notebookDao.insert(d, n);
                } else {
                    notebookDao.update(d, n);
                }
            }

            // A backup tag may collide by name with a local one; link entries to whichever survives.
            Map<String, String> tagIdMap = new HashMap<>();
            for (Tag t : data.tags) {
                tagIdMap.put(t.getId(), tagDao.findOrInsert(d, t).getId());
            }

            for (Entry e : data.entries) {
                Entry existing = entryDao.getById(d, e.getId());
                if (existing == null) {
                    entryDao.insert(d, e);
                } else if (e.getUpdatedAt() > existing.getUpdatedAt()) {
                    entryDao.update(d, e);
                } else {
                    continue;
                }
                if (data.includesTags) {
                    List<String> tagIds = new ArrayList<>();
                    Set<String> backupTagIds = data.entryTagIds.get(e.getId());
                    if (backupTagIds != null) {
                        for (String backupTagId : backupTagIds) {
                            String localId = tagIdMap.get(backupTagId);
                            if (localId != null) tagIds.add(localId);
                        }
                    }
                    tagDao.setTagsForEntry(d, e.getId(), tagIds);
                }
            }
        });
    }

    public void saveEntryWithTags(Entry entry, List<String> tagNames, OperationCallback cb) {
        inTransaction(cb, d -> {
            if (entryDao.update(d, entry) == 0) {
                entryDao.insert(d, entry);
            }
            List<String> tagIds = new ArrayList<>();
            for (String name : tagNames) {
                String trimmed = name == null ? "" : name.trim();
                if (trimmed.isEmpty()) continue;
                tagIds.add(tagDao.findOrCreateByName(d, trimmed).getId());
            }
            tagDao.setTagsForEntry(d, entry.getId(), tagIds);
        });
    }

    public void deleteEntry(String entryId) {
        write(null, d -> entryDao.delete(d, entryId));
    }

    public void createTag(String name, OperationCallback cb) {
        write(cb, d -> tagDao.findOrCreateByName(d, name.trim()));
    }

    public void renameTag(String tagId, String newName, OperationCallback cb) {
        write(cb, d -> {
            try {
                tagDao.update(d, new Tag(tagId, newName.trim(), 0));
            } catch (RuntimeException duplicateName) {
                throw new UserFacingException(appContext.getString(R.string.error_tag_name_in_use));
            }
        });
    }

    public void deleteTag(String tagId) {
        write(null, d -> tagDao.delete(d, tagId));
    }

    public void saveNotebook(Notebook notebook) {
        write(null, d -> {
            if (notebookDao.update(d, notebook) == 0) {
                notebookDao.insert(d, notebook);
            }
        });
    }

    public void deleteNotebook(String notebookId, String moveEntriesToId, OperationCallback cb) {
        inTransaction(cb, d -> {
            int entryCount = entryDao.countByNotebook(d, notebookId);
            if (entryCount > 0 && moveEntriesToId == null) {
                throw new UserFacingException(appContext.getResources().getQuantityString(
                        R.plurals.error_notebook_not_empty, entryCount, entryCount));
            }
            if (entryCount > 0) {
                entryDao.moveEntries(d, notebookId, moveEntriesToId);
            }
            notebookDao.delete(d, notebookId);
        });
    }

    // ---- Plumbing ----------------------------------------------------------------------------

    private interface DbWork {
        void run(SQLiteDatabase db) throws Exception;
    }

    /** An error whose message is already localized and safe to show to the user. */
    private static final class UserFacingException extends RuntimeException {
        UserFacingException(String message) {
            super(message);
        }
    }

    /**
     * Runs a write on the IO thread, refreshes observers on success and reports the outcome.
     * A locked vault is reported as a failure rather than silently dropped.
     */
    private void write(OperationCallback cb, DbWork work) {
        io.execute(() -> {
            SQLiteDatabase d = db;
            if (d == null) {
                postResult(cb, false, appContext.getString(R.string.error_vault_locked));
                return;
            }
            try {
                work.run(d);
            } catch (UserFacingException e) {
                postResult(cb, false, e.getMessage());
                return;
            } catch (Exception e) {
                postResult(cb, false, appContext.getString(R.string.error_generic));
                return;
            }
            triggerRefresh();
            postResult(cb, true, null);
        });
    }

    private void inTransaction(OperationCallback cb, DbWork work) {
        write(cb, d -> {
            d.beginTransaction();
            try {
                work.run(d);
                d.setTransactionSuccessful();
            } finally {
                d.endTransaction();
            }
        });
    }

    private <T> LiveData<T> observe(T whenLocked, Function<SQLiteDatabase, T> query) {
        return Transformations.switchMap(refreshTrigger, ignored -> {
            MutableLiveData<T> result = new MutableLiveData<>();
            io.execute(() -> {
                SQLiteDatabase d = db;
                result.postValue(d == null ? whenLocked : query.apply(d));
            });
            return result;
        });
    }

    private <T> void read(T whenLocked, Function<SQLiteDatabase, T> query, ResultCallback<T> callback) {
        io.execute(() -> {
            SQLiteDatabase d = db;
            T value = (d == null) ? whenLocked : query.apply(d);
            mainHandler.post(() -> callback.onResult(value));
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
