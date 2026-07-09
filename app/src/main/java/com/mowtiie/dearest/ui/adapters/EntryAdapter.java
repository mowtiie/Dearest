package com.mowtiie.dearest.ui.adapters;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;
import com.mowtiie.dearest.R;
import com.mowtiie.dearest.data.model.Entry;

public class EntryAdapter extends ListAdapter<Entry, EntryAdapter.EntryViewHolder> {

    public interface OnEntryClick {
        void onEntryClick(Entry entry);
    }

    private final OnEntryClick clickListener;

    public EntryAdapter(OnEntryClick clickListener) {
        super(DIFF);
        this.clickListener = clickListener;
    }

    private static final DiffUtil.ItemCallback<Entry> DIFF = new DiffUtil.ItemCallback<Entry>() {
        @Override public boolean areItemsTheSame(@NonNull Entry a, @NonNull Entry b) {
            return a.getId().equals(b.getId());
        }
        @Override public boolean areContentsTheSame(@NonNull Entry a, @NonNull Entry b) {
            return a.hasSameContentAs(b);
        }
    };

    @NonNull
    @Override
    public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_entry, parent, false);
        return new EntryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder holder, int position) {
        holder.bind(getItem(position), clickListener);
    }

    static class EntryViewHolder extends RecyclerView.ViewHolder {
        private final MaterialTextView title, snippet, date;

        EntryViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.entry_title);
            snippet = itemView.findViewById(R.id.entry_snippet);
            date = itemView.findViewById(R.id.entry_date);
        }

        void bind(Entry entry, OnEntryClick listener) {
            title.setText(displayTitle(entry));
            snippet.setText(entry.getBody());
            date.setText(DateUtils.getRelativeTimeSpanString(entry.getCreatedAt(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
            itemView.setOnClickListener(v -> listener.onEntryClick(entry));
        }

        private String displayTitle(Entry entry) {
            String t = entry.getTitle();
            if (t != null && !t.trim().isEmpty()) return t.trim();
            String body = entry.getBody();
            if (body != null) {
                String firstLine = body.trim();
                int nl = firstLine.indexOf('\n');
                if (nl > 0) firstLine = firstLine.substring(0, nl);
                if (!firstLine.isEmpty()) return firstLine;
            }
            return itemView.getContext().getString(R.string.entry_untitled);
        }
    }
}
