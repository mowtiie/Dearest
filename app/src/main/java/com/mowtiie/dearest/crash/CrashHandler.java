package com.mowtiie.dearest.crash;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final int MAX_CAUSE_DEPTH = 10;

    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    private CrashHandler(Context context) {
        this.context = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public static void install(Context context) {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(context));
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        try {
            writeReport(t, e);
        } catch (Throwable ignored) {
        }

        if (defaultHandler != null) {
            defaultHandler.uncaughtException(t, e);
        } else {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
        }
    }

    private void writeReport(Thread thread, Throwable throwable) throws Exception {
        File reportFile = CrashReporter.getReportFile(context);
        File parent = reportFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(reportFile), StandardCharsets.UTF_8)) {
            writer.write(buildReport(thread, throwable));
        }
    }

    /**
     * A stack trace with exception messages left out. Messages can quote user input, such as
     * the text of a failed search, and the user is invited to copy or share this report.
     */
    static String traceWithoutMessages(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        Throwable current = throwable;
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
            sb.append(depth == 0 ? "" : "Caused by: ").append(current.getClass().getName()).append('\n');
            for (StackTraceElement frame : current.getStackTrace()) {
                sb.append("\tat ").append(frame).append('\n');
            }
            Throwable cause = current.getCause();
            current = (cause == current) ? null : cause;
        }
        return sb.toString();
    }

    private String buildReport(Thread thread, Throwable throwable) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== Dearest Crash Report ===\n");
        sb.append("Time: ").append(new Date()).append("\n");
        sb.append("Thread: ").append(thread.getName()).append("\n");
        sb.append("\n");

        sb.append("--- App Info ---\n");
        sb.append("Package: ").append(context.getPackageName()).append("\n");
        try {
            PackageInfo pkg = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            sb.append("Version: ").append(pkg.versionName).append(" (").append(pkg.versionCode).append(")\n");
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        sb.append("\n");

        sb.append("--- Device Info ---\n");
        sb.append("Manufacturer: ").append(Build.MANUFACTURER).append("\n");
        sb.append("Model: ").append(Build.MODEL).append("\n");
        sb.append("Android: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("\n");

        sb.append("--- Stack Trace ---\n");
        sb.append(traceWithoutMessages(throwable));

        return sb.toString();
    }
}