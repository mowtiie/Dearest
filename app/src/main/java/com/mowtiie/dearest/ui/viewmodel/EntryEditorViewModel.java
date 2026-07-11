package com.mowtiie.dearest.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mowtiie.dearest.data.model.Entry;
import com.mowtiie.dearest.data.model.Notebook;

import java.util.List;

public class EntryEditorViewModel extends DearestViewModel {

    private final MutableLiveData<Entry> entry = new MutableLiveData<>();
    private final MutableLiveData<Boolean> finished = new MutableLiveData<>(false);
    private final MutableLiveData<String> notebookId = new MutableLiveData<>();
    private final LiveData<List<Notebook>> notebooks;

    private boolean initialized;
    private String entryId;
    private Entry original;

    public EntryEditorViewModel(@NonNull Application application) {
        super(application);
        notebooks = repository().observeNotebooks();
    }

    public void init(@Nullable String existingEntryId, @Nullable String targetNotebookId) {
        if (initialized) return;
        initialized = true;
        this.entryId = existingEntryId;

        if (existingEntryId == null) {
            entry.setValue(null);
            notebookId.setValue(targetNotebookId);
        } else {
            repository().getEntry(existingEntryId, loaded -> {
                original = loaded;
                if (loaded != null) {
                    notebookId.setValue(loaded.getNotebookId());
                }
                entry.setValue(loaded);
            });
        }
    }

    public LiveData<Entry>          entry()      { return entry; }
    public LiveData<Boolean>        finished()   { return finished; }
    public LiveData<String>         notebookId() { return notebookId; }
    public LiveData<List<Notebook>> notebooks()  { return notebooks; }

    public boolean isNew() {
        return entryId == null;
    }

    public void setNotebook(String id) {
        if (id != null && !id.equals(notebookId.getValue())) {
            notebookId.setValue(id);
        }
    }

    public void save(String title, String body) {
        String t = normalize(title);
        String b = normalize(body);
        String nb = notebookId.getValue();

        if (isNew()) {
            if (t.isEmpty() && b.isEmpty()) {
                finished.setValue(true);
                return;
            }
            if (nb == null) {
                finished.setValue(true);
                return;
            }
            Entry created = Entry.createNew(nb, t, b);
            entryId = created.getId();
            original = created;
            repository().saveEntry(created);
        } else {
            boolean notebookChanged = original != null && !equal(original.getNotebookId(), nb);
            if (original != null && !notebookChanged
                    && equal(original.getTitle(), t) && equal(original.getBody(), b)) {
                finished.setValue(true);
                return;
            }
            Entry base = (original != null)
                    ? original
                    : new Entry(entryId, nb, t, b,
                    System.currentTimeMillis(), System.currentTimeMillis());
            base.setTitle(t);
            base.setBody(b);
            if (nb != null) base.setNotebookId(nb);
            base.touch();
            original = base;
            repository().saveEntry(base);
        }
        finished.setValue(true);
    }

    public void delete() {
        if (!isNew() && entryId != null) {
            repository().deleteEntry(entryId);
        }
        finished.setValue(true);
    }

    public boolean hasUnsavedChanges(String title, String body) {
        String t = normalize(title);
        String b = normalize(body);
        if (isNew()) {
            return !t.isEmpty() || !b.isEmpty();
        }
        if (original == null) return false;
        return !equal(original.getTitle(), t)
                || !equal(original.getBody(), b)
                || !equal(original.getNotebookId(), notebookId.getValue());
    }

    private static String normalize(@Nullable String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean equal(@Nullable String a, @Nullable String b) {
        return normalize(a).equals(normalize(b));
    }
}