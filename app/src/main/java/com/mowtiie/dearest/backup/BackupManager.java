package com.mowtiie.dearest.backup;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.data.repository.DearestRepository;
import com.mowtiie.dearest.security.BackupCrypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BackupManager {

    public enum Format { JSON, CSV, TEXT }

    public interface Callback {
        void onComplete(boolean success, @Nullable String message);
    }

    private final Context appContext;
    private final DearestRepository repository;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public BackupManager(Context context, DearestRepository repository) {
        this.appContext = context.getApplicationContext();
        this.repository = repository;
    }

    public void exportEncrypted(Uri target, char[] password, Callback cb) {
        repository.loadAll((notebooks, entries) -> io.execute(() -> {
            boolean ok = false;
            String message = null;
            try (OutputStream os = appContext.getContentResolver().openOutputStream(target)) {
                if (os == null) throw new IOException("Could not open the file");
                byte[] json = BackupSerializer.toJson(new BackupData(notebooks, entries))
                        .getBytes(StandardCharsets.UTF_8);
                String envelope = BackupCrypto.encrypt(json, password);
                os.write(envelope.getBytes(StandardCharsets.UTF_8));
                ok = true;
            } catch (Exception e) {
                message = e.getMessage();
            }
            done(cb, ok, message);
        }));
    }

    public void exportPlain(Uri target, Format format, Callback cb) {
        repository.loadAll((notebooks, entries) -> io.execute(() -> {
            boolean ok = false;
            String message = null;
            try (OutputStream os = appContext.getContentResolver().openOutputStream(target)) {
                if (os == null) throw new IOException("Could not open the file");
                BackupData data = new BackupData(notebooks, entries);
                String content;
                switch (format) {
                    case JSON: content = BackupSerializer.toJson(data); break;
                    case CSV:  content = BackupSerializer.toCsv(data); break;
                    default:   content = BackupSerializer.toPlainText(data); break;
                }
                os.write(content.getBytes(StandardCharsets.UTF_8));
                ok = true;
            } catch (Exception e) {
                message = e.getMessage();
            }
            done(cb, ok, message);
        }));
    }

    public void importEncrypted(Uri source, char[] password, boolean replaceAll, Callback cb) {
        io.execute(() -> {
            try {
                String envelope = readAll(source);
                byte[] json = BackupCrypto.decrypt(envelope, password);
                BackupData data = BackupSerializer.fromJson(
                        new String(json, StandardCharsets.UTF_8));
                repository.importAll(data.notebooks, data.entries, replaceAll, cb::onComplete);
            } catch (Exception e) {
                done(cb, false, appContext.getString(R.string.backup_import_failed));
            }
        });
    }

    private String readAll(Uri source) throws IOException {
        try (InputStream is = appContext.getContentResolver().openInputStream(source)) {
            if (is == null) throw new IOException("Could not open the file");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) bos.write(buffer, 0, read);
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private void done(Callback cb, boolean ok, @Nullable String message) {
        main.post(() -> cb.onComplete(ok, message));
    }
}
