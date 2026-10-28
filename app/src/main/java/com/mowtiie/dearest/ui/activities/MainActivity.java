package com.mowtiie.dearest.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.mowtiie.dearest.DearestApp;
import com.mowtiie.dearest.R;
import com.mowtiie.dearest.crash.CrashReporter;
import com.mowtiie.dearest.data.model.DeletedEntry;
import com.mowtiie.dearest.data.repository.DearestRepository;
import com.mowtiie.dearest.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends DearestActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private boolean crashDialogShown;
    private Snackbar undoBar;

    private final ActivityResultLauncher<Intent> saveCrashLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                        android.net.Uri uri = result.getData().getData();
                        if (uri == null) return;

                        if (CrashReporter.writeReportToUri(this, uri)) {
                            Toast.makeText(this, R.string.toast_crash_save_success, Toast.LENGTH_SHORT).show();
                            CrashReporter.deleteReport(this);
                        } else {
                            Toast.makeText(this, R.string.toast_crash_save_failure, Toast.LENGTH_SHORT).show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavHostFragment host = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = host.getNavController();

        NavigationUI.setupWithNavController(binding.bottomNav, navController);
        appBarConfiguration = new AppBarConfiguration.Builder(binding.bottomNav.getMenu()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        applyInsets();

        DearestApp.from(this).repository().deletedEntry().observe(this, this::showUndoDelete);

        DearestApp.from(this).lockState().observe(this, locked -> {
            if (Boolean.FALSE.equals(locked) && !crashDialogShown) {
                crashDialogShown = true;
                CrashReporter.showDialogIfPending(this, saveCrashLauncher);
            }
        });
    }

    private void showUndoDelete(DeletedEntry deleted) {
        if (deleted == null) {
            // Locked or discarded: the snapshot is gone, so the prompt must go too.
            if (undoBar != null) undoBar.dismiss();
            return;
        }
        DearestRepository repository = DearestApp.from(this).repository();
        // A snapshot redelivered after a rotation only gets what is left of its window.
        int remainingMs = (int) deleted.remainingMs();
        if (remainingMs <= 0) {
            repository.discardDeleted(deleted);
            return;
        }
        Snackbar bar = Snackbar.make(binding.getRoot(), R.string.entry_deleted, remainingMs)
                .setAnchorView(binding.bottomNav)
                .setAction(R.string.action_undo, v -> repository.restoreEntry(deleted, (ok, error) -> {
                    if (!ok) Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                }))
                .addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar dismissed, int event) {
                        if (undoBar == dismissed) undoBar = null;
                        // Dismissed in code (lock, or this activity going away) leaves the
                        // snapshot for the next prompt; the repository expires it on its own.
                        if (event != DISMISS_EVENT_MANUAL) repository.discardDeleted(deleted);
                    }
                });
        undoBar = bar;
        bar.show();
    }

    @Override
    protected void onDestroy() {
        undoBar = null;
        super.onDestroy();
    }

    private void applyInsets() {
        View root = binding.getRoot();
        View appBar = binding.appBar;
        View bottomNav = binding.bottomNav;
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, wi) -> {
            Insets bars = wi.getInsets(WindowInsetsCompat.Type.systemBars());
            appBar.setPadding(appBar.getPaddingLeft(), bars.top, appBar.getPaddingRight(), appBar.getPaddingBottom());
            bottomNav.setPadding(bottomNav.getPaddingLeft(), bottomNav.getPaddingTop(), bottomNav.getPaddingRight(), bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(root);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }
}