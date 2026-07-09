package com.mowtiie.dearest.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.ui.viewmodel.EntryEditorViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class EntryEditorActivity extends DearestActivity {

    private static final String EXTRA_ENTRY_ID = "com.dearest.extra.ENTRY_ID";
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

        titleField = findViewById(R.id.editor_title);
        bodyField = findViewById(R.id.editor_body);

        viewModel = new ViewModelProvider(this).get(EntryEditorViewModel.class);
        viewModel.init(getIntent().getStringExtra(EXTRA_ENTRY_ID), getIntent().getStringExtra(EXTRA_NOTEBOOK_ID));

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

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { saveAndFinish(); }
        });
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