package com.minima.android.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //MinimaLogger.log("MINIMA RECEIVER "+intent.getAction());

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            ServiceStarterJobService.enqueueWork(context, new Intent());
        }
    }

}
