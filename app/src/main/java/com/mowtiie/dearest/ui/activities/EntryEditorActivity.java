package com.mowtiie.dearest.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.databinding.ActivityEntryEditorBinding;
import com.mowtiie.dearest.ui.fragments.EntryEditorFragment;

/** Full-screen host for {@link EntryEditorFragment}, used where there is no detail pane. */
public class EntryEditorActivity extends DearestActivity implements EntryEditorFragment.Host {

    private static final String EXTRA_ENTRY_ID = "com.dearest.extra.ENTRY_ID";
    private static final String EXTRA_NOTEBOOK_ID = "com.dearest.extra.NOTEBOOK_ID";

    public static void open(Context context, @Nullable String entryId, @Nullable String notebookId) {
        Intent intent = new Intent(context, EntryEditorActivity.class);
        intent.putExtra(EXTRA_ENTRY_ID, entryId);
        intent.putExtra(EXTRA_NOTEBOOK_ID, notebookId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(ActivityEntryEditorBinding.inflate(getLayoutInflater()).getRoot());

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.editor_container, EntryEditorFragment.newInstance(
                            getIntent().getStringExtra(EXTRA_ENTRY_ID),
                            getIntent().getStringExtra(EXTRA_NOTEBOOK_ID), true))
                    .commit();
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                Fragment editor = getSupportFragmentManager().findFragmentById(R.id.editor_container);
                if (editor instanceof EntryEditorFragment) {
                    ((EntryEditorFragment) editor).saveAndFinish();
                } else {
                    finish();
                }
            }
        });
    }

    @Override
    public void onEditorFinished() {
        finish();
    }
}
