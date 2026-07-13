package com.mowtiie.dearest.ui.activities;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.mowtiie.dearest.DearestApp;
import com.mowtiie.dearest.R;
import com.mowtiie.dearest.databinding.ActivityUnlockBinding;
import com.mowtiie.dearest.security.BiometricGate;
import com.mowtiie.dearest.ui.LoadingDialog;
import com.mowtiie.dearest.ui.viewmodel.UnlockViewModel;
import com.mowtiie.dearest.ui.viewmodel.UnlockViewModel.Mode;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import javax.crypto.Cipher;

public class UnlockActivity extends DearestActivity {

    private ActivityUnlockBinding binding;
    private UnlockViewModel viewModel;
    private LoadingDialog loadingDialog;

    private Mode mode = Mode.UNLOCK;
    private CountDownTimer lockoutTimer;
    private boolean autoPromptedBiometric;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUnlockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadingDialog = new LoadingDialog(this);

        clearErrorOnEdit(binding.passphraseInput, binding.passphraseLayout);
        clearErrorOnEdit(binding.confirmInput, binding.confirmLayout);

        viewModel = new ViewModelProvider(this).get(UnlockViewModel.class);
        viewModel.mode().observe(this, this::applyMode);
        viewModel.busy().observe(this, this::applyBusy);
        viewModel.error().observe(this, this::showError);
        viewModel.lockoutRemainingMs().observe(this, this::applyLockout);

        DearestApp.from(this).lockState().observe(this, locked -> {
            if (Boolean.FALSE.equals(locked)) finish();
        });

        binding.primaryButton.setOnClickListener(v -> submit());
        binding.biometricButton.setOnClickListener(v -> tryBiometricUnlock());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { moveTaskToBack(true); }
        });
    }

    private void applyMode(Mode m) {
        this.mode = m;
        boolean setup = (m == Mode.SETUP);
        binding.confirmLayout.setVisibility(setup ? View.VISIBLE : View.GONE);
        binding.setupExplainer.setVisibility(setup ? View.VISIBLE : View.GONE);
        binding.heading.setText(setup ? R.string.unlock_heading_setup : R.string.unlock_heading);
        binding.primaryButton.setText(setup ? R.string.unlock_action_create : R.string.unlock_action_unlock);

        boolean biometricUsable = !setup && biometricUsable();
        binding.biometricButton.setVisibility(biometricUsable ? View.VISIBLE : View.GONE);
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
            binding.biometricButton.setVisibility(View.GONE);
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
        binding.primaryButton.setEnabled(!b);
        binding.passphraseInput.setEnabled(!b);
        binding.confirmInput.setEnabled(!b);
    }

    private void showError(@Nullable String message) {
        binding.passphraseLayout.setError(message);
    }

    private void applyLockout(Long remainingMs) {
        if (lockoutTimer != null) {
            lockoutTimer.cancel();
            lockoutTimer = null;
        }
        long ms = (remainingMs == null) ? 0L : remainingMs;
        if (ms <= 0L) {
            binding.primaryButton.setEnabled(true);
            return;
        }
        binding.primaryButton.setEnabled(false);
        lockoutTimer = new CountDownTimer(ms, 1000) {
            @Override public void onTick(long msLeft) {
                showError(getString(R.string.unlock_locked_out, (msLeft / 1000) + 1));
            }
            @Override public void onFinish() {
                binding.primaryButton.setEnabled(true);
                showError(null);
            }
        }.start();
    }

    private void submit() {
        binding.passphraseLayout.setError(null);
        binding.confirmLayout.setError(null);

        char[] passphrase = extractChars(binding.passphraseInput.getText());
        binding.passphraseInput.setText("");

        if (passphrase.length == 0) {
            wipe(passphrase);
            if (mode == Mode.SETUP) {
                binding.confirmInput.setText("");
            }
            binding.passphraseLayout.setError(getString(R.string.unlock_error_empty_passphrase));
            return;
        }

        if (mode == Mode.SETUP) {
            char[] confirmation = extractChars(binding.confirmInput.getText());
            binding.confirmInput.setText("");
            if (confirmation.length == 0) {
                wipe(passphrase);
                wipe(confirmation);
                binding.confirmLayout.setError(getString(R.string.unlock_error_empty_confirm));
                return;
            }
            viewModel.setup(passphrase, confirmation);
        } else {
            viewModel.unlock(passphrase);
        }
    }

    private static void clearErrorOnEdit(TextInputEditText field, TextInputLayout layout) {
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { layout.setError(null); }
        });
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