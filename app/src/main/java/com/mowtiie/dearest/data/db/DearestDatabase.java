package com.mowtiie.dearest.data.db;

import android.content.Context;

import com.mowtiie.dearest.R;

import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteOpenHelper;

import java.util.UUID;

public class DearestDatabase extends SQLiteOpenHelper {

    public static final String DB_NAME    = "dearest.db";
    public static final int    DB_VERSION = 4;

    private static boolean sLibraryLoaded = false;

    private final Context appContext;

    public DearestDatabase(Context context, byte[] databaseKey) {
        this(context, DB_NAME, databaseKey);
    }

    /** For tests, which must never open the real database file. */
    public DearestDatabase(Context context, String name, byte[] databaseKey) {
        super(context.getApplicationContext(), name, databaseKey, null, DB_VERSION,
                0, null, null, true);
        this.appContext = context.getApplicationContext();
    }

    public static synchronized void loadLibrary() {
        if (!sLibraryLoaded) {
            System.loadLibrary("sqlcipher");
            sLibraryLoaded = true;
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_NOTEBOOKS);
        db.execSQL(CREATE_ENTRIES);
        db.execSQL(INDEX_ENTRIES_NOTEBOOK);
        db.execSQL(INDEX_ENTRIES_CREATED);
        db.execSQL(INDEX_ENTRIES_UPDATED);

        db.execSQL(CREATE_FTS);
        db.execSQL(TRIGGER_AFTER_INSERT);
        db.execSQL(TRIGGER_AFTER_DELETE);
        db.execSQL(TRIGGER_AFTER_UPDATE);

        createTagTables(db);

        seedDefaultNotebook(db);
        createSectionTables(db);
        seedDefaultSections(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createTagTables(db);
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + DatabaseContract.Notebooks.TABLE +
                    " ADD COLUMN " + DatabaseContract.Notebooks.COL_DESCRIPTION + " TEXT;");
        }
        if (oldVersion < 4) {
            db.execSQL(CREATE_SECTIONS);
            db.execSQL(INDEX_SECTIONS_NOTEBOOK);
            db.execSQL("ALTER TABLE " + DatabaseContract.Entries.TABLE + " ADD COLUMN " +
                    DatabaseContract.Entries.COL_SECTION + " TEXT;");
            db.execSQL("CREATE INDEX idx_entries_section ON " + DatabaseContract.Entries.TABLE +
                    "(" + DatabaseContract.Entries.COL_SECTION + ");");
            seedDefaultSections(db);
        }
    }

    private void createTagTables(SQLiteDatabase db) {
        db.execSQL(CREATE_TAGS);
        db.execSQL(INDEX_TAGS_NAME);
        db.execSQL(CREATE_ENTRY_TAGS);
        db.execSQL(INDEX_ENTRY_TAGS_TAG);
    }

    private void createSectionTables(SQLiteDatabase db) {
        db.execSQL(CREATE_SECTIONS);
        db.execSQL(INDEX_SECTIONS_NOTEBOOK);
        db.execSQL("CREATE INDEX idx_entries_section ON " + DatabaseContract.Entries.TABLE +
                "(" + DatabaseContract.Entries.COL_SECTION + ");");
    }

    private void seedDefaultSections(SQLiteDatabase db) {
        try (android.database.Cursor c = db.query(DatabaseContract.Notebooks.TABLE,
                new String[]{DatabaseContract.Notebooks.COL_UUID}, null, null, null, null, null)) {
            while (c.moveToNext()) {
                String notebookId = c.getString(0);
                String sectionId;
                try (android.database.Cursor s = db.query(DatabaseContract.Sections.TABLE,
                        new String[]{DatabaseContract.Sections.COL_UUID},
                        DatabaseContract.Sections.COL_NOTEBOOK + " = ?",
                        new String[]{notebookId}, null, null, null)) {
                    if (s.moveToFirst()) sectionId = s.getString(0);
                    else {
                        sectionId = UUID.randomUUID().toString();
                        db.execSQL("INSERT INTO sections (uuid, notebook_uuid, name, position, created_at) VALUES (?, ?, ?, 0, ?)",
                                new Object[]{sectionId, notebookId, appContext.getString(R.string.default_section_name), System.currentTimeMillis()});
                    }
                }
                db.execSQL("UPDATE entries SET section_uuid = ? WHERE notebook_uuid = ? AND section_uuid IS NULL",
                        new Object[]{sectionId, notebookId});
            }
        }
    }

    private void seedDefaultNotebook(SQLiteDatabase db) {
        db.execSQL(
                "INSERT INTO " + DatabaseContract.Notebooks.TABLE + " (" +
                        DatabaseContract.Notebooks.COL_UUID     + ", " +
                        DatabaseContract.Notebooks.COL_NAME     + ", " +
                        DatabaseContract.Notebooks.COL_POSITION + ", " +
                        DatabaseContract.Notebooks.COL_CREATED  + ") VALUES (?, ?, ?, ?);",
                new Object[]{
                        UUID.randomUUID().toString(),
                        appContext.getString(R.string.default_notebook_name),
                        0,
                        System.currentTimeMillis()
                });
    }

    private static final String CREATE_NOTEBOOKS =
            "CREATE TABLE " + DatabaseContract.Notebooks.TABLE + " (" +
                    DatabaseContract.Notebooks._ID           + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DatabaseContract.Notebooks.COL_UUID      + " TEXT    NOT NULL UNIQUE, " +
                    DatabaseContract.Notebooks.COL_NAME      + " TEXT    NOT NULL, " +
                    DatabaseContract.Notebooks.COL_DESCRIPTION + " TEXT, " +
                    DatabaseContract.Notebooks.COL_POSITION  + " INTEGER NOT NULL DEFAULT 0, " +
                    DatabaseContract.Notebooks.COL_CREATED   + " INTEGER NOT NULL);";

    private static final String CREATE_ENTRIES =
            "CREATE TABLE " + DatabaseContract.Entries.TABLE + " (" +
                    DatabaseContract.Entries._ID          + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DatabaseContract.Entries.COL_UUID     + " TEXT    NOT NULL UNIQUE, " +
                    DatabaseContract.Entries.COL_NOTEBOOK + " TEXT    NOT NULL, " +
                    DatabaseContract.Entries.COL_SECTION + " TEXT, " +
                    DatabaseContract.Entries.COL_TITLE    + " TEXT, " +
                    DatabaseContract.Entries.COL_BODY     + " TEXT, " +
                    DatabaseContract.Entries.COL_CREATED  + " INTEGER NOT NULL, " +
                    DatabaseContract.Entries.COL_UPDATED  + " INTEGER NOT NULL, " +
                    "FOREIGN KEY (" + DatabaseContract.Entries.COL_NOTEBOOK + ") REFERENCES " +
                    DatabaseContract.Notebooks.TABLE + "(" + DatabaseContract.Notebooks.COL_UUID + "));";

    private static final String CREATE_SECTIONS = "CREATE TABLE sections (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT, uuid TEXT NOT NULL UNIQUE, " +
            "notebook_uuid TEXT NOT NULL, name TEXT NOT NULL, position INTEGER NOT NULL DEFAULT 0, " +
            "created_at INTEGER NOT NULL, UNIQUE(notebook_uuid, name COLLATE NOCASE), " +
            "FOREIGN KEY(notebook_uuid) REFERENCES notebooks(uuid) ON DELETE CASCADE);";
    private static final String INDEX_SECTIONS_NOTEBOOK = "CREATE INDEX idx_sections_notebook ON sections(notebook_uuid, position);";

    private static final String INDEX_ENTRIES_NOTEBOOK =
            "CREATE INDEX idx_entries_notebook ON " + DatabaseContract.Entries.TABLE +
                    "(" + DatabaseContract.Entries.COL_NOTEBOOK + ");";

    private static final String INDEX_ENTRIES_CREATED =
            "CREATE INDEX idx_entries_created ON " + DatabaseContract.Entries.TABLE +
                    "(" + DatabaseContract.Entries.COL_CREATED + ");";

    private static final String INDEX_ENTRIES_UPDATED =
            "CREATE INDEX idx_entries_updated ON " + DatabaseContract.Entries.TABLE +
                    "(" + DatabaseContract.Entries.COL_UPDATED + ");";

    private static final String CREATE_FTS =
            "CREATE VIRTUAL TABLE " + DatabaseContract.EntriesFts.TABLE + " USING fts5(" +
                    DatabaseContract.EntriesFts.COL_TITLE + ", " +
                    DatabaseContract.EntriesFts.COL_BODY  + ");";

    private static final String TRIGGER_AFTER_INSERT =
            "CREATE TRIGGER entries_ai AFTER INSERT ON " + DatabaseContract.Entries.TABLE + " BEGIN " +
                    "INSERT INTO " + DatabaseContract.EntriesFts.TABLE + "(rowid, " +
                    DatabaseContract.EntriesFts.COL_TITLE + ", " + DatabaseContract.EntriesFts.COL_BODY + ") " +
                    "VALUES (new." + DatabaseContract.Entries._ID + ", new." +
                    DatabaseContract.Entries.COL_TITLE + ", new." + DatabaseContract.Entries.COL_BODY + "); " +
                    "END;";

    private static final String TRIGGER_AFTER_DELETE =
            "CREATE TRIGGER entries_ad AFTER DELETE ON " + DatabaseContract.Entries.TABLE + " BEGIN " +
                    "DELETE FROM " + DatabaseContract.EntriesFts.TABLE + " WHERE rowid = old." +
                    DatabaseContract.Entries._ID + "; " +
                    "END;";

    private static final String TRIGGER_AFTER_UPDATE =
            "CREATE TRIGGER entries_au AFTER UPDATE ON " + DatabaseContract.Entries.TABLE + " BEGIN " +
                    "UPDATE " + DatabaseContract.EntriesFts.TABLE + " SET " +
                    DatabaseContract.EntriesFts.COL_TITLE + " = new." + DatabaseContract.Entries.COL_TITLE + ", " +
                    DatabaseContract.EntriesFts.COL_BODY  + " = new." + DatabaseContract.Entries.COL_BODY + " " +
                    "WHERE rowid = new." + DatabaseContract.Entries._ID + "; " +
                    "END;";

    private static final String CREATE_TAGS =
            "CREATE TABLE " + DatabaseContract.Tags.TABLE + " (" +
                    DatabaseContract.Tags._ID       + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DatabaseContract.Tags.COL_UUID  + " TEXT NOT NULL UNIQUE, " +
                    DatabaseContract.Tags.COL_NAME  + " TEXT NOT NULL UNIQUE COLLATE NOCASE, " +
                    DatabaseContract.Tags.COL_CREATED + " INTEGER NOT NULL);";

    private static final String INDEX_TAGS_NAME =
            "CREATE INDEX idx_tags_name ON " + DatabaseContract.Tags.TABLE +
                    "(" + DatabaseContract.Tags.COL_NAME + ");";

    private static final String CREATE_ENTRY_TAGS =
            "CREATE TABLE " + DatabaseContract.EntryTags.TABLE + " (" +
                    DatabaseContract.EntryTags.COL_ENTRY + " TEXT NOT NULL, " +
                    DatabaseContract.EntryTags.COL_TAG   + " TEXT NOT NULL, " +
                    "PRIMARY KEY (" + DatabaseContract.EntryTags.COL_ENTRY + ", " +
                    DatabaseContract.EntryTags.COL_TAG + "), " +
                    "FOREIGN KEY (" + DatabaseContract.EntryTags.COL_ENTRY + ") REFERENCES " +
                    DatabaseContract.Entries.TABLE + "(" + DatabaseContract.Entries.COL_UUID +
                    ") ON DELETE CASCADE, " +
                    "FOREIGN KEY (" + DatabaseContract.EntryTags.COL_TAG + ") REFERENCES " +
                    DatabaseContract.Tags.TABLE + "(" + DatabaseContract.Tags.COL_UUID +
                    ") ON DELETE CASCADE);";

    private static final String INDEX_ENTRY_TAGS_TAG =
            "CREATE INDEX idx_entry_tags_tag ON " + DatabaseContract.EntryTags.TABLE +
                    "(" + DatabaseContract.EntryTags.COL_TAG + ");";
}
