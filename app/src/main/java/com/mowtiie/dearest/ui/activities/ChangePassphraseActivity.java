package com.mowtiie.dearest.ui.activities;

import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.ui.InsetsUtil;
import com.mowtiie.dearest.ui.LoadingDialog;
import com.mowtiie.dearest.ui.viewmodel.ChangePassphraseViewModel;
import com.google.android.material.appbar.MaterialToolbar;

public class ChangePassphraseActivity extends DearestActivity {

    private ChangePassphraseViewModel viewModel;
    private EditText currentInput;
    private EditText newInput;
    private EditText confirmInput;
    private Button changeButton;
    private TextView errorText;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_passphrase);

        MaterialToolbar toolbar = findViewById(R.id.cp_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        InsetsUtil.applyToolbarAndBottom(findViewById(R.id.cp_root), findViewById(R.id.cp_app_bar));

        currentInput = findViewById(R.id.current_input);
        newInput = findViewById(R.id.new_input);
        confirmInput = findViewById(R.id.confirm_input);
        changeButton = findViewById(R.id.change_button);
        errorText = findViewById(R.id.error_text);
        loadingDialog = new LoadingDialog(this);

        viewModel = new ViewModelProvider(this).get(ChangePassphraseViewModel.class);
        viewModel.busy().observe(this, this::applyBusy);
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

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(message == null ? View.GONE : View.VISIBLE);
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