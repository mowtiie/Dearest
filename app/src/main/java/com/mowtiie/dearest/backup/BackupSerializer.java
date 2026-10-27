package com.mowtiie.dearest.backup;

import com.mowtiie.dearest.data.model.Entry;
import com.mowtiie.dearest.data.model.Notebook;
import com.mowtiie.dearest.data.model.Tag;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BackupSerializer {

    private static final int VERSION = 2;
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private BackupSerializer() {}

    public static String toJson(BackupData data) {
        try {
            JSONObject root = new JSONObject();
            root.put("format", "dearest");
            root.put("version", VERSION);
            root.put("exportedAt", System.currentTimeMillis());

            JSONArray notebooks = new JSONArray();
            for (Notebook n : data.notebooks) {
                JSONObject o = new JSONObject();
                o.put("id", n.getId());
                o.put("name", n.getName());
                o.put("description", n.getDescription() == null ? JSONObject.NULL : n.getDescription());
                o.put("position", n.getPosition());
                o.put("createdAt", n.getCreatedAt());
                notebooks.put(o);
            }
            root.put("notebooks", notebooks);

            JSONArray tags = new JSONArray();
            for (Tag t : data.tags) {
                JSONObject o = new JSONObject();
                o.put("id", t.getId());
                o.put("name", t.getName());
                o.put("createdAt", t.getCreatedAt());
                tags.put(o);
            }
            root.put("tags", tags);

            JSONArray entries = new JSONArray();
            for (Entry e : data.entries) {
                JSONObject o = new JSONObject();
                o.put("id", e.getId());
                o.put("notebookId", e.getNotebookId());
                o.put("title", e.getTitle() == null ? JSONObject.NULL : e.getTitle());
                o.put("body", e.getBody() == null ? JSONObject.NULL : e.getBody());
                o.put("createdAt", e.getCreatedAt());
                o.put("updatedAt", e.getUpdatedAt());
                JSONArray tagIds = new JSONArray();
                Set<String> linked = data.entryTagIds.get(e.getId());
                if (linked != null) {
                    for (String tagId : linked) tagIds.put(tagId);
                }
                o.put("tags", tagIds);
                entries.put(o);
            }
            root.put("entries", entries);
            return root.toString();
        } catch (JSONException e) {
            throw new RuntimeException("Backup JSON encoding failed", e);
        }
    }

    public static BackupData fromJson(String json) throws JSONException {
        JSONObject root = new JSONObject(json);

        List<Notebook> notebooks = new ArrayList<>();
        JSONArray na = root.optJSONArray("notebooks");
        if (na != null) {
            for (int i = 0; i < na.length(); i++) {
                JSONObject o = na.getJSONObject(i);
                notebooks.add(new Notebook(
                        o.getString("id"),
                        o.getString("name"),
                        (o.isNull("description")) ? null : o.optString("description", null),
                        o.optInt("position", i),
                        o.optLong("createdAt", System.currentTimeMillis())));
            }
        }

        List<Tag> tags = new ArrayList<>();
        JSONArray ta = root.optJSONArray("tags");
        if (ta != null) {
            for (int i = 0; i < ta.length(); i++) {
                JSONObject o = ta.getJSONObject(i);
                tags.add(new Tag(
                        o.getString("id"),
                        o.getString("name"),
                        o.optLong("createdAt", System.currentTimeMillis())));
            }
        }

        Map<String, Set<String>> entryTagIds = new HashMap<>();
        List<Entry> entries = new ArrayList<>();
        JSONArray ea = root.optJSONArray("entries");
        if (ea != null) {
            for (int i = 0; i < ea.length(); i++) {
                JSONObject o = ea.getJSONObject(i);
                entries.add(new Entry(
                        o.getString("id"),
                        o.getString("notebookId"),
                        o.isNull("title") ? null : o.optString("title"),
                        o.isNull("body") ? null : o.optString("body"),
                        o.optLong("createdAt", System.currentTimeMillis()),
                        o.optLong("updatedAt", System.currentTimeMillis())));
                JSONArray linked = o.optJSONArray("tags");
                if (linked != null && linked.length() > 0) {
                    Set<String> ids = new LinkedHashSet<>();
                    for (int j = 0; j < linked.length(); j++) ids.add(linked.getString(j));
                    entryTagIds.put(o.getString("id"), ids);
                }
            }
        }
        return root.has("tags")
                ? new BackupData(notebooks, entries, tags, entryTagIds)
                : new BackupData(notebooks, entries);
    }

    public static String toPlainText(BackupData data) {
        Map<String, String> names = notebookNames(data);
        StringBuilder sb = new StringBuilder();
        for (Entry e : data.entries) {
            String nb = names.get(e.getNotebookId());
            sb.append(nb == null ? "" : nb).append("  \u2022  ")
                    .append(DATE_FMT.format(Instant.ofEpochMilli(e.getCreatedAt()))).append('\n');
            if (e.getTitle() != null && !e.getTitle().trim().isEmpty()) {
                sb.append(e.getTitle()).append('\n');
            }
            String tagLine = tagNames(data, e);
            if (!tagLine.isEmpty()) sb.append("Tags: ").append(tagLine).append('\n');
            sb.append('\n');
            if (e.getBody() != null) sb.append(e.getBody()).append('\n');
            sb.append("\n----------------------------------------\n\n");
        }
        return sb.toString();
    }

    public static String toCsv(BackupData data) {
        Map<String, String> names = notebookNames(data);
        StringBuilder sb = new StringBuilder();
        sb.append("Notebook,Created,Updated,Title,Body,Tags\r\n");
        for (Entry e : data.entries) {
            String nb = names.get(e.getNotebookId());
            sb.append(csv(nb == null ? "" : nb)).append(',')
                    .append(csv(DATE_FMT.format(Instant.ofEpochMilli(e.getCreatedAt())))).append(',')
                    .append(csv(DATE_FMT.format(Instant.ofEpochMilli(e.getUpdatedAt())))).append(',')
                    .append(csv(e.getTitle() == null ? "" : e.getTitle())).append(',')
                    .append(csv(e.getBody() == null ? "" : e.getBody())).append(',')
                    .append(csv(tagNames(data, e))).append("\r\n");
        }
        return sb.toString();
    }

    private static String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    /** Comma-separated tag names for an entry, sorted case-insensitively. */
    private static String tagNames(BackupData data, Entry entry) {
        Set<String> ids = data.entryTagIds.get(entry.getId());
        if (ids == null || ids.isEmpty()) return "";
        Map<String, String> byId = new HashMap<>();
        for (Tag t : data.tags) byId.put(t.getId(), t.getName());
        List<String> names = new ArrayList<>();
        for (String id : ids) {
            String name = byId.get(id);
            if (name != null) names.add(name);
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return String.join(", ", names);
    }

    private static Map<String, String> notebookNames(BackupData data) {
        Map<String, String> map = new HashMap<>();
        for (Notebook n : data.notebooks) map.put(n.getId(), n.getName());
        return map;
    }
}