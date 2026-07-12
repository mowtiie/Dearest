package com.mowtiie.dearest.backup;

import com.mowtiie.dearest.data.model.Entry;
import com.mowtiie.dearest.data.model.Notebook;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BackupSerializer {

    private static final int VERSION = 1;
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

            JSONArray entries = new JSONArray();
            for (Entry e : data.entries) {
                JSONObject o = new JSONObject();
                o.put("id", e.getId());
                o.put("notebookId", e.getNotebookId());
                o.put("title", e.getTitle() == null ? JSONObject.NULL : e.getTitle());
                o.put("body", e.getBody() == null ? JSONObject.NULL : e.getBody());
                o.put("createdAt", e.getCreatedAt());
                o.put("updatedAt", e.getUpdatedAt());
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
            }
        }
        return new BackupData(notebooks, entries);
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
            sb.append('\n');
            if (e.getBody() != null) sb.append(e.getBody()).append('\n');
            sb.append("\n----------------------------------------\n\n");
        }
        return sb.toString();
    }

    public static String toCsv(BackupData data) {
        Map<String, String> names = notebookNames(data);
        StringBuilder sb = new StringBuilder();
        sb.append("Notebook,Created,Updated,Title,Body\r\n");
        for (Entry e : data.entries) {
            String nb = names.get(e.getNotebookId());
            sb.append(csv(nb == null ? "" : nb)).append(',')
                    .append(csv(DATE_FMT.format(Instant.ofEpochMilli(e.getCreatedAt())))).append(',')
                    .append(csv(DATE_FMT.format(Instant.ofEpochMilli(e.getUpdatedAt())))).append(',')
                    .append(csv(e.getTitle() == null ? "" : e.getTitle())).append(',')
                    .append(csv(e.getBody() == null ? "" : e.getBody())).append("\r\n");
        }
        return sb.toString();
    }

    private static String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static Map<String, String> notebookNames(BackupData data) {
        Map<String, String> map = new HashMap<>();
        for (Notebook n : data.notebooks) map.put(n.getId(), n.getName());
        return map;
    }
}