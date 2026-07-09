package com.mowtiie.dearest.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.data.model.Notebook;
import com.mowtiie.dearest.ui.activities.EntryEditorActivity;
import com.mowtiie.dearest.ui.adapters.EntryAdapter;
import com.mowtiie.dearest.ui.viewmodel.JournalViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class JournalFragment extends Fragment {

    private JournalViewModel viewModel;
    private EntryAdapter adapter;
    private ChipGroup notebookChips;
    private View emptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_journal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(JournalViewModel.class);

        notebookChips = view.findViewById(R.id.notebook_chips);
        emptyState = view.findViewById(R.id.empty_state);

        RecyclerView list = view.findViewById(R.id.entries_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new EntryAdapter(entry -> EntryEditorActivity.open(requireContext(), entry.getId(), null));
        list.setAdapter(adapter);

        FloatingActionButton fab = view.findViewById(R.id.fab_new_entry);
        fab.setOnClickListener(v -> EntryEditorActivity.open(requireContext(), null, viewModel.notebookForNewEntry()));

        viewModel.entries().observe(getViewLifecycleOwner(), entries -> {
            adapter.submitList(entries);
            boolean empty = (entries == null || entries.isEmpty());
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        });

        viewModel.notebooks().observe(getViewLifecycleOwner(), this::bindNotebookChips);
    }

    private void bindNotebookChips(List<Notebook> notebooks) {
        notebookChips.removeAllViews();
        notebookChips.addView(createChip(getString(R.string.filter_all), null));
        if (notebooks != null) {
            for (Notebook n : notebooks) {
                notebookChips.addView(createChip(n.getName(), n.getId()));
            }
        }
        checkSelectedChip();
    }

    private Chip createChip(String label, @Nullable String notebookId) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_filter_chip, notebookChips, false);
        chip.setText(label);
        chip.setTag(notebookId);
        chip.setOnClickListener(v -> viewModel.selectNotebook(notebookId));
        return chip;
    }

    private void checkSelectedChip() {
        String selected = viewModel.selectedNotebookId().getValue();
        for (int i = 0; i < notebookChips.getChildCount(); i++) {
            Chip chip = (Chip) notebookChips.getChildAt(i);
            boolean match = (selected == null)
                    ? chip.getTag() == null
                    : selected.equals(chip.getTag());
            if (match) {
                chip.setChecked(true);
                break;
            }
        }
    }
}