package com.mowtiie.dearest.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.lifecycle.Observer;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.data.TestRepository;
import com.mowtiie.dearest.data.TestRepository.Outcome;
import com.mowtiie.dearest.data.model.DeletedEntry;
import com.mowtiie.dearest.data.model.Entry;
import com.mowtiie.dearest.data.model.Notebook;
import com.mowtiie.dearest.data.model.Tag;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(AndroidJUnit4.class)
public class DearestRepositoryTest {

    private TestRepository t;
    private DearestRepository repo;
    private String nb;

    @Before
    public void setUp() {
        t = new TestRepository().open();
        repo = t.repository;
        nb = t.firstNotebookId();
    }

    @After
    public void tearDown() {
        t.close();
    }

    private Entry entry(String id, String title) {
        return new Entry(id, nb, title, "body of " + title, 100L, 200L);
    }

    // ---- Locked vault ------------------------------------------------------------------------

    @Test
    public void writesFailWithTheLockedMessageWhileLocked() {
        t.lock();

        Outcome outcome = t.save(entry("e-1", "t"));

        assertFalse(outcome.success);
        assertEquals(t.context.getString(R.string.error_vault_locked), outcome.error);
    }

    @Test
    public void readsReturnTheirEmptyValueWhileLocked() {
        t.save(entry("e-1", "t"), "tag");
        t.lock();

        assertNull(t.getEntry("e-1"));
        assertTrue(t.getTags("e-1").isEmpty());
        assertTrue(t.observeOnce(repo.observeAllEntries(), v -> true).isEmpty());
        assertTrue(t.observeOnce(repo.observeNotebooks(), v -> true).isEmpty());
        assertTrue(t.observeOnce(repo.observeTags(), v -> true).isEmpty());
    }

    @Test
    public void dataIsBackAfterReopening() {
        t.save(entry("e-1", "t"), "tag");
        t.lock();
        assertFalse(repo.isOpen());

        t.open();

        assertEquals("t", t.getEntry("e-1").getTitle());
        assertEquals(Collections.singletonList("tag"), t.getTagNames("e-1"));
    }

    @Test
    public void observersSeeEmptyListsWhileLockedAndTheDataAgainOnceUnlocked() {
        t.save(entry("e-1", "t"));
        assertEquals(1, t.observeOnce(repo.observeAllEntries(), v -> !v.isEmpty()).size());

        t.lock();
        assertTrue(t.observeOnce(repo.observeAllEntries(), List::isEmpty).isEmpty());

        t.open();
        assertEquals(1, t.observeOnce(repo.observeAllEntries(), v -> !v.isEmpty()).size());
    }

    // ---- Refresh -----------------------------------------------------------------------------

    @Test
    public void writesReachALiveObserver() {
        AtomicInteger size = new AtomicInteger(-1);
        Observer<List<Entry>> observer = list -> size.set(list.size());
        TestRepository.runOnMain(() -> repo.observeAllEntries().observeForever(observer));
        try {
            TestRepository.await("initial empty list", () -> size.get() == 0);

            t.save(entry("e-1", "first"));
            TestRepository.await("list refreshes after save", () -> size.get() == 1);

            repo.deleteEntry("e-1");
            TestRepository.await("list refreshes after delete", () -> size.get() == 0);
        } finally {
            TestRepository.runOnMain(() -> repo.observeAllEntries().removeObserver(observer));
        }
    }

    @Test
    public void savingAnEntryWithoutNewTagsDoesNotReloadTheTagList() {
        AtomicInteger tagLoads = new AtomicInteger();
        Observer<List<Tag>> observer = list -> tagLoads.incrementAndGet();
        TestRepository.runOnMain(() -> repo.observeTags().observeForever(observer));
        try {
            TestRepository.await("initial tag load", () -> tagLoads.get() >= 1);
            TestRepository.settle();
            int before = tagLoads.get();

            t.save(entry("e-1", "no tags"));
            TestRepository.settle();
            assertEquals(before, tagLoads.get());

            t.save(entry("e-2", "new tag"), "fresh");
            TestRepository.await("tag list reloads for a new tag", () -> tagLoads.get() > before);
        } finally {
            TestRepository.runOnMain(() -> repo.observeTags().removeObserver(observer));
        }
    }

    @Test
    public void savingAnEntryDoesNotReloadTheNotebookList() {
        AtomicInteger loads = new AtomicInteger();
        Observer<List<Notebook>> observer = list -> loads.incrementAndGet();
        TestRepository.runOnMain(() -> repo.observeNotebooks().observeForever(observer));
        try {
            TestRepository.await("initial notebook load", () -> loads.get() >= 1);
            TestRepository.settle();
            int before = loads.get();

            t.save(entry("e-1", "t"));
            TestRepository.settle();

            assertEquals(before, loads.get());
        } finally {
            TestRepository.runOnMain(() -> repo.observeNotebooks().removeObserver(observer));
        }
    }

    // ---- Save --------------------------------------------------------------------------------

    @Test
    public void savingAnExistingEntryUpdatesItAndReplacesItsTags() {
        t.save(entry("e-1", "old"), "a", "b");

        t.save(new Entry("e-1", nb, "new", "new body", 100L, 300L), "b", "c");

        Entry saved = t.getEntry("e-1");
        assertEquals("new", saved.getTitle());
        assertEquals(100L, saved.getCreatedAt());
        List<String> names = t.getTagNames("e-1");
        assertEquals(2, names.size());
        assertTrue(names.containsAll(Arrays.asList("b", "c")));
    }

    @Test
    public void blankAndDuplicateTagNamesAreHandled() {
        t.save(entry("e-1", "t"), "  spaced  ", "", "   ", "SPACED");

        assertEquals(Collections.singletonList("spaced"), t.getTagNames("e-1"));
    }

    // ---- Undo --------------------------------------------------------------------------------

    private DeletedEntry deleteAndAwait(String id) {
        repo.deleteEntry(id);
        return t.observeOnce(repo.deletedEntry(), v -> v.entry.getId().equals(id));
    }

    @Test
    public void deletingPublishesASnapshotWithTags() {
        t.save(entry("e-1", "keep me"), "alpha", "beta");

        DeletedEntry deleted = deleteAndAwait("e-1");

        assertNull(t.getEntry("e-1"));
        assertEquals("e-1", deleted.entry.getId());
        assertEquals("body of keep me", deleted.entry.getBody());
        assertEquals(2, deleted.tagNames.size());
        assertTrue(deleted.tagNames.containsAll(Arrays.asList("alpha", "beta")));
    }

    @Test
    public void restoringBringsBackTheEntryAndItsTags() {
        t.save(entry("e-1", "keep me"), "alpha", "beta");
        DeletedEntry deleted = deleteAndAwait("e-1");

        Outcome outcome = t.restore(deleted);

        assertTrue(outcome.success);
        Entry restored = t.getEntry("e-1");
        assertNotNull(restored);
        assertEquals("keep me", restored.getTitle());
        assertEquals(100L, restored.getCreatedAt());
        assertEquals(200L, restored.getUpdatedAt());
        assertTrue(t.getTagNames("e-1").containsAll(Arrays.asList("alpha", "beta")));
    }

    @Test
    public void restoringRecreatesATagThatWasDeletedMeanwhile() {
        t.save(entry("e-1", "x"), "only-here");
        DeletedEntry deleted = deleteAndAwait("e-1");
        Tag tag = t.observeOnce(repo.observeTags(), v -> !v.isEmpty()).get(0);
        repo.deleteTag(tag.getId());
        t.observeOnce(repo.observeTags(), List::isEmpty);

        t.restore(deleted);

        assertEquals(Collections.singletonList("only-here"), t.getTagNames("e-1"));
    }

    @Test
    public void deletingAMissingEntryPublishesNothing() {
        repo.deleteEntry("does-not-exist");
        TestRepository.settle();

        assertNull(repo.deletedEntry().getValue());
    }

    @Test
    public void discardingForgetsOnlyTheMatchingSnapshot() {
        t.save(entry("e-1", "one"));
        t.save(entry("e-2", "two"));
        DeletedEntry first = deleteAndAwait("e-1");
        DeletedEntry second = deleteAndAwait("e-2");

        TestRepository.runOnMain(() -> repo.discardDeleted(first));
        assertSame(second, repo.deletedEntry().getValue());

        TestRepository.runOnMain(() -> repo.discardDeleted(second));
        assertNull(repo.deletedEntry().getValue());
    }

    @Test
    public void lockingDropsTheUndoSnapshot() {
        t.save(entry("e-1", "secret"));
        deleteAndAwait("e-1");

        t.lock();

        TestRepository.await("snapshot cleared", () -> repo.deletedEntry().getValue() == null);
    }

    @Test
    public void restoringWhileLockedFails() {
        t.save(entry("e-1", "x"));
        DeletedEntry deleted = deleteAndAwait("e-1");
        t.lock();

        Outcome outcome = t.restore(deleted);

        assertFalse(outcome.success);
        assertEquals(t.context.getString(R.string.error_vault_locked), outcome.error);
    }

    @Test
    public void deleteReportsSuccessAndFailure() {
        t.save(entry("e-1", "t"));

        Outcome ok = t.blocking(cb -> repo.deleteEntry("e-1",
                (success, err) -> cb.set(new Outcome(success, err))));
        assertTrue(ok.success);
        assertNull(t.getEntry("e-1"));

        t.lock();
        Outcome locked = t.blocking(cb -> repo.deleteEntry("e-1",
                (success, err) -> cb.set(new Outcome(success, err))));
        assertFalse(locked.success);
        assertEquals(t.context.getString(R.string.error_vault_locked), locked.error);
    }

    @Test
    public void theUndoSnapshotExpiresOnItsOwn() {
        repo.setUndoExpiryMs(300);
        t.save(entry("e-1", "t"));

        deleteAndAwait("e-1");

        TestRepository.await("snapshot expires", () -> repo.deletedEntry().getValue() == null);
    }

    @Test
    public void aSnapshotKnowsHowMuchOfItsWindowIsLeft() {
        t.save(entry("e-1", "t"));
        DeletedEntry deleted = deleteAndAwait("e-1");

        long remaining = deleted.remainingMs();

        assertTrue(remaining > 0 && remaining <= DeletedEntry.UNDO_WINDOW_MS);
    }

    @Test
    public void restoringAfterItsNotebookWasDeletedUsesAnotherNotebook() {
        Notebook other = addNotebook();
        t.save(new Entry("e-1", other.getId(), "t", "b", 1L, 1L), "tag");
        DeletedEntry deleted = deleteAndAwait("e-1");
        Outcome removed = t.blocking(cb -> repo.deleteNotebook(other.getId(), null,
                (ok, err) -> cb.set(new Outcome(ok, err))));
        assertTrue(removed.success);

        Outcome outcome = t.restore(deleted);

        assertTrue(outcome.success);
        Entry restored = t.getEntry("e-1");
        assertEquals(nb, restored.getNotebookId());
        assertEquals("t", restored.getTitle());
        assertEquals(Collections.singletonList("tag"), t.getTagNames("e-1"));
    }

    // ---- Notebooks ---------------------------------------------------------------------------

    private Notebook addNotebook() {
        Notebook other = Notebook.createNew("Other", null, 1);
        repo.saveNotebook(other);
        t.observeOnce(repo.observeNotebooks(), v -> v.size() == 2);
        return other;
    }

    @Test
    public void deletingANonEmptyNotebookWithoutATargetIsRefused() {
        Notebook other = addNotebook();
        t.save(new Entry("e-1", other.getId(), "t", "b", 1L, 1L));

        Outcome outcome = t.blocking(cb -> repo.deleteNotebook(other.getId(), null,
                (ok, err) -> cb.set(new Outcome(ok, err))));

        assertFalse(outcome.success);
        assertNotNull(outcome.error);
        assertEquals(other.getId(), t.getEntry("e-1").getNotebookId());
    }

    @Test
    public void deletingANotebookMovesItsEntries() {
        Notebook other = addNotebook();
        t.save(new Entry("e-1", other.getId(), "t", "b", 1L, 1L));

        Outcome outcome = t.blocking(cb -> repo.deleteNotebook(other.getId(), nb,
                (ok, err) -> cb.set(new Outcome(ok, err))));

        assertTrue(outcome.success);
        assertEquals(nb, t.getEntry("e-1").getNotebookId());
        assertEquals(1, t.observeOnce(repo.observeNotebooks(), v -> v.size() == 1).size());
    }
}
