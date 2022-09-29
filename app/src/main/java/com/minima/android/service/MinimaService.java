package com.minima.android.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.minima.android.MainActivity;

import org.minima.Minima;
import org.minima.system.Main;
import org.minima.system.network.webhooks.NotifyManager;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONObject;
import org.minima.utils.messages.Message;
import org.minima.utils.messages.MessageListener;

import java.util.ArrayList;
import com.minima.android.ui.backup.Bip39Activity;
//import com.minima.android.R;
//import com.minima.boot.Alarm;

/** Foreground Service for the Minima Node
 *
 *  Elias Nemr
 *
 * 23 April 2020
 * */
public class MinimaService extends Service {

    static boolean TEST     = true;
    static boolean GENESIS  = true;

    //Currently Binding doesn't work as we run in a separate process..
    public class MyBinder extends Binder {
        public MinimaService getService() {
            return mService;
        }
    }
    private IBinder mBinder = new MyBinder();
    MinimaService mService;

    //The alarm to ensure Minima doesn't stop
    Alarm mAlarm;

    //The Battery receiver
    BroadcastReceiver mBatteryReceiver;

    //Minima Main Starter
    public static Minima minima;

    //Used to update the Notification
    Handler mHandler;

    NotificationManager mNotificationManager;
    android.app.Notification mNotification;

    //Start Minima When Notification is clicked..
    PendingIntent mPendingIntent;

    //Information for the Notification
    JSONObject mTxPowJSON = null;

    public static final String CHANNEL_ID = "MinimaServiceChannel";

    PowerManager.WakeLock mWakeLock;
    WifiManager.WifiLock mWifiLock;

    public Bip39Activity mArchiveListener = null;

    @Override
    public void onCreate() {
        super.onCreate();

        MinimaLogger.log("Minima Service Started");

        //Power
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Minima::MiniPower");
        if(!mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }

        //WiFi..
        WifiManager wifi = (WifiManager) getSystemService(WIFI_SERVICE);
        mWifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL, "Minima::MiniWiFi");
        if(!mWifiLock.isHeld()){
            mWifiLock.acquire();
        }

        //Start Minima
        minima = new Minima();

        mHandler = new Handler(Looper.getMainLooper());

        // Create our notification channel here & add channel to Manager's list
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Minima Node Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
        );

        mNotificationManager = getSystemService(NotificationManager.class);
        mNotificationManager.createNotificationChannel(serviceChannel);

        Intent NotificationIntent = new Intent(getBaseContext(), MainActivity.class);
//        mPendingIntent = PendingIntent.getActivity(getBaseContext(), 0
//                , NotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mPendingIntent = PendingIntent.getActivity(getBaseContext(), 0
                    , NotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        }else {
            mPendingIntent = PendingIntent.getActivity(getBaseContext(), 0
                    , NotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        }

        //Set the Alarm..
        mAlarm = new Alarm();
        mAlarm.cancelAlarm(this);
        mAlarm.setAlarm(this);

        mService = this;

        //Add a Minima listener..
        Main.setMinimaListener(new MessageListener() {
            @Override
            public void processMessage(Message zMessage) {
                if(zMessage.getMessageType().equals(MinimaLogger.MINIMA_LOG)){
//                    Console.writeLine(zMessage.getString("log"));

                }else if(zMessage.getMessageType().equals(NotifyManager.NOTIFY_POST)){
                    //Get the JSON..
                    JSONObject notify = (JSONObject) zMessage.getObject("notify");

                    //MinimaLogger.log("NOTIFY : "+notify.toString());

                    //What is the Event..
                    String event    = (String) notify.get("event");
                    JSONObject data = (JSONObject) notify.get("data");

                    if(event.equals("NEWBLOCK")) {

                        //Get the TxPoW
                        mTxPowJSON = (JSONObject) data.get("txpow");

                        //Show a notification
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                //Set status Bar notification
                                setMinimaNotification();
                            }
                        });

                    }else if(event.equals("ARCHIVEUPDATE")){
                        String message = data.getString("message");
                        if(mArchiveListener!=null){
                            mArchiveListener.updateLoader(message);
                        }
                    }
                }
            }
        });

        //Start her up..
        ArrayList<String> vars = new ArrayList<>();

        vars.add("-daemon");

        vars.add("-data");
        vars.add(getFilesDir().getAbsolutePath());

        //Normal
        vars.add("-isclient");
        vars.add("-mobile");

        vars.add("-mdsenable");

        vars.add("-noshutdownhook");

        if(TEST) {
            vars.add("-nop2p");
            vars.add("-test");
        }

        if(GENESIS) {
//            vars.add("-nop2p");
            vars.add("-genesis");
        }

        //vars.add("-connect");
        //vars.add("35.228.18.150:9001");
        //vars.add("10.0.2.2:9001");

        minima.mainStarter(vars.toArray(new String[0]));

        //Notify User service is now running!
        Toast.makeText(this, "Minima Service Started", Toast.LENGTH_SHORT).show();

        //Listen to Battery Events
        addBatteryListener();
   }

    public Minima getMinima(){
        return minima;
    }

    public Notification createNotification(String zText){
        mNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(zText)
                .setContentText("Minima Status Channel")
                .setSmallIcon(com.minima.android.R.drawable.ic_minima)
                .setContentIntent(mPendingIntent)
                .build();

        return mNotification;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //Set status Bar notification
        setMinimaNotification();

        return START_STICKY;
    }

    private void setMinimaNotification(){
        //Set the default message
        if(mTxPowJSON == null){
            startForeground(1, createNotification("Starting up.. please wait.."));

        }else{
            JSONObject header = (JSONObject)mTxPowJSON.get("header");

            String block    = (String) header.get("block");
            String date     = (String) header.get("date");

            startForeground(1, createNotification("Block " + block + " @ " + date));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        MinimaLogger.log("Minima Service onDestroy start");

        //QUIT nicely..
        String resp = minima.runMinimaCMD("quit");

        //Not listening anymore..
        Main.setMinimaListener(null);

        //Shut the channel..
        mNotificationManager.deleteNotificationChannel(CHANNEL_ID);

        //Mention..
        Toast.makeText(this, "Minima Service Stopped", Toast.LENGTH_SHORT).show();

        //Release the wakelocks..
        mWakeLock.release();
        mWifiLock.release();

        //Remove the receiver
        if(mBatteryReceiver != null){
            unregisterReceiver(mBatteryReceiver);
        }

        MinimaLogger.log("Minima Service onDestroy end");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public static boolean isPlugged(Context context) {
        boolean isPlugged= false;
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;

        return isPlugged;
    }

    public void addBatteryListener(){
        //Listen for Battery Events..
        mBatteryReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

                //Make sure has started up..
                if(getMinima().getMain() == null){
                    return;
                }

                //What Happened..
                if (plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB) {
                    // on AC power
                    //MinimaLogger.log("BATTERY PLUGGED IN");

                    //Set PoW to regular
                    getMinima().getMain().setNormalAutoMineSpeed();

                } else if (plugged == 0) {

                    // on battery power
                    //MinimaLogger.log("BATTERY NOT PLUGGED IN");

                    //Set PoW to regular
                    getMinima().getMain().setLowPowAutoMineSpeed();


                } else {
                    // intent didnt include extra info
                    MinimaLogger.log("BATTERY NO EXTRA INFO");
                }
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mBatteryReceiver, filter);
    }
}

