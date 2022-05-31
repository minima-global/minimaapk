package com.minima.android.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.minima.utils.MinimaLogger;

public class Alarm extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent){
        MinimaLogger.log("ALARM RECEIVED : Start Service");

        //Create the Minima Service Intent
        Intent serviceintent = new Intent(context, MinimaService.class);
        context.startForegroundService(serviceintent);

        //Send a start service JOB
        //ServiceStarterJobService.enqueueWork(context, new Intent());
    }

    public void setAlarm(Context context){
        MinimaLogger.log("ALARM SET");

        AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, Alarm.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), AlarmManager.INTERVAL_FIFTEEN_MINUTES , pi); // Millisec * Second * Minute
//        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60 * 1 , pi); // Millisec * Second * Minute
    }

    public void cancelAlarm(Context context){
        MinimaLogger.log("ALARM CANCELLED");

        Intent intent = new Intent(context, Alarm.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
}
