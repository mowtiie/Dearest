package com.mowtiie.dearest.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.data.model.Notebook;
import com.mowtiie.dearest.ui.adapters.NotebookAdapter;
import com.mowtiie.dearest.ui.viewmodel.NotebookViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class NotebooksFragment extends Fragment implements NotebookAdapter.Listener {

    private NotebookViewModel viewModel;
    private NotebookAdapter adapter;
    private ItemTouchHelper touchHelper;
    private boolean dragging;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notebooks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(NotebookViewModel.class);
        adapter = new NotebookAdapter(this);

        RecyclerView list = view.findViewById(R.id.notebooks_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        touchHelper = new ItemTouchHelper(dragCallback());
        touchHelper.attachToRecyclerView(list);

        FloatingActionButton fab = view.findViewById(R.id.fab_add_notebook);
        fab.setOnClickListener(v ->
                showNameDialog(R.string.add_notebook_title, null, viewModel::createNotebook));

        viewModel.notebooks().observe(getViewLifecycleOwner(), notebooks -> {
            if (!dragging) adapter.setItems(notebooks);
        });
    }

    @Override
    public void onRename(Notebook notebook) {
        showNameDialog(R.string.rename_notebook_title, notebook.getName(),
                name -> viewModel.renameNotebook(notebook, name));
    }

    @Override
    public void onDelete(Notebook notebook) {
        List<Notebook> all = viewModel.notebooks().getValue();
        if (all != null && all.size() <= 1) {
            toast(getString(R.string.cannot_delete_last));
            return;
        }
        viewModel.countEntries(notebook.getId(), count -> {
            if (count == 0) {
                confirmDeleteEmpty(notebook);
            } else {
                promptMoveThenDelete(notebook, count, all);
            }
        });
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder holder) {
        touchHelper.startDrag(holder);
    }

    private interface NameCallback {
        void onName(String name);
    }

    private void showNameDialog(int titleRes, @Nullable String prefill, NameCallback callback) {
        View content = getLayoutInflater().inflate(R.layout.dialog_edit_name, null);
        EditText input = content.findViewById(R.id.name_input);
        if (prefill != null) {
            input.setText(prefill);
            input.setSelection(input.getText().length());
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(titleRes)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_save,
                        (d, w) -> callback.onName(input.getText().toString()))
                .show();
    }

    private void confirmDeleteEmpty(Notebook notebook) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_notebook_title)
                .setMessage(R.string.delete_notebook_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.editor_delete,
                        (d, w) -> deleteNotebook(notebook.getId(), null))
                .show();
    }

    private void promptMoveThenDelete(Notebook notebook, int count, List<Notebook> all) {
        List<Notebook> others = new ArrayList<>();
        if (all != null) {
            for (Notebook n : all) {
                if (!n.getId().equals(notebook.getId())) others.add(n);
            }
        }
        String[] names = new String[others.size()];
        for (int i = 0; i < others.size(); i++) names[i] = others.get(i).getName();

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_notebook_move_title)
                .setMessage(getString(R.string.delete_notebook_move_message, count))
                .setNegativeButton(android.R.string.cancel, null)
                .setItems(names, (d, which) ->
                        deleteNotebook(notebook.getId(), others.get(which).getId()))
                .show();
    }

    private void deleteNotebook(String id, @Nullable String moveToId) {
        viewModel.deleteNotebook(id, moveToId, (success, error) -> {
            if (success) {
                toast(getString(R.string.notebook_deleted));
            } else if (error != null) {
                toast(error);
            }
        });
    }

    private ItemTouchHelper.Callback dragCallback() {
        return new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

            @Override public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override public boolean onMove(@NonNull RecyclerView rv,
                                            @NonNull RecyclerView.ViewHolder vh,
                                            @NonNull RecyclerView.ViewHolder target) {
                adapter.onItemMove(vh.getBindingAdapterPosition(),
                        target.getBindingAdapterPosition());
                return true;
            }

            @Override public void onSelectedChanged(@Nullable RecyclerView.ViewHolder vh,
                                                    int actionState) {
                super.onSelectedChanged(vh, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) dragging = true;
            }

            @Override public void clearView(@NonNull RecyclerView rv,
                                            @NonNull RecyclerView.ViewHolder vh) {
                super.clearView(rv, vh);
                dragging = false;
                viewModel.saveOrder(adapter.getItems());
            }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) { }
        };
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}