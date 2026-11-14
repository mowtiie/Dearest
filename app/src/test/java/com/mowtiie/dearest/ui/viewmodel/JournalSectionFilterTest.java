package com.mowtiie.dearest.ui.viewmodel;

import static org.junit.Assert.assertEquals;

import com.mowtiie.dearest.data.model.Entry;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class JournalSectionFilterTest {
    @Test public void sectionHistoryIsScopedAndNewestFirst() {
        Entry recent = new Entry("recent", "nb-1", "section-a", "new", "", 30L, 30L);
        Entry otherSection = new Entry("other-section", "nb-1", "section-b", "other", "", 40L, 40L);
        Entry older = new Entry("older", "nb-1", "section-a", "old", "", 10L, 10L);
        Entry otherNotebook = new Entry("other-notebook", "nb-2", "section-a", "foreign", "", 50L, 50L);

        List<Entry> result = JournalViewModel.filterForScope(
                Arrays.asList(older, otherSection, otherNotebook, recent), "nb-1", "section-a");

        assertEquals(2, result.size());
        assertEquals("recent", result.get(0).getId());
        assertEquals("older", result.get(1).getId());
    }

    @Test public void notebookFilterWithoutASectionKeepsAllItsSections() {
        Entry a = new Entry("a", "nb-1", "section-a", null, "", 1L, 1L);
        Entry b = new Entry("b", "nb-1", "section-b", null, "", 2L, 2L);
        Entry other = new Entry("c", "nb-2", "section-c", null, "", 3L, 3L);
        assertEquals(2, JournalViewModel.filterForScope(Arrays.asList(a, b, other), "nb-1", null).size());
    }
}
