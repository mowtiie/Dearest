package com.mowtiie.dearest.ui.adapters;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.data.model.Section;

import java.util.ArrayList;
import java.util.List;

public final class SectionAdapter extends RecyclerView.Adapter<SectionAdapter.Holder> {
    public interface Listener {
        void onOpen(Section section);
        void onRename(Section section);
        void onDelete(Section section);
        void onStartDrag(RecyclerView.ViewHolder holder);
    }

    private final Listener listener;
    private final List<Section> items = new ArrayList<>();
    private final java.util.Map<String, Integer> counts = new java.util.HashMap<>();

    public SectionAdapter(Listener listener) { this.listener = listener; }
    public List<Section> getItems() { return new ArrayList<>(items); }
    public void setItems(List<Section> sections) {
        items.clear(); if (sections != null) items.addAll(sections); notifyDataSetChanged();
    }
    public void setEntryCount(String sectionId, int count) {
        counts.put(sectionId, count);
        for (int i=0;i<items.size();i++) if (items.get(i).getId().equals(sectionId)) { notifyItemChanged(i); break; }
    }
    public void onItemMove(int from, int to) {
        if (from < 0 || to < 0 || from >= items.size() || to >= items.size()) return;
        items.add(to, items.remove(from)); notifyItemMoved(from, to);
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_section, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder h, int position) {
        Section section = items.get(position);
        h.name.setText(section.getName());
        int count = counts.getOrDefault(section.getId(), 0);
        h.count.setText(h.itemView.getResources().getQuantityString(R.plurals.section_entry_count, count, count));
        h.itemView.setOnClickListener(v -> listener.onOpen(section));
        h.edit.setOnClickListener(v -> listener.onRename(section));
        h.delete.setOnClickListener(v -> listener.onDelete(section));
        h.handle.setOnTouchListener((v, e) -> {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) listener.onStartDrag(h);
            return false;
        });
    }
    @Override public int getItemCount() { return items.size(); }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView name, count;
        final ImageView handle;
        final ImageButton edit, delete;
        Holder(@NonNull View item) {
            super(item);
            name = item.findViewById(R.id.section_name);
            count = item.findViewById(R.id.section_count);
            handle = item.findViewById(R.id.section_drag_handle);
            edit = item.findViewById(R.id.section_edit_button);
            delete = item.findViewById(R.id.section_delete_button);
        }
    }
}
