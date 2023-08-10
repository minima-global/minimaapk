package com.minima.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;

import com.minima.android.browser.MDSBrowserTest;
import com.minima.android.intro.OnboardingOne;
import com.minima.android.service.MinimaService;

public class LauncherActivity extends AppCompatActivity implements ServiceConnection {

    boolean mServiceConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Start the Minima Service..
        Intent minimaintent = new Intent(getBaseContext(), MinimaService.class);
        startForegroundService(minimaintent);
        bindService(minimaintent, this, Context.BIND_AUTO_CREATE);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //Check if we need to display our Onboarding
        if (!sharedPreferences.getBoolean("FIRST_RUN", true)) {
            Intent intent = new Intent(this, StartMinimaActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);

//            Intent intent = new Intent(this, MDSBrowserTest.class);
//            startActivity(intent);

        }else{

//            SharedPreferences.Editor sharedPreferencesEditor = PreferenceManager.getDefaultSharedPreferences(this).edit();
//            sharedPreferencesEditor.putBoolean("FIRST_RUN", false);
//            sharedPreferencesEditor.apply();

            Intent intent = new Intent(this, OnboardingOne.class);
            startActivity(intent);
        }

        finish();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mServiceConnected = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mServiceConnected = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Unbind from the service..
        if(mServiceConnected) {
            unbindService(this);
        }
    }
}