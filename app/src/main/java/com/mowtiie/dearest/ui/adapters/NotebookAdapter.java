package com.mowtiie.dearest.ui.adapters;

import android.annotation.SuppressLint;
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
import com.mowtiie.dearest.data.model.Notebook;

import java.util.ArrayList;
import java.util.List;

public class NotebookAdapter extends RecyclerView.Adapter<NotebookAdapter.NotebookViewHolder> {

    public interface Listener {
        void onRename(Notebook notebook);
        void onDelete(Notebook notebook);
        void onStartDrag(RecyclerView.ViewHolder holder);
    }

    private final List<Notebook> items = new ArrayList<>();
    private final Listener listener;
    private boolean reorderEnabled = true;

    public NotebookAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setReorderEnabled(boolean enabled) {
        if (this.reorderEnabled != enabled) {
            this.reorderEnabled = enabled;
            notifyDataSetChanged();
        }
    }

    public void setItems(List<Notebook> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    public List<Notebook> getItems() {
        return new ArrayList<>(items);
    }

    public void onItemMove(int from, int to) {
        if (from < 0 || to < 0 || from >= items.size() || to >= items.size()) return;
        items.add(to, items.remove(from));
        notifyItemMoved(from, to);
    }

    @NonNull
    @Override
    public NotebookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notebook, parent, false);
        return new NotebookViewHolder(v);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public void onBindViewHolder(@NonNull NotebookViewHolder holder, int position) {
        Notebook nb = items.get(position);
        holder.name.setText(nb.getName());
        holder.itemView.setOnClickListener(v -> listener.onRename(nb));
        holder.delete.setOnClickListener(v -> listener.onDelete(nb));

        holder.handle.setVisibility(reorderEnabled ? View.VISIBLE : View.GONE);
        if (reorderEnabled) {
            holder.handle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    listener.onStartDrag(holder);
                }
                return false;
            });
        } else {
            holder.handle.setOnTouchListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class NotebookViewHolder extends RecyclerView.ViewHolder {
        final ImageView handle;
        final TextView name;
        final ImageButton delete;

        NotebookViewHolder(@NonNull View itemView) {
            super(itemView);
            handle = itemView.findViewById(R.id.drag_handle);
            name = itemView.findViewById(R.id.notebook_name);
            delete = itemView.findViewById(R.id.delete_button);
        }
    }
}