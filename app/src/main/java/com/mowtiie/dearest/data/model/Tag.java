package com.mowtiie.dearest.data.model;

import java.util.Objects;
import java.util.UUID;

public class Tag {

    private final String id;
    private String name;
    private final long createdAt;

    public Tag(String id, String name, long createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    public static Tag createNew(String name) {
        return new Tag(UUID.randomUUID().toString(), name, System.currentTimeMillis());
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public long getCreatedAt() { return createdAt; }

    public void setName(String name) { this.name = name; }

    public boolean hasSameContentAs(Tag other) {
        return other != null && Objects.equals(name, other.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag)) return false;
        return id.equals(((Tag) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
