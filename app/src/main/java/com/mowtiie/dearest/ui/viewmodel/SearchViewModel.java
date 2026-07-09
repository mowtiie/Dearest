package com.mowtiie.dearest.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.mowtiie.dearest.data.model.Entry;

import java.util.List;

public class SearchViewModel extends DearestViewModel {

    private static final long DEBOUNCE_MS = 250L;

    private final MutableLiveData<String> query = new MutableLiveData<>("");
    private final LiveData<List<Entry>> results;

    private String pendingText = "";
    private final Runnable commit = () -> query.setValue(pendingText);

    public SearchViewModel(@NonNull Application application) {
        super(application);
        results = Transformations.switchMap(query, q ->
                repository().observeSearch(q == null ? "" : q));
    }

    public LiveData<List<Entry>> results() {
        return results;
    }

    public LiveData<String> query() {
        return query;
    }

    public void setQuery(@Nullable String text) {
        String t = (text == null) ? "" : text;
        pendingText = t;
        main.removeCallbacks(commit);
        if (t.trim().isEmpty()) {
            query.setValue("");
        } else {
            main.postDelayed(commit, DEBOUNCE_MS);
        }
    }

    @Override
    protected void onCleared() {
        main.removeCallbacks(commit);
        super.onCleared();
    }
}