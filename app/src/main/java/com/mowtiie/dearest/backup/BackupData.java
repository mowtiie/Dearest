package com.mowtiie.dearest.backup;

import com.mowtiie.dearest.data.model.Entry;
import com.mowtiie.dearest.data.model.Notebook;

import java.util.List;

public final class BackupData {

    public final List<Notebook> notebooks;
    public final List<Entry> entries;

    public BackupData(List<Notebook> notebooks, List<Entry> entries) {
        this.notebooks = notebooks;
        this.entries = entries;
    }
}

