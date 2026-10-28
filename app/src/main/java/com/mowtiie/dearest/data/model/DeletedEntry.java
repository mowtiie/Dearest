package com.mowtiie.dearest.data.model;

import android.os.SystemClock;

import java.util.List;

/**
 * Everything needed to bring back an entry deleted a moment ago. Held in memory only, never
 * persisted, and dropped when the journal locks or {@link #UNDO_WINDOW_MS} passes.
 */
public final class DeletedEntry {

    public static final long UNDO_WINDOW_MS = 8000;

    public final Entry entry;
    public final List<String> tagNames;
    private final long deletedAt = SystemClock.elapsedRealtime();

    public DeletedEntry(Entry entry, List<String> tagNames) {
        this.entry = entry;
        this.tagNames = tagNames;
    }

    /** How much of the undo window is left; zero or less once it has passed. */
    public long remainingMs() {
        return UNDO_WINDOW_MS - (SystemClock.elapsedRealtime() - deletedAt);
    }
}
