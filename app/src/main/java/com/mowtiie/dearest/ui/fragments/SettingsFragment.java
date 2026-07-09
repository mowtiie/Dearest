package com.mowtiie.dearest.ui.fragments;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.mowtiie.dearest.DearestApp;
import com.mowtiie.dearest.R;

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
        wireComingSoon("pref_backup_export");
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