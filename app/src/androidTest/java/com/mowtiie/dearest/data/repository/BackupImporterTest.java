package com.mowtiie.dearest.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mowtiie.dearest.backup.BackupData;
import com.mowtiie.dearest.data.TestDatabase;
import com.mowtiie.dearest.data.dao.EntryDao;
import com.mowtiie.dearest.data.dao.NotebookDao;
import com.mowtiie.dearest.data.dao.TagDao;
import com.mowtiie.dearest.data.model.Entry;
import com.mowtiie.dearest.data.model.Notebook;
import com.mowtiie.dearest.data.model.Tag;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class BackupImporterTest {

    private final EntryDao entries = new EntryDao();
    private final NotebookDao notebooks = new NotebookDao();
    private final TagDao tags = new TagDao();
    private final BackupImporter importer = new BackupImporter();

    private TestDatabase testDb;
    private SQLiteDatabase db;

    @Before
    public void setUp() {
        testDb = new TestDatabase();
        db = testDb.open();
    }

    @After
    public void tearDown() {
        testDb.close();
    }

    private static Set<String> set(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    private static Map<String, Set<String>> links(String entryId, String... tagIds) {
        Map<String, Set<String>> map = new HashMap<>();
        map.put(entryId, set(tagIds));
        return map;
    }

    private Set<String> tagNamesOf(String entryId) {
        Set<String> names = new HashSet<>();
        for (Tag t : tags.getTagsForEntry(db, entryId)) names.add(t.getName());
        return names;
    }

    private void apply(BackupData data, boolean replaceAll) {
        db.beginTransaction();
        try {
            importer.apply(db, data, replaceAll);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Test
    public void importsNotebooksEntriesAndTagAssignments() {
        Notebook nb = new Notebook("nb-9", "Imported", "desc", 5, 10L);
        Entry e = new Entry("e-1", "nb-9", "hello", "world", 1L, 1L);
        List<Tag> backupTags = Arrays.asList(new Tag("t-1", "work", 5L), new Tag("t-2", "ideas", 6L));

        apply(new BackupData(Collections.singletonList(nb), Collections.singletonList(e),
                backupTags, links("e-1", "t-1", "t-2")), false);

        assertEquals("desc", notebooks.getById(db, "nb-9").getDescription());
        assertEquals("world", entries.getById(db, "e-1").getBody());
        assertEquals(set("work", "ideas"), tagNamesOf("e-1"));
    }

    @Test
    public void backupTagWithSameNameAsLocalTagLinksToTheLocalOne() {
        Tag local = tags.findOrCreateByName(db, "Work");
        String nb = notebooks.getAll(db).get(0).getId();
        Entry e = new Entry("e-1", nb, "t", "b", 1L, 1L);

        apply(new BackupData(Collections.emptyList(), Collections.singletonList(e),
                Collections.singletonList(new Tag("backup-id", "work", 1L)),
                links("e-1", "backup-id")), false);

        assertEquals("no duplicate tag is created", 1, tags.count(db));
        List<Tag> linked = tags.getTagsForEntry(db, "e-1");
        assertEquals(1, linked.size());
        assertEquals(local.getId(), linked.get(0).getId());
    }

    @Test
    public void newerBackupEntryReplacesLocalAndItsTags() {
        String nb = notebooks.getAll(db).get(0).getId();
        Tag old = tags.findOrCreateByName(db, "old");
        entries.insert(db, new Entry("e-1", nb, "local", "local body", 1L, 10L));
        tags.setTagsForEntry(db, "e-1", Collections.singletonList(old.getId()));

        apply(new BackupData(Collections.emptyList(),
                Collections.singletonList(new Entry("e-1", nb, "from backup", "new body", 1L, 20L)),
                Collections.singletonList(new Tag("t-new", "fresh", 1L)),
                links("e-1", "t-new")), false);

        assertEquals("from backup", entries.getById(db, "e-1").getTitle());
        assertEquals(set("fresh"), tagNamesOf("e-1"));
    }

    @Test
    public void olderBackupEntryKeepsTheLocalEntryAndItsTags() {
        String nb = notebooks.getAll(db).get(0).getId();
        Tag mine = tags.findOrCreateByName(db, "mine");
        entries.insert(db, new Entry("e-1", nb, "local", "local body", 1L, 50L));
        tags.setTagsForEntry(db, "e-1", Collections.singletonList(mine.getId()));

        apply(new BackupData(Collections.emptyList(),
                Collections.singletonList(new Entry("e-1", nb, "stale", "stale body", 1L, 20L)),
                Collections.singletonList(new Tag("t-x", "other", 1L)),
                links("e-1", "t-x")), false);

        assertEquals("local", entries.getById(db, "e-1").getTitle());
        assertEquals(set("mine"), tagNamesOf("e-1"));
    }

    @Test
    public void backupWithoutTagDataLeavesExistingTagAssignmentsAlone() {
        String nb = notebooks.getAll(db).get(0).getId();
        Tag keep = tags.findOrCreateByName(db, "keep");
        entries.insert(db, new Entry("e-1", nb, "local", "body", 1L, 10L));
        tags.setTagsForEntry(db, "e-1", Collections.singletonList(keep.getId()));

        // Two-argument constructor = a backup written before tags were supported.
        apply(new BackupData(Collections.emptyList(),
                Collections.singletonList(new Entry("e-1", nb, "updated", "body", 1L, 99L))), false);

        assertEquals("updated", entries.getById(db, "e-1").getTitle());
        assertEquals(set("keep"), tagNamesOf("e-1"));
    }

    @Test
    public void replaceAllWipesEntriesNotebooksAndTagsFirst() {
        String nb = notebooks.getAll(db).get(0).getId();
        Tag gone = tags.findOrCreateByName(db, "gone");
        entries.insert(db, new Entry("old-entry", nb, "t", "b", 1L, 1L));
        tags.setTagsForEntry(db, "old-entry", Collections.singletonList(gone.getId()));

        Notebook fresh = new Notebook("nb-new", "Fresh", null, 0, 1L);
        Entry e = new Entry("e-new", "nb-new", "t", "b", 1L, 1L);
        apply(new BackupData(Collections.singletonList(fresh), Collections.singletonList(e),
                Collections.singletonList(new Tag("t-kept", "kept", 1L)),
                links("e-new", "t-kept")), true);

        assertNull(entries.getById(db, "old-entry"));
        assertNull(notebooks.getById(db, nb));
        assertEquals(1, notebooks.getAll(db).size());
        assertEquals(1, tags.count(db));
        assertEquals(set("kept"), tagNamesOf("e-new"));
        assertNotNull(entries.getById(db, "e-new"));
    }

    @Test
    public void importedEntriesAreSearchable() {
        Notebook nb = new Notebook("nb-9", "Imported", null, 0, 1L);
        Entry e = new Entry("e-1", "nb-9", "Lighthouse", "keeper of the light", 1L, 1L);
        apply(new BackupData(Collections.singletonList(nb), Collections.singletonList(e)), false);

        assertEquals(1, entries.search(db, "lighthouse").size());
        assertTrue(entries.search(db, "submarine").isEmpty());
    }
}
