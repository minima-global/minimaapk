package com.minima.android.dependencies.backupSync.recurring;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.minima.android.dependencies.backupSync.BackupSyncProvider;
import com.minima.android.dependencies.backupSync.minima.MinimaBackupUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class BackupRecurringAlarmManager extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println("RAFARAFA - backing up at: " + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(OffsetDateTime.now()));
        backup(context);
    }

    private void backup(Context context) {
        BackupSyncProvider
                .getGoogleDriveProvider(context)
                .uploadBackup(
                        context,
                        MinimaBackupUtils.createBackup(context)
                );
    }

    public static void setAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, BackupRecurringAlarmManager.class);

        am.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                createPendingIntent(context, intent)
        );
    }

    private static PendingIntent createPendingIntent(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return PendingIntent.getBroadcast(context, backupAlarmRequestCode, intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            return PendingIntent.getBroadcast(context, backupAlarmRequestCode, intent, 0);
        }
    }

    private static final int backupAlarmRequestCode = 10;
}