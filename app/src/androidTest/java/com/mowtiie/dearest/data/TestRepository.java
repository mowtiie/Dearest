package com.mowtiie.dearest.data;

import android.app.Application;
import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.test.platform.app.InstrumentationRegistry;

import com.mowtiie.dearest.data.db.DearestDatabase;
import com.mowtiie.dearest.data.model.Entry;
import com.mowtiie.dearest.data.model.Notebook;
import com.mowtiie.dearest.data.model.Tag;
import com.mowtiie.dearest.data.repository.DearestRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/**
 * A repository over a throwaway database, with blocking helpers so tests can read straight-line.
 * Tests run off the main thread; LiveData work is hopped onto it here. Always {@link #close()}.
 */
public final class TestRepository implements AutoCloseable {

    private static final long TIMEOUT_MS = 5000;

    public final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    public final Application application = (Application) context.getApplicationContext();
    public final DearestRepository repository;
    private final String dbName = "test-" + UUID.randomUUID() + ".db";

    public TestRepository() {
        DearestDatabase.loadLibrary();
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 9);
        repository = DearestRepository.createForTest(context, () -> true, key::clone, dbName);
    }

    /** Opens the database and waits until the repository is usable. */
    public TestRepository open() {
        repository.open();
        await("repository opens", repository::isOpen);
        return this;
    }

    /** Locks the repository and waits until it is closed. */
    public void lock() {
        repository.close();
        await("repository closes", () -> !repository.isOpen());
    }

    public String firstNotebookId() {
        List<Notebook> notebooks = observeOnce(repository.observeNotebooks(), v -> !v.isEmpty());
        return notebooks.get(0).getId();
    }

    public Entry getEntry(String id) {
        return blocking(cb -> repository.getEntry(id, cb::set));
    }

    public List<Tag> getTags(String entryId) {
        return blocking(cb -> repository.getTagsForEntry(entryId, cb::set));
    }

    public List<String> getTagNames(String entryId) {
        List<String> names = new ArrayList<>();
        for (Tag t : getTags(entryId)) names.add(t.getName());
        return names;
    }

    /** Result of an operation: null error means success. */
    public static final class Outcome {
        public final boolean success;
        public final String error;

        public Outcome(boolean success, String error) {
            this.success = success;
            this.error = error;
        }
    }

    public Outcome save(Entry entry, String... tagNames) {
        return blocking(cb -> repository.saveEntryWithTags(entry, Arrays.asList(tagNames),
                (ok, err) -> cb.set(new Outcome(ok, err))));
    }

    public Outcome restore(com.mowtiie.dearest.data.model.DeletedEntry deleted) {
        return blocking(cb -> repository.restoreEntry(deleted,
                (ok, err) -> cb.set(new Outcome(ok, err))));
    }

    /** Waits for the first value of a LiveData that satisfies the condition. */
    public <T> T observeOnce(LiveData<T> data, java.util.function.Predicate<T> accept) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();
        Observer<T> observer = value -> {
            if (value != null && accept.test(value) && result.compareAndSet(null, value)) {
                latch.countDown();
            }
        };
        runOnMain(() -> data.observeForever(observer));
        try {
            if (!latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                throw new AssertionError("LiveData never produced an accepted value");
            }
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        } finally {
            runOnMain(() -> data.removeObserver(observer));
        }
        return result.get();
    }


    /** Runs an async call that reports through a callback and waits for the callback. */
    public <T> T blocking(java.util.function.Consumer<Setter<T>> call) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> out = new AtomicReference<>();
        Setter<T> setter = value -> {
            out.set(value);
            latch.countDown();
        };
        call.accept(setter);
        try {
            if (!latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                throw new AssertionError("Callback never arrived");
            }
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
        return out.get();
    }

    public interface Setter<T> {
        void set(T value);
    }

    public static void await(String what, BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) throw new AssertionError("Timed out: " + what);
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }
        }
    }

    public static void runOnMain(Runnable r) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(r);
    }

    /** Lets in-flight posts settle, for asserting that something did not happen. */
    public static void settle() {
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void close() {
        if (repository.isOpen()) lock();
        context.deleteDatabase(dbName);
    }
}
