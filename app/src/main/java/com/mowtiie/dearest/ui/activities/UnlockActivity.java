package com.mowtiie.dearest.ui.activities;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.mowtiie.dearest.DearestApp;
import com.mowtiie.dearest.R;
import com.mowtiie.dearest.security.BiometricGate;
import com.mowtiie.dearest.ui.LoadingDialog;
import com.mowtiie.dearest.ui.viewmodel.UnlockViewModel;
import com.mowtiie.dearest.ui.viewmodel.UnlockViewModel.Mode;

import javax.crypto.Cipher;

public class UnlockActivity extends DearestActivity {

    private UnlockViewModel viewModel;

    private TextView heading;
    private TextView explainer;
    private EditText passphraseInput;
    private EditText confirmInput;
    private Button primaryButton;
    private Button biometricButton;
    private TextView errorText;
    private LoadingDialog loadingDialog;

    private Mode mode = Mode.UNLOCK;
    private CountDownTimer lockoutTimer;
    private boolean autoPromptedBiometric;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unlock);

        heading = findViewById(R.id.heading);
        explainer = findViewById(R.id.setup_explainer);
        passphraseInput = findViewById(R.id.passphrase_input);
        confirmInput = findViewById(R.id.confirm_input);
        primaryButton = findViewById(R.id.primary_button);
        biometricButton = findViewById(R.id.biometric_button);
        errorText = findViewById(R.id.error_text);
        loadingDialog = new LoadingDialog(this);

        viewModel = new ViewModelProvider(this).get(UnlockViewModel.class);
        viewModel.mode().observe(this, this::applyMode);
        viewModel.busy().observe(this, this::applyBusy);
        viewModel.error().observe(this, this::showError);
        viewModel.lockoutRemainingMs().observe(this, this::applyLockout);

        DearestApp.from(this).lockState().observe(this, locked -> {
            if (Boolean.FALSE.equals(locked)) finish();
        });

        primaryButton.setOnClickListener(v -> submit());
        biometricButton.setOnClickListener(v -> tryBiometricUnlock());

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

        boolean biometricUsable = !setup && biometricUsable();
        biometricButton.setVisibility(biometricUsable ? View.VISIBLE : View.GONE);
        if (biometricUsable && !autoPromptedBiometric) {
            autoPromptedBiometric = true;
            tryBiometricUnlock();
        }
    }

    private boolean biometricUsable() {
        BiometricGate gate = DearestApp.from(this).biometricGate();
        return gate.isEnrolled() && BiometricGate.isAvailable(this);
    }

    private void tryBiometricUnlock() {
        BiometricGate gate = DearestApp.from(this).biometricGate();
        if (!gate.isEnrolled() || !BiometricGate.isAvailable(this)) return;

        Cipher cipher;
        try {
            cipher = gate.getUnlockCipher();
        } catch (KeyPermanentlyInvalidatedException invalidated) {
            gate.disable();
            biometricButton.setVisibility(View.GONE);
            showError(getString(R.string.biometric_reset));
            return;
        } catch (Exception e) {
            return;
        }

        BiometricPrompt prompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        DearestApp.from(UnlockActivity.this).setAutoLockSuppressed(false);
                        try {
                            BiometricPrompt.CryptoObject crypto = result.getCryptoObject();
                            gate.completeUnlock(crypto.getCipher());
                            DearestApp.from(UnlockActivity.this).onUnlocked();
                        } catch (Exception e) {
                            showError(getString(R.string.biometric_failed));
                        }
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        DearestApp.from(UnlockActivity.this).setAutoLockSuppressed(false);
                    }
                });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_title))
                .setSubtitle(getString(R.string.biometric_subtitle))
                .setNegativeButtonText(getString(R.string.biometric_use_passphrase))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build();

        DearestApp.from(this).setAutoLockSuppressed(true);
        prompt.authenticate(info, new BiometricPrompt.CryptoObject(cipher));
    }

    private void applyBusy(Boolean busy) {
        boolean b = Boolean.TRUE.equals(busy);
        if (b) {
            loadingDialog.show();
        } else {
            loadingDialog.dismiss();
        }
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

        if (passphrase.length == 0) {
            wipe(passphrase);
            if (mode == Mode.SETUP) {
                confirmInput.setText("");
            }
            showError(getString(R.string.unlock_error_empty_passphrase));
            return;
        }

        if (mode == Mode.SETUP) {
            char[] confirmation = extractChars(confirmInput.getText());
            confirmInput.setText("");
            if (confirmation.length == 0) {
                wipe(passphrase);
                wipe(confirmation);
                showError(getString(R.string.unlock_error_empty_confirm));
                return;
            }
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

    private static void wipe(char[] array) {
        java.util.Arrays.fill(array, '\0');
    }

    @Override
    protected void onDestroy() {
        if (lockoutTimer != null) lockoutTimer.cancel();
        loadingDialog.dismiss();
        super.onDestroy();
    }
}