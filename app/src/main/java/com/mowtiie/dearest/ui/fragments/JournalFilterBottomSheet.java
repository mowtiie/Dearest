package com.mowtiie.dearest.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.data.model.Notebook;
import com.mowtiie.dearest.data.model.Tag;
import com.mowtiie.dearest.databinding.BottomSheetJournalFilterBinding;
import com.mowtiie.dearest.ui.viewmodel.JournalViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;
import java.util.Set;

public class JournalFilterBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "JournalFilterBottomSheet";

    private BottomSheetJournalFilterBinding binding;
    private JournalViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetJournalFilterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireParentFragment()).get(JournalViewModel.class);

        viewModel.notebooks().observe(getViewLifecycleOwner(), this::bindNotebookChips);
        viewModel.tags().observe(getViewLifecycleOwner(), this::bindTagChips);

        binding.filterClearButton.setOnClickListener(v -> {
            viewModel.selectNotebook(null);
            viewModel.clearTagFilter();
            bindNotebookChips(viewModel.notebooks().getValue());
            bindTagChips(viewModel.tags().getValue());
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void bindNotebookChips(@Nullable List<Notebook> notebooks) {
        ChipGroup notebookChips = binding.filterNotebookChips;
        notebookChips.removeAllViews();
        notebookChips.addView(createNotebookChip(getString(R.string.filter_all), null));
        if (notebooks != null) {
            for (Notebook n : notebooks) {
                notebookChips.addView(createNotebookChip(n.getName(), n.getId()));
            }
        }
        String selected = viewModel.selectedNotebookId().getValue();
        for (int i = 0; i < notebookChips.getChildCount(); i++) {
            Chip chip = (Chip) notebookChips.getChildAt(i);
            boolean match = (selected == null) ? chip.getTag() == null : selected.equals(chip.getTag());
            if (match) {
                chip.setChecked(true);
                break;
            }
        }
    }

    private Chip createNotebookChip(String label, @Nullable String notebookId) {
        Chip chip = (Chip) getLayoutInflater()
                .inflate(R.layout.item_filter_chip, binding.filterNotebookChips, false);
        chip.setText(label);
        chip.setTag(notebookId);
        chip.setOnClickListener(v -> viewModel.selectNotebook(notebookId));
        return chip;
    }

    private void bindTagChips(@Nullable List<Tag> tags) {
        ChipGroup tagChips = binding.filterTagChips;
        tagChips.removeAllViews();
        boolean hasTags = tags != null && !tags.isEmpty();
        binding.filterTagsLabel.setVisibility(hasTags ? View.VISIBLE : View.GONE);
        tagChips.setVisibility(hasTags ? View.VISIBLE : View.GONE);
        if (!hasTags) return;

        Set<String> selected = viewModel.selectedTagIds().getValue();
        for (Tag t : tags) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_filter_chip, tagChips, false);
            chip.setText(t.getName());
            chip.setTag(t.getId());
            chip.setChecked(selected != null && selected.contains(t.getId()));
            chip.setOnClickListener(v -> viewModel.toggleTag(t.getId()));
            tagChips.addView(chip);
        }
    }
}