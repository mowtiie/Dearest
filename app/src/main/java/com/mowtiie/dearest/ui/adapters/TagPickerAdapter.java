package com.mowtiie.dearest.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.data.model.Tag;

import java.util.Set;

public class TagPickerAdapter extends ListAdapter<Tag, TagPickerAdapter.ViewHolder> {

    public interface Listener {
        void onToggle(Tag tag);
    }

    private final Listener listener;
    private Set<String> selectedIds = java.util.Collections.emptySet();

    public TagPickerAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    public void setSelected(Set<String> selectedIds) {
        this.selectedIds = selectedIds != null ? selectedIds : java.util.Collections.emptySet();
        notifyDataSetChanged();
    }

    private static final DiffUtil.ItemCallback<Tag> DIFF = new DiffUtil.ItemCallback<Tag>() {
        @Override public boolean areItemsTheSame(@NonNull Tag a, @NonNull Tag b) {
            return a.getId().equals(b.getId());
        }
        @Override public boolean areContentsTheSame(@NonNull Tag a, @NonNull Tag b) {
            return a.hasSameContentAs(b);
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tag_pickable, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Tag tag = getItem(position);
        holder.name.setText(tag.getName());
        holder.checkbox.setChecked(selectedIds.contains(tag.getId()));
        holder.itemView.setOnClickListener(v -> listener.onToggle(tag));
        holder.checkbox.setOnClickListener(v -> listener.onToggle(tag));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final CheckBox checkbox;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tag_pickable_name);
            checkbox = itemView.findViewById(R.id.tag_pickable_checkbox);
        }
    }
}