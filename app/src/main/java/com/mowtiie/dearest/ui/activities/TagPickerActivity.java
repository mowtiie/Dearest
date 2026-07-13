package com.mowtiie.dearest.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.data.model.Tag;
import com.mowtiie.dearest.databinding.ActivityTagPickerBinding;
import com.mowtiie.dearest.databinding.DialogEditNameBinding;
import com.mowtiie.dearest.ui.InsetsUtil;
import com.mowtiie.dearest.ui.adapters.TagPickerAdapter;
import com.mowtiie.dearest.ui.viewmodel.TagPickerViewModel;
import com.mowtiie.dearest.ui.viewmodel.TagPickerViewModel.Sort;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class TagPickerActivity extends DearestActivity implements TagPickerAdapter.Listener {

    private static final String EXTRA_SELECTED_NAMES = "com.dearest.extra.SELECTED_TAG_NAMES";

    private ActivityTagPickerBinding binding;
    private TagPickerViewModel viewModel;
    private TagPickerAdapter adapter;

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
        binding = ActivityTagPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.tagPickerToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        InsetsUtil.applyToolbarAndBottom(binding.tagPickerRoot, binding.tagPickerAppBar);

        viewModel = new ViewModelProvider(this).get(TagPickerViewModel.class);
        List<String> initialNames = getIntent().getStringArrayListExtra(EXTRA_SELECTED_NAMES);
        viewModel.setInitialSelection(initialNames != null ? initialNames : new ArrayList<>());

        adapter = new TagPickerAdapter(this);
        binding.tagPickerList.setLayoutManager(new LinearLayoutManager(this));
        binding.tagPickerList.setAdapter(adapter);

        viewModel.visibleTags().observe(this, tags -> {
            adapter.submitList(tags);
            binding.tagPickerEmpty.setVisibility((tags == null || tags.isEmpty()) ? View.VISIBLE : View.GONE);
        });
        viewModel.selectedIds().observe(this, ids -> adapter.setSelected(ids));

        binding.fabAddTag.setOnClickListener(v -> showAddTagDialog());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { finishWithResult(); }
        });
    }

    @Override
    public void onToggle(Tag tag) {
        viewModel.toggle(tag.getId());
    }

    private void showAddTagDialog() {
        DialogEditNameBinding dialogBinding = DialogEditNameBinding.inflate(getLayoutInflater());

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.add_tag_title)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_save,
                        (d, w) -> viewModel.createTag(dialogBinding.nameInput.getText().toString()))
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