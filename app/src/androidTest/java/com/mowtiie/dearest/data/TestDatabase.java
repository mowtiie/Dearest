package com.mowtiie.dearest.data;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.mowtiie.dearest.data.db.DearestDatabase;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import java.util.UUID;

/**
 * A throwaway encrypted database with its own file name, so tests never touch the real journal.
 * Always {@link #close()} it (try-with-resources or {@code @After}).
 */
public final class TestDatabase implements AutoCloseable {

    public final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    public final String name = "test-" + UUID.randomUUID() + ".db";
    public final byte[] key = new byte[32];

    private DearestDatabase helper;
    private SQLiteDatabase db;

    public TestDatabase() {
        DearestDatabase.loadLibrary();
        java.util.Arrays.fill(key, (byte) 7);
    }

    /** Opens (creating or upgrading) the database with the current app schema. */
    public SQLiteDatabase open() {
        closeConnection();
        helper = new DearestDatabase(context, name, key);
        db = helper.getWritableDatabase();
        return db;
    }

    public void closeConnection() {
        if (db != null) db.close();
        db = null;
        helper = null;
    }

    @Override
    public void close() {
        closeConnection();
        context.deleteDatabase(name);
    }
}
