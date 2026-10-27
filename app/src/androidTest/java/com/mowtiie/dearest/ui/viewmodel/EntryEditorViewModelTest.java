package com.mowtiie.dearest.ui.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mowtiie.dearest.data.TestRepository;
import com.mowtiie.dearest.data.model.DeletedEntry;
import com.mowtiie.dearest.data.model.Entry;
import com.mowtiie.dearest.data.model.Notebook;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The editor's save, autosave and delete logic against a real (throwaway) repository. Autosave
 * is exercised by calling {@code autosave} directly; the activity's 2 s debounce is not covered.
 */
@RunWith(AndroidJUnit4.class)
public class EntryEditorViewModelTest {

    private TestRepository t;
    private String nb;
    private EntryEditorViewModel vm;

    @Before
    public void setUp() {
        t = new TestRepository().open();
        nb = t.firstNotebookId();
        vm = newViewModel();
    }

    @After
    public void tearDown() {
        t.close();
    }

    private EntryEditorViewModel newViewModel() {
        AtomicReference<EntryEditorViewModel> ref = new AtomicReference<>();
        TestRepository.runOnMain(() ->
                ref.set(new EntryEditorViewModel(t.application, t.repository)));
        return ref.get();
    }

    private void onMain(Runnable r) {
        TestRepository.runOnMain(r);
    }

    private void openNew() {
        onMain(() -> vm.init(null, nb));
    }

    private void openExisting(String id) {
        onMain(() -> vm.init(id, null));
        TestRepository.await("entry loads", () -> vm.entry().getValue() != null);
        TestRepository.await("tags load", () -> vm.tagNames().getValue() != null);
    }

    /** The one entry in the journal, once it has been written. */
    private Entry onlyEntry() {
        List<Entry> all = t.observeOnce(t.repository.observeAllEntries(), v -> !v.isEmpty());
        assertEquals(1, all.size());
        return t.getEntry(all.get(0).getId());
    }

    private Entry seed(String id, String title, String... tags) {
        Entry e = new Entry(id, nb, title, "body", 100L, 200L);
        t.save(e, tags);
        return e;
    }

    // ---- New entries -------------------------------------------------------------------------

    @Test
    public void aBlankNewEntryIsNotSaved() {
        openNew();

        onMain(() -> vm.autosave("  ", "\n"));
        TestRepository.settle();

        assertTrue(t.repository.isOpen());
        assertTrue(t.observeOnce(t.repository.observeAllEntries(), v -> true).isEmpty());
    }

    @Test
    public void aNewEntryWithTextIsSavedWithItsNotebookAndTags() {
        openNew();
        onMain(() -> vm.addTag("mood"));

        onMain(() -> vm.autosave("  Title ", "Body"));

        Entry saved = onlyEntry();
        assertEquals("Title", saved.getTitle());
        assertEquals("Body", saved.getBody());
        assertEquals(nb, saved.getNotebookId());
        assertEquals(Collections.singletonList("mood"), t.getTagNames(saved.getId()));
    }

    @Test
    public void autosavingTwiceUpdatesTheSameEntry() {
        openNew();
        onMain(() -> vm.autosave("one", ""));
        Entry first = onlyEntry();

        onMain(() -> vm.autosave("one", "two"));
        TestRepository.await("body updated", () -> "two".equals(t.getEntry(first.getId()).getBody()));

        assertEquals(1, t.observeOnce(t.repository.observeAllEntries(), v -> !v.isEmpty()).size());
    }

    @Test
    public void aNewEntryWithNoNotebookIsNotSavedUntilOneIsChosen() {
        onMain(() -> vm.init(null, null));

        onMain(() -> vm.autosave("title", "body"));
        TestRepository.settle();
        assertTrue(t.observeOnce(t.repository.observeAllEntries(), v -> true).isEmpty());

        onMain(() -> vm.ensureNotebook(Collections.singletonList(
                new Notebook(nb, "n", null, 0, 1L))));
        onMain(() -> vm.autosave("title", "body"));

        assertEquals(nb, onlyEntry().getNotebookId());
    }

    @Test
    public void ensureNotebookDoesNotOverrideAChoice() {
        openNew();

        onMain(() -> vm.ensureNotebook(Collections.singletonList(
                new Notebook("other", "o", null, 0, 1L))));

        assertEquals(nb, vm.notebookId().getValue());
    }

    // ---- Existing entries --------------------------------------------------------------------

    @Test
    public void autosaveWithoutChangesDoesNotWrite() {
        seed("e-1", "title", "tag");
        openExisting("e-1");

        onMain(() -> vm.autosave("title", "body"));
        TestRepository.settle();

        assertEquals(200L, t.getEntry("e-1").getUpdatedAt());
    }

    @Test
    public void changingOnlyTagsAutosavesThem() {
        seed("e-1", "title", "old");
        openExisting("e-1");

        onMain(() -> vm.addTag("added"));
        onMain(() -> vm.autosave("title", "body"));

        TestRepository.await("tags saved", () -> t.getTagNames("e-1").contains("added"));
        assertTrue(t.getTagNames("e-1").contains("old"));
        assertEquals(100L, t.getEntry("e-1").getCreatedAt());
    }

    @Test
    public void changingOnlyTheNotebookAutosavesIt() {
        seed("e-1", "title");
        Notebook other = Notebook.createNew("Other", null, 1);
        t.repository.saveNotebook(other);
        t.observeOnce(t.repository.observeNotebooks(), v -> v.size() == 2);
        openExisting("e-1");

        onMain(() -> vm.setNotebook(other.getId()));
        onMain(() -> vm.autosave("title", "body"));

        TestRepository.await("notebook saved",
                () -> other.getId().equals(t.getEntry("e-1").getNotebookId()));
    }

    @Test
    public void aFailedTextAutosaveIsRetriedOnceTheVaultIsBack() {
        seed("e-1", "title");
        openExisting("e-1");
        t.lock();

        onMain(() -> vm.autosave("title", "edited"));
        TestRepository.settle();
        t.open();
        assertEquals("body", t.getEntry("e-1").getBody());
        onMain(() -> vm.autosave("title", "edited"));

        TestRepository.await("edit saved after unlock",
                () -> "edited".equals(t.getEntry("e-1").getBody()));
    }

    @Test
    public void aFailedTagAutosaveIsRetriedOnceTheVaultIsBack() {
        seed("e-1", "title");
        openExisting("e-1");
        onMain(() -> vm.addTag("lost"));
        t.lock();

        onMain(() -> vm.autosave("title", "body"));
        TestRepository.settle();
        t.open();
        onMain(() -> vm.autosave("title", "body"));

        TestRepository.await("tag saved after unlock", () -> t.getTagNames("e-1").contains("lost"));
    }

    @Test
    public void saveFinishesTheEditor() {
        openNew();

        onMain(() -> vm.save("title", "body"));

        assertEquals(Boolean.TRUE, vm.finished().getValue());
        assertNotNull(onlyEntry());
    }

    // ---- Delete and undo ---------------------------------------------------------------------

    @Test
    public void deletingRemovesTheEntryFinishesAndOffersUndoWithTags() {
        seed("e-1", "title", "alpha");
        openExisting("e-1");

        onMain(() -> vm.delete());

        assertEquals(Boolean.TRUE, vm.finished().getValue());
        DeletedEntry deleted = t.observeOnce(t.repository.deletedEntry(),
                v -> v.entry.getId().equals("e-1"));
        assertEquals(Collections.singletonList("alpha"), deleted.tagNames);
        assertNull(t.getEntry("e-1"));

        t.restore(deleted);
        assertEquals("title", t.getEntry("e-1").getTitle());
        assertEquals(Collections.singletonList("alpha"), t.getTagNames("e-1"));
    }

    @Test
    public void deletingAnUnsavedNewEntryOnlyFinishes() {
        openNew();

        onMain(() -> vm.delete());
        TestRepository.settle();

        assertEquals(Boolean.TRUE, vm.finished().getValue());
        assertNull(t.repository.deletedEntry().getValue());
    }

    // ---- Tags and unsaved changes ------------------------------------------------------------

    @Test
    public void tagsAreTrimmedAndDeduplicatedIgnoringCase() {
        openNew();

        onMain(() -> {
            vm.addTag(" Mood ");
            vm.addTag("mood");
            vm.addTag("   ");
            vm.addTag(null);
            vm.addTag("Work");
        });

        assertEquals(Arrays.asList("Mood", "Work"), vm.tagNames().getValue());

        onMain(() -> vm.removeTag("MOOD"));

        assertEquals(Collections.singletonList("Work"), vm.tagNames().getValue());
    }

    @Test
    public void unsavedChangesAreDetectedForNewEntries() {
        openNew();
        assertFalse(vm.hasUnsavedChanges("", ""));
        assertTrue(vm.hasUnsavedChanges("x", ""));

        onMain(() -> vm.addTag("t"));
        assertTrue(vm.hasUnsavedChanges("", ""));
    }

    @Test
    public void unsavedChangesAreDetectedForExistingEntries() {
        seed("e-1", "title", "tag");
        openExisting("e-1");

        assertFalse(vm.hasUnsavedChanges("title", "body"));
        assertFalse(vm.hasUnsavedChanges(" title ", "body "));
        assertTrue(vm.hasUnsavedChanges("title", "changed"));

        onMain(() -> vm.addTag("another"));
        assertTrue(vm.hasUnsavedChanges("title", "body"));
    }
}
