package com.mowtiie.dearest.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.mowtiie.dearest.data.model.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TagPickerViewModel extends DearestViewModel {

    public enum Sort { NAME_ASC, NAME_DESC }

    private final LiveData<List<Tag>> allTags;
    private final MutableLiveData<String> query = new MutableLiveData<>("");
    private final MutableLiveData<Sort> sort = new MutableLiveData<>(Sort.NAME_ASC);
    private final MutableLiveData<Set<String>> selectedIds = new MutableLiveData<>(new HashSet<>());
    private final MediatorLiveData<List<Tag>> visibleTags = new MediatorLiveData<>();

    private boolean initialSelectionApplied;
    private List<String> pendingInitialNames = Collections.emptyList();

    public TagPickerViewModel(@NonNull Application application) {
        super(application);
        allTags = repository().observeTags();

        visibleTags.addSource(allTags, tags -> {
            applyInitialSelectionIfNeeded(tags);
            recompute();
        });
        visibleTags.addSource(query, q -> recompute());
        visibleTags.addSource(sort, s -> recompute());
    }

    public void setInitialSelection(List<String> names) {
        this.pendingInitialNames = names;
        applyInitialSelectionIfNeeded(allTags.getValue());
    }

    public LiveData<List<Tag>>     visibleTags() { return visibleTags; }
    public LiveData<Set<String>>   selectedIds() { return selectedIds; }
    public LiveData<Sort>          sort()        { return sort; }

    public void setQuery(@Nullable String q) {
        String v = (q == null) ? "" : q;
        if (!v.equals(query.getValue())) query.setValue(v);
    }

    public void setSort(Sort s) {
        if (s != null && s != sort.getValue()) sort.setValue(s);
    }

    public void toggle(String tagId) {
        Set<String> current = selectedIds.getValue();
        Set<String> next = new HashSet<>(current == null ? Collections.emptySet() : current);
        if (!next.remove(tagId)) {
            next.add(tagId);
        }
        selectedIds.setValue(next);
    }

    public void createTag(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) return;
        repository().createTag(trimmed, (ok, error) -> {
            if (!ok) return;
            List<Tag> current = allTags.getValue();
            if (current == null) return;
            for (Tag t : current) {
                if (t.getName().equalsIgnoreCase(trimmed)) {
                    selectNewly(t.getId());
                    return;
                }
            }
        });
    }

    public List<String> selectedNames() {
        List<Tag> tags = allTags.getValue();
        Set<String> ids = selectedIds.getValue();
        List<String> names = new ArrayList<>();
        if (tags != null && ids != null) {
            for (Tag t : tags) {
                if (ids.contains(t.getId())) names.add(t.getName());
            }
        }
        return names;
    }

    private void selectNewly(String tagId) {
        Set<String> current = selectedIds.getValue();
        Set<String> next = new HashSet<>(current == null ? Collections.emptySet() : current);
        next.add(tagId);
        selectedIds.setValue(next);
    }

    private void applyInitialSelectionIfNeeded(@Nullable List<Tag> tags) {
        if (initialSelectionApplied || tags == null) return;
        initialSelectionApplied = true;
        Set<String> ids = new HashSet<>();
        for (Tag t : tags) {
            for (String name : pendingInitialNames) {
                if (t.getName().equalsIgnoreCase(name)) {
                    ids.add(t.getId());
                    break;
                }
            }
        }
        selectedIds.setValue(ids);
    }

    private void recompute() {
        List<Tag> base = allTags.getValue();
        if (base == null) {
            visibleTags.setValue(Collections.emptyList());
            return;
        }
        String q = query.getValue();
        List<Tag> out = new ArrayList<>();
        if (q == null || q.trim().isEmpty()) {
            out.addAll(base);
        } else {
            String needle = q.trim().toLowerCase(Locale.getDefault());
            for (Tag t : base) {
                if (t.getName().toLowerCase(Locale.getDefault()).contains(needle)) out.add(t);
            }
        }
        if (sort.getValue() == Sort.NAME_DESC) {
            Collections.sort(out, (a, b) -> b.getName().compareToIgnoreCase(a.getName()));
        } else {
            Collections.sort(out, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        }
        visibleTags.setValue(out);
    }
}