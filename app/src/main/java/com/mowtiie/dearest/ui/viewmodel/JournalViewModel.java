package com.mowtiie.dearest.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.mowtiie.dearest.data.model.Entry;
import com.mowtiie.dearest.data.model.Notebook;

import java.util.List;
import java.util.Objects;

public class JournalViewModel extends DearestViewModel {

    private final MutableLiveData<String> selectedNotebookId = new MutableLiveData<>();

    private final LiveData<List<Notebook>> notebooks;
    private final LiveData<List<Entry>> entries;
    private final LiveData<Notebook> selectedNotebook;

    public JournalViewModel(@NonNull Application application) {
        super(application);

        notebooks = repository().observeNotebooks();

        entries = Transformations.switchMap(selectedNotebookId, id ->
                id == null ? repository().observeAllEntries() : repository().observeEntries(id));

        selectedNotebook = Transformations.switchMap(selectedNotebookId, id ->
                Transformations.map(notebooks, list -> findById(list, id)));

        selectedNotebookId.setValue(null);
    }

    public LiveData<List<Notebook>> notebooks() { return notebooks; }
    public LiveData<List<Entry>> entries() { return entries; }
    public LiveData<Notebook> selectedNotebook() { return selectedNotebook; }
    public LiveData<String> selectedNotebookId() { return selectedNotebookId; }

    public void selectNotebook(@Nullable String notebookId) {
        if (!equal(notebookId, selectedNotebookId.getValue())) {
            selectedNotebookId.setValue(notebookId);
        }
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

    @Nullable
    private static Notebook findById(@Nullable List<Notebook> list, @Nullable String id) {
        if (list == null || id == null) return null;
        for (Notebook n : list) {
            if (n.getId().equals(id)) return n;
        }
        return null;
    }

    private static boolean equal(@Nullable String a, @Nullable String b) {
        return Objects.equals(a, b);
    }
}
