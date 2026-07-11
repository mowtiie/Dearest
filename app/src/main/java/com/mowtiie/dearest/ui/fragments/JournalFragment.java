package com.mowtiie.dearest.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.data.model.Notebook;
import com.mowtiie.dearest.data.model.Tag;
import com.mowtiie.dearest.ui.activities.EntryEditorActivity;
import com.mowtiie.dearest.ui.adapters.EntryAdapter;
import com.mowtiie.dearest.ui.viewmodel.JournalViewModel;
import com.mowtiie.dearest.ui.viewmodel.JournalViewModel.Sort;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.Set;

public class JournalFragment extends Fragment {

    private JournalViewModel viewModel;
    private EntryAdapter adapter;
    private ChipGroup notebookChips;
    private View tagChipScroll;
    private ChipGroup tagChips;
    private View emptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_journal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(JournalViewModel.class);

        notebookChips = view.findViewById(R.id.notebook_chips);
        tagChipScroll = view.findViewById(R.id.tag_chip_scroll);
        tagChips = view.findViewById(R.id.tag_chips);
        emptyState = view.findViewById(R.id.empty_state);

        RecyclerView list = view.findViewById(R.id.entries_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new EntryAdapter(entry ->
                EntryEditorActivity.open(requireContext(), entry.getId(), null));
        list.setAdapter(adapter);

        FloatingActionButton fab = view.findViewById(R.id.fab_new_entry);
        fab.setOnClickListener(v ->
                EntryEditorActivity.open(requireContext(), null, viewModel.notebookForNewEntry()));

        viewModel.entries().observe(getViewLifecycleOwner(), entries -> {
            adapter.submitList(entries);
            emptyState.setVisibility((entries == null || entries.isEmpty()) ? View.VISIBLE : View.GONE);
        });
        viewModel.notebooks().observe(getViewLifecycleOwner(), this::bindNotebookChips);
        viewModel.tags().observe(getViewLifecycleOwner(), this::bindTagChips);
        viewModel.selectedTagIds().observe(getViewLifecycleOwner(), ids -> checkSelectedTagChips());

        setupToolbarMenu();
    }

    private void setupToolbarMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                inflater.inflate(R.menu.menu_journal, menu);

                SearchView search = (SearchView) menu.findItem(R.id.action_search).getActionView();
                if (search != null) {
                    search.setQueryHint(getString(R.string.journal_search_hint));
                    search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                        @Override public boolean onQueryTextSubmit(String q) { return false; }
                        @Override public boolean onQueryTextChange(String q) {
                            viewModel.setQuery(q);
                            return true;
                        }
                    });
                }
                checkCurrentSort(menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.sort_newest) { viewModel.setSort(Sort.NEWEST); item.setChecked(true); return true; }
                if (id == R.id.sort_oldest) { viewModel.setSort(Sort.OLDEST); item.setChecked(true); return true; }
                if (id == R.id.sort_title)  { viewModel.setSort(Sort.TITLE);  item.setChecked(true); return true; }
                return false;
            }
        }, getViewLifecycleOwner());
    }

    private void checkCurrentSort(Menu menu) {
        Sort current = viewModel.sort().getValue();
        if (current == null) current = Sort.NEWEST;
        int itemId;
        switch (current) {
            case OLDEST: itemId = R.id.sort_oldest; break;
            case TITLE:  itemId = R.id.sort_title;  break;
            default:     itemId = R.id.sort_newest; break;
        }
        MenuItem item = menu.findItem(itemId);
        if (item != null) item.setChecked(true);
    }

    private void bindNotebookChips(List<Notebook> notebooks) {
        notebookChips.removeAllViews();
        notebookChips.addView(createNotebookChip(getString(R.string.filter_all), null));
        if (notebooks != null) {
            for (Notebook n : notebooks) {
                notebookChips.addView(createNotebookChip(n.getName(), n.getId()));
            }
        }
        checkSelectedNotebookChip();
    }

    private Chip createNotebookChip(String label, @Nullable String notebookId) {
        Chip chip = (Chip) getLayoutInflater()
                .inflate(R.layout.item_filter_chip, notebookChips, false);
        chip.setText(label);
        chip.setTag(notebookId);
        chip.setOnClickListener(v -> viewModel.selectNotebook(notebookId));
        return chip;
    }

    private void checkSelectedNotebookChip() {
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

    private void bindTagChips(@Nullable List<Tag> tags) {
        tagChips.removeAllViews();
        tagChipScroll.setVisibility((tags == null || tags.isEmpty()) ? View.GONE : View.VISIBLE);
        if (tags != null) {
            for (Tag t : tags) {
                tagChips.addView(createTagChip(t));
            }
        }
        checkSelectedTagChips();
    }

    private Chip createTagChip(Tag tag) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_filter_chip, tagChips, false);
        chip.setText(tag.getName());
        chip.setTag(tag.getId());
        chip.setOnClickListener(v -> viewModel.toggleTag(tag.getId()));
        return chip;
    }

    private void checkSelectedTagChips() {
        Set<String> selected = viewModel.selectedTagIds().getValue();
        for (int i = 0; i < tagChips.getChildCount(); i++) {
            Chip chip = (Chip) tagChips.getChildAt(i);
            boolean checked = selected != null && selected.contains(chip.getTag());
            if (chip.isChecked() != checked) chip.setChecked(checked);
        }
    }
}