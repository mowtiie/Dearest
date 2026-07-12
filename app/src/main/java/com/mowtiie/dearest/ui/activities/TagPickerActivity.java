package com.mowtiie.dearest.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.data.model.Tag;
import com.mowtiie.dearest.ui.InsetsUtil;
import com.mowtiie.dearest.ui.adapters.TagPickerAdapter;
import com.mowtiie.dearest.ui.viewmodel.TagPickerViewModel;
import com.mowtiie.dearest.ui.viewmodel.TagPickerViewModel.Sort;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class TagPickerActivity extends DearestActivity implements TagPickerAdapter.Listener {

    private static final String EXTRA_SELECTED_NAMES = "com.dearest.extra.SELECTED_TAG_NAMES";

    private TagPickerViewModel viewModel;
    private TagPickerAdapter adapter;
    private View emptyView;

    public static Intent createIntent(Context context, List<String> currentNames) {
        Intent intent = new Intent(context, TagPickerActivity.class);
        intent.putStringArrayListExtra(EXTRA_SELECTED_NAMES, new ArrayList<>(currentNames));
        return intent;
    }

    @Nullable
    public static List<String> extractResult(@Nullable Intent data) {
        return data == null ? null : data.getStringArrayListExtra(EXTRA_SELECTED_NAMES);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_picker);

        MaterialToolbar toolbar = findViewById(R.id.tag_picker_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        InsetsUtil.applyToolbarAndBottom(
                findViewById(R.id.tag_picker_root), findViewById(R.id.tag_picker_app_bar));

        emptyView = findViewById(R.id.tag_picker_empty);

        viewModel = new ViewModelProvider(this).get(TagPickerViewModel.class);
        List<String> initialNames = getIntent().getStringArrayListExtra(EXTRA_SELECTED_NAMES);
        viewModel.setInitialSelection(initialNames != null ? initialNames : new ArrayList<>());

        adapter = new TagPickerAdapter(this);
        RecyclerView list = findViewById(R.id.tag_picker_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        viewModel.visibleTags().observe(this, tags -> {
            adapter.submitList(tags);
            emptyView.setVisibility((tags == null || tags.isEmpty()) ? View.VISIBLE : View.GONE);
        });
        viewModel.selectedIds().observe(this, ids -> adapter.setSelected(ids));

        FloatingActionButton fab = findViewById(R.id.fab_add_tag);
        fab.setOnClickListener(v -> showAddTagDialog());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { finishWithResult(); }
        });
    }

    @Override
    public void onToggle(Tag tag) {
        viewModel.toggle(tag.getId());
    }

    private void showAddTagDialog() {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_edit_name, null);
        android.widget.EditText input = content.findViewById(R.id.name_input);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.add_tag_title)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_save,
                        (d, w) -> viewModel.createTag(input.getText().toString()))
                .show();
    }

    private void finishWithResult() {
        Intent result = new Intent();
        result.putStringArrayListExtra(EXTRA_SELECTED_NAMES, new ArrayList<>(viewModel.selectedNames()));
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tag_picker, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView search = (SearchView) searchItem.getActionView();
        if (search != null) {
            search.setQueryHint(getString(R.string.tag_picker_search_hint));
            search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override public boolean onQueryTextSubmit(String q) { return false; }
                @Override public boolean onQueryTextChange(String q) {
                    viewModel.setQuery(q);
                    return true;
                }
            });
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finishWithResult();
            return true;
        }
        if (id == R.id.action_done) {
            finishWithResult();
            return true;
        }
        if (id == R.id.sort_tag_name_asc) {
            viewModel.setSort(Sort.NAME_ASC);
            item.setChecked(true);
            return true;
        }
        if (id == R.id.sort_tag_name_desc) {
            viewModel.setSort(Sort.NAME_DESC);
            item.setChecked(true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}