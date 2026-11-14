package com.mowtiie.dearest.data.dao;

import android.content.ContentValues;
import android.database.Cursor;

import com.mowtiie.dearest.data.model.Section;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public final class SectionDao {
    public List<Section> getByNotebook(SQLiteDatabase db, String notebookId) {
        List<Section> out = new ArrayList<>();
        try (Cursor c = db.query("sections", columns(),
                "notebook_uuid = ?", new String[]{notebookId}, null, null,
                "position ASC, created_at ASC")) {
            while (c.moveToNext()) out.add(fromCursor(c));
        }
        return out;
    }

    public Section getById(SQLiteDatabase db, String id) {
        try (Cursor c = db.query("sections", columns(), "uuid = ?",
                new String[]{id}, null, null, null)) {
            return c.moveToFirst() ? fromCursor(c) : null;
        }
    }

    public Section getByName(SQLiteDatabase db, String notebookId, String name) {
        try (Cursor c = db.query("sections", columns(),
                "notebook_uuid = ? AND name = ? COLLATE NOCASE",
                new String[]{notebookId, name}, null, null, null)) {
            return c.moveToFirst() ? fromCursor(c) : null;
        }
    }

    public void save(SQLiteDatabase db, Section s) {
        ContentValues values = new ContentValues();
        values.put("uuid", s.getId());
        values.put("notebook_uuid", s.getNotebookId());
        values.put("name", s.getName());
        values.put("position", s.getPosition());
        values.put("created_at", s.getCreatedAt());
        if (db.update("sections", values, "uuid = ?", new String[]{s.getId()}) == 0) {
            db.insertOrThrow("sections", null, values);
        }
    }

    private static String[] columns() {
        return new String[]{"uuid", "notebook_uuid", "name", "position", "created_at"};
    }

    private static Section fromCursor(Cursor c) {
        return new Section(c.getString(0), c.getString(1), c.getString(2),
                c.getInt(3), c.getLong(4));
    }
}
