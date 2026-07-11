package com.mowtiie.dearest.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.data.model.Notebook;
import com.mowtiie.dearest.data.model.Tag;
import com.mowtiie.dearest.ui.InsetsUtil;
import com.mowtiie.dearest.ui.viewmodel.EntryEditorViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class EntryEditorActivity extends DearestActivity {

    private static final String EXTRA_ENTRY_ID    = "com.dearest.extra.ENTRY_ID";
    private static final String EXTRA_NOTEBOOK_ID = "com.dearest.extra.NOTEBOOK_ID";

    public static void open(Context context, @Nullable String entryId, @Nullable String notebookId) {
        Intent intent = new Intent(context, EntryEditorActivity.class);
        intent.putExtra(EXTRA_ENTRY_ID, entryId);
        intent.putExtra(EXTRA_NOTEBOOK_ID, notebookId);
        context.startActivity(intent);
    }

    private EntryEditorViewModel viewModel;
    private EditText titleField;
    private EditText bodyField;
    private View notebookSelector;
    private TextView notebookSelectorName;
    private ChipGroup tagChipGroup;
    private boolean shouldPopulate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_editor);

        MaterialToolbar toolbar = findViewById(R.id.editor_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        InsetsUtil.applyToolbarAndBottom(findViewById(R.id.editor_root), findViewById(R.id.editor_app_bar));

        titleField = findViewById(R.id.editor_title);
        bodyField = findViewById(R.id.editor_body);
        notebookSelector = findViewById(R.id.notebook_selector);
        notebookSelectorName = findViewById(R.id.notebook_selector_name);
        tagChipGroup = findViewById(R.id.tag_chip_group);

        viewModel = new ViewModelProvider(this).get(EntryEditorViewModel.class);
        viewModel.init(getIntent().getStringExtra(EXTRA_ENTRY_ID),
                getIntent().getStringExtra(EXTRA_NOTEBOOK_ID));

        shouldPopulate = (savedInstanceState == null);
        viewModel.entry().observe(this, entry -> {
            if (shouldPopulate && entry != null) {
                titleField.setText(entry.getTitle());
                bodyField.setText(entry.getBody());
                shouldPopulate = false;
            }
        });
        viewModel.finished().observe(this, done -> {
            if (Boolean.TRUE.equals(done)) finish();
        });

        viewModel.notebooks().observe(this, notebooks -> updateNotebookLabel());
        viewModel.notebookId().observe(this, id -> updateNotebookLabel());
        notebookSelector.setOnClickListener(v -> showNotebookPicker());

        viewModel.tagNames().observe(this, this::renderTagChips);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { saveAndFinish(); }
        });
    }

    private void updateNotebookLabel() {
        List<Notebook> notebooks = viewModel.notebooks().getValue();
        String id = viewModel.notebookId().getValue();
        if (notebooks == null || id == null) return;
        for (Notebook n : notebooks) {
            if (n.getId().equals(id)) {
                notebookSelectorName.setText(n.getName());
                return;
            }
        }
    }

    private void showNotebookPicker() {
        List<Notebook> notebooks = viewModel.notebooks().getValue();
        if (notebooks == null || notebooks.isEmpty()) return;

        String currentId = viewModel.notebookId().getValue();
        List<String> ids = new ArrayList<>();
        String[] names = new String[notebooks.size()];
        int checked = 0;
        for (int i = 0; i < notebooks.size(); i++) {
            Notebook n = notebooks.get(i);
            ids.add(n.getId());
            names[i] = n.getName();
            if (n.getId().equals(currentId)) checked = i;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.editor_choose_notebook)
                .setSingleChoiceItems(names, checked, (dialog, which) -> {
                    viewModel.setNotebook(ids.get(which));
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void renderTagChips(@Nullable List<String> tagNames) {
        tagChipGroup.removeAllViews();
        if (tagNames != null) {
            for (String name : tagNames) {
                Chip chip = (Chip) getLayoutInflater()
                        .inflate(R.layout.item_tag_chip, tagChipGroup, false);
                chip.setText(name);
                chip.setCloseIconVisible(true);
                chip.setOnCloseIconClickListener(v -> viewModel.removeTag(name));
                tagChipGroup.addView(chip);
            }
        }
        tagChipGroup.addView(createAddTagChip());
    }

    private Chip createAddTagChip() {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_tag_chip, tagChipGroup, false);
        chip.setText(R.string.editor_add_tag);
        chip.setChipIconResource(R.drawable.ic_add);
        chip.setChipIconVisible(true);
        chip.setCloseIconVisible(false);
        chip.setOnClickListener(v -> showAddTagDialog());
        return chip;
    }

    private void showAddTagDialog() {
        View content = getLayoutInflater().inflate(R.layout.dialog_add_tag, null);
        AutoCompleteTextView input = content.findViewById(R.id.tag_name_input);

        List<Tag> allTags = viewModel.allTags().getValue();
        List<String> suggestions = new ArrayList<>();
        if (allTags != null) {
            for (Tag t : allTags) suggestions.add(t.getName());
        }
        input.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, suggestions));

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.editor_add_tag)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_save,
                        (d, w) -> viewModel.addTag(input.getText().toString()))
                .show();
    }

    private void saveAndFinish() {
        viewModel.save(titleField.getText().toString(), bodyField.getText().toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_entry_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem delete = menu.findItem(R.id.action_delete);
        if (delete != null) delete.setVisible(!viewModel.isNew());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            saveAndFinish();
            return true;
        }
        if (id == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.editor_delete_confirm_title)
                .setMessage(R.string.editor_delete_confirm_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.editor_delete, (d, w) -> viewModel.delete())
                .show();
    }
}