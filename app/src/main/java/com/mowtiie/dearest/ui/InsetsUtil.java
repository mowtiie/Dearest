package com.mowtiie.dearest.ui;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class InsetsUtil {

    private InsetsUtil() {}

    public static void applyTop(View view) {
        final int l = view.getPaddingLeft(), t = view.getPaddingTop(),
                r = view.getPaddingRight(), b = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, wi) -> {
            Insets s = wi.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(l, t + s.top, r, b);
            return wi;
        });
        ViewCompat.requestApplyInsets(view);
    }

    public static void applyBottom(View view) {
        final int l = view.getPaddingLeft(), t = view.getPaddingTop(),
                r = view.getPaddingRight(), b = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, wi) -> {
            Insets s = wi.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(l, t, r, b + s.bottom);
            return wi;
        });
        ViewCompat.requestApplyInsets(view);
    }

    public static void applyTopAndBottom(View view) {
        final int l = view.getPaddingLeft(), t = view.getPaddingTop(),
                r = view.getPaddingRight(), b = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, wi) -> {
            Insets s = wi.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(l, t + s.top, r, b + s.bottom);
            return wi;
        });
        ViewCompat.requestApplyInsets(view);
    }
}
