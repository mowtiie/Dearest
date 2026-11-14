package com.mowtiie.dearest.data.model;

import java.util.UUID;

public final class Section {
    private final String id;
    private final String notebookId;
    private final String name;
    private final int position;
    private final long createdAt;

    public Section(String id, String notebookId, String name, int position, long createdAt) {
        this.id = id;
        this.notebookId = notebookId;
        this.name = name;
        this.position = position;
        this.createdAt = createdAt;
    }
    public static Section createNew(String notebookId, String name, int position) {
        return new Section(UUID.randomUUID().toString(), notebookId, name, position, System.currentTimeMillis());
    }
    public String getId() { return id; }
    public String getNotebookId() { return notebookId; }
    public String getName() { return name; }
    public int getPosition() { return position; }
    public long getCreatedAt() { return createdAt; }
}
