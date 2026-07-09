package com.mowtiie.dearest.data.model;


import java.util.Objects;
import java.util.UUID;

public class Notebook {

    private final String id;
    private String name;
    private int position;
    private final long createdAt;

    public Notebook(String id, String name, int position, long createdAt) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.createdAt = createdAt;
    }

    public static Notebook createNew(String name, int position) {
        return new Notebook(UUID.randomUUID().toString(), name, position, System.currentTimeMillis());
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getPosition() { return position; }
    public long getCreatedAt() { return createdAt; }

    public void setName(String name) { this.name = name; }
    public void setPosition(int position) { this.position = position; }

    public boolean hasSameContentAs(Notebook other) {
        return other != null
                && Objects.equals(name, other.name)
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