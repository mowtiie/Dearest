package com.mowtiie.dearest.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.data.model.Notebook;
import com.mowtiie.dearest.ui.InsetsUtil;
import com.mowtiie.dearest.ui.viewmodel.EntryEditorViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
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
    private AutoCompleteTextView notebookDropdown;
    private android.widget.TextView notebookDescriptionHint;
    private ChipGroup tagChipGroup;
    private boolean shouldPopulate;

    private final List<String> notebookIdsForDropdown = new ArrayList<>();

    private final ActivityResultLauncher<Intent> tagPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK) return;
                List<String> names = TagPickerActivity.extractResult(result.getData());
                if (names != null) viewModel.setTags(names);
            });

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
        notebookDropdown = findViewById(R.id.notebook_dropdown);
        notebookDescriptionHint = findViewById(R.id.notebook_description_hint);
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

        viewModel.notebooks().observe(this, notebooks -> bindNotebookDropdown());
        viewModel.notebookId().observe(this, id -> bindNotebookDropdown());

        viewModel.tagNames().observe(this, this::renderTagChips);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { saveAndFinish(); }
        });
    }

    private void bindNotebookDropdown() {
        List<Notebook> notebooks = viewModel.notebooks().getValue();
        String currentId = viewModel.notebookId().getValue();
        if (notebooks == null) return;

        notebookIdsForDropdown.clear();
        List<String> names = new ArrayList<>();
        String currentName = null;
        String currentDescription = null;
        for (Notebook n : notebooks) {
            notebookIdsForDropdown.add(n.getId());
            names.add(n.getName());
            if (n.getId().equals(currentId)) {
                currentName = n.getName();
                currentDescription = n.getDescription();
            }
        }

        notebookDropdown.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, names));
        notebookDropdown.setOnItemClickListener((parent, view, position, id) ->
                viewModel.setNotebook(notebookIdsForDropdown.get(position)));

        notebookDropdown.setText(currentName, false);

        boolean hasDescription = currentDescription != null && !currentDescription.trim().isEmpty();
        notebookDescriptionHint.setVisibility(hasDescription ? View.VISIBLE : View.GONE);
        if (hasDescription) notebookDescriptionHint.setText(currentDescription);
    }

    private void renderTagChips(@Nullable List<String> tagNames) {
        tagChipGroup.removeAllViews();
        if (tagNames != null) {
            for (String name : tagNames) {
                Chip chip = (Chip) getLayoutInflater()
                        .inflate(R.layout.item_tag_chip, tagChipGroup, false);
                chip.setText(name);
                chip.setChipIconResource(R.drawable.ic_tag);
                chip.setChipIconTintResource(R.color.md_theme_onSurface);
                chip.setChipIconVisible(true);
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
        chip.setChipIconTintResource(R.color.md_theme_onSurface);
        chip.setChipIconVisible(true);
        chip.setCloseIconVisible(false);
        chip.setOnClickListener(v -> openTagPicker());
        return chip;
    }

    private void openTagPicker() {
        List<String> current = viewModel.tagNames().getValue();
        Intent intent = TagPickerActivity.createIntent(this,
                current != null ? current : Collections.emptyList());
        tagPickerLauncher.launch(intent);
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