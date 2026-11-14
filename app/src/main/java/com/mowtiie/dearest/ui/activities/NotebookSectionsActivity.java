package com.mowtiie.dearest.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mowtiie.dearest.DearestApp;
import com.mowtiie.dearest.R;
import com.mowtiie.dearest.data.model.Section;
import com.mowtiie.dearest.databinding.ActivityNotebookSectionsBinding;
import com.mowtiie.dearest.ui.InsetsUtil;
import com.mowtiie.dearest.ui.SecureDialogBuilder;
import com.mowtiie.dearest.ui.adapters.SectionAdapter;

import java.util.List;

/** Browses and manages the child Sections of one Notebook. */
public final class NotebookSectionsActivity extends DearestActivity implements SectionAdapter.Listener {
    private static final String EXTRA_NOTEBOOK_ID = "com.dearest.extra.SECTIONS_NOTEBOOK_ID";
    private static final String EXTRA_NOTEBOOK_NAME = "com.dearest.extra.SECTIONS_NOTEBOOK_NAME";

    public static void open(Context context, String notebookId, String notebookName) {
        Intent intent = new Intent(context, NotebookSectionsActivity.class)
                .putExtra(EXTRA_NOTEBOOK_ID, notebookId)
                .putExtra(EXTRA_NOTEBOOK_NAME, notebookName);
        context.startActivity(intent);
    }

    private ActivityNotebookSectionsBinding binding;
    private SectionAdapter adapter;
    private ItemTouchHelper touchHelper;
    private String notebookId;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotebookSectionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        notebookId = getIntent().getStringExtra(EXTRA_NOTEBOOK_ID);
        String notebookName = getIntent().getStringExtra(EXTRA_NOTEBOOK_NAME);
        binding.sectionsToolbar.setTitle(notebookName == null ? getString(R.string.sections_title) : notebookName);
        binding.sectionsToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        binding.sectionsToolbar.setNavigationContentDescription(R.string.cd_navigate_up);
        binding.sectionsToolbar.setNavigationOnClickListener(v -> finish());
        InsetsUtil.applyToolbarAndBottom(binding.sectionsRoot, binding.sectionsToolbar);
        InsetsUtil.limitContentWidth(binding.sectionsList);

        adapter = new SectionAdapter(this);
        binding.sectionsList.setLayoutManager(new LinearLayoutManager(this));
        binding.sectionsList.setAdapter(adapter);
        touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override public boolean isLongPressDragEnabled() { return false; }
            @Override public boolean onMove(RecyclerView rv, RecyclerView.ViewHolder from,
                                           RecyclerView.ViewHolder to) {
                adapter.onItemMove(from.getBindingAdapterPosition(), to.getBindingAdapterPosition());
                return true;
            }
            @Override public void onSelectedChanged(@Nullable RecyclerView.ViewHolder holder, int state) {
                super.onSelectedChanged(holder, state);
                if (state == ItemTouchHelper.ACTION_STATE_DRAG) dragging = true;
            }
            @Override public void clearView(RecyclerView rv, RecyclerView.ViewHolder holder) {
                super.clearView(rv, holder);
                if (dragging) persistOrder(adapter.getItems());
                dragging = false;
            }
            @Override public void onSwiped(RecyclerView.ViewHolder holder, int direction) {}
        });
        touchHelper.attachToRecyclerView(binding.sectionsList);

        binding.addSectionButton.setOnClickListener(v -> showSectionNameDialog(null));
        DearestApp.from(this).repository().observeSections(notebookId).observe(this, sections -> {
            if (!dragging) adapter.setItems(sections);
            binding.sectionsEmpty.setVisibility(sections == null || sections.isEmpty() ? View.VISIBLE : View.GONE);
            refreshCounts(sections);
        });
    }

    private boolean dragging;

    @Override protected void onResume() {
        super.onResume();
        if (adapter != null) refreshCounts(adapter.getItems());
    }

    private void refreshCounts(@Nullable List<Section> sections) {
        if (sections == null) return;
        for (Section section : sections) {
            DearestApp.from(this).repository().countEntriesInSection(section.getId(),
                    count -> { if (adapter != null) adapter.setEntryCount(section.getId(), count); });
        }
    }

    @Override public void onOpen(Section section) {
        SectionJournalActivity.open(this, notebookId, section.getId(), section.getName());
    }

    @Override public void onRename(Section section) { showSectionNameDialog(section); }

    private void showSectionNameDialog(@Nullable Section existing) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(R.string.section_name_hint);
        if (existing != null) input.setText(existing.getName());
        new SecureDialogBuilder(this)
                .setTitle(existing == null ? R.string.action_add_section : R.string.section_rename)
                .setView(input)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) return;
                    Section section = existing == null
                            ? Section.createNew(notebookId, name, adapter.getItems().size())
                            : new Section(existing.getId(), notebookId, name, existing.getPosition(), existing.getCreatedAt());
                    DearestApp.from(this).repository().saveSection(section, (ok, error) -> {
                        if (!ok) showMessage(error == null ? getString(R.string.error_generic) : error);
                    });
                }).show();
    }

    @Override public void onDelete(Section section) {
        List<Section> all = adapter.getItems();
        if (all.size() <= 1) {
            showMessage(getString(R.string.section_keep_one));
            return;
        }
        List<Section> destinations = new java.util.ArrayList<>();
        for (Section candidate : all) if (!candidate.getId().equals(section.getId())) destinations.add(candidate);
        String[] names = new String[destinations.size()];
        for (int i=0;i<destinations.size();i++) names[i] = destinations.get(i).getName();
        new SecureDialogBuilder(this).setTitle(R.string.section_delete_move_title)
                .setMessage(R.string.section_delete_move_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setItems(names, (dialog, which) -> DearestApp.from(this).repository().deleteSection(
                        section.getId(), destinations.get(which).getId(), (ok, error) -> {
                            if (ok) showMessage(getString(R.string.section_deleted));
                            else showMessage(error == null ? getString(R.string.error_generic) : error);
                        })).show();
    }

    @Override public void onStartDrag(RecyclerView.ViewHolder holder) { touchHelper.startDrag(holder); }

    private void persistOrder(List<Section> sections) {
        for (int i=0;i<sections.size();i++) {
            Section section = sections.get(i);
            if (section.getPosition() != i) {
                Section ordered = new Section(section.getId(), section.getNotebookId(), section.getName(), i, section.getCreatedAt());
                DearestApp.from(this).repository().saveSection(ordered, (ok, error) -> {
                    if (!ok) showMessage(error == null ? getString(R.string.error_generic) : error);
                });
            }
        }
    }

    private void showMessage(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
}
