package com.mowtiie.dearest.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.mowtiie.dearest.data.model.Entry;
import com.mowtiie.dearest.data.model.Notebook;
import com.mowtiie.dearest.data.model.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class JournalViewModel extends DearestViewModel {

    public enum Sort { NEWEST, OLDEST, TITLE }

    private final MutableLiveData<String> selectedNotebookId = new MutableLiveData<>();
    private final MutableLiveData<Set<String>> selectedTagIds = new MutableLiveData<>(new HashSet<>());
    private final MutableLiveData<String> query = new MutableLiveData<>("");
    private final MutableLiveData<Sort> sort = new MutableLiveData<>(Sort.NEWEST);

    private final LiveData<List<Notebook>> notebooks;
    private final LiveData<List<Tag>> tags;
    private final LiveData<Map<String, Set<String>>> entryTagLinks;
    private final LiveData<List<Entry>> notebookEntries;
    private final LiveData<Notebook> selectedNotebook;
    private final MediatorLiveData<List<Entry>> entries = new MediatorLiveData<>();

    public JournalViewModel(@NonNull Application application) {
        super(application);

        notebooks = repository().observeNotebooks();
        tags = repository().observeTags();
        entryTagLinks = repository().observeEntryTagLinks();

        notebookEntries = Transformations.switchMap(selectedNotebookId, id ->
                id == null ? repository().observeAllEntries() : repository().observeEntries(id));

        selectedNotebook = Transformations.switchMap(selectedNotebookId, id ->
                Transformations.map(notebooks, list -> findById(list, id)));

        entries.addSource(notebookEntries, e -> recompute());
        entries.addSource(entryTagLinks, m -> recompute());
        entries.addSource(selectedTagIds, s -> recompute());
        entries.addSource(query, q -> recompute());
        entries.addSource(sort, s -> recompute());

        selectedNotebookId.setValue(null);
    }

    public LiveData<List<Notebook>>            notebooks()          { return notebooks; }
    public LiveData<List<Tag>>                 tags()               { return tags; }
    public LiveData<List<Entry>>               entries()            { return entries; }
    public LiveData<Notebook>                  selectedNotebook()   { return selectedNotebook; }
    public LiveData<String>                    selectedNotebookId() { return selectedNotebookId; }
    public LiveData<Set<String>>               selectedTagIds()     { return selectedTagIds; }
    public LiveData<Sort>                      sort()               { return sort; }

    public void selectNotebook(@Nullable String notebookId) {
        if (!equal(notebookId, selectedNotebookId.getValue())) {
            selectedNotebookId.setValue(notebookId);
        }
    }

    public void toggleTag(String tagId) {
        Set<String> current = selectedTagIds.getValue();
        Set<String> next = new HashSet<>(current == null ? Collections.emptySet() : current);
        if (!next.remove(tagId)) {
            next.add(tagId);
        }
        selectedTagIds.setValue(next);
    }

    public void clearTagFilter() {
        if (selectedTagIds.getValue() != null && !selectedTagIds.getValue().isEmpty()) {
            selectedTagIds.setValue(new HashSet<>());
        }
    }

    public void setQuery(@Nullable String q) {
        String v = (q == null) ? "" : q;
        if (!v.equals(query.getValue())) query.setValue(v);
    }

    public void setSort(Sort s) {
        if (s != null && s != sort.getValue()) sort.setValue(s);
    }

    @Nullable
    public String notebookForNewEntry() {
        String id = selectedNotebookId.getValue();
        if (id != null) return id;
        List<Notebook> list = notebooks.getValue();
        return (list != null && !list.isEmpty()) ? list.get(0).getId() : null;
    }

    public void deleteEntry(String entryId) {
        repository().deleteEntry(entryId);
    }

    private void recompute() {
        List<Entry> base = notebookEntries.getValue();
        if (base == null) {
            entries.setValue(Collections.emptyList());
            return;
        }

        Set<String> wantedTags = selectedTagIds.getValue();
        Map<String, Set<String>> links = entryTagLinks.getValue();
        String q = query.getValue();
        String needle = (q == null || q.trim().isEmpty()) ? null : q.trim().toLowerCase(Locale.getDefault());

        List<Entry> out = new ArrayList<>();
        for (Entry e : base) {
            if (wantedTags != null && !wantedTags.isEmpty() && !matchesAnyTag(e, wantedTags, links)) {
                continue;
            }
            if (needle != null && !matchesText(e, needle)) {
                continue;
            }
            out.add(e);
        }
        sortEntries(out, sort.getValue());
        entries.setValue(out);
    }

    private static boolean matchesAnyTag(Entry e, Set<String> wantedTags,
                                         @Nullable Map<String, Set<String>> links) {
        if (links == null) return false;
        Set<String> entryTags = links.get(e.getId());
        if (entryTags == null || entryTags.isEmpty()) return false;
        for (String tagId : wantedTags) {
            if (entryTags.contains(tagId)) return true;
        }
        return false;
    }

    private static boolean matchesText(Entry e, String needle) {
        String t = e.getTitle() == null ? "" : e.getTitle().toLowerCase(Locale.getDefault());
        String b = e.getBody() == null ? "" : e.getBody().toLowerCase(Locale.getDefault());
        return t.contains(needle) || b.contains(needle);
    }

    private static void sortEntries(List<Entry> list, @Nullable Sort sort) {
        Sort s = (sort == null) ? Sort.NEWEST : sort;
        switch (s) {
            case OLDEST:
                Collections.sort(list, (a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()));
                break;
            case TITLE:
                Collections.sort(list, (a, b) -> title(a).compareToIgnoreCase(title(b)));
                break;
            default:
                Collections.sort(list, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                break;
        }
    }

    private static String title(Entry e) {
        return e.getTitle() == null ? "" : e.getTitle();
    }

    @Nullable
    private static Notebook findById(@Nullable List<Notebook> list, @Nullable String id) {
        if (list == null || id == null) return null;
        for (Notebook n : list) {
            if (n.getId().equals(id)) return n;
        }
        return null;
    }

    private static boolean equal(@Nullable String a, @Nullable String b) {
        return a == null ? b == null : a.equals(b);
    }
}