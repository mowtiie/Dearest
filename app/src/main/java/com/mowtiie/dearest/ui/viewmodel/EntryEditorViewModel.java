package com.mowtiie.dearest.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mowtiie.dearest.data.model.Entry;
import com.mowtiie.dearest.data.model.Notebook;
import com.mowtiie.dearest.data.model.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class EntryEditorViewModel extends DearestViewModel {

    private final MutableLiveData<Entry> entry = new MutableLiveData<>();
    private final MutableLiveData<Boolean> finished = new MutableLiveData<>(false);
    private final MutableLiveData<String> notebookId = new MutableLiveData<>();
    private final MutableLiveData<List<String>> tagNames = new MutableLiveData<>(Collections.emptyList());
    private final LiveData<List<Notebook>> notebooks;
    private final LiveData<List<Tag>> allTags;

    private boolean initialized;
    private String entryId;
    private Entry original;
    private List<String> originalTagNames = Collections.emptyList();

    public EntryEditorViewModel(@NonNull Application application) {
        super(application);
        notebooks = repository().observeNotebooks();
        allTags = repository().observeTags();
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
            repository().getTagsForEntry(existingEntryId, loadedTags -> {
                List<String> names = new ArrayList<>();
                for (Tag t : loadedTags) names.add(t.getName());
                originalTagNames = names;
                tagNames.setValue(names);
            });
        }
    }

    public LiveData<Entry>          entry()      { return entry; }
    public LiveData<Boolean>        finished()   { return finished; }
    public LiveData<String>         notebookId() { return notebookId; }
    public LiveData<List<Notebook>> notebooks()  { return notebooks; }
    public LiveData<List<String>>   tagNames()   { return tagNames; }
    public LiveData<List<Tag>>      allTags()    { return allTags; }

    public boolean isNew() {
        return entryId == null;
    }

    public void setNotebook(String id) {
        if (id != null && !id.equals(notebookId.getValue())) {
            notebookId.setValue(id);
        }
    }

    public void addTag(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) return;
        List<String> current = tagNames.getValue();
        List<String> next = new ArrayList<>(current == null ? Collections.emptyList() : current);
        for (String existing : next) {
            if (existing.equalsIgnoreCase(trimmed)) return;
        }
        next.add(trimmed);
        tagNames.setValue(next);
    }

    public void removeTag(String name) {
        List<String> current = tagNames.getValue();
        if (current == null) return;
        List<String> next = new ArrayList<>(current);
        next.removeIf(n -> n.equalsIgnoreCase(name));
        tagNames.setValue(next);
    }

    public void setTags(List<String> names) {
        tagNames.setValue(new ArrayList<>(names));
    }

    public void save(String title, String body) {
        String t = normalize(title);
        String b = normalize(body);
        String nb = notebookId.getValue();
        List<String> tags = tagNames.getValue();
        if (tags == null) tags = Collections.emptyList();

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
            originalTagNames = tags;
            repository().saveEntryWithTags(created, tags, (ok, err) -> { /* best-effort */ });
        } else {
            boolean notebookChanged = original != null && !equal(original.getNotebookId(), nb);
            boolean tagsChanged = !sameTags(originalTagNames, tags);
            if (original != null && !notebookChanged && !tagsChanged
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
            originalTagNames = tags;
            repository().saveEntryWithTags(base, tags, (ok, err) -> { /* best-effort */ });
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
        List<String> tags = tagNames.getValue();
        if (isNew()) {
            return !t.isEmpty() || !b.isEmpty() || (tags != null && !tags.isEmpty());
        }
        if (original == null) return false;
        return !equal(original.getTitle(), t)
                || !equal(original.getBody(), b)
                || !equal(original.getNotebookId(), notebookId.getValue())
                || !sameTags(originalTagNames, tags);
    }

    private static String normalize(@Nullable String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean equal(@Nullable String a, @Nullable String b) {
        return normalize(a).equals(normalize(b));
    }

    private static boolean sameTags(@Nullable List<String> a, @Nullable List<String> b) {
        Set<String> sa = normalizeSet(a);
        Set<String> sb = normalizeSet(b);
        return sa.equals(sb);
    }

    private static Set<String> normalizeSet(@Nullable List<String> list) {
        Set<String> set = new LinkedHashSet<>();
        if (list != null) {
            for (String s : list) set.add(s.toLowerCase(Locale.getDefault()));
        }
        return set;
    }
}