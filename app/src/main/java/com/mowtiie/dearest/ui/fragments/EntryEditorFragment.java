package com.mowtiie.dearest.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mowtiie.dearest.R;
import com.mowtiie.dearest.data.model.Notebook;
import com.mowtiie.dearest.databinding.FragmentEntryEditorBinding;
import com.mowtiie.dearest.ui.InsetsUtil;
import com.mowtiie.dearest.ui.activities.TagPickerActivity;
import com.mowtiie.dearest.ui.viewmodel.EntryEditorViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Writes or edits one entry. Hosted full screen by {@code EntryEditorActivity}, or in the detail
 * pane of the Journal on wide screens. The host is told when the editor is done (saved, deleted).
 */
public class EntryEditorFragment extends Fragment {

    public interface Host {
        void onEditorFinished();
    }

    private static final String ARG_ENTRY_ID = "entryId";
    private static final String ARG_NOTEBOOK_ID = "notebookId";
    private static final String ARG_STANDALONE = "standalone";
    private static final long AUTOSAVE_DELAY_MS = 2000L;

    /**
     * @param standalone true when the editor owns the whole screen: it then applies the system bar
     *                   insets and shows an up button. False in a pane, where the parent does.
     */
    public static EntryEditorFragment newInstance(@Nullable String entryId,
                                                  @Nullable String notebookId, boolean standalone) {
        Bundle args = new Bundle();
        args.putString(ARG_ENTRY_ID, entryId);
        args.putString(ARG_NOTEBOOK_ID, notebookId);
        args.putBoolean(ARG_STANDALONE, standalone);
        EntryEditorFragment fragment = new EntryEditorFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private FragmentEntryEditorBinding binding;
    private EntryEditorViewModel viewModel;

    private final Handler autosaveHandler = new Handler(Looper.getMainLooper());
    private final Runnable autosaveRunnable = this::autosave;
    private final List<String> notebookIdsForDropdown = new ArrayList<>();
    private boolean shouldPopulate;
    private boolean finishing;

    private final ActivityResultLauncher<Intent> tagPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != android.app.Activity.RESULT_OK) return;
                List<String> names = TagPickerActivity.extractResult(result.getData());
                if (names != null && viewModel != null) viewModel.setTags(names);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentEntryEditorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        boolean standalone = args.getBoolean(ARG_STANDALONE);

        if (standalone) {
            InsetsUtil.applyToolbarAndBottom(binding.editorRoot, binding.editorAppBar);
            binding.editorToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            binding.editorToolbar.setNavigationContentDescription(R.string.cd_navigate_up);
            binding.editorToolbar.setNavigationOnClickListener(v -> saveAndFinish());
        }
        InsetsUtil.limitContentWidth(binding.editorScroll);

        binding.editorToolbar.inflateMenu(R.menu.menu_entry_editor);
        binding.editorToolbar.setOnMenuItemClickListener(this::onMenuItemSelected);

        viewModel = new ViewModelProvider(this).get(EntryEditorViewModel.class);
        viewModel.init(args.getString(ARG_ENTRY_ID), args.getString(ARG_NOTEBOOK_ID));
        binding.editorToolbar.setTitle(viewModel.isNew() ? R.string.editor_title_new : R.string.editor_title_edit);
        updateMenu();

        shouldPopulate = (savedInstanceState == null);
        viewModel.entry().observe(getViewLifecycleOwner(), entry -> {
            if (shouldPopulate && entry != null) {
                binding.editorTitle.setText(entry.getTitle());
                binding.editorBody.setText(entry.getBody());
                shouldPopulate = false;
            }
        });
        viewModel.deleteError().observe(getViewLifecycleOwner(), message -> {
            if (message == null) return;
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            viewModel.deleteErrorShown();
        });
        viewModel.finished().observe(getViewLifecycleOwner(), done -> {
            if (Boolean.TRUE.equals(done)) {
                finishing = true;
                Host host = host();
                if (host != null) host.onEditorFinished();
            }
        });

        viewModel.notebooks().observe(getViewLifecycleOwner(), notebooks -> {
            viewModel.ensureNotebook(notebooks);
            bindNotebookDropdown();
        });
        viewModel.notebookId().observe(getViewLifecycleOwner(), id -> {
            bindNotebookDropdown();
            scheduleAutosave();
        });

        viewModel.tagNames().observe(getViewLifecycleOwner(), names -> {
            renderTagChips(names);
            scheduleAutosave();
        });

        TextWatcher autosaveWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { scheduleAutosave(); }
        };
        binding.editorTitle.addTextChangedListener(autosaveWatcher);
        binding.editorBody.addTextChangedListener(autosaveWatcher);
    }

    @Nullable
    private Host host() {
        Fragment parent = getParentFragment();
        if (parent instanceof Host) return (Host) parent;
        return requireActivity() instanceof Host ? (Host) requireActivity() : null;
    }

    @Override
    public void onStop() {
        autosaveHandler.removeCallbacks(autosaveRunnable);
        if (!finishing) autosave();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        autosaveHandler.removeCallbacks(autosaveRunnable);
        super.onDestroyView();
        binding = null;
    }

    /** The entry being edited, or null for a new one. */
    @Nullable
    public String entryId() {
        return requireArguments().getString(ARG_ENTRY_ID);
    }

    /** Saves and closes the editor, as the up button and back do. */
    public void saveAndFinish() {
        if (binding == null) {
            // No view yet, so nothing typed to save; back must still leave the editor.
            Host host = host();
            if (host != null) host.onEditorFinished();
            return;
        }
        autosaveHandler.removeCallbacks(autosaveRunnable);
        viewModel.save(binding.editorTitle.getText().toString(), binding.editorBody.getText().toString());
    }

    private void bindNotebookDropdown() {
        if (binding == null) return;
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

        binding.notebookDropdown.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names));
        binding.notebookDropdown.setOnItemClickListener((parent, v, position, id) -> viewModel.setNotebook(notebookIdsForDropdown.get(position)));

        binding.notebookDropdown.setText(currentName, false);

        boolean hasDescription = currentDescription != null && !currentDescription.trim().isEmpty();
        binding.notebookDescriptionHint.setVisibility(hasDescription ? View.VISIBLE : View.GONE);
        if (hasDescription) binding.notebookDescriptionHint.setText(currentDescription);
    }

    private void renderTagChips(@Nullable List<String> tagNames) {
        if (binding == null) return;
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
        Intent intent = TagPickerActivity.createIntent(requireContext(),
                current != null ? current : Collections.emptyList());
        tagPickerLauncher.launch(intent);
    }

    private void scheduleAutosave() {
        autosaveHandler.removeCallbacks(autosaveRunnable);
        autosaveHandler.postDelayed(autosaveRunnable, AUTOSAVE_DELAY_MS);
    }

    private void autosave() {
        if (binding == null) return;
        boolean wasNew = viewModel.isNew();
        viewModel.autosave(binding.editorTitle.getText().toString(),
                binding.editorBody.getText().toString());
        if (wasNew && !viewModel.isNew()) updateMenu();
    }

    /** Share and delete only make sense once the entry exists. */
    private void updateMenu() {
        Menu menu = binding.editorToolbar.getMenu();
        boolean existingEntry = !viewModel.isNew();
        MenuItem share = menu.findItem(R.id.action_share);
        if (share != null) share.setVisible(existingEntry);
        MenuItem delete = menu.findItem(R.id.action_delete);
        if (delete != null) delete.setVisible(existingEntry);
    }

    private boolean onMenuItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_share) {
            shareEntry();
            return true;
        }
        if (id == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return false;
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
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.editor_delete_confirm_title)
                .setMessage(R.string.editor_delete_confirm_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.editor_delete, (d, w) -> {
                    autosaveHandler.removeCallbacks(autosaveRunnable);
                    viewModel.delete();
                })
                .show();
    }
}
