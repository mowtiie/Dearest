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
    private final BackupImporter backupImporter = new BackupImporter();
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    // One refresh signal per group of data, so a write only reloads the lists it can affect.
    private static final int ENTRIES = 1;
    private static final int NOTEBOOKS = 1 << 1;
    private static final int TAGS = 1 << 2;
    private static final int LINKS = 1 << 3;
    private static final int ALL = ENTRIES | NOTEBOOKS | TAGS | LINKS;

    private final MutableLiveData<Object> entriesTrigger = new MutableLiveData<>();
    private final MutableLiveData<Object> notebooksTrigger = new MutableLiveData<>();
    private final MutableLiveData<Object> tagsTrigger = new MutableLiveData<>();
    private final MutableLiveData<Object> linksTrigger = new MutableLiveData<>();

    private volatile SQLiteDatabase db;
    private DearestDatabase dbHelper;

    private DearestRepository(Context context, KeyManager keyManager) {
        this.appContext = context.getApplicationContext();
        this.keyManager = keyManager;
        invalidate(ALL);
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
            invalidate(ALL);
        });
    }

    public void close() {
        io.execute(() -> {
            if (db != null) {
                db.close();
                db = null;
            }
            dbHelper = null;
            invalidate(ALL);
        });
    }

    public boolean isOpen() {
        return db != null;
    }

    // ---- Reads -------------------------------------------------------------------------------

    public LiveData<List<Notebook>> observeNotebooks() {
        return observe(NOTEBOOKS, Collections.emptyList(), notebookDao::getAll);
    }

    public LiveData<List<Entry>> observeEntries(String notebookId) {
        return observe(ENTRIES, Collections.emptyList(), d -> entryDao.getByNotebook(d, notebookId));
    }

    public LiveData<List<Entry>> observeAllEntries() {
        return observe(ENTRIES, Collections.emptyList(), entryDao::getAll);
    }

    public LiveData<List<Long>> observeCreatedAtBetween(long from, long to) {
        return observe(ENTRIES, Collections.emptyList(), d -> entryDao.getCreatedAtBetween(d, from, to));
    }

    public LiveData<List<Entry>> observeEntriesCreatedBetween(long from, long to) {
        return observe(ENTRIES, Collections.emptyList(), d -> entryDao.getCreatedBetween(d, from, to));
    }

    public LiveData<Entry> observeEntry(String entryId) {
        return observe(ENTRIES, null, d -> entryDao.getById(d, entryId));
    }

    public LiveData<List<Entry>> observeSearch(String rawQuery) {
        return observe(ENTRIES, Collections.emptyList(), d -> entryDao.search(d, rawQuery));
    }

    public LiveData<List<Tag>> observeTags() {
        return observe(TAGS, Collections.emptyList(), tagDao::getAll);
    }

    public LiveData<Map<String, Set<String>>> observeEntryTagLinks() {
        return observe(LINKS, Collections.emptyMap(), tagDao::getAllEntryTagLinks);
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
        inTransaction(cb, ALL, d -> backupImporter.apply(d, data, replaceAll));
    }

    public void saveEntryWithTags(Entry entry, List<String> tagNames, OperationCallback cb) {
        inTransaction(cb, ENTRIES | LINKS, d -> {
            int tagsBefore = tagDao.count(d);
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
            if (tagDao.count(d) != tagsBefore) invalidate(TAGS);
        });
    }

    public void deleteEntry(String entryId) {
        write(null, ENTRIES | LINKS, d -> entryDao.delete(d, entryId));
    }

    public void createTag(String name, OperationCallback cb) {
        write(cb, TAGS, d -> tagDao.findOrCreateByName(d, name.trim()));
    }

    public void renameTag(String tagId, String newName, OperationCallback cb) {
        write(cb, TAGS, d -> {
            try {
                tagDao.update(d, new Tag(tagId, newName.trim(), 0));
            } catch (RuntimeException duplicateName) {
                throw new UserFacingException(appContext.getString(R.string.error_tag_name_in_use));
            }
        });
    }

    public void deleteTag(String tagId) {
        write(null, TAGS | LINKS, d -> tagDao.delete(d, tagId));
    }

    public void saveNotebook(Notebook notebook) {
        write(null, NOTEBOOKS, d -> {
            if (notebookDao.update(d, notebook) == 0) {
                notebookDao.insert(d, notebook);
            }
        });
    }

    public void deleteNotebook(String notebookId, String moveEntriesToId, OperationCallback cb) {
        inTransaction(cb, NOTEBOOKS | ENTRIES, d -> {
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
    private void write(OperationCallback cb, int tables, DbWork work) {
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
            invalidate(tables);
            postResult(cb, true, null);
        });
    }

    private void inTransaction(OperationCallback cb, int tables, DbWork work) {
        write(cb, tables, d -> {
            d.beginTransaction();
            try {
                work.run(d);
                d.setTransactionSuccessful();
            } finally {
                d.endTransaction();
            }
        });
    }

    private <T> LiveData<T> observe(int table, T whenLocked, Function<SQLiteDatabase, T> query) {
        return Transformations.switchMap(trigger(table), ignored -> {
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

    private LiveData<Object> trigger(int table) {
        switch (table) {
            case NOTEBOOKS: return notebooksTrigger;
            case TAGS:      return tagsTrigger;
            case LINKS:     return linksTrigger;
            default:        return entriesTrigger;
        }
    }

    /** Tells observers of the given data groups to reload. Safe from any thread. */
    private void invalidate(int tables) {
        if ((tables & ENTRIES) != 0) entriesTrigger.postValue(new Object());
        if ((tables & NOTEBOOKS) != 0) notebooksTrigger.postValue(new Object());
        if ((tables & TAGS) != 0) tagsTrigger.postValue(new Object());
        if ((tables & LINKS) != 0) linksTrigger.postValue(new Object());
    }

    private void postResult(OperationCallback cb, boolean success, String error) {
        if (cb != null) {
            mainHandler.post(() -> cb.onComplete(success, error));
        }
    }
}
