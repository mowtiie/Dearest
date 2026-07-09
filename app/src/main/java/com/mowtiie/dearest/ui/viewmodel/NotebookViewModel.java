package com.mowtiie.dearest.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.mowtiie.dearest.data.model.Notebook;

import java.util.List;
import java.util.function.Consumer;

public class NotebookViewModel extends DearestViewModel {

    public interface DeleteCallback {
        void onResult(boolean success, @Nullable String error);
    }

    private final LiveData<List<Notebook>> notebooks;

    public NotebookViewModel(@NonNull Application application) {
        super(application);
        notebooks = repository().observeNotebooks();
    }

    public LiveData<List<Notebook>> notebooks() {
        return notebooks;
    }

    public void createNotebook(String name) {
        String n = normalize(name);
        if (n.isEmpty()) return;
        repository().saveNotebook(Notebook.createNew(n, nextPosition()));
    }

    public void renameNotebook(Notebook notebook, String newName) {
        String n = normalize(newName);
        if (n.isEmpty() || n.equals(notebook.getName())) return;
        repository().saveNotebook(
                new Notebook(notebook.getId(), n, notebook.getPosition(), notebook.getCreatedAt()));
    }

    public void saveOrder(List<Notebook> ordered) {
        for (int i = 0; i < ordered.size(); i++) {
            Notebook nb = ordered.get(i);
            if (nb.getPosition() != i) {
                repository().saveNotebook(
                        new Notebook(nb.getId(), nb.getName(), i, nb.getCreatedAt()));
            }
        }
    }

    public void countEntries(String notebookId, Consumer<Integer> callback) {
        repository().countEntries(notebookId, callback::accept);
    }

    public void deleteNotebook(String notebookId, @Nullable String moveEntriesToId, DeleteCallback callback) {
        repository().deleteNotebook(notebookId, moveEntriesToId, callback::onResult);
    }

    private int nextPosition() {
        List<Notebook> list = notebooks.getValue();
        int next = 0;
        if (list != null) {
            for (Notebook n : list) next = Math.max(next, n.getPosition() + 1);
        }
        return next;
    }

    private static String normalize(@Nullable String s) {
        return s == null ? "" : s.trim();
    }
}