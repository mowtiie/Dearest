package com.mowtiie.dearest.data.model;

import androidx.annotation.Nullable;

import java.util.Objects;
import java.util.UUID;

public class Notebook {

    private final String id;
    private String name;
    @Nullable
    private String description;
    private int position;
    private final long createdAt;

    public Notebook(String id, String name, @Nullable String description,
                    int position, long createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.position = position;
        this.createdAt = createdAt;
    }

    public static Notebook createNew(String name, @Nullable String description, int position) {
        return new Notebook(UUID.randomUUID().toString(), name, description, position,
                System.currentTimeMillis());
    }

    public String getId() { return id; }
    public String getName() { return name; }
    @Nullable public String getDescription() { return description; }
    public int getPosition() { return position; }
    public long getCreatedAt() { return createdAt; }

    public void setName(String name) { this.name = name; }
    public void setDescription(@Nullable String description) { this.description = description; }
    public void setPosition(int position) { this.position = position; }

    public boolean hasSameContentAs(Notebook other) {
        return other != null
                && Objects.equals(name, other.name)
                && Objects.equals(description, other.description)
                && position == other.position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notebook)) return false;
        return id.equals(((Notebook) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}