package com.minima.android;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.minima.android.MainActivity;
import com.minima.android.dynamite.OnboardingOne;
import com.minima.android.mdshub.MiniBrowser;
import com.minima.android.service.MinimaService;

import org.minima.system.Main;
import org.minima.system.mds.MDSManager;
import org.minima.system.network.maxima.MaximaManager;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;

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
//        builder.setCancelable(false);
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

                    //Wait for Maxima..
                    MaximaManager max = Main.getInstance().getMaxima();
                    while(max == null || !max.isInited()) {
                        Thread.sleep(2000);
                        max = Main.getInstance().getMaxima();

                        if(max==null){
                            MinimaLogger.log("Waiting for Maxima.. max null");
                        }else{
                            MinimaLogger.log("Waiting for Maxima.. ");
                        }
                    }
                    MinimaLogger.log("Maxima started.. ");

                    //Run Status..
                    String status = mMinima.getMinima().runMinimaCMD("status",false);

                    //Make a JSON
                    JSONObject json = (JSONObject) new JSONParser().parse(status);

                    //Get the status..
                    while(!(boolean)json.get("status")){
                        MinimaLogger.log("Waiting for Status .. "+json.toString());

                        Thread.sleep(2000);

                        //Run Status..
                        status = mMinima.getMinima().runMinimaCMD("status");

                        //Make a JSON
                        json = (JSONObject) new JSONParser().parse(status);
                    }
                    MinimaLogger.log("Status true.. ");

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

                //Start her up..
                Intent intent = new Intent(StartMinimaActivity.this, MiniBrowser.class);
                intent.putExtra("url","https://127.0.0.1:9003/"+minihubid+"/index.html?uid="+sessionid);
                startActivity(intent);

                //Close this window..
                StartMinimaActivity.this.finish();
            }
        };

        Thread tt = new Thread(checker);
        tt.start();
    }

}
