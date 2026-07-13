package com.mowtiie.dearest.ui.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.databinding.ActivityChangePassphraseBinding;
import com.mowtiie.dearest.ui.InsetsUtil;
import com.mowtiie.dearest.ui.LoadingDialog;
import com.mowtiie.dearest.ui.viewmodel.ChangePassphraseViewModel;
import com.mowtiie.dearest.ui.viewmodel.ChangePassphraseViewModel.ErrorField;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ChangePassphraseActivity extends DearestActivity {

    private ActivityChangePassphraseBinding binding;
    private ChangePassphraseViewModel viewModel;
    private LoadingDialog loadingDialog;

    @Nullable private ErrorField pendingErrorField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePassphraseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.cpToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        InsetsUtil.applyToolbarAndBottom(binding.cpRoot, binding.cpAppBar);

        loadingDialog = new LoadingDialog(this);

        clearErrorOnEdit(binding.currentInput, binding.currentLayout);
        clearErrorOnEdit(binding.newInput, binding.newLayout);
        clearErrorOnEdit(binding.confirmInput, binding.confirmLayout);

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

        binding.changeButton.setOnClickListener(v -> submit());
    }

    private void submit() {
        binding.currentLayout.setError(null);
        binding.newLayout.setError(null);
        binding.confirmLayout.setError(null);

        char[] current = extractChars(binding.currentInput.getText());
        char[] next = extractChars(binding.newInput.getText());
        char[] confirm = extractChars(binding.confirmInput.getText());
        binding.currentInput.setText("");
        binding.newInput.setText("");
        binding.confirmInput.setText("");
        viewModel.change(current, next, confirm);
    }

    private void applyBusy(Boolean busy) {
        boolean b = Boolean.TRUE.equals(busy);
        if (b) {
            loadingDialog.show();
        } else {
            loadingDialog.dismiss();
        }
        binding.changeButton.setEnabled(!b);
        binding.currentInput.setEnabled(!b);
        binding.newInput.setEnabled(!b);
        binding.confirmInput.setEnabled(!b);
    }

    private void showError(@Nullable String message) {
        if (message == null || pendingErrorField == null) return;
        switch (pendingErrorField) {
            case CURRENT: binding.currentLayout.setError(message); break;
            case NEW:     binding.newLayout.setError(message); break;
            case CONFIRM: binding.confirmLayout.setError(message); break;
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