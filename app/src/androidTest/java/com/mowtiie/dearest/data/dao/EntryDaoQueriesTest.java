package com.mowtiie.dearest.data.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mowtiie.dearest.data.TestDatabase;
import com.mowtiie.dearest.data.model.Entry;
import com.mowtiie.dearest.data.model.Notebook;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class EntryDaoQueriesTest {

    private final EntryDao entries = new EntryDao();
    private final NotebookDao notebooks = new NotebookDao();

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
    public void listQueriesReturnSnippetsButFullQueriesReturnTheWholeBody() {
        char[] chars = new char[EntryDao.SNIPPET_LENGTH + 500];
        Arrays.fill(chars, 'x');
        String longBody = new String(chars);
        entries.insert(db, new Entry("e-1", nb, "long", longBody, 1L, 1L));

        assertEquals(EntryDao.SNIPPET_LENGTH, entries.getAll(db).get(0).getBody().length());
        assertEquals(EntryDao.SNIPPET_LENGTH, entries.getByNotebook(db, nb).get(0).getBody().length());
        assertEquals(EntryDao.SNIPPET_LENGTH, entries.search(db, "long").get(0).getBody().length());
        assertEquals(longBody.length(), entries.getAllFull(db).get(0).getBody().length());
        assertEquals(longBody, entries.getById(db, "e-1").getBody());
    }

    @Test
    public void shortBodiesAreNotChanged() {
        entries.insert(db, new Entry("e-1", nb, "t", "short", 1L, 1L));
        assertEquals("short", entries.getAll(db).get(0).getBody());
    }

    @Test
    public void dateRangeIsStartInclusiveEndExclusive() {
        entries.insert(db, new Entry("before", nb, "a", "b", 99L, 99L));
        entries.insert(db, new Entry("start", nb, "a", "b", 100L, 100L));
        entries.insert(db, new Entry("inside", nb, "a", "b", 150L, 150L));
        entries.insert(db, new Entry("end", nb, "a", "b", 200L, 200L));

        List<Entry> inRange = entries.getCreatedBetween(db, 100L, 200L);
        assertEquals(2, inRange.size());
        assertEquals("inside", inRange.get(0).getId());
        assertEquals("start", inRange.get(1).getId());

        List<Long> times = entries.getCreatedAtBetween(db, 100L, 200L);
        assertTrue(times.contains(100L) && times.contains(150L) && times.size() == 2);
    }

    @Test
    public void searchMatchesPrefixesAcrossTitleAndBody() {
        entries.insert(db, new Entry("e-1", nb, "Garden notes", "planted tomatoes", 1L, 1L));
        entries.insert(db, new Entry("e-2", nb, "Work", "meeting about gardening", 2L, 2L));
        entries.insert(db, new Entry("e-3", nb, "Other", "nothing relevant", 3L, 3L));

        assertEquals(2, entries.search(db, "garden").size());
        assertEquals(1, entries.search(db, "tomat").size());
        assertTrue(entries.search(db, "relevantly").isEmpty());
    }

    @Test
    public void searchTreatsFtsSyntaxAsPlainText() {
        entries.insert(db, new Entry("e-1", nb, "t", "plain words", 1L, 1L));
        // These would be query-syntax errors if passed to MATCH unescaped.
        assertTrue(entries.search(db, "\"unbalanced").isEmpty());
        assertTrue(entries.search(db, "OR AND NOT (").isEmpty());
        assertEquals(1, entries.search(db, "plain*").size());
    }

    @Test
    public void movingEntriesRetargetsOnlyTheSourceNotebook() {
        Notebook other = Notebook.createNew("Other", null, 1);
        notebooks.insert(db, other);
        entries.insert(db, new Entry("e-1", nb, "t", "b", 1L, 1L));
        entries.insert(db, new Entry("e-2", other.getId(), "t", "b", 2L, 2L));

        Notebook third = Notebook.createNew("Third", null, 2);
        notebooks.insert(db, third);
        assertEquals(1, entries.moveEntries(db, nb, third.getId()));

        assertEquals(0, entries.countByNotebook(db, nb));
        assertEquals(1, entries.countByNotebook(db, third.getId()));
        assertEquals(1, entries.countByNotebook(db, other.getId()));
    }
}
