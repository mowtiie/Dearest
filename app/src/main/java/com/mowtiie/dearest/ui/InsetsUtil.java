package com.mowtiie.dearest.ui;

import android.view.View;

import com.mowtiie.dearest.R;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class InsetsUtil {

    private InsetsUtil() {}

    /**
     * Keeps content readable on tablets, foldables and landscape by centering it in at most
     * {@code content_max_width}. Extra width becomes horizontal padding on top of any existing padding.
     */
    public static void limitContentWidth(View view) {
        final int maxWidth = view.getResources().getDimensionPixelSize(R.dimen.content_max_width);
        final int baseLeft = view.getPaddingLeft(), baseRight = view.getPaddingRight();
        view.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int extra = Math.max(0, ((right - left) - maxWidth) / 2);
            if (v.getPaddingLeft() != baseLeft + extra || v.getPaddingRight() != baseRight + extra) {
                v.post(() -> v.setPadding(baseLeft + extra, v.getPaddingTop(), baseRight + extra, v.getPaddingBottom()));
            }
        });
    }

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