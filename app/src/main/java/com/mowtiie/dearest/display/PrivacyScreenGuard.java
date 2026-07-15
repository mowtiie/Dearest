package com.mowtiie.dearest.display;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.WeakHashMap;
import java.util.Map;

public final class PrivacyScreenGuard {

    private final DisplayPrefs prefs;
    private final Map<Activity, View> overlays = new WeakHashMap<>();

    private PrivacyScreenGuard(DisplayPrefs prefs) {
        this.prefs = prefs;
    }

    public static void install(Application app, DisplayPrefs prefs) {
        PrivacyScreenGuard guard = new PrivacyScreenGuard(prefs);
        app.registerActivityLifecycleCallbacks(guard.callbacks());
    }

    private Application.ActivityLifecycleCallbacks callbacks() {
        return new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}
            @Override public void onActivityStarted(@NonNull Activity activity) {}
            @Override public void onActivityResumed(@NonNull Activity activity) { hide(activity); }
            @Override public void onActivityPaused(@NonNull Activity activity) { show(activity); }
            @Override public void onActivityStopped(@NonNull Activity activity) {}
            @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
            @Override public void onActivityDestroyed(@NonNull Activity activity) { overlays.remove(activity); }
        };
    }

    private void show(Activity activity) {
        if (!prefs.isPrivacyScreenEnabled()) return;
        if (overlays.containsKey(activity)) return;

        ViewGroup root = activity.findViewById(android.R.id.content);
        if (root == null) return;

        View cover = new View(activity);
        cover.setBackgroundColor(resolveSurfaceColor(activity));
        root.addView(cover, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        overlays.put(activity, cover);
    }

    private void hide(Activity activity) {
        View cover = overlays.remove(activity);
        if (cover != null && cover.getParent() != null) {
            ((ViewGroup) cover.getParent()).removeView(cover);
        }
    }

    private static int resolveSurfaceColor(Context context) {
        android.util.TypedValue value = new android.util.TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, value, true);
        return value.data;
    }
}