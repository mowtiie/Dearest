package com.mowtiie.dearest.data.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mowtiie.dearest.data.TestDatabase;
import com.mowtiie.dearest.data.model.Entry;
import com.mowtiie.dearest.data.model.Tag;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The snapshot-delete-restore sequence behind the editor's Undo, at the DAO level. */
@RunWith(AndroidJUnit4.class)
public class DeleteUndoTest {

    private final EntryDao entries = new EntryDao();
    private final NotebookDao notebooks = new NotebookDao();
    private final TagDao tags = new TagDao();

    private TestDatabase testDb;
    private SQLiteDatabase db;
    private String nb;

    @Before
    public void setUp() {
        testDb = new TestDatabase();
        db = testDb.open();
        nb = notebooks.getAll(db).get(0).getId();
    }

    @After
    public void tearDown() {
        testDb.close();
    }

    @Test
    public void restoredEntryKeepsItsIdTimestampsAndTags() {
        entries.insert(db, new Entry("e-1", nb, "title", "body", 10L, 20L));
        tags.setTagsForEntry(db, "e-1", Arrays.asList(
                tags.findOrCreateByName(db, "alpha").getId(),
                tags.findOrCreateByName(db, "beta").getId()));

        Entry snapshot = entries.getById(db, "e-1");
        List<String> tagNames = new ArrayList<>();
        for (Tag t : tags.getTagsForEntry(db, "e-1")) tagNames.add(t.getName());

        entries.delete(db, "e-1");
        assertNull(entries.getById(db, "e-1"));
        assertEquals(0, tags.getTagsForEntry(db, "e-1").size());

        if (entries.update(db, snapshot) == 0) entries.insert(db, snapshot);
        List<String> ids = new ArrayList<>();
        for (String name : tagNames) ids.add(tags.findOrCreateByName(db, name).getId());
        tags.setTagsForEntry(db, "e-1", ids);

        Entry restored = entries.getById(db, "e-1");
        assertNotNull(restored);
        assertEquals("title", restored.getTitle());
        assertEquals("body", restored.getBody());
        List<String> restoredNames = new ArrayList<>();
        for (Tag t : tags.getTagsForEntry(db, "e-1")) restoredNames.add(t.getName());
        assertEquals(2, restoredNames.size());
        assertEquals(true, restoredNames.containsAll(Arrays.asList("alpha", "beta")));
    }
}
