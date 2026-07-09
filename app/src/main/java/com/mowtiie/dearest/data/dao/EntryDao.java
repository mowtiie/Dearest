package com.mowtiie.dearest.data.dao;

import android.content.ContentValues;
import android.database.Cursor;

import com.mowtiie.dearest.data.db.DatabaseContract.Entries;
import com.mowtiie.dearest.data.db.DatabaseContract.EntriesFts;
import com.mowtiie.dearest.data.model.Entry;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EntryDao {

    private static final String[] COLUMNS = {
            Entries.COL_UUID,
            Entries.COL_NOTEBOOK,
            Entries.COL_TITLE,
            Entries.COL_BODY,
            Entries.COL_CREATED,
            Entries.COL_UPDATED
    };

    public long insert(SQLiteDatabase db, Entry entry) {
        return db.insertOrThrow(Entries.TABLE, null, toValues(entry));
    }

    public int update(SQLiteDatabase db, Entry entry) {
        return db.update(Entries.TABLE, toValues(entry),
                Entries.COL_UUID + " = ?", new String[]{ entry.getId() });
    }

    public int delete(SQLiteDatabase db, String entryId) {
        return db.delete(Entries.TABLE, Entries.COL_UUID + " = ?", new String[]{ entryId });
    }

    public int moveEntries(SQLiteDatabase db, String fromNotebookId, String toNotebookId) {
        ContentValues values = new ContentValues();
        values.put(Entries.COL_NOTEBOOK, toNotebookId);
        return db.update(Entries.TABLE, values,
                Entries.COL_NOTEBOOK + " = ?", new String[]{ fromNotebookId });
    }

    public Entry getById(SQLiteDatabase db, String id) {
        try (Cursor c = db.query(Entries.TABLE, COLUMNS,
                Entries.COL_UUID + " = ?", new String[]{ id }, null, null, null)) {
            return c.moveToFirst() ? fromCursor(c) : null;
        }
    }

    public List<Entry> getByNotebook(SQLiteDatabase db, String notebookId) {
        try (Cursor c = db.query(Entries.TABLE, COLUMNS,
                Entries.COL_NOTEBOOK + " = ?", new String[]{ notebookId },
                null, null, Entries.COL_CREATED + " DESC")) {
            return collect(c);
        }
    }

    public List<Entry> getAll(SQLiteDatabase db) {
        try (Cursor c = db.query(Entries.TABLE, COLUMNS,
                null, null, null, null, Entries.COL_CREATED + " DESC")) {
            return collect(c);
        }
    }

    public List<Entry> search(SQLiteDatabase db, String rawQuery) {
        String match = toFtsQuery(rawQuery);
        if (match == null) return Collections.emptyList();

        final String sql =
                "SELECT e." + Entries.COL_UUID + ", e." + Entries.COL_NOTEBOOK +
                        ", e." + Entries.COL_TITLE + ", e." + Entries.COL_BODY +
                        ", e." + Entries.COL_CREATED + ", e." + Entries.COL_UPDATED +
                        " FROM " + EntriesFts.TABLE + " f" +
                        " JOIN " + Entries.TABLE + " e ON e." + Entries._ID + " = f.rowid" +
                        " WHERE " + EntriesFts.TABLE + " MATCH ?" +
                        " ORDER BY rank";
        try (Cursor c = db.rawQuery(sql, new String[]{ match })) {
            return collect(c);
        }
    }

    public int countByNotebook(SQLiteDatabase db, String notebookId) {
        try (Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + Entries.TABLE +
                        " WHERE " + Entries.COL_NOTEBOOK + " = ?", new String[]{ notebookId })) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }


    private ContentValues toValues(Entry entry) {
        ContentValues values = new ContentValues();
        values.put(Entries.COL_UUID, entry.getId());
        values.put(Entries.COL_NOTEBOOK, entry.getNotebookId());
        values.put(Entries.COL_TITLE, entry.getTitle());
        values.put(Entries.COL_BODY, entry.getBody());
        values.put(Entries.COL_CREATED, entry.getCreatedAt());
        values.put(Entries.COL_UPDATED, entry.getUpdatedAt());
        return values;
    }

    private List<Entry> collect(Cursor c) {
        List<Entry> out = new ArrayList<>(c.getCount());
        while (c.moveToNext()) out.add(fromCursor(c));
        return out;
    }

    private Entry fromCursor(Cursor c) {
        return new Entry(
                c.getString(c.getColumnIndexOrThrow(Entries.COL_UUID)),
                c.getString(c.getColumnIndexOrThrow(Entries.COL_NOTEBOOK)),
                c.getString(c.getColumnIndexOrThrow(Entries.COL_TITLE)),
                c.getString(c.getColumnIndexOrThrow(Entries.COL_BODY)),
                c.getLong(c.getColumnIndexOrThrow(Entries.COL_CREATED)),
                c.getLong(c.getColumnIndexOrThrow(Entries.COL_UPDATED))
        );
    }

    private static String toFtsQuery(String raw) {
        if (raw == null) return null;
        StringBuilder sb = new StringBuilder();
        for (String token : raw.trim().split("\\s+")) {
            String clean = token.replaceAll("[\"*()\\[\\]:^-]", "");
            if (clean.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append('"').append(clean).append("\"*");
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}