package com.mowtiie.dearest.data.db;

import android.content.Context;

import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteOpenHelper;

import java.util.UUID;

public class DearestDatabase extends SQLiteOpenHelper {

    public static final String DB_NAME = "dearest.db";
    public static final int DB_VERSION = 2;

    private static final String DEFAULT_NOTEBOOK_NAME = "My Journal";

    private static boolean sLibraryLoaded = false;

    public DearestDatabase(Context context, byte[] databaseKey) {
        super(context.getApplicationContext(), DB_NAME, databaseKey, null, DB_VERSION,
                0, null, null, true);
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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createTagTables(db);
        }
    }

    private void createTagTables(SQLiteDatabase db) {
        db.execSQL(CREATE_TAGS);
        db.execSQL(INDEX_TAGS_NAME);
        db.execSQL(CREATE_ENTRY_TAGS);
        db.execSQL(INDEX_ENTRY_TAGS_TAG);
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
                        DEFAULT_NOTEBOOK_NAME,
                        0,
                        System.currentTimeMillis()
                });
    }


    private static final String CREATE_NOTEBOOKS =
            "CREATE TABLE " + DatabaseContract.Notebooks.TABLE + " (" +
                    DatabaseContract.Notebooks._ID          + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DatabaseContract.Notebooks.COL_UUID     + " TEXT    NOT NULL UNIQUE, " +
                    DatabaseContract.Notebooks.COL_NAME     + " TEXT    NOT NULL, " +
                    DatabaseContract.Notebooks.COL_POSITION + " INTEGER NOT NULL DEFAULT 0, " +
                    DatabaseContract.Notebooks.COL_CREATED  + " INTEGER NOT NULL);";

    private static final String CREATE_ENTRIES =
            "CREATE TABLE " + DatabaseContract.Entries.TABLE + " (" +
                    DatabaseContract.Entries._ID          + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DatabaseContract.Entries.COL_UUID     + " TEXT    NOT NULL UNIQUE, " +
                    DatabaseContract.Entries.COL_NOTEBOOK + " TEXT    NOT NULL, " +
                    DatabaseContract.Entries.COL_TITLE    + " TEXT, " +
                    DatabaseContract.Entries.COL_BODY     + " TEXT, " +
                    DatabaseContract.Entries.COL_CREATED  + " INTEGER NOT NULL, " +
                    DatabaseContract.Entries.COL_UPDATED  + " INTEGER NOT NULL, " +
                    "FOREIGN KEY (" + DatabaseContract.Entries.COL_NOTEBOOK + ") REFERENCES " +
                    DatabaseContract.Notebooks.TABLE + "(" + DatabaseContract.Notebooks.COL_UUID + "));";

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