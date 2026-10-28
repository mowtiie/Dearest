package com.mowtiie.dearest.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mowtiie.dearest.display.SecureWindow;

/** The dialog builder for every dialog in the app, so none shows up in a screenshot. */
public class SecureDialogBuilder extends MaterialAlertDialogBuilder {

    private final Context appContext;

    public SecureDialogBuilder(@NonNull Context context) {
        super(context);
        appContext = context.getApplicationContext();
    }

    @NonNull
    @Override
    public AlertDialog create() {
        AlertDialog dialog = super.create();
        SecureWindow.apply(appContext, dialog.getWindow());
        return dialog;
    }
}
