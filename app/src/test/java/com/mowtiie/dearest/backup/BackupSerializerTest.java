package com.mowtiie.dearest.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.mowtiie.dearest.data.model.Entry;
import com.mowtiie.dearest.data.model.Notebook;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class BackupSerializerTest {

    @Test
    public void jsonRoundTripPreservesContent() throws Exception {
        Notebook nb = new Notebook("nb-1", "Dreams", "weird ones", 2, 1000L);
        Entry e = new Entry("e-1", "nb-1", "Title \"quoted\"", "line1\nline2, with comma", 2000L, 3000L);
        BackupData data = new BackupData(Collections.singletonList(nb), Collections.singletonList(e));

        BackupData back = BackupSerializer.fromJson(BackupSerializer.toJson(data));

        assertEquals(1, back.notebooks.size());
        assertEquals("Dreams", back.notebooks.get(0).getName());
        assertEquals("weird ones", back.notebooks.get(0).getDescription());
        assertEquals(2, back.notebooks.get(0).getPosition());
        Entry out = back.entries.get(0);
        assertEquals("e-1", out.getId());
        assertEquals("Title \"quoted\"", out.getTitle());
        assertEquals("line1\nline2, with comma", out.getBody());
        assertEquals(2000L, out.getCreatedAt());
        assertEquals(3000L, out.getUpdatedAt());
    }

    @Test
    public void nullFieldsSurviveRoundTrip() throws Exception {
        Notebook nb = new Notebook("nb-1", "Plain", null, 0, 1L);
        Entry e = new Entry("e-1", "nb-1", null, null, 1L, 1L);
        BackupData back = BackupSerializer.fromJson(BackupSerializer.toJson(
                new BackupData(Collections.singletonList(nb), Collections.singletonList(e))));

        assertNull(back.notebooks.get(0).getDescription());
        assertNull(back.entries.get(0).getTitle());
        assertNull(back.entries.get(0).getBody());
    }
}
