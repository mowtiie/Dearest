package com.mowtiie.dearest.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.mowtiie.dearest.data.model.Notebook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class NotebookViewModel extends DearestViewModel {

    public enum Sort { MANUAL, NAME_ASC, NAME_DESC }

    public interface DeleteCallback {
        void onResult(boolean success, @Nullable String error);
    }

    private final LiveData<List<Notebook>> allNotebooks;
    private final MutableLiveData<String> query = new MutableLiveData<>("");
    private final MutableLiveData<Sort> sort = new MutableLiveData<>(Sort.MANUAL);
    private final MediatorLiveData<List<Notebook>> notebooks = new MediatorLiveData<>();
    private final MediatorLiveData<Boolean> canReorder = new MediatorLiveData<>();

    public NotebookViewModel(@NonNull Application application) {
        super(application);
        allNotebooks = repository().observeNotebooks();

        notebooks.addSource(allNotebooks, x -> recompute());
        notebooks.addSource(query, x -> recompute());
        notebooks.addSource(sort, x -> recompute());

        canReorder.addSource(query, x -> recomputeReorder());
        canReorder.addSource(sort, x -> recomputeReorder());
        recomputeReorder();
    }

    public LiveData<List<Notebook>> notebooks()   { return notebooks; }
    public LiveData<Sort>           sort()        { return sort; }
    public LiveData<Boolean>        canReorder()  { return canReorder; }

    public void setQuery(@Nullable String q) {
        String v = (q == null) ? "" : q;
        if (!v.equals(query.getValue())) query.setValue(v);
    }

    public void setSort(Sort s) {
        if (s != null && s != sort.getValue()) sort.setValue(s);
    }

    public void createNotebook(String name, @Nullable String description) {
        String n = normalize(name);
        if (n.isEmpty()) return;
        repository().saveNotebook(Notebook.createNew(n, blankToNull(description), nextPosition()));
    }

    public void updateNotebook(Notebook notebook, String newName, @Nullable String newDescription) {
        String n = normalize(newName);
        String d = blankToNull(newDescription);
        if (n.isEmpty()) return;
        if (n.equals(notebook.getName()) && java.util.Objects.equals(d, notebook.getDescription())) {
            return;
        }
        repository().saveNotebook(
                new Notebook(notebook.getId(), n, d, notebook.getPosition(), notebook.getCreatedAt()));
    }

    public void saveOrder(List<Notebook> ordered) {
        for (int i = 0; i < ordered.size(); i++) {
            Notebook nb = ordered.get(i);
            if (nb.getPosition() != i) {
                repository().saveNotebook(
                        new Notebook(nb.getId(), nb.getName(), nb.getDescription(), i, nb.getCreatedAt()));
            }
        }
    }

    public void countEntries(String notebookId, Consumer<Integer> callback) {
        repository().countEntries(notebookId, callback::accept);
    }

    public void deleteNotebook(String notebookId, @Nullable String moveEntriesToId,
                               DeleteCallback callback) {
        repository().deleteNotebook(notebookId, moveEntriesToId, callback::onResult);
    }

    private void recompute() {
        List<Notebook> base = allNotebooks.getValue();
        if (base == null) {
            notebooks.setValue(Collections.emptyList());
            return;
        }
        String q = query.getValue();
        List<Notebook> out = new ArrayList<>();
        if (q == null || q.trim().isEmpty()) {
            out.addAll(base);
        } else {
            String needle = q.trim().toLowerCase(Locale.getDefault());
            for (Notebook n : base) {
                if (n.getName().toLowerCase(Locale.getDefault()).contains(needle)) out.add(n);
            }
        }
        Sort s = sort.getValue();
        if (s == Sort.NAME_ASC) {
            Collections.sort(out, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        } else if (s == Sort.NAME_DESC) {
            Collections.sort(out, (a, b) -> b.getName().compareToIgnoreCase(a.getName()));
        }
        notebooks.setValue(out);
    }

    private void recomputeReorder() {
        boolean noQuery = query.getValue() == null || query.getValue().trim().isEmpty();
        canReorder.setValue(sort.getValue() == Sort.MANUAL && noQuery);
    }

    private int nextPosition() {
        List<Notebook> list = allNotebooks.getValue();
        int next = 0;
        if (list != null) {
            for (Notebook n : list) next = Math.max(next, n.getPosition() + 1);
        }
        return next;
    }

    private static String normalize(@Nullable String s) {
        return s == null ? "" : s.trim();
    }

    @Nullable
    private static String blankToNull(@Nullable String s) {
        String n = normalize(s);
        return n.isEmpty() ? null : n;
    }
}