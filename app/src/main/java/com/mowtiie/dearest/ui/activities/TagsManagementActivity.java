package com.mowtiie.dearest.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.data.model.Tag;
import com.mowtiie.dearest.ui.InsetsUtil;
import com.mowtiie.dearest.ui.adapters.TagAdapter;
import com.mowtiie.dearest.ui.viewmodel.TagViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class TagsManagementActivity extends DearestActivity implements TagAdapter.Listener {

    private TagViewModel viewModel;
    private TagAdapter adapter;
    private View emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tags_management);

        MaterialToolbar toolbar = findViewById(R.id.tags_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        InsetsUtil.applyToolbarAndBottom(findViewById(R.id.tags_root), findViewById(R.id.tags_app_bar));

        emptyView = findViewById(R.id.tags_empty);
        viewModel = new ViewModelProvider(this).get(TagViewModel.class);
        adapter = new TagAdapter(this);

        RecyclerView list = findViewById(R.id.tags_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        viewModel.tags().observe(this, tags -> {
            adapter.submitList(tags);
            emptyView.setVisibility((tags == null || tags.isEmpty()) ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onRename(Tag tag) {
        View content = getLayoutInflater().inflate(R.layout.dialog_edit_name, null);
        EditText input = content.findViewById(R.id.name_input);
        input.setText(tag.getName());
        input.setSelection(input.getText().length());

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.rename_tag_title)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_save, (d, w) ->
                        viewModel.renameTag(tag, input.getText().toString(), (ok, error) -> {
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