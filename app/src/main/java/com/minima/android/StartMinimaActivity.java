package com.minima.android;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
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
import androidx.core.app.ServiceCompat;

import com.minima.android.browser.MDSBrowserTest;
import com.minima.android.browser.MiniBrowser;
import com.minima.android.service.MinimaService;

import org.minima.database.minidapps.MiniDAPP;
import org.minima.system.Main;
import org.minima.system.mds.MDSManager;
import org.minima.utils.MinimaLogger;
import org.minima.utils.MinimaUncaughtException;

import java.util.ArrayList;

public class StartMinimaActivity extends AppCompatActivity implements ServiceConnection {

    /**
     * Main Minmia Service
     */
    MinimaService mMinima = null;

    /**
     * Connecting Dialog
     */
    Dialog mDialog;

    /**
     * Startup Error Dialog
     */
    Dialog mStartErrorDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set some static vars
        MiniBrowser.mShutDownMode    = false;
        MiniBrowser.mShutDownCompact = false;
        MiniBrowser.mMinimaSSLCert   = null;

        //Catch ALL Uncaught Exceptions..
        Thread.setDefaultUncaughtExceptionHandler(new MinimaUncaughtException());

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
        boolean minst = (Main.getInstance() != null);
        MinimaLogger.log("Wait for Startup.. Main.getInstance = "+minst);
        if(Main.getInstance() != null){
            MDSManager mds = Main.getInstance().getMDSManager();

            boolean mdsinst = (Main.getInstance().getMDSManager() != null);
            MinimaLogger.log("Wait for Startup.. MDSManager = "+mdsinst);
            if(mdsinst){
                MinimaLogger.log("Wait for Startup.. MDSManager started = "+mds.hasStarted());
            }

            if(mds != null && mds.hasStarted()) {
                //Ready to go!
                checkStartup();
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
                checkStartup();
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

    public void checkStartup(){

        boolean sterror = Main.getInstance().isStartupError();
        String errormsg = Main.getInstance().getStartupErrorMsg();

        if(sterror){

            MinimaLogger.log("Startup Check Error:"+sterror+" "+errormsg);

            Runnable rr = new Runnable() {
                @Override
                public void run() {
                    //Create a Dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(StartMinimaActivity.this);
                    builder.setCancelable(true);
                    builder.setTitle("Serious Startup Error");
                    builder.setMessage("There was an error initialising your Minima Database!\n\n" +
                            "Please perform a chain-resync or restore your node from a backup..\n\n" +
                            "Full Error Message : "+errormsg);

                    builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            //Start the hub..
                            startMiniHUB();
                        }
                    });

                    mStartErrorDialog = builder.create();
                    mStartErrorDialog.show();
                }
            };

            runOnUiThread(rr);

        }else{

            //All fine..
            startMiniHUB();
        }
    }

    //Start the MiniHUB
    private void startMiniHUB(){

        //Get the MDS Manager
        MDSManager mds = Main.getInstance().getMDSManager();

        //Now start the HUB..
        String minihubid = mds.getDefaultMiniHUB();
//        String minihubid = mds.getMiniDAPPFromName("terminal").getUID();

        //Get the sessionid..
        String sessionid = mds.convertMiniDAPPID(minihubid);

        //Reset the SSL cert
        MiniBrowser.mMinimaSSLCert = null;

        //Set the correct starting page
        String minihub = "https://127.0.0.1:9003/"+minihubid+"/index.html?uid="+sessionid;

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
