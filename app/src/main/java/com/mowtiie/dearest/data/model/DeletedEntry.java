package com.mowtiie.dearest.data.model;

import java.util.List;

/**
 * Everything needed to bring back an entry deleted a moment ago. Held in memory only, never
 * persisted, and dropped when the journal locks.
 */
public final class DeletedEntry {

    public final Entry entry;
    public final List<String> tagNames;

    public DeletedEntry(Entry entry, List<String> tagNames) {
        this.entry = entry;
        this.tagNames = tagNames;
    }
}
