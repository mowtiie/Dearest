package com.mowtiie.dearest.data.dao;

import android.content.ContentValues;
import android.database.Cursor;

import com.mowtiie.dearest.data.db.DatabaseContract.Notebooks;
import com.mowtiie.dearest.data.model.Notebook;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class NotebookDao {

    private static final String[] COLUMNS = {
            Notebooks.COL_UUID,
            Notebooks.COL_NAME,
            Notebooks.COL_DESCRIPTION,
            Notebooks.COL_POSITION,
            Notebooks.COL_CREATED
    };

    public long insert(SQLiteDatabase db, Notebook notebook) {
        return db.insertOrThrow(Notebooks.TABLE, null, toValues(notebook));
    }

    public int update(SQLiteDatabase db, Notebook notebook) {
        return db.update(Notebooks.TABLE, toValues(notebook),
                Notebooks.COL_UUID + " = ?", new String[]{ notebook.getId() });
    }

    public int delete(SQLiteDatabase db, String notebookId) {
        return db.delete(Notebooks.TABLE, Notebooks.COL_UUID + " = ?",
                new String[]{ notebookId });
    }

    public Notebook getById(SQLiteDatabase db, String id) {
        try (Cursor c = db.query(Notebooks.TABLE, COLUMNS,
                Notebooks.COL_UUID + " = ?", new String[]{ id }, null, null, null)) {
            return c.moveToFirst() ? fromCursor(c) : null;
        }
    }

    public List<Notebook> getAll(SQLiteDatabase db) {
        String orderBy = Notebooks.COL_POSITION + " ASC, " + Notebooks.COL_CREATED + " ASC";
        try (Cursor c = db.query(Notebooks.TABLE, COLUMNS,
                null, null, null, null, orderBy)) {
            List<Notebook> out = new ArrayList<>(c.getCount());
            while (c.moveToNext()) out.add(fromCursor(c));
            return out;
        }
    }

    private ContentValues toValues(Notebook notebook) {
        ContentValues values = new ContentValues();
        values.put(Notebooks.COL_UUID, notebook.getId());
        values.put(Notebooks.COL_NAME, notebook.getName());
        values.put(Notebooks.COL_DESCRIPTION, notebook.getDescription());
        values.put(Notebooks.COL_POSITION, notebook.getPosition());
        values.put(Notebooks.COL_CREATED, notebook.getCreatedAt());
        return values;
    }

    private Notebook fromCursor(Cursor c) {
        int descIdx = c.getColumnIndexOrThrow(Notebooks.COL_DESCRIPTION);
        return new Notebook(
                c.getString(c.getColumnIndexOrThrow(Notebooks.COL_UUID)),
                c.getString(c.getColumnIndexOrThrow(Notebooks.COL_NAME)),
                c.isNull(descIdx) ? null : c.getString(descIdx),
                c.getInt(c.getColumnIndexOrThrow(Notebooks.COL_POSITION)),
                c.getLong(c.getColumnIndexOrThrow(Notebooks.COL_CREATED))
        );
    }
}