package com.mowtiie.dearest.display;

import android.content.Context;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.mowtiie.dearest.DearestApp;

/**
 * Keeps a window out of screenshots, screen recordings and the recents thumbnail unless the user
 * allowed screenshots. Every window needs it: dialogs and sheets do not inherit the flag from
 * the activity behind them.
 */
public final class SecureWindow {

    private SecureWindow() {}

    public static void apply(Context context, @Nullable Window window) {
        if (window == null) return;
        if (DearestApp.from(context).displayPrefs().isScreenshotsAllowed()) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        }
    }
}
