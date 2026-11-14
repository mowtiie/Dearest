package com.mowtiie.dearest.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class SectionAssignmentTest {
    @Test public void switchingNotebookClearsPreviousSectionImmediately() {
        assertNull(SectionAssignment.afterNotebookChange("notebook-a", "notebook-b", "section-a"));
        assertEquals("section-a", SectionAssignment.afterNotebookChange("notebook-a", "notebook-a", "section-a"));
    }

    @Test public void targetNotebookCannotKeepASectionFromPreviousNotebook() {
        Section a = new Section("section-a", "notebook-a", "General", 0, 1L);
        Section b = new Section("section-b", "notebook-b", "General", 0, 2L);
        assertEquals("section-b", SectionAssignment.resolveForNotebook(Arrays.asList(a, b), "notebook-b", "section-a"));
        assertNull(SectionAssignment.resolveForNotebook(Collections.singletonList(a), "notebook-b", "section-a"));
    }
}
