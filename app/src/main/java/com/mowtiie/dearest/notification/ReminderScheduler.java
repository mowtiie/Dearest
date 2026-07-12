package com.mowtiie.dearest.notification;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

import com.mowtiie.dearest.R;

import java.util.Calendar;

public final class ReminderScheduler {

    static final String CHANNEL_ID = "daily_reminder";
    private static final int REQUEST_CODE = 1001;

    private ReminderScheduler() {}

    public static void ensureChannel(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.reminder_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(context.getString(R.string.reminder_channel_description));
            nm.createNotificationChannel(channel);
        }
    }

    public static void schedule(Context context, int hour, int minute) {
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (next.getTimeInMillis() <= System.currentTimeMillis()) {
            next.add(Calendar.DAY_OF_MONTH, 1);
        }

        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pendingIntent(context));
    }

    public static void cancel(Context context) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        alarmManager.cancel(pendingIntent(context));
    }

    static void rescheduleIfEnabled(Context context) {
        ReminderPrefs prefs = new ReminderPrefs(context);
        if (prefs.isEnabled()) {
            schedule(context, prefs.getHour(), prefs.getMinute());
        }
    }

    private static PendingIntent pendingIntent(Context context) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags);
    }
}
