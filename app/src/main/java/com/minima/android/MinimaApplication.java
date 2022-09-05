package com.minima.android;

import android.app.Application;

import com.minima.android.dependencies.backupSync.recurring.BackupRecurringAlarmManager;

public class MinimaApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        BackupRecurringAlarmManager.setAlarm(this);
    }
}
