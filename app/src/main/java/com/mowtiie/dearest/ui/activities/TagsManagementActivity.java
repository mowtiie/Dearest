package com.mowtiie.dearest.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.data.model.Tag;
import com.mowtiie.dearest.databinding.ActivityTagsManagementBinding;
import com.mowtiie.dearest.databinding.DialogEditNameBinding;
import com.mowtiie.dearest.ui.InsetsUtil;
import com.mowtiie.dearest.ui.adapters.TagAdapter;
import com.mowtiie.dearest.ui.viewmodel.TagViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class TagsManagementActivity extends DearestActivity implements TagAdapter.Listener {

    private ActivityTagsManagementBinding binding;
    private TagViewModel viewModel;
    private TagAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTagsManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.tagsToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        InsetsUtil.applyToolbarAndBottom(binding.tagsRoot, binding.tagsAppBar);

        viewModel = new ViewModelProvider(this).get(TagViewModel.class);
        adapter = new TagAdapter(this);

        binding.tagsList.setLayoutManager(new LinearLayoutManager(this));
        binding.tagsList.setAdapter(adapter);

        viewModel.tags().observe(this, tags -> {
            adapter.submitList(tags);
            binding.tagsEmpty.setVisibility((tags == null || tags.isEmpty()) ? View.VISIBLE : View.GONE);
        });

        binding.fabAddTag.setOnClickListener(v -> showAddTagDialog());
    }

    private void showAddTagDialog() {
        DialogEditNameBinding dialogBinding = DialogEditNameBinding.inflate(getLayoutInflater());

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.add_tag_title)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_save, (d, w) ->
                        viewModel.createTag(dialogBinding.nameInput.getText().toString(), (ok, error) -> {
                            if (!ok && error != null) {
                                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                            }
                        }))
                .show();
    }

    @Override
    public void onRename(Tag tag) {
        DialogEditNameBinding dialogBinding = DialogEditNameBinding.inflate(getLayoutInflater());
        dialogBinding.nameInput.setText(tag.getName());
        dialogBinding.nameInput.setSelection(dialogBinding.nameInput.getText().length());

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.rename_tag_title)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_save, (d, w) ->
                        viewModel.renameTag(tag, dialogBinding.nameInput.getText().toString(), (ok, error) -> {
                            if (!ok && error != null) {
                                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                            }
                        }))
                .show();
    }

    @Override
    public void onDelete(Tag tag) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_tag_title)
                .setMessage(getString(R.string.delete_tag_message, tag.getName()))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.editor_delete, (d, w) -> viewModel.deleteTag(tag))
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}