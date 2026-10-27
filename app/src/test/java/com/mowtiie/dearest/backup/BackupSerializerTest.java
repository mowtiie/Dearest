package com.mowtiie.dearest.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.mowtiie.dearest.data.model.Entry;
import com.mowtiie.dearest.data.model.Notebook;
import com.mowtiie.dearest.data.model.Tag;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static BackupData sampleWithTags() {
        Notebook nb = new Notebook("nb-1", "Journal", null, 0, 1L);
        Entry tagged = new Entry("e-1", "nb-1", "Tagged", "body", 1L, 1L);
        Entry plain = new Entry("e-2", "nb-1", "Plain", "body", 2L, 2L);
        List<Tag> tags = Arrays.asList(new Tag("t-1", "work", 10L), new Tag("t-2", "Ideas", 20L));
        Map<String, Set<String>> links = new HashMap<>();
        links.put("e-1", new LinkedHashSet<>(Arrays.asList("t-1", "t-2")));
        return new BackupData(Collections.singletonList(nb), Arrays.asList(tagged, plain), tags, links);
    }

    @Test
    public void tagsAndAssignmentsSurviveJsonRoundTrip() throws Exception {
        BackupData back = BackupSerializer.fromJson(BackupSerializer.toJson(sampleWithTags()));

        assertTrue(back.includesTags);
        assertEquals(2, back.tags.size());
        assertEquals("work", back.tags.get(0).getName());
        assertEquals(10L, back.tags.get(0).getCreatedAt());
        assertEquals(new HashSet<>(Arrays.asList("t-1", "t-2")), back.entryTagIds.get("e-1"));
        assertNull(back.entryTagIds.get("e-2"));
    }

    @Test
    public void backupsWithoutTagsAreRecognisedSoImportKeepsExistingTags() throws Exception {
        String legacy = "{\"format\":\"dearest\",\"version\":1,\"notebooks\":[],"
                + "\"entries\":[{\"id\":\"e-1\",\"notebookId\":\"nb-1\",\"title\":\"t\",\"body\":\"b\"}]}";
        BackupData back = BackupSerializer.fromJson(legacy);

        assertFalse(back.includesTags);
        assertEquals(1, back.entries.size());
        assertTrue(back.tags.isEmpty());
    }

    @Test
    public void csvAndPlainTextListTagNamesSorted() {
        BackupData data = sampleWithTags();

        String csv = BackupSerializer.toCsv(data);
        assertTrue(csv.startsWith("Notebook,Created,Updated,Title,Body,Tags\r\n"));
        assertTrue(csv.contains("\"Ideas, work\""));

        assertTrue(BackupSerializer.toPlainText(data).contains("Tags: Ideas, work"));
    }
}
