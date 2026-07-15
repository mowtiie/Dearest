package com.mowtiie.dearest.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.data.model.Notebook;
import com.mowtiie.dearest.databinding.ActivityEntryEditorBinding;
import com.mowtiie.dearest.ui.InsetsUtil;
import com.mowtiie.dearest.ui.viewmodel.EntryEditorViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EntryEditorActivity extends DearestActivity {

    private static final String EXTRA_ENTRY_ID = "com.dearest.extra.ENTRY_ID";
    private static final String EXTRA_NOTEBOOK_ID = "com.dearest.extra.NOTEBOOK_ID";

    public static void open(Context context, @Nullable String entryId, @Nullable String notebookId) {
        Intent intent = new Intent(context, EntryEditorActivity.class);
        intent.putExtra(EXTRA_ENTRY_ID, entryId);
        intent.putExtra(EXTRA_NOTEBOOK_ID, notebookId);
        context.startActivity(intent);
    }

    private ActivityEntryEditorBinding binding;
    private EntryEditorViewModel viewModel;
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
        binding = ActivityEntryEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.editorToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        InsetsUtil.applyToolbarAndBottom(binding.editorRoot, binding.editorAppBar);

        viewModel = new ViewModelProvider(this).get(EntryEditorViewModel.class);
        viewModel.init(getIntent().getStringExtra(EXTRA_ENTRY_ID),
                getIntent().getStringExtra(EXTRA_NOTEBOOK_ID));
        setTitle(viewModel.isNew() ? R.string.editor_title_new : R.string.editor_title_edit);

        shouldPopulate = (savedInstanceState == null);
        viewModel.entry().observe(this, entry -> {
            if (shouldPopulate && entry != null) {
                binding.editorTitle.setText(entry.getTitle());
                binding.editorBody.setText(entry.getBody());
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

        binding.notebookDropdown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names));
        binding.notebookDropdown.setOnItemClickListener((parent, view, position, id) -> viewModel.setNotebook(notebookIdsForDropdown.get(position)));

        binding.notebookDropdown.setText(currentName, false);

        boolean hasDescription = currentDescription != null && !currentDescription.trim().isEmpty();
        binding.notebookDescriptionHint.setVisibility(hasDescription ? View.VISIBLE : View.GONE);
        if (hasDescription) binding.notebookDescriptionHint.setText(currentDescription);
    }

    private void renderTagChips(@Nullable List<String> tagNames) {
        binding.tagChipGroup.removeAllViews();
        if (tagNames != null) {
            for (String name : tagNames) {
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_tag_chip, binding.tagChipGroup, false);
                chip.setText(name);
                chip.setChipIconResource(R.drawable.ic_tag);
                chip.setChipIconTintResource(R.color.md_theme_onSurface);
                chip.setChipIconVisible(true);
                chip.setCloseIconVisible(true);
                chip.setOnCloseIconClickListener(v -> viewModel.removeTag(name));
                binding.tagChipGroup.addView(chip);
            }
        }
        binding.tagChipGroup.addView(createAddTagChip());
    }

    private Chip createAddTagChip() {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_tag_chip, binding.tagChipGroup, false);
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
        viewModel.save(binding.editorTitle.getText().toString(), binding.editorBody.getText().toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_entry_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean existingEntry = !viewModel.isNew();
        MenuItem share = menu.findItem(R.id.action_share);
        if (share != null) share.setVisible(existingEntry);
        MenuItem delete = menu.findItem(R.id.action_delete);
        if (delete != null) delete.setVisible(existingEntry);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            saveAndFinish();
            return true;
        }
        if (id == R.id.action_share) {
            shareEntry();
            return true;
        }
        if (id == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void shareEntry() {
        String title = binding.editorTitle.getText().toString().trim();
        String body = binding.editorBody.getText().toString().trim();

        StringBuilder text = new StringBuilder();
        if (!title.isEmpty()) text.append(title).append("\n\n");
        text.append(body);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        if (!title.isEmpty()) shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        shareIntent.putExtra(Intent.EXTRA_TEXT, text.toString());
        startActivity(Intent.createChooser(shareIntent, getString(R.string.editor_share)));
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