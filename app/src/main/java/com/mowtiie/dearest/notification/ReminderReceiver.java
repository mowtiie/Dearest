package com.mowtiie.dearest.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.ui.activities.MainActivity;

public class ReminderReceiver extends BroadcastReceiver {

    private static final int NOTIFICATION_ID = 2001;

    @Override
    public void onReceive(Context context, Intent intent) {
        ReminderScheduler.ensureChannel(context);
        showNotification(context);

        ReminderScheduler.rescheduleIfEnabled(context);
    }

    private void showNotification(Context context) {
        Intent openApp = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, ReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.reminder_notification_title))
                .setContentText(context.getString(R.string.reminder_notification_text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException ignored) {
        }
    }
}