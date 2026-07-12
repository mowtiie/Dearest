package com.mowtiie.dearest.ui;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.mowtiie.dearest.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class LoadingDialog {

    private final Context context;
    private final AlertDialog dialog;
    private final TextView messageView;

    public LoadingDialog(@NonNull Context context) {
        this(context, R.string.loading_default_message);
    }

    public LoadingDialog(@NonNull Context context, @StringRes int messageRes) {
        this.context = context;
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null);
        messageView = view.findViewById(R.id.loading_message);
        messageView.setText(messageRes);

        dialog = new MaterialAlertDialogBuilder(context)
                .setView(view)
                .setCancelable(false)
                .create();
        dialog.setCanceledOnTouchOutside(false);
    }

    public void setMessage(@StringRes int messageRes) {
        messageView.setText(messageRes);
    }

    public void show() {
        if (isHostAlive() && !dialog.isShowing()) {
            dialog.show();
        }
    }

    public void dismiss() {
        if (dialog.isShowing()) {
            try {
                dialog.dismiss();
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private boolean isHostAlive() {
        if (!(context instanceof Activity)) return true;
        Activity activity = (Activity) context;
        return !activity.isFinishing() && !activity.isDestroyed();
    }
}