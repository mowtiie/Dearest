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

    public static void applyToolbarAndBottom(View container, View topBar) {
        final int cl = container.getPaddingLeft(), ct = container.getPaddingTop(),
                cr = container.getPaddingRight(), cb = container.getPaddingBottom();
        final int tl = topBar.getPaddingLeft(), tt = topBar.getPaddingTop(),
                tr = topBar.getPaddingRight(), tb = topBar.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(container, (v, wi) -> {
            Insets s = wi.getInsets(WindowInsetsCompat.Type.systemBars());
            topBar.setPadding(tl, tt + s.top, tr, tb);
            v.setPadding(cl, ct, cr, cb + s.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(container);
    }
}