package com.minima.android;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.minima.android.browser.MiniBrowser;
import com.minima.android.service.MinimaService;

import org.minima.system.Main;
import org.minima.system.mds.MDSManager;
import org.minima.utils.MinimaLogger;

public class StartMinimaActivity extends AppCompatActivity implements ServiceConnection {

    /**
     * Main Minmia Service
     */
    MinimaService mMinima;

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

        //show a Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setView(R.layout.progress);

        mDialog = builder.create();
        mDialog.show();

    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        MinimaLogger.log("CONNECTED TO SERVICE");
        MinimaService.MyBinder binder = (MinimaService.MyBinder)iBinder;
        mMinima = binder.getService();

        waitForMinimaToStartUp();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        MinimaLogger.log("DISCONNECTED TO SERVICE");
    }

    /**
     * Wait for Minima to Startup fully..
     */
    public void waitForMinimaToStartUp(){
        MinimaLogger.log("MAINACTIVITY - waiting for Minima to StartUp..");

        Runnable checker = new Runnable() {
            @Override
            public void run() {
                try{

                    //Check..
                    while(Main.getInstance() == null){
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
                    MinimaLogger.log("Minima started.. ");

                }catch(Exception exc) {
                    MinimaLogger.log(exc);
                }

                MinimaLogger.log("MAINACTIVITY - Minima StartUp complete..");

                //remove the dialog..
                mDialog.dismiss();

                //Get the MDS Manager
                MDSManager mds = Main.getInstance().getMDSManager();

                //Now start the HUB..
                String minihubid = mds.getDefaultMiniHUB();

                //Get the sessionid..
                String sessionid = mds.convertMiniDAPPID(minihubid);

                String minihub = "https://127.0.0.1:9003/"+minihubid+"/index.html?uid="+sessionid;
                MinimaLogger.log("MINIHUB : "+minihub);

                //Start her up..
                Intent intent = new Intent(StartMinimaActivity.this, MiniBrowser.class);
                intent.putExtra("url",minihub);
                startActivity(intent);

                //Close this window..
                StartMinimaActivity.this.finish();
            }
        };

        Thread tt = new Thread(checker);
        tt.start();
    }

}
