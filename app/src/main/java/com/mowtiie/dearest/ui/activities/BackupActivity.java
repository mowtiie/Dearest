package com.mowtiie.dearest.ui.activities;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.mowtiie.dearest.DearestApp;
import com.mowtiie.dearest.R;
import com.mowtiie.dearest.backup.BackupManager;
import com.mowtiie.dearest.backup.BackupManager.Format;
import com.mowtiie.dearest.ui.viewmodel.UnlockViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.LocalDate;
import java.util.Arrays;

public class BackupActivity extends DearestActivity {

    private BackupManager backupManager;
    private char[] pendingBackupPassword;
    private Uri pendingImportUri;

    private final ActivityResultLauncher<String> createEncrypted =
            registerForActivityResult(
                    new ActivityResultContracts.CreateDocument("application/octet-stream"),
                    this::onEncryptedTarget);

    private final ActivityResultLauncher<String> createJson =
            registerForActivityResult(
                    new ActivityResultContracts.CreateDocument("application/json"),
                    uri -> onPlainTarget(uri, Format.JSON));

    private final ActivityResultLauncher<String> createCsv =
            registerForActivityResult(
                    new ActivityResultContracts.CreateDocument("text/csv"),
                    uri -> onPlainTarget(uri, Format.CSV));

    private final ActivityResultLauncher<String> createText =
            registerForActivityResult(
                    new ActivityResultContracts.CreateDocument("text/plain"),
                    uri -> onPlainTarget(uri, Format.TEXT));

    private final ActivityResultLauncher<String[]> openBackup =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    this::onImportSource);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        MaterialToolbar toolbar = findViewById(R.id.backup_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        backupManager = DearestApp.from(this).backupManager();

        findViewById(R.id.btn_create_backup).setOnClickListener(v -> promptBackupPassword());
        findViewById(R.id.btn_restore).setOnClickListener(v ->
                openBackup.launch(new String[]{"*/*"}));
        findViewById(R.id.btn_export_plain).setOnClickListener(v -> warnThenExportPlain());
    }

    private void promptBackupPassword() {
        View content = getLayoutInflater().inflate(R.layout.dialog_backup_password, null);
        EditText password = content.findViewById(R.id.backup_password);
        EditText confirm = content.findViewById(R.id.backup_password_confirm);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.backup_password_title)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    char[] p = extractChars(password.getText());
                    char[] c = extractChars(confirm.getText());
                    if (!Arrays.equals(p, c)) {
                        wipe(p); wipe(c);
                        toast(getString(R.string.backup_password_mismatch));
                        return;
                    }
                    if (p.length < UnlockViewModel.MIN_PASSPHRASE_LENGTH) {
                        wipe(p); wipe(c);
                        toast(getString(R.string.backup_password_short,
                                UnlockViewModel.MIN_PASSPHRASE_LENGTH));
                        return;
                    }
                    wipe(c);
                    pendingBackupPassword = p;
                    createEncrypted.launch(fileName("dearest-backup", "dearest"));
                })
                .show();
    }

    private void onEncryptedTarget(@Nullable Uri uri) {
        char[] password = pendingBackupPassword;
        pendingBackupPassword = null;
        if (uri == null) {
            wipe(password);
            return;
        }
        backupManager.exportEncrypted(uri, password, (ok, msg) ->
                toast(ok ? getString(R.string.backup_saved) : orError(msg)));
    }

    private void warnThenExportPlain() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.backup_export_warning_title)
                .setMessage(R.string.backup_export_warning_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.backup_export_warning_continue, (d, w) -> chooseFormat())
                .show();
    }

    private void chooseFormat() {
        String[] formats = {"JSON", "CSV", getString(R.string.backup_format_text)};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.backup_choose_format)
                .setItems(formats, (d, which) -> {
                    switch (which) {
                        case 0: createJson.launch(fileName("dearest-entries", "json")); break;
                        case 1: createCsv.launch(fileName("dearest-entries", "csv")); break;
                        default: createText.launch(fileName("dearest-entries", "txt")); break;
                    }
                })
                .show();
    }

    private void onPlainTarget(@Nullable Uri uri, Format format) {
        if (uri == null) return;
        backupManager.exportPlain(uri, format, (ok, msg) ->
                toast(ok ? getString(R.string.export_saved) : orError(msg)));
    }

    private void onImportSource(@Nullable Uri uri) {
        if (uri == null) return;
        pendingImportUri = uri;

        View content = getLayoutInflater().inflate(R.layout.dialog_import, null);
        EditText password = content.findViewById(R.id.import_password);
        CheckBox replace = content.findViewById(R.id.replace_checkbox);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.import_title)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.import_action, (d, w) -> {
                    char[] p = extractChars(password.getText());
                    boolean replaceAll = replace.isChecked();
                    Uri source = pendingImportUri;
                    pendingImportUri = null;
                    backupManager.importEncrypted(source, p, replaceAll, (ok, msg) ->
                            toast(ok ? getString(R.string.backup_restored) : orError(msg)));
                })
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private String fileName(String prefix, String extension) {
        return prefix + "-" + LocalDate.now() + "." + extension;
    }

    private String orError(@Nullable String message) {
        return message != null ? message : getString(R.string.backup_import_failed);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private static char[] extractChars(Editable editable) {
        if (editable == null) return new char[0];
        char[] out = new char[editable.length()];
        editable.getChars(0, editable.length(), out, 0);
        return out;
    }

    private static void wipe(char[] array) {
        if (array != null) Arrays.fill(array, '\0');
    }
}