package com.mowtiie.dearest.ui.activities;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.lifecycle.ViewModelProvider;

import com.mowtiie.dearest.DearestApp;
import com.mowtiie.dearest.R;
import com.mowtiie.dearest.ui.viewmodel.UnlockViewModel;
import com.mowtiie.dearest.ui.viewmodel.UnlockViewModel.Mode;
import com.google.android.material.progressindicator.CircularProgressIndicator;

public class UnlockActivity extends DearestActivity {

    private UnlockViewModel viewModel;

    private TextView heading;
    private TextView explainer;
    private EditText passphraseInput;
    private EditText confirmInput;
    private Button primaryButton;
    private TextView errorText;
    private CircularProgressIndicator progress;

    private Mode mode = Mode.UNLOCK;
    private CountDownTimer lockoutTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unlock);

        heading = findViewById(R.id.heading);
        explainer = findViewById(R.id.setup_explainer);
        passphraseInput = findViewById(R.id.passphrase_input);
        confirmInput = findViewById(R.id.confirm_input);
        primaryButton = findViewById(R.id.primary_button);
        errorText = findViewById(R.id.error_text);
        progress = findViewById(R.id.progress);

        viewModel = new ViewModelProvider(this).get(UnlockViewModel.class);

        viewModel.mode().observe(this, this::applyMode);
        viewModel.busy().observe(this, this::applyBusy);
        viewModel.error().observe(this, this::showError);
        viewModel.lockoutRemainingMs().observe(this, this::applyLockout);

        DearestApp.from(this).lockState().observe(this, locked -> {
            if (Boolean.FALSE.equals(locked)) finish();
        });

        primaryButton.setOnClickListener(v -> submit());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { moveTaskToBack(true); }
        });
    }

    private void applyMode(Mode m) {
        this.mode = m;
        boolean setup = (m == Mode.SETUP);
        confirmInput.setVisibility(setup ? View.VISIBLE : View.GONE);
        explainer.setVisibility(setup ? View.VISIBLE : View.GONE);
        heading.setText(setup ? R.string.unlock_heading_setup : R.string.unlock_heading);
        primaryButton.setText(setup ? R.string.unlock_action_create : R.string.unlock_action_unlock);
    }

    private void applyBusy(Boolean busy) {
        boolean b = Boolean.TRUE.equals(busy);
        progress.setVisibility(b ? View.VISIBLE : View.GONE);
        primaryButton.setEnabled(!b);
        passphraseInput.setEnabled(!b);
        confirmInput.setEnabled(!b);
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(message == null ? View.GONE : View.VISIBLE);
    }

    private void applyLockout(Long remainingMs) {
        if (lockoutTimer != null) {
            lockoutTimer.cancel();
            lockoutTimer = null;
        }
        long ms = (remainingMs == null) ? 0L : remainingMs;
        if (ms <= 0L) {
            primaryButton.setEnabled(true);
            return;
        }
        primaryButton.setEnabled(false);
        lockoutTimer = new CountDownTimer(ms, 1000) {
            @Override public void onTick(long msLeft) {
                showError(getString(R.string.unlock_locked_out, (msLeft / 1000) + 1));
            }
            @Override public void onFinish() {
                primaryButton.setEnabled(true);
                showError(null);
            }
        }.start();
    }

    private void submit() {
        char[] passphrase = extractChars(passphraseInput.getText());
        passphraseInput.setText("");
        if (mode == Mode.SETUP) {
            char[] confirmation = extractChars(confirmInput.getText());
            confirmInput.setText("");
            viewModel.setup(passphrase, confirmation);
        } else {
            viewModel.unlock(passphrase);
        }
    }

    private static char[] extractChars(Editable editable) {
        if (editable == null) return new char[0];
        char[] out = new char[editable.length()];
        editable.getChars(0, editable.length(), out, 0);
        return out;
    }

    @Override
    protected void onDestroy() {
        if (lockoutTimer != null) lockoutTimer.cancel();
        super.onDestroy();
    }
}
