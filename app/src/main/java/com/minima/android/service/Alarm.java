package com.minima.android.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.minima.utils.MinimaLogger;

public class Alarm extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent){
        //MinimaLogger.log("MINIMA ALARM RECEIVED : Start Service");

        //Create the Minima Service Intent
        try{
            Intent serviceintent = new Intent(context, MinimaService.class);
            context.startForegroundService(serviceintent);
        }catch(Exception exc){
            MinimaLogger.log("Cannot start foreground service : "+exc);
        }

        //Send a start service JOB
        //ServiceStarterJobService.enqueueWork(context, new Intent());
    }

    public void setAlarm(Context context){
        //MinimaLogger.log("MINIMA ALARM SET");

        AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, Alarm.class);

        PendingIntent pi;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        }else {
            pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        }

        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), AlarmManager.INTERVAL_FIFTEEN_MINUTES , pi); // Millisec * Second * Minute
//        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60 * 1 , pi); // Millisec * Second * Minute
    }

    public void cancelAlarm(Context context){
        //MinimaLogger.log("MINIMA ALARM CANCELLED");

        Intent intent = new Intent(context, Alarm.class);

        PendingIntent pi;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        }else {
            pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pi);
    }
}
