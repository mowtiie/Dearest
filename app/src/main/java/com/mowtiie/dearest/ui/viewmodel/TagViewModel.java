package com.mowtiie.dearest.ui.viewmodel;


import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.mowtiie.dearest.data.model.Tag;

public class TagViewModel extends DearestViewModel {

    public interface RenameCallback {
        void onResult(boolean success, String error);
    }

    private final LiveData<java.util.List<Tag>> tags;

    public TagViewModel(@NonNull Application application) {
        super(application);
        tags = repository().observeTags();
    }

    public LiveData<java.util.List<Tag>> tags() {
        return tags;
    }

    public void renameTag(Tag tag, String newName, RenameCallback cb) {
        String trimmed = newName == null ? "" : newName.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase(tag.getName())) {
            cb.onResult(true, null);
            return;
        }
        repository().renameTag(tag.getId(), trimmed, cb::onResult);
    }

    public void deleteTag(Tag tag) {
        repository().deleteTag(tag.getId());
    }
}