package com.mowtiie.dearest.ui.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.mowtiie.dearest.display.DisplayPrefs;
import com.mowtiie.dearest.notification.ReminderPrefs;
import com.mowtiie.dearest.notification.ReminderScheduler;
import com.mowtiie.dearest.security.BiometricGate;
import com.mowtiie.dearest.ui.activities.BackupActivity;
import com.mowtiie.dearest.ui.activities.ChangePassphraseActivity;
import com.mowtiie.dearest.ui.activities.TagsManagementActivity;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.Calendar;

import javax.crypto.Cipher;

public class SettingsFragment extends PreferenceFragmentCompat {

    private static final String TAG = "SettingsFragment";

    private ReminderPrefs reminderPrefs;
    private DisplayPrefs displayPrefs;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                SwitchPreferenceCompat toggle = findPreference("pref_daily_reminder");
                if (granted) {
                    enableReminder(toggle);
                } else {
                    if (toggle != null) toggle.setChecked(false);
                    Toast.makeText(requireContext(), R.string.reminder_permission_denied,
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_settings, rootKey);

        DearestApp app = DearestApp.from(requireContext());
        reminderPrefs = new ReminderPrefs(requireContext());
        displayPrefs = app.displayPrefs();

        wireAppearance(app);
        wireLockTimeout(app);
        wireBiometric(app);
        wireLockNow(app);
        wirePrivacyScreen();
        wireChangePassphrase();
        wireManageTags();
        wireBackupExport();
        wireDailyReminder();
        wireVersion();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (reminderPrefs != null && reminderPrefs.isEnabled()) {
            ReminderScheduler.schedule(requireContext(), reminderPrefs.getHour(), reminderPrefs.getMinute());
        }
    }

    private void wireAppearance(DearestApp app) {
        ListPreference theme = findPreference("pref_theme");
        if (theme != null) {
            theme.setValue(displayPrefs.getThemeMode());
            theme.setOnPreferenceChangeListener((pref, newValue) -> {
                app.setThemeMode((String) newValue);
                return true;
            });
        }

        ListPreference contrast = findPreference("pref_contrast");
        if (contrast != null) {
            contrast.setValue(displayPrefs.getContrast());
            contrast.setOnPreferenceChangeListener((pref, newValue) -> {
                displayPrefs.setContrast((String) newValue);
                requireActivity().recreate();
                return true;
            });
        }

        SwitchPreferenceCompat dynamicColor = findPreference("pref_dynamic_color");
        if (dynamicColor != null) {
            boolean available = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && DynamicColors.isDynamicColorAvailable();
            dynamicColor.setEnabled(available);
            dynamicColor.setChecked(displayPrefs.isDynamicColorEnabled());
            if (!available) dynamicColor.setSummary(R.string.settings_dynamic_color_unavailable);
            dynamicColor.setOnPreferenceChangeListener((pref, newValue) -> {
                displayPrefs.setDynamicColorEnabled((Boolean) newValue);
                requireActivity().recreate();
                return true;
            });
        }
    }

    private void wirePrivacyScreen() {
        SwitchPreferenceCompat privacyScreen = findPreference("pref_privacy_screen");
        if (privacyScreen == null) return;
        privacyScreen.setChecked(displayPrefs.isPrivacyScreenEnabled());
        privacyScreen.setOnPreferenceChangeListener((pref, newValue) -> {
            displayPrefs.setPrivacyScreenEnabled((Boolean) newValue);
            return true;
        });
    }

    private void wireLockTimeout(DearestApp app) {
        androidx.preference.ListPreference timeout = findPreference("pref_lock_timeout");
        if (timeout == null) return;
        timeout.setValue(String.valueOf(app.getLockTimeoutMs()));
        timeout.setOnPreferenceChangeListener((pref, newValue) -> {
            app.setLockTimeoutMs(Long.parseLong((String) newValue));
            return true;
        });
    }

    private void wireBiometric(DearestApp app) {
        SwitchPreferenceCompat bio = findPreference("pref_biometric");
        if (bio == null) return;
        BiometricGate gate = app.biometricGate();
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

    private void enrollBiometric(SwitchPreferenceCompat pref) {
        BiometricGate gate = DearestApp.from(requireContext()).biometricGate();
        Cipher cipher;
        try {
            cipher = gate.getEnrollCipher();
        } catch (Exception e) {
            Log.e(TAG, "getEnrollCipher failed", e);
            toast(R.string.biometric_enroll_failed);
            return;
        }

        DearestApp app = DearestApp.from(requireContext());
        BiometricPrompt prompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(requireContext()),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        app.setAutoLockSuppressed(false);
                        try {
                            gate.completeEnroll(result.getCryptoObject().getCipher());
                            pref.setChecked(true);
                            toast(R.string.biometric_enabled);
                        } catch (Exception e) {
                            Log.e(TAG, "completeEnroll failed", e);
                            toast(R.string.biometric_enroll_failed);
                        }
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        app.setAutoLockSuppressed(false);
                    }
                });

        app.setAutoLockSuppressed(true);
        prompt.authenticate(
                new BiometricPrompt.PromptInfo.Builder()
                        .setTitle(getString(R.string.biometric_title))
                        .setSubtitle(getString(R.string.biometric_subtitle))
                        .setNegativeButtonText(getString(R.string.biometric_use_passphrase))
                        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                        .build(),
                new BiometricPrompt.CryptoObject(cipher));
    }

    private void wireLockNow(DearestApp app) {
        Preference lockNow = findPreference("pref_lock_now");
        if (lockNow != null) {
            lockNow.setOnPreferenceClickListener(p -> {
                app.lockNow();
                return true;
            });
        }
    }

    private void wireChangePassphrase() {
        Preference p = findPreference("pref_change_passphrase");
        if (p != null) {
            p.setOnPreferenceClickListener(pref -> {
                startActivity(new Intent(requireContext(), ChangePassphraseActivity.class));
                return true;
            });
        }
    }

    private void wireManageTags() {
        Preference p = findPreference("pref_manage_tags");
        if (p != null) {
            p.setOnPreferenceClickListener(pref -> {
                startActivity(new Intent(requireContext(), TagsManagementActivity.class));
                return true;
            });
        }
    }

    private void wireBackupExport() {
        Preference p = findPreference("pref_backup_export");
        if (p != null) {
            p.setOnPreferenceClickListener(pref -> {
                startActivity(new Intent(requireContext(), BackupActivity.class));
                return true;
            });
        }
    }

    private void wireDailyReminder() {
        SwitchPreferenceCompat toggle = findPreference("pref_daily_reminder");
        Preference timePref = findPreference("pref_reminder_time");
        if (toggle == null || timePref == null) return;

        toggle.setChecked(reminderPrefs.isEnabled());
        timePref.setEnabled(reminderPrefs.isEnabled());
        updateTimeSummary(timePref);

        toggle.setOnPreferenceChangeListener((pref, newValue) -> {
            boolean turningOn = (Boolean) newValue;
            if (turningOn) {
                if (needsNotificationPermission()) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    return false;
                }
                enableReminder((SwitchPreferenceCompat) pref);
                return false;
            } else {
                reminderPrefs.setEnabled(false);
                reminderPrefs.setPromptedExactAlarm(false);
                ReminderScheduler.cancel(requireContext());
                timePref.setEnabled(false);
                return true;
            }
        });

        timePref.setOnPreferenceClickListener(pref -> {
            showTimePicker(timePref);
            return true;
        });
    }

    private boolean needsNotificationPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED;
    }

    private void enableReminder(@Nullable SwitchPreferenceCompat toggle) {
        reminderPrefs.setEnabled(true);
        ReminderScheduler.schedule(requireContext(), reminderPrefs.getHour(), reminderPrefs.getMinute());
        if (toggle != null) toggle.setChecked(true);
        Preference timePref = findPreference("pref_reminder_time");
        if (timePref != null) timePref.setEnabled(true);

        maybePromptForExactAlarm();
    }

    private void maybePromptForExactAlarm() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        if (ReminderScheduler.canScheduleExact(requireContext())) return;
        if (reminderPrefs.hasPromptedExactAlarm()) return;

        reminderPrefs.setPromptedExactAlarm(true);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.exact_alarm_prompt_title)
                .setMessage(R.string.exact_alarm_prompt_message)
                .setNegativeButton(R.string.exact_alarm_prompt_not_now, null)
                .setPositiveButton(R.string.exact_alarm_prompt_grant, (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + requireContext().getPackageName()));
                    startActivity(intent);
                })
                .show();
    }

    private void showTimePicker(Preference timePref) {
        int format = DateFormat.is24HourFormat(requireContext())
                ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H;
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(format)
                .setHour(reminderPrefs.getHour())
                .setMinute(reminderPrefs.getMinute())
                .setTitleText(R.string.settings_reminder_time)
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            reminderPrefs.setTime(picker.getHour(), picker.getMinute());
            if (reminderPrefs.isEnabled()) {
                ReminderScheduler.schedule(requireContext(), picker.getHour(), picker.getMinute());
            }
            updateTimeSummary(timePref);
        });
        picker.show(getParentFragmentManager(), "reminder_time_picker");
    }

    private void updateTimeSummary(Preference timePref) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, reminderPrefs.getHour());
        cal.set(Calendar.MINUTE, reminderPrefs.getMinute());
        timePref.setSummary(DateFormat.getTimeFormat(requireContext()).format(cal.getTime()));
    }

    private void wireVersion() {
        Preference version = findPreference("pref_version");
        if (version != null) {
            version.setSummary(appVersion());
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

    private void toast(int stringRes) {
        Toast.makeText(requireContext(), stringRes, Toast.LENGTH_SHORT).show();
    }
}