package com.mowtiie.dearest.ui.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.data.model.Entry;
import com.mowtiie.dearest.ui.activities.EntryEditorActivity;
import com.mowtiie.dearest.ui.adapters.EntryAdapter;
import com.mowtiie.dearest.ui.viewmodel.SearchViewModel;

import java.util.List;

public class SearchFragment extends Fragment {

    private SearchViewModel viewModel;
    private EntryAdapter adapter;
    private TextView emptyView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        emptyView = view.findViewById(R.id.search_empty);

        RecyclerView results = view.findViewById(R.id.search_results);
        results.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new EntryAdapter(entry -> EntryEditorActivity.open(requireContext(), entry.getId(), null));
        results.setAdapter(adapter);

        EditText input = view.findViewById(R.id.search_input);
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                viewModel.setQuery(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        viewModel.results().observe(getViewLifecycleOwner(), entries -> {
            adapter.submitList(entries);
            updateEmptyState(entries);
        });
        viewModel.query().observe(getViewLifecycleOwner(), q ->
                updateEmptyState(viewModel.results().getValue()));
    }

    private void updateEmptyState(@Nullable List<Entry> entries) {
        String query = viewModel.query().getValue();
        boolean noQuery = (query == null || query.trim().isEmpty());
        boolean noResults = (entries == null || entries.isEmpty());

        if (noQuery) {
            emptyView.setText(R.string.search_prompt);
            emptyView.setVisibility(View.VISIBLE);
        } else if (noResults) {
            emptyView.setText(R.string.search_no_results);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }
}
