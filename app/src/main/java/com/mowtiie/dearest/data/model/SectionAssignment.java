package com.mowtiie.dearest.data.model;

import androidx.annotation.Nullable;

import java.util.List;

/** Pure section scoping rules shared by the editor and its tests. */
public final class SectionAssignment {
    private SectionAssignment() {}

    /** Keeps a preferred section only when it belongs to the target notebook; otherwise picks its first section. */
    @Nullable
    public static String resolveForNotebook(List<Section> sections, String notebookId,
                                            @Nullable String preferredSectionId) {
        if (sections == null || notebookId == null) return null;
        String first = null;
        for (Section section : sections) {
            if (!notebookId.equals(section.getNotebookId())) continue;
            if (first == null) first = section.getId();
            if (preferredSectionId != null && preferredSectionId.equals(section.getId())) return preferredSectionId;
        }
        return first;
    }

    @Nullable
    public static String afterNotebookChange(String previousNotebookId, String nextNotebookId,
                                             @Nullable String currentSectionId) {
        return previousNotebookId == null ? (nextNotebookId == null ? currentSectionId : null)
                : previousNotebookId.equals(nextNotebookId) ? currentSectionId : null;
    }

    /** Assigns an entry to a target Section only when both belong to the same Notebook. */
    public static boolean moveEntry(Entry entry, Section target) {
        if (entry == null || target == null || !entry.getNotebookId().equals(target.getNotebookId())) return false;
        entry.setSectionId(target.getId());
        return true;
    }
}
