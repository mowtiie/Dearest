package com.mowtiie.dearest.data.repository;

import com.mowtiie.dearest.backup.BackupData;
import com.mowtiie.dearest.data.dao.EntryDao;
import com.mowtiie.dearest.data.dao.NotebookDao;
import com.mowtiie.dearest.data.dao.TagDao;
import com.mowtiie.dearest.data.db.DatabaseContract;
import com.mowtiie.dearest.data.model.Entry;
import com.mowtiie.dearest.data.model.Notebook;
import com.mowtiie.dearest.data.model.Tag;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Merges a backup into an open database. Call inside a transaction. */
final class BackupImporter {

    private final EntryDao entryDao = new EntryDao();
    private final NotebookDao notebookDao = new NotebookDao();
    private final TagDao tagDao = new TagDao();

    /**
     * With {@code replaceAll} everything is wiped first. Otherwise notebooks and entries are merged
     * by id, the newer entry wins, and an entry kept because the local copy is newer also keeps its
     * local tags. Backups without tag data never touch existing tag assignments.
     */
    void apply(SQLiteDatabase d, BackupData data, boolean replaceAll) {
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
    }
}
