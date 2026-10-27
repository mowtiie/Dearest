package com.mowtiie.dearest.data.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mowtiie.dearest.data.TestDatabase;
import com.mowtiie.dearest.data.dao.EntryDao;
import com.mowtiie.dearest.data.dao.NotebookDao;
import com.mowtiie.dearest.data.dao.TagDao;
import com.mowtiie.dearest.data.model.Entry;
import com.mowtiie.dearest.data.model.Notebook;

import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteOpenHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class DearestDatabaseTest {

    private final EntryDao entries = new EntryDao();
    private final NotebookDao notebooks = new NotebookDao();
    private final TagDao tags = new TagDao();

    private TestDatabase testDb;

    @Before
    public void setUp() {
        testDb = new TestDatabase();
    }

    @After
    public void tearDown() {
        testDb.close();
    }

    @Test
    public void freshDatabaseSeedsOneDefaultNotebook() {
        SQLiteDatabase db = testDb.open();
        List<Notebook> all = notebooks.getAll(db);
        assertEquals(1, all.size());
        assertEquals("My Journal", all.get(0).getName());
    }

    @Test
    public void foreignKeysAreEnforced() {
        SQLiteDatabase db = testDb.open();
        try {
            entries.insert(db, new Entry("e-1", "no-such-notebook", "t", "b", 1L, 1L));
            fail("an entry must not point at a missing notebook");
        } catch (RuntimeException expected) {
            // constraint failure
        }
    }

    @Test
    public void searchIndexFollowsInsertUpdateAndDelete() {
        SQLiteDatabase db = testDb.open();
        String nb = notebooks.getAll(db).get(0).getId();

        entries.insert(db, new Entry("e-1", nb, "Holiday", "walked along the harbour", 1L, 1L));
        assertEquals(1, entries.search(db, "harb").size());

        entries.update(db, new Entry("e-1", nb, "Holiday", "stayed home", 1L, 2L));
        assertTrue(entries.search(db, "harbour").isEmpty());
        assertEquals(1, entries.search(db, "home").size());

        entries.delete(db, "e-1");
        assertTrue(entries.search(db, "home").isEmpty());
    }

    @Test
    public void reopeningWithTheSameKeyKeepsTheData() {
        SQLiteDatabase db = testDb.open();
        String nb = notebooks.getAll(db).get(0).getId();
        entries.insert(db, new Entry("e-1", nb, "kept", "body", 1L, 1L));

        db = testDb.open();
        assertNotNull(entries.getById(db, "e-1"));
    }

    @Test
    public void wrongKeyCannotOpenTheDatabase() {
        SQLiteDatabase db = testDb.open();
        testDb.closeConnection();
        byte[] wrong = new byte[32];
        try {
            new DearestDatabase(testDb.context, testDb.name, wrong).getWritableDatabase();
            fail("opening with the wrong key must fail");
        } catch (RuntimeException expected) {
            // not a database / bad key
        }
    }

    @Test
    public void upgradeFromVersion1KeepsDataAndAddsTagsAndDescription() {
        createLegacyDatabase(1);

        SQLiteDatabase db = testDb.open();

        Entry kept = entries.getById(db, "e-1");
        assertNotNull(kept);
        assertEquals("old entry", kept.getTitle());
        assertEquals(1, entries.search(db, "legacy").size());

        Notebook nb = notebooks.getById(db, "nb-1");
        assertNotNull(nb);
        assertNull(nb.getDescription());
        nb.setDescription("now with a description");
        notebooks.update(db, nb);
        assertEquals("now with a description", notebooks.getById(db, "nb-1").getDescription());

        assertEquals(0, tags.count(db));
        tags.findOrCreateByName(db, "migrated");
        assertEquals(1, tags.count(db));
    }

    @Test
    public void upgradeFromVersion2OnlyAddsTheDescriptionColumn() {
        createLegacyDatabase(2);

        SQLiteDatabase db = testDb.open();

        assertNotNull(entries.getById(db, "e-1"));
        Notebook nb = notebooks.getById(db, "nb-1");
        nb.setDescription("added in v3");
        notebooks.update(db, nb);
        assertEquals("added in v3", notebooks.getById(db, "nb-1").getDescription());
    }

    /**
     * Builds the schema as earlier releases created it (v1: no tags and no notebook description;
     * v2: tags added) with one notebook and one entry, then closes it for the upgrade under test.
     */
    private void createLegacyDatabase(int version) {
        SQLiteOpenHelper legacy = new SQLiteOpenHelper(testDb.context, testDb.name, testDb.key,
                null, version, 0, null, null, true) {
            @Override
            public void onConfigure(SQLiteDatabase db) {
                db.setForeignKeyConstraintsEnabled(true);
            }

            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE notebooks (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "uuid TEXT NOT NULL UNIQUE, name TEXT NOT NULL, "
                        + "position INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL);");
                db.execSQL("CREATE TABLE entries (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "uuid TEXT NOT NULL UNIQUE, notebook_uuid TEXT NOT NULL, title TEXT, body TEXT, "
                        + "created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, "
                        + "FOREIGN KEY (notebook_uuid) REFERENCES notebooks(uuid));");
                db.execSQL("CREATE VIRTUAL TABLE entries_fts USING fts5(title, body);");
                db.execSQL("CREATE TRIGGER entries_ai AFTER INSERT ON entries BEGIN "
                        + "INSERT INTO entries_fts(rowid, title, body) VALUES (new._id, new.title, new.body); END;");
                db.execSQL("CREATE TRIGGER entries_ad AFTER DELETE ON entries BEGIN "
                        + "DELETE FROM entries_fts WHERE rowid = old._id; END;");
                db.execSQL("CREATE TRIGGER entries_au AFTER UPDATE ON entries BEGIN "
                        + "UPDATE entries_fts SET title = new.title, body = new.body WHERE rowid = new._id; END;");
                if (version >= 2) {
                    db.execSQL("CREATE TABLE tags (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + "uuid TEXT NOT NULL UNIQUE, name TEXT NOT NULL UNIQUE COLLATE NOCASE, "
                            + "created_at INTEGER NOT NULL);");
                    db.execSQL("CREATE TABLE entry_tags (entry_uuid TEXT NOT NULL, tag_uuid TEXT NOT NULL, "
                            + "PRIMARY KEY (entry_uuid, tag_uuid), "
                            + "FOREIGN KEY (entry_uuid) REFERENCES entries(uuid) ON DELETE CASCADE, "
                            + "FOREIGN KEY (tag_uuid) REFERENCES tags(uuid) ON DELETE CASCADE);");
                }
                db.execSQL("INSERT INTO notebooks (uuid, name, position, created_at) "
                        + "VALUES ('nb-1', 'Old notebook', 0, 1);");
                db.execSQL("INSERT INTO entries (uuid, notebook_uuid, title, body, created_at, updated_at) "
                        + "VALUES ('e-1', 'nb-1', 'old entry', 'a legacy body', 1, 1);");
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                // never upgraded: this helper only creates the old schema
            }
        };
        legacy.getWritableDatabase().close();
    }
}
