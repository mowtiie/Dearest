package com.mowtiie.dearest.data;


import java.util.Objects;
import java.util.UUID;

public class Entry {

    private final String id;
    private String notebookId;
    private String title;
    private String body;
    private final long createdAt;
    private long updatedAt;

    public Entry(String id, String notebookId, String title, String body,
                 long createdAt, long updatedAt) {
        this.id = id;
        this.notebookId = notebookId;
        this.title = title;
        this.body = body;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Entry createNew(String notebookId, String title, String body) {
        long now = System.currentTimeMillis();
        return new Entry(UUID.randomUUID().toString(), notebookId, title, body, now, now);
    }

    public String getId() { return id; }
    public String getNotebookId() { return notebookId; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }

    public void setNotebookId(String notebookId) { this.notebookId = notebookId; }
    public void setTitle(String title) { this.title = title; }
    public void setBody(String body) { this.body = body; }

    public void touch() { this.updatedAt = System.currentTimeMillis(); }

    public boolean hasSameContentAs(Entry other) {
        return other != null
                && Objects.equals(title, other.title)
                && Objects.equals(body, other.body)
                && Objects.equals(notebookId, other.notebookId)
                && updatedAt == other.updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entry)) return false;
        return id.equals(((Entry) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}