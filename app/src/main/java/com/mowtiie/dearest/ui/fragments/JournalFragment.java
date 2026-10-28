package com.mowtiie.dearest.ui.fragments;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
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

import java.util.List;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.data.model.Entry;
import com.mowtiie.dearest.ui.InsetsUtil;
import com.mowtiie.dearest.databinding.FragmentJournalBinding;
import com.mowtiie.dearest.ui.activities.EntryEditorActivity;
import com.mowtiie.dearest.ui.adapters.EntryAdapter;
import com.mowtiie.dearest.ui.viewmodel.JournalViewModel;
import com.mowtiie.dearest.ui.viewmodel.JournalViewModel.Sort;

public class JournalFragment extends Fragment implements EntryEditorFragment.Host {

    private static final String DETAIL_TAG = "entry_detail";

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
        InsetsUtil.limitContentWidth(binding.entriesList);
        removeStaleDetail();
        adapter = new EntryAdapter(entry -> openEntry(entry.getId(), null));
        binding.entriesList.setAdapter(adapter);

        binding.fabNewEntry.setOnClickListener(v ->
                openEntry(null, viewModel.notebookForNewEntry()));

        viewModel.entries().observe(getViewLifecycleOwner(), entries -> {
            adapter.submitList(entries);
            updateEmptyState();
        });
        viewModel.filtering().observe(getViewLifecycleOwner(), filtering -> updateEmptyState());

        viewModel.selectedNotebookId().observe(getViewLifecycleOwner(), id -> updateFilterIcon());
        viewModel.selectedTagIds().observe(getViewLifecycleOwner(), ids -> updateFilterIcon());

        showDetailPlaceholderIfEmpty();
        setupToolbarMenu();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private boolean isTwoPane() {
        return binding != null && binding.detailContainer != null;
    }

    /** Opens an entry (or a new one in the given notebook) in the detail pane, or full screen. */
    private void openEntry(@Nullable String entryId, @Nullable String notebookId) {
        if (!isTwoPane()) {
            EntryEditorActivity.open(requireContext(), entryId, notebookId);
            return;
        }
        Fragment current = getChildFragmentManager().findFragmentByTag(DETAIL_TAG);
        if (current instanceof EntryEditorFragment && entryId != null
                && entryId.equals(((EntryEditorFragment) current).entryId())) {
            return;
        }
        getChildFragmentManager().beginTransaction()
                .replace(R.id.detail_container, EntryEditorFragment.newInstance(entryId, notebookId, false), DETAIL_TAG)
                .commit();
        binding.detailPlaceholder.setVisibility(View.GONE);
    }

    @Override
    public void onEditorFinished() {
        Fragment detail = getChildFragmentManager().findFragmentByTag(DETAIL_TAG);
        if (detail != null) {
            getChildFragmentManager().beginTransaction().remove(detail).commit();
        }
        if (binding != null && binding.detailPlaceholder != null) {
            binding.detailPlaceholder.setVisibility(View.VISIBLE);
        }
    }

    private void showDetailPlaceholderIfEmpty() {
        if (!isTwoPane()) return;
        boolean hasDetail = getChildFragmentManager().findFragmentByTag(DETAIL_TAG) != null;
        binding.detailPlaceholder.setVisibility(hasDetail ? View.GONE : View.VISIBLE);
    }

    /** A detail editor restored after the window shrank to one pane has no container to live in. */
    private void removeStaleDetail() {
        if (isTwoPane()) return;
        Fragment stale = getChildFragmentManager().findFragmentByTag(DETAIL_TAG);
        if (stale != null) {
            getChildFragmentManager().beginTransaction().remove(stale).commit();
        }
    }

    /** Tells "your journal is empty" apart from "nothing matches the search or filters". */
    private void updateEmptyState() {
        if (binding == null) return;
        List<Entry> entries = viewModel.entries().getValue();
        boolean empty = entries == null || entries.isEmpty();
        binding.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (!empty) return;
        boolean filtering = Boolean.TRUE.equals(viewModel.filtering().getValue());
        binding.emptyTitle.setText(filtering ? R.string.search_no_results : R.string.journal_empty_title);
        binding.emptySubtitle.setText(filtering ? R.string.search_no_results_hint : R.string.journal_empty_subtitle);
    }

    private void setupToolbarMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                inflater.inflate(R.menu.menu_journal, menu);

                SearchView search = (SearchView) menu.findItem(R.id.action_search).getActionView();
                if (search != null) {
                    search.setQueryHint(getString(R.string.journal_search_hint));
                    // Keep what is searched for out of the keyboard's learned suggestions.
                    search.setImeOptions(search.getImeOptions() | EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
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