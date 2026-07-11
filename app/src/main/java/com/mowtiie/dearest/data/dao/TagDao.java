package com.mowtiie.dearest.data.dao;

import android.content.ContentValues;
import android.database.Cursor;

import com.mowtiie.dearest.data.db.DatabaseContract.EntryTags;
import com.mowtiie.dearest.data.db.DatabaseContract.Tags;
import com.mowtiie.dearest.data.model.Tag;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TagDao {

    private static final String[] TAG_COLUMNS = {
            Tags.COL_UUID, Tags.COL_NAME, Tags.COL_CREATED
    };

    public List<Tag> getAll(SQLiteDatabase db) {
        try (Cursor c = db.query(Tags.TABLE, TAG_COLUMNS,
                null, null, null, null, Tags.COL_NAME + " COLLATE NOCASE ASC")) {
            List<Tag> out = new ArrayList<>(c.getCount());
            while (c.moveToNext()) out.add(fromCursor(c));
            return out;
        }
    }

    public Tag findOrCreateByName(SQLiteDatabase db, String name) {
        try (Cursor c = db.query(Tags.TABLE, TAG_COLUMNS,
                Tags.COL_NAME + " = ?", new String[]{ name }, null, null, null)) {
            if (c.moveToFirst()) return fromCursor(c);
        }
        Tag created = Tag.createNew(name);
        ContentValues values = new ContentValues();
        values.put(Tags.COL_UUID, created.getId());
        values.put(Tags.COL_NAME, created.getName());
        values.put(Tags.COL_CREATED, created.getCreatedAt());
        db.insertOrThrow(Tags.TABLE, null, values);
        return created;
    }

    public int update(SQLiteDatabase db, Tag tag) {
        ContentValues values = new ContentValues();
        values.put(Tags.COL_NAME, tag.getName());
        return db.update(Tags.TABLE, values, Tags.COL_UUID + " = ?", new String[]{ tag.getId() });
    }

    public int delete(SQLiteDatabase db, String tagId) {
        return db.delete(Tags.TABLE, Tags.COL_UUID + " = ?", new String[]{ tagId });
    }

    public List<Tag> getTagsForEntry(SQLiteDatabase db, String entryId) {
        String sql = "SELECT t." + Tags.COL_UUID + ", t." + Tags.COL_NAME + ", t." + Tags.COL_CREATED +
                " FROM " + Tags.TABLE + " t" +
                " JOIN " + EntryTags.TABLE + " et ON et." + EntryTags.COL_TAG + " = t." + Tags.COL_UUID +
                " WHERE et." + EntryTags.COL_ENTRY + " = ?" +
                " ORDER BY t." + Tags.COL_NAME + " COLLATE NOCASE ASC";
        try (Cursor c = db.rawQuery(sql, new String[]{ entryId })) {
            List<Tag> out = new ArrayList<>(c.getCount());
            while (c.moveToNext()) out.add(fromCursor(c));
            return out;
        }
    }

    public void setTagsForEntry(SQLiteDatabase db, String entryId, List<String> tagIds) {
        db.delete(EntryTags.TABLE, EntryTags.COL_ENTRY + " = ?", new String[]{ entryId });
        for (String tagId : tagIds) {
            ContentValues values = new ContentValues();
            values.put(EntryTags.COL_ENTRY, entryId);
            values.put(EntryTags.COL_TAG, tagId);
            db.insertWithOnConflict(EntryTags.TABLE, null, values,
                    SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    public Map<String, Set<String>> getAllEntryTagLinks(SQLiteDatabase db) {
        Map<String, Set<String>> map = new HashMap<>();
        try (Cursor c = db.query(EntryTags.TABLE,
                new String[]{ EntryTags.COL_ENTRY, EntryTags.COL_TAG },
                null, null, null, null, null)) {
            int entryIdx = c.getColumnIndexOrThrow(EntryTags.COL_ENTRY);
            int tagIdx = c.getColumnIndexOrThrow(EntryTags.COL_TAG);
            while (c.moveToNext()) {
                String entryId = c.getString(entryIdx);
                String tagId = c.getString(tagIdx);
                map.computeIfAbsent(entryId, k -> new HashSet<>()).add(tagId);
            }
        }
        return map;
    }

    private Tag fromCursor(Cursor c) {
        return new Tag(
                c.getString(c.getColumnIndexOrThrow(Tags.COL_UUID)),
                c.getString(c.getColumnIndexOrThrow(Tags.COL_NAME)),
                c.getLong(c.getColumnIndexOrThrow(Tags.COL_CREATED)));
    }
}