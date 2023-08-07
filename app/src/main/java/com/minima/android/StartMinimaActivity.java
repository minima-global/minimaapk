package com.minima.android;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.minima.android.browser.MDSBrowserTest;
import com.minima.android.browser.MiniBrowser;
import com.minima.android.service.MinimaService;

import org.minima.system.Main;
import org.minima.system.mds.MDSManager;
import org.minima.utils.MinimaLogger;

public class StartMinimaActivity extends AppCompatActivity implements ServiceConnection {

    /**
     * Main Minmia Service
     */
    MinimaService mMinima = null;

    /**
     * Connecting Dialog
     */
    Dialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Start the Minima Service..
        Intent minimaintent = new Intent(getBaseContext(), MinimaService.class);
        startForegroundService(minimaintent);
        bindService(minimaintent, this, Context.BIND_AUTO_CREATE);

        //Create a Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setView(R.layout.progress);
        mDialog = builder.create();

        //Check for Battery Optimisation
        checkBattery();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        MinimaService.MyBinder binder = (MinimaService.MyBinder)iBinder;
        mMinima = binder.getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mMinima = null;
    }

    /**
     * Wait for Minima to Startup fully..
     */
    public void waitForMinimaToStartUp(){

        //Fast check..
        if(Main.getInstance() != null){
            MDSManager mds = Main.getInstance().getMDSManager();
            if(mds != null && mds.hasStarted()) {
                //Ready to go!
                startMiniHUB();
                return;
            }
        }

        MinimaLogger.log("MAINACTIVITY - waiting for Minima to StartUp..");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDialog.show();
            }
        });

        Runnable checker = new Runnable() {
            @Override
            public void run() {
                try{

                    //Check..
                    while(Main.getInstance() == null || mMinima == null){
                        Thread.sleep(500);
                    }

                    //Wait for MDS..
                    MDSManager mds = Main.getInstance().getMDSManager();
                    while(mds == null || !mds.hasStarted()) {
                        //Wait a sec..
                        Thread.sleep(2000);

                        //Try Again
                        mds = Main.getInstance().getMDSManager();
                    }

                }catch(Exception exc) {
                    MinimaLogger.log(exc);
                }

                MinimaLogger.log("Minima StartUp complete..");

                //Set the first Notification
                mMinima.setTopBlock();

                //remove the dialog..
                mDialog.dismiss();

                //Start her up
                startMiniHUB();
            }
        };

        Thread tt = new Thread(checker);
        tt.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Unbind from the service..
        if(mMinima != null) {
            unbindService(this);
        }
    }

    //Enable Ignore Battery
    public void checkBattery() {

        String packageName = getPackageName();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm.isIgnoringBatteryOptimizations(packageName)) {

            //Wait for startup sequence..
            waitForMinimaToStartUp();

        } else {

            //Run intent and get the result
            ActivityResultLauncher<Intent> startActivityForResult = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        //Wait for startup sequence..
                        waitForMinimaToStartUp();
                    }
            );

            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));

            startActivityForResult.launch(intent);
        }
    }

    //Start the MiniHUB
    public void startMiniHUB(){

        //Get the MDS Manager
        MDSManager mds = Main.getInstance().getMDSManager();

        //Now start the HUB..
        String minihubid = mds.getDefaultMiniHUB();

        //Get the sessionid..
        String sessionid = mds.convertMiniDAPPID(minihubid);

        String minihub = "https://127.0.0.1:9003/"+minihubid+"/index.html?uid="+sessionid;
        //String minihub = "https://127.0.0.1:9003/myerrorpage.html";

        //Set shutdown mode.. to FALSE
        MiniBrowser.mShutDownMode = false;

        //Start her up..
        Intent intent = new Intent(StartMinimaActivity.this, MiniBrowser.class);
        intent.putExtra("url",minihub);
        intent.putExtra("ishub",true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);

        //Close this window..
        StartMinimaActivity.this.finish();
    }
}
