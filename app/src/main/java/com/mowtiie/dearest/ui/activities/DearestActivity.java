package com.mowtiie.dearest.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.DearestApp;
import com.mowtiie.dearest.display.DisplayPrefs;
import com.mowtiie.dearest.display.SecureWindow;

public abstract class DearestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        applySecureFlag();
        applyContrastOverlay();
        super.onCreate(savedInstanceState);
        if (requiresUnlock()) {
            DearestApp.from(this).lockState().observe(this, locked -> {
                if (Boolean.TRUE.equals(locked)) {
                    startActivity(new Intent(this, UnlockActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
                }
            });
        }
    }

    /** Screens that show journal data must cover themselves with the unlock screen whenever the app locks. */
    protected boolean requiresUnlock() {
        return true;
    }

    private void applySecureFlag() {
        SecureWindow.apply(this, getWindow());
    }

    private void applyContrastOverlay() {
        String contrast = DearestApp.from(this).displayPrefs().getContrast();
        int overlayRes;
        switch (contrast) {
            case DisplayPrefs.CONTRAST_HIGH:
                overlayRes = R.style.Theme_Dearest_HighContrast;
            break;
            case DisplayPrefs.CONTRAST_MEDIUM:
                overlayRes = R.style.Theme_Dearest_MediumContrast;
            break;
            default:
                return;
        }
        getTheme().applyStyle(overlayRes, true);
    }
}