package com.mowtiie.dearest.ui.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.ui.InsetsUtil;
import com.mowtiie.dearest.ui.LoadingDialog;
import com.mowtiie.dearest.ui.viewmodel.ChangePassphraseViewModel;
import com.mowtiie.dearest.ui.viewmodel.ChangePassphraseViewModel.ErrorField;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ChangePassphraseActivity extends DearestActivity {

    private ChangePassphraseViewModel viewModel;
    private TextInputLayout currentLayout;
    private TextInputLayout newLayout;
    private TextInputLayout confirmLayout;
    private TextInputEditText currentInput;
    private TextInputEditText newInput;
    private TextInputEditText confirmInput;
    private Button changeButton;
    private LoadingDialog loadingDialog;

    @Nullable private ErrorField pendingErrorField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_passphrase);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        InsetsUtil.applyToolbarAndBottom(findViewById(R.id.root_view), findViewById(R.id.app_bar));

        currentLayout = findViewById(R.id.current_layout);
        newLayout = findViewById(R.id.new_layout);
        confirmLayout = findViewById(R.id.confirm_layout);
        currentInput = findViewById(R.id.current_input);
        newInput = findViewById(R.id.new_input);
        confirmInput = findViewById(R.id.confirm_input);
        changeButton = findViewById(R.id.change_button);
        loadingDialog = new LoadingDialog(this);

        clearErrorOnEdit(currentInput, currentLayout);
        clearErrorOnEdit(newInput, newLayout);
        clearErrorOnEdit(confirmInput, confirmLayout);

        viewModel = new ViewModelProvider(this).get(ChangePassphraseViewModel.class);
        viewModel.busy().observe(this, this::applyBusy);
        viewModel.errorField().observe(this, field -> pendingErrorField = field);
        viewModel.error().observe(this, this::showError);
        viewModel.success().observe(this, ok -> {
            if (Boolean.TRUE.equals(ok)) {
                Toast.makeText(this, R.string.change_pass_success, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        changeButton.setOnClickListener(v -> submit());
    }

    private void submit() {
        currentLayout.setError(null);
        newLayout.setError(null);
        confirmLayout.setError(null);

        char[] current = extractChars(currentInput.getText());
        char[] next = extractChars(newInput.getText());
        char[] confirm = extractChars(confirmInput.getText());
        currentInput.setText("");
        newInput.setText("");
        confirmInput.setText("");
        viewModel.change(current, next, confirm);
    }

    private void applyBusy(Boolean busy) {
        boolean b = Boolean.TRUE.equals(busy);
        if (b) {
            loadingDialog.show();
        } else {
            loadingDialog.dismiss();
        }
        changeButton.setEnabled(!b);
        currentInput.setEnabled(!b);
        newInput.setEnabled(!b);
        confirmInput.setEnabled(!b);
    }

    private void showError(@Nullable String message) {
        if (message == null || pendingErrorField == null) return;
        switch (pendingErrorField) {
            case CURRENT: currentLayout.setError(message); break;
            case NEW:     newLayout.setError(message); break;
            case CONFIRM: confirmLayout.setError(message); break;
        }
    }

    private static void clearErrorOnEdit(TextInputEditText field, TextInputLayout layout) {
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { layout.setError(null); }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private static char[] extractChars(Editable editable) {
        if (editable == null) return new char[0];
        char[] out = new char[editable.length()];
        editable.getChars(0, editable.length(), out, 0);
        return out;
    }

    @Override
    protected void onDestroy() {
        loadingDialog.dismiss();
        super.onDestroy();
    }
}