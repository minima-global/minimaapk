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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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

import org.minima.Minima;
import org.minima.database.MinimaDB;
import org.minima.objects.TxPoW;
import org.minima.system.Main;
import org.minima.system.mds.MDSManager;
import org.minima.system.network.webhooks.NotifyManager;
import org.minima.system.params.ParamConfigurer;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONObject;
import org.minima.utils.messages.Message;
import org.minima.utils.messages.MessageListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

import com.minima.android.StartMinimaActivity;
import com.minima.android.browser.MiniBrowser;
import com.minima.android.browser.NotifyBrowser;

/** Foreground Service for the Minima Node
 *
 *  Elias Nemr
 *
 * 23 April 2020
 * */
public class MinimaService extends Service {

    public static boolean mHaveStartedShutdown = false;

    //Currently Binding doesn't work as we run in a separate process..
    public class MyBinder extends Binder {
        public MinimaService getService() {
            return mService;
        }
    }
    private IBinder mBinder = new MyBinder();
    MinimaService mService;

    //The alarm to ensure Minima doesn't stop
    static boolean mCancelAlarmOnShutdown = false;
    Alarm mAlarm;

    //The Battery receiver
    BroadcastReceiver mBatteryReceiver;

    //Minima Main Starter
    public static Minima minima;

    //Used to update the Notification
    Handler mHandler;

    NotificationManager mNotificationManager;
    android.app.Notification mNotification;

    //Information for the Notification
    JSONObject mTxPowJSON = null;

    public static final String CHANNEL_ID = "MinimaServiceChannel";

    PowerManager.WakeLock mWakeLock;
    WifiManager.WifiLock mWifiLock;

    public static MiniBrowser mNotifyShutdown;

    @Override
    public void onCreate() {
        super.onCreate();

        MinimaLogger.log("Minima Service Started");

        //No Browser subscribed yet..
        mNotifyShutdown = null;

        //Have not started shutdown
        mHaveStartedShutdown    = false;
        mCancelAlarmOnShutdown  = false;

        //Set some default static vars
        MiniBrowser.mShutDownMode    = false;
        MiniBrowser.mShutDownCompact = false;
        MiniBrowser.mMinimaSSLCert   = null;

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

        //Set the Alarm..
        mAlarm = new Alarm();
        mAlarm.cancelAlarm(this);
        mAlarm.setAlarm(this);

        //Store this
        mService = this;

        //Add a Minima listener..
        Main.setMinimaListener(new MessageListener() {
            @Override
            public void processMessage(Message zMessage) throws Exception {

                if(zMessage.getMessageType().equals(NotifyManager.NOTIFY_POST)){
                    //Get the JSON..
                    JSONObject notify = (JSONObject) zMessage.getObject("notify");

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

                    }else if(event.equals("NEWBALANCE")){

                        MinimaLogger.log("SERVICE received NEWBALANCE");

                        //Notify the User
                        createMiniDAPPNotification("0xFF","Minima","Your balance has changed");

                    }else if(event.equals("NOTIFICATION")){

                        boolean show = (boolean)data.get("show");
                        String uid = data.getString("uid", "NO UID");

                        if(show) {
                            String title = data.getString("title", "NO TITLE");
                            String text = data.getString("text", "NO MSG");

                            createMiniDAPPNotification(uid, title, text);
                        }else{
                            cancelNotification(uid);
                        }

                    }else if(event.equals("SHUTDOWN")){

                        MinimaLogger.log("SERVICE Received SHUTDOWN!");
                        if(!haveStartedShutdown()){
                            stopSelf();
                        }else{
                            MinimaLogger.log("SERVICE allready SHUTDOWN");
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

        vars.add("-basefolder");
        vars.add(getFilesDir().getAbsolutePath());

        //Normal
        vars.add("-isclient");
        vars.add("-mobile");
        vars.add("-limitbandwidth");

        vars.add("-mdsenable");

        //TESTER HACK
//        vars.add("-noconnect");
//        vars.add("-mdspassword");
//        vars.add("123");

//        vars.add("-genesis");
//        vars.add("-nop2p");
//        vars.add("-test");

        vars.add("-nosyncibd");

        vars.add("-noshutdownhook");

        //Are there any EXTRA params..
        SharedPreferences pref  = getSharedPreferences("startup_params",MODE_PRIVATE);
        String prefstring       = pref.getString("extra_params","");

        //Remove -clean..
        String newprefs = prefstring.replaceAll("-clean","");
        SharedPreferences.Editor edit = pref.edit();
        edit.putString("extra_params",newprefs);
        edit.apply();

        //Check if Valid!
        boolean validparams = ParamConfigurer.checkParams(prefstring);

        if(validparams) {
            if (!prefstring.equals("")) {
                StringTokenizer strtok = new StringTokenizer(prefstring, " ");
                while (strtok.hasMoreTokens()) {
                    String param = strtok.nextToken();
                    vars.add(param);
                }
            }
        }else{

            //Notify User service is now running!
            Toast.makeText(this, "[!] Minima EXTRA Params Error.. pls fix", Toast.LENGTH_LONG).show();
        }

        //Start her up!
        minima.mainStarter(vars.toArray(new String[0]));

        //Notify User service is now running!
        Toast.makeText(this, "Minima Service Started", Toast.LENGTH_SHORT).show();

        //Listen to Battery Events
        addBatteryListener();
   }

    public Minima getMinima(){
        return minima;
    }

    public void setTopBlock(){
        try{
            //Get the Tip
            TxPoW tip =  MinimaDB.getDB().getTxPoWTree().getTip().getTxPoW();

            //Set it..
            mTxPowJSON = tip.toJSON();

            //Show a notification
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    //Set status Bar notification
                    setMinimaNotification();
                }
            });

        }catch(Exception exc){
            //MinimaLogger.log(exc);
        }
    }

    private PendingIntent createDynamicPendingIntent(String zUID){

        //Has the MDS even started..
        boolean validmds = false;
        if(Main.getInstance() != null) {
            if (Main.getInstance().getMDSManager() != null) {
                if (Main.getInstance().getMDSManager().hasStarted()) {
                    validmds = true;
                }
            }
        }

        if(!validmds){

            //What to start
            return createDefaultPending();
        }

        //What is the UID
        String uid = zUID;

        if(zUID.equals("0x00") || zUID.equals("0xFF")){

            //Jump to MiniHUB
            uid = Main.getInstance().getMDSManager().getDefaultMiniHUB();
        }

        //Is it the MiniHUb
        PendingIntent pending =  null;
        try{

            //It's a MiniDAPP..
            MDSManager mds = Main.getInstance().getMDSManager();

            //Get the sessionid..
            String sessionid = mds.convertMiniDAPPID(uid);

            //What is the start URL
            String starturl = "https://127.0.0.1:9003/"+uid+"/index.html?uid="+sessionid;

            //Now create the
            Intent NotificationIntent = null;
            if(zUID.equals("0x00") || zUID.equals("0xFF")){
                //Popup the MAIN window
                NotificationIntent = new Intent(getBaseContext(), MiniBrowser.class);
                NotificationIntent.putExtra("ishub",true);
            }else{
                //Open a NEW Window
                NotificationIntent = new Intent(getBaseContext(), NotifyBrowser.class);
                NotificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            }

            //Set the URL
            NotificationIntent.putExtra("url",starturl);

            //And build the pending..
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                pending = PendingIntent.getActivity(getBaseContext(), 0
                        , NotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            }else {
                pending = PendingIntent.getActivity(getBaseContext(), 0
                        , NotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            }

        }catch(Exception exc){
            MinimaLogger.log(exc);

            //What to start
            pending = createDefaultPending();
        }

        return pending;
    }

    private PendingIntent createDefaultPending(){
        //What to start
        PendingIntent pending =  null;
        Intent NotificationIntent = new Intent(getBaseContext(), StartMinimaActivity.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pending = PendingIntent.getActivity(getBaseContext(), 0
                    , NotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }else {
            pending = PendingIntent.getActivity(getBaseContext(), 0
                    , NotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }


        return pending;
    }

    public Notification createNotification(String zText){
        mNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(zText)
                .setContentText("Minima Status Channel")
                .setSilent(true)
                .setSmallIcon(com.minima.android.R.drawable.ic_minima)
                .setContentIntent(createDynamicPendingIntent("0x00"))
                .build();

        return mNotification;
    }

    public void createMiniDAPPNotification(String zMiniDAPPUID, String zTitle, String zText){

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(zTitle)
                .setContentText(zText)
                .setAutoCancel(true)
                .setSmallIcon(com.minima.android.R.drawable.ic_minima)
                .setContentIntent(createDynamicPendingIntent(zMiniDAPPUID))
                .build();

        if(zMiniDAPPUID.equals("0xFF")){
            mNotificationManager.notify(zMiniDAPPUID, 3, notification);
        }else{
            mNotificationManager.notify(zMiniDAPPUID, 2, notification);
        }
    }

    public void cancelNotification(String zMiniDAPPUID){
        mNotificationManager.cancel(zMiniDAPPUID,2);
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

            //Basic message
            startForeground(1, createNotification("Starting up.. please wait.."));

        }else{
            JSONObject header = (JSONObject)mTxPowJSON.get("header");

            String block    = header.getString("block");
            long timemilli  = Long.valueOf(header.getString("timemilli"));
            String date     = MinimaLogger.DATEFORMAT.format(new Date(timemilli));

            //String date     = (String) header.get("date");

            //Set block time message
            startForeground(1, createNotification(block + " @ " + date));
        }
    }

    public static boolean haveStartedShutdown(){
        return mHaveStartedShutdown;
    }

    public static void cancelAlarm(){
        mCancelAlarmOnShutdown = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //Have started shutdown
        mHaveStartedShutdown = true;
        MinimaLogger.log("Minima Service onDestroy start");

        //QUIT nicely..
        try{
            String resp = null;
            MinimaLogger.log("COMPACT DB ON QUIT "+MiniBrowser.mShutDownCompact);
            if(MiniBrowser.mShutDownCompact){
                resp = minima.runMinimaCMD("quit compact:true");
            }else{
                resp = minima.runMinimaCMD("quit");
            }

            //Do we cancel the alarm
            if(mCancelAlarmOnShutdown){
                MinimaLogger.log("SERVICE Cancel Restart Alarm");
                mAlarm.cancelAlarm(this);
            }

        }catch(Exception exc){
            MinimaLogger.log(exc);
        }

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

        //Try and close the MiniBrowser Window
        try{
            if(mNotifyShutdown!=null){

                int counter = 0;
                while(counter<5000 && Main.getInstance() != null){
                    counter += 100;
                    Thread.sleep(100);
                }

                MinimaLogger.log("MiniService : Shutting down last MiniBrowser window.. ");
                mNotifyShutdown.serviceHasShutDown();
            }
        }catch(Exception exc){
            MinimaLogger.log(exc);
        }

        //NULL the main Instance..
        Main.ClearMainInstance();

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

        //Seen an error here
        try{
            registerReceiver(mBatteryReceiver, filter);
        }catch(Exception exc){
            MinimaLogger.log("ERROR adding Battery listener..");
            getMinima().getMain().setLowPowAutoMineSpeed();
        }
    }
}

