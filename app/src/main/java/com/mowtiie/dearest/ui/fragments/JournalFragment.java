package com.mowtiie.dearest.ui.fragments;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.databinding.FragmentJournalBinding;
import com.mowtiie.dearest.ui.activities.EntryEditorActivity;
import com.mowtiie.dearest.ui.adapters.EntryAdapter;
import com.mowtiie.dearest.ui.viewmodel.JournalViewModel;
import com.mowtiie.dearest.ui.viewmodel.JournalViewModel.Sort;

public class JournalFragment extends Fragment {

    private FragmentJournalBinding binding;
    private JournalViewModel viewModel;
    private EntryAdapter adapter;
    private MenuItem filterMenuItem;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentJournalBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(JournalViewModel.class);

        binding.entriesList.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new EntryAdapter(entry ->
                EntryEditorActivity.open(requireContext(), entry.getId(), null));
        binding.entriesList.setAdapter(adapter);

        binding.fabNewEntry.setOnClickListener(v ->
                EntryEditorActivity.open(requireContext(), null, viewModel.notebookForNewEntry()));

        viewModel.entries().observe(getViewLifecycleOwner(), entries -> {
            adapter.submitList(entries);
            binding.emptyState.setVisibility((entries == null || entries.isEmpty()) ? View.VISIBLE : View.GONE);
        });

        viewModel.selectedNotebookId().observe(getViewLifecycleOwner(), id -> updateFilterIcon());
        viewModel.selectedTagIds().observe(getViewLifecycleOwner(), ids -> updateFilterIcon());

        setupToolbarMenu();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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
                filterMenuItem = menu.findItem(R.id.action_filter);
                updateFilterIcon();
                checkCurrentSort(menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.action_filter) {
                    new JournalFilterBottomSheet().show(getChildFragmentManager(), JournalFilterBottomSheet.TAG);
                    return true;
                }
                if (id == R.id.sort_newest) { viewModel.setSort(Sort.NEWEST); item.setChecked(true); return true; }
                if (id == R.id.sort_oldest) { viewModel.setSort(Sort.OLDEST); item.setChecked(true); return true; }
                if (id == R.id.sort_title)  { viewModel.setSort(Sort.TITLE);  item.setChecked(true); return true; }
                return false;
            }
        }, getViewLifecycleOwner());
    }

    private void updateFilterIcon() {
        if (filterMenuItem == null) return;
        boolean active = viewModel.selectedNotebookId().getValue() != null
                || (viewModel.selectedTagIds().getValue() != null
                && !viewModel.selectedTagIds().getValue().isEmpty());
        Drawable icon = filterMenuItem.getIcon();
        if (icon == null) return;
        Drawable wrapped = DrawableCompat.wrap(icon.mutate());
        int color = ContextCompat.getColor(requireContext(), active ? R.color.md_theme_primary: R.color.md_theme_onSurfaceVariant);
        DrawableCompat.setTint(wrapped, color);
        filterMenuItem.setIcon(wrapped);
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
}