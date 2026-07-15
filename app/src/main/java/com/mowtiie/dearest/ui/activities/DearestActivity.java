package com.mowtiie.dearest.ui.activities;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.DearestApp;
import com.mowtiie.dearest.display.DisplayPrefs;

public abstract class DearestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        applyContrastOverlay();
        super.onCreate(savedInstanceState);
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