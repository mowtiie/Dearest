package com.mowtiie.dearest.ui.fragments;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.mowtiie.dearest.DearestApp;
import com.mowtiie.dearest.R;
import com.mowtiie.dearest.security.BiometricGate;
import com.mowtiie.dearest.ui.activities.BackupActivity;

import javax.crypto.Cipher;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_settings, rootKey);

        DearestApp app = DearestApp.from(requireContext());

        ListPreference timeout = findPreference("pref_lock_timeout");
        if (timeout != null) {
            timeout.setValue(String.valueOf(app.getLockTimeoutMs()));
            timeout.setOnPreferenceChangeListener((pref, newValue) -> {
                app.setLockTimeoutMs(Long.parseLong((String) newValue));
                return true;
            });
        }

        Preference lockNow = findPreference("pref_lock_now");
        if (lockNow != null) {
            lockNow.setOnPreferenceClickListener(p -> {
                app.lockNow();
                return true;
            });
        }

        Preference version = findPreference("pref_version");
        if (version != null) {
            version.setSummary(appVersion());
        }

        wireComingSoon("pref_change_passphrase");
        wireComingSoon("pref_manage_notebooks");

        SwitchPreferenceCompat bio = findPreference("pref_biometric");
        BiometricGate gate = DearestApp.from(requireContext()).biometricGate();
        if (bio != null) {
            boolean available = BiometricGate.isAvailable(requireContext());
            bio.setEnabled(available);
            bio.setChecked(gate.isEnrolled());
            if (!available) bio.setSummary(R.string.settings_biometric_unavailable);
            bio.setOnPreferenceChangeListener((pref, newValue) -> {
                if ((Boolean) newValue) {
                    enrollBiometric((SwitchPreferenceCompat) pref);
                    return false;
                }
                gate.disable();
                return true;
            });
        }

        Preference backup = findPreference("pref_backup_export");
        if (backup != null) {
            backup.setOnPreferenceClickListener(p -> {
                startActivity(new Intent(requireContext(), BackupActivity.class));
                return true;
            });
        }
    }

    private void enrollBiometric(SwitchPreferenceCompat pref) {
        BiometricGate gate = DearestApp.from(requireContext()).biometricGate();
        Cipher cipher;
        try {
            cipher = gate.getEnrollCipher();
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.biometric_enroll_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        BiometricPrompt prompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(requireContext()),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        try {
                            gate.completeEnroll(result.getCryptoObject().getCipher());
                            pref.setChecked(true);
                            Toast.makeText(requireContext(), R.string.biometric_enabled, Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(requireContext(), R.string.biometric_enroll_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        prompt.authenticate(
                new BiometricPrompt.PromptInfo.Builder()
                        .setTitle(getString(R.string.biometric_title))
                        .setSubtitle(getString(R.string.biometric_subtitle))
                        .setNegativeButtonText(getString(R.string.biometric_use_passphrase))
                        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                        .build(),
                new BiometricPrompt.CryptoObject(cipher));
    }

    private void wireComingSoon(String key) {
        Preference p = findPreference(key);
        if (p != null) {
            p.setOnPreferenceClickListener(pref -> {
                Toast.makeText(requireContext(), R.string.coming_soon, Toast.LENGTH_SHORT).show();
                return true;
            });
        }
    }

    private String appVersion() {
        try {
            PackageManager pm = requireContext().getPackageManager();
            return pm.getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }
}