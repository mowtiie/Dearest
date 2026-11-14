package com.mowtiie.dearest.backup;

import com.mowtiie.dearest.data.model.Entry;
import com.mowtiie.dearest.data.model.Notebook;
import com.mowtiie.dearest.data.model.Tag;
import com.mowtiie.dearest.data.model.Section;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BackupData {

    public final List<Notebook> notebooks;
    public final List<Entry> entries;
    public final List<Tag> tags;
    public final List<Section> sections;
    /** Entry id to the ids of the tags attached to it. */
    public final Map<String, Set<String>> entryTagIds;
    /**
     * False for backups written before tags were supported. Importing such a backup must leave
     * existing tag assignments alone rather than clearing them.
     */
    public final boolean includesTags;

    public BackupData(List<Notebook> notebooks, List<Entry> entries) {
        this(notebooks, entries, Collections.emptyList(), Collections.emptyList(), Collections.emptyMap(), false);
    }

    public BackupData(List<Notebook> notebooks, List<Entry> entries,
                      List<Tag> tags, Map<String, Set<String>> entryTagIds) {
        this(notebooks, entries, Collections.emptyList(), tags, entryTagIds, true);
    }

    public BackupData(List<Notebook> notebooks, List<Entry> entries, List<Section> sections,
                      List<Tag> tags, Map<String, Set<String>> entryTagIds) {
        this(notebooks, entries, sections, tags, entryTagIds, true);
    }

    private BackupData(List<Notebook> notebooks, List<Entry> entries,
                       List<Section> sections, List<Tag> tags, Map<String, Set<String>> entryTagIds, boolean includesTags) {
        this.notebooks = notebooks;
        this.entries = entries;
        this.tags = tags;
        this.sections = sections;
        this.entryTagIds = entryTagIds;
        this.includesTags = includesTags;
    }
}
