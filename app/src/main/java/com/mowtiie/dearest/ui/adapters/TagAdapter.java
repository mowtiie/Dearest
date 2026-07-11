package com.mowtiie.dearest.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.data.model.Tag;

public class TagAdapter extends ListAdapter<Tag, TagAdapter.TagViewHolder> {

    public interface Listener {
        void onRename(Tag tag);
        void onDelete(Tag tag);
    }

    private final Listener listener;

    public TagAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
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
    public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tag, parent, false);
        return new TagViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class TagViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final ImageButton delete;

        TagViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tag_name);
            delete = itemView.findViewById(R.id.delete_button);
        }

        void bind(Tag tag, Listener listener) {
            name.setText(tag.getName());
            itemView.setOnClickListener(v -> listener.onRename(tag));
            delete.setOnClickListener(v -> listener.onDelete(tag));
        }
    }
}