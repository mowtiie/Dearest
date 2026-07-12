package com.mowtiie.dearest.data.db;

import android.provider.BaseColumns;

public final class DatabaseContract {

    private DatabaseContract() {}

    public static final class Notebooks implements BaseColumns {
        public static final String TABLE          = "notebooks";
        public static final String COL_UUID       = "uuid";
        public static final String COL_NAME       = "name";
        public static final String COL_DESCRIPTION = "description";
        public static final String COL_POSITION   = "position";
        public static final String COL_CREATED    = "created_at";
    }

    public static final class Entries implements BaseColumns {
        public static final String TABLE        = "entries";
        public static final String COL_UUID     = "uuid";
        public static final String COL_NOTEBOOK = "notebook_uuid";
        public static final String COL_TITLE    = "title";
        public static final String COL_BODY     = "body";
        public static final String COL_CREATED  = "created_at";
        public static final String COL_UPDATED  = "updated_at";
    }

    public static final class EntriesFts {
        public static final String TABLE     = "entries_fts";
        public static final String COL_TITLE = "title";
        public static final String COL_BODY  = "body";
    }

    public static final class Tags implements BaseColumns {
        public static final String TABLE       = "tags";
        public static final String COL_UUID    = "uuid";
        public static final String COL_NAME    = "name";
        public static final String COL_CREATED = "created_at";
    }

    public static final class EntryTags {
        public static final String TABLE       = "entry_tags";
        public static final String COL_ENTRY   = "entry_uuid";
        public static final String COL_TAG     = "tag_uuid";
    }
}