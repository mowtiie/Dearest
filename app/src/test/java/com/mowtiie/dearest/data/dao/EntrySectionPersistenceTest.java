package com.mowtiie.dearest.data.dao;

import static org.junit.Assert.assertEquals;

import android.content.ContentValues;

import com.mowtiie.dearest.data.db.DatabaseContract;
import com.mowtiie.dearest.data.model.Entry;
import com.mowtiie.dearest.data.model.Section;
import com.mowtiie.dearest.data.model.SectionAssignment;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class EntrySectionPersistenceTest {
    @Test public void entrySectionOwnershipIsWrittenAndCanBeMoved() {
        Entry entry = new Entry("entry-1", "notebook-1", "section-a", "title", "body", 1L, 1L);
        EntryDao dao = new EntryDao();

        ContentValues beforeMove = dao.toValues(entry);
        assertEquals("section-a", beforeMove.getAsString(DatabaseContract.Entries.COL_SECTION));

        assertEquals(true, SectionAssignment.moveEntry(entry,
                new Section("section-b", "notebook-1", "Second", 1, 2L)));
        ContentValues afterMove = dao.toValues(entry);
        assertEquals("section-b", afterMove.getAsString(DatabaseContract.Entries.COL_SECTION));
        assertEquals("notebook-1", afterMove.getAsString(DatabaseContract.Entries.COL_NOTEBOOK));
        assertEquals(false, SectionAssignment.moveEntry(entry,
                new Section("foreign-section", "notebook-2", "Foreign", 0, 3L)));
    }
}
