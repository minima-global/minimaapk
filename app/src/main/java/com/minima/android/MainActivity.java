package com.minima.android;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.minima.android.databinding.ActivityMainBinding;
import com.minima.android.files.InstallAssetMiniDAPP;
import com.minima.android.files.InstallMiniDAPP;
import com.minima.android.files.RestoreBackup;
import com.minima.android.service.MinimaService;
import com.minima.android.ui.home.HomeFragment;
import com.minima.android.ui.maxima.MaximaFragment;
import com.minima.android.ui.maxima.MyDetailsActivity;
import com.minima.android.ui.mds.MDSFragment;

import org.minima.Minima;
import org.minima.system.Main;
import org.minima.system.network.maxima.MaximaManager;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;

public class MainActivity extends AppCompatActivity  implements ServiceConnection {

    /**
     * Open File operations
     */
    public static int REQUEST_INSTALLMINI   = 42;
    public static int REQUEST_RESTORE       = 43;

    /**
     * Main Minmia Service
     */
    MinimaService mMinima;

    /**
     * Current Battery Status
     */
    int mBatteryStaus = -1;

    /**
     * Update this when mindapp installed
     */
    public MDSFragment mMDSFragment         = null;
    public HomeFragment mHomeFragment       = null;
    public MaximaFragment mMaximaFragment   = null;

    //Loader while connecting to Minima
    ProgressDialog mLoader = null;

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_mds, R.id.nav_maxima, R.id.nav_backup, R.id.nav_help)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        changeNavigationIcons();
        NavigationUI.setupWithNavController(navigationView, navController);

        //Start the Minima Service..
        Intent minimaintent = new Intent(getBaseContext(), MinimaService.class);
        startForegroundService(minimaintent);
        bindService(minimaintent, this, Context.BIND_AUTO_CREATE);

        //Wait for Minima to fully start up..
        mLoader = new ProgressDialog(this);
        mLoader.setTitle("Connecting to Minima");
        mLoader.setMessage("Please wait..");
        mLoader.setCanceledOnTouchOutside(false);
        mLoader.show();
    }

    private void changeNavigationIcons() {
        // Menu button
        Drawable existingNavigationIcon = binding.appBarMain.toolbar.getNavigationIcon();
        if (existingNavigationIcon != null) {
            existingNavigationIcon = existingNavigationIcon.mutate();
            existingNavigationIcon.setTint(Color.RED);
        }

        binding.appBarMain.toolbar.setNavigationIcon(existingNavigationIcon);

        // Overflow button
        Drawable overflowButton = binding.appBarMain.toolbar.getOverflowIcon();
        if (overflowButton != null) {
            overflowButton.setColorFilter(getColor(R.color.coreBlackContrast), PorterDuff.Mode.SRC_ATOP);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_status:
//                shutdown();
                runStatus();
                return true;

            case R.id.action_maxima_identity:
                //Wait for Maxima..
                if(Main.getInstance() == null){
                    //Not ready yet..
                    Toast.makeText(this,"Maxima not initialised yet..",Toast.LENGTH_SHORT).show();
                    return true;
                }

                MaximaManager max = Main.getInstance().getMaxima();
                if(max == null || !max.isInited()) {
                    //Not ready yet..
                    Toast.makeText(this,"Maxima not initialised yet..",Toast.LENGTH_SHORT).show();
                    return true;
                }

                //Show your details
                Intent intent = new Intent(MainActivity.this, MyDetailsActivity.class);
                startActivity(intent);

                return true;

            case R.id.action_battery:
                openBatteryOptimisation();
                return true;

//            case R.id.action_requestbattery:
//                requestBatteryCheck(true);
//                return true;
//
//            case R.id.action_shutdown:
//
////                if(mMinima != null){
////                    mMinima.shutdownComplete(this);
////                }
////
//                //unbindService(this);
//
//                Intent minimaintent = new Intent(getBaseContext(), MinimaService.class);
//                stopService(minimaintent);
//
//                finish();
//
//                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void shutdown(){

        Runnable close = new Runnable() {
            @Override
            public void run() {
                if(mMinima != null){
                    String res = mMinima.getMinima().runMinimaCMD("quit");
                    MinimaLogger.log(res);
                }

                //Stop the service
                Intent minimaintent = new Intent(getBaseContext(), MinimaService.class);
                stopService(minimaintent);

                //Shut this down..
                finish();
            }
        };

        Thread closer = new Thread(close);
        closer.start();
    }

    public Minima getMinima(){
        if(mMinima == null){
            return null;
        }

        return mMinima.getMinima();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        MinimaLogger.log("CONNECTED TO SERVICE");
        MinimaService.MyBinder binder = (MinimaService.MyBinder)iBinder;
        mMinima = binder.getService();

        waitForMinimaToStartUp();
    }

    public void waitForMinimaToStartUp(){
        Runnable checker = new Runnable() {
            @Override
            public void run() {
                try{
                    //Wait for Maxima..
                    MaximaManager max = Main.getInstance().getMaxima();
                    while(max == null || !max.isInited()) {
                        Thread.sleep(1000);
                        max = Main.getInstance().getMaxima();
                    }

                    //Run Status..
                    String status = mMinima.getMinima().runMinimaCMD("status",false);

                    //Make a JSON
                    JSONObject json = (JSONObject) new JSONParser().parse(status);

                    //Get the status..
                    while(!(boolean)json.get("status")){
                        Thread.sleep(1000);

                        //Run Status..
                        status = mMinima.getMinima().runMinimaCMD("status");

                        //Make a JSON
                        json = (JSONObject) new JSONParser().parse(status);

                        MinimaLogger.log("Waiting for Status .. "+json.toString());
                    }

                    //Install the MiniDApps..
                    installMiniDAPPs();

                    //OK - Status returned OK..
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Hide the Loader
                            if(mLoader != null){
                                mLoader.cancel();
                            }

                            //Update fragments
                            if(mMDSFragment != null){
                                mMDSFragment.updateMDSList();
                            }

                            if(mHomeFragment != null){
                                mHomeFragment.updateUI();
                            }

                            if(mMaximaFragment != null){
                                mMaximaFragment.updateUI();
                            }

                            //And check for Battery..
                            requestBatteryCheck();
                        }
                    });

                }catch(Exception exc) {
                    MinimaLogger.log(exc);
                }
            }
        };

        Thread tt = new Thread(checker);
        tt.start();
    }

    public void installMiniDAPPs(){
        //Do we need to install apps..
        SharedPreferences pref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        if(!pref.getBoolean("minidapps_installed",false)){

            //Install them..
            new InstallAssetMiniDAPP("block-0.1.5.mds.zip", MainActivity.this).run();
            new InstallAssetMiniDAPP("wallet-0.1.5.mds.zip", MainActivity.this).run();
            new InstallAssetMiniDAPP("terminal-1.91.mds.zip", MainActivity.this).run();
            new InstallAssetMiniDAPP("maxsolo-1.81.mds.zip", MainActivity.this).run();
            new InstallAssetMiniDAPP("helpdocs-0.1.1.mds.zip", MainActivity.this).run();
//            new InstallAssetMiniDAPP("incentive-1.1.mds.zip", MainActivity.this).run();
            new InstallAssetMiniDAPP("scriptide-1.7.mds.zip", MainActivity.this).run();
//            new InstallAssetMiniDAPP("2048-3.mds.zip", MainActivity.this).run();

            //And that's that
            SharedPreferences.Editor edit = pref.edit();
            edit.putBoolean("minidapps_installed", true);
            edit.apply();
        }
    }

//    public void addBatteryListener(){
//        //Listen for Battery Events..
//        BroadcastReceiver receiver = new BroadcastReceiver() {
//            public void onReceive(Context context, Intent intent) {
//                int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
//
//                //Is this a new setting
//                if(mBatteryStaus == plugged) {
//                    //No change..
//                    return;
//                }
//                mBatteryStaus = plugged;
//
//                //What Happened..
//                if (plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB) {
//                    // on AC power
//                    MinimaLogger.log("BATTERY PLUGGED IN");
//
//                    //Set PoW to regular
//                    if(mMinima != null){
//                        mMinima.getMinima().getMain().setNormalAutoMineSpeed();
//                    }
//
//                } else if (plugged == 0) {
//                    // on battery power
//                    MinimaLogger.log("BATTERY NOT PLUGGED IN");
//
//                    //Set PoW to regular
//                    if(mMinima != null){
//                        mMinima.getMinima().getMain().setLowPowAutoMineSpeed();
//                    }
//
//                } else {
//                    // intent didnt include extra info
//                    MinimaLogger.log("BATTERY NO EXTRA INFO");
//                }
//            }
//        };
//        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
//        registerReceiver(receiver, filter);
//    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        MinimaLogger.log("DISCONNECTED TO SERVICE");
        mMinima = null;
    }

    public void runStatus(){

        if(mMinima == null){
            Toast.makeText(this,"Not connected yet",Toast.LENGTH_SHORT).show();
            return;
        }

        //Run Status Command
        String status = mMinima.getMinima().runMinimaCMD("status");

        new AlertDialog.Builder(this)
                .setTitle("Minima Status")
                .setMessage(status)
                .setIcon(R.drawable.outline_info_24)
                .setNegativeButton(android.R.string.no, null)
                .show();

    }

    /**
     * Show a messgae requesting access to battery settings
     */
    public boolean requestBatteryCheck(){
        String packageName = getPackageName();
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        boolean ignoring = pm.isIgnoringBatteryOptimizations(packageName);
        MinimaLogger.log("Battery Is Ignored : "+ignoring);
        if (!ignoring) {
            new AlertDialog.Builder(this)
                    .setTitle("Battery Optimise")
                    .setMessage("Minima needs to run in the background to validate and secure your coins.\n\n" +
                            "You can see this setting in your options menu in the top right.")
                    // Specifying a listener allows you to take an action before dismissing the dialog.
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Continue with delete operation
                            checkBatteryOptimisation();
                        }
                    })
                    // A null listener allows the button to dismiss the dialog and take no further action.
//                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();

            return true;
        }

        return false;
    }

    public void checkBatteryOptimisation(){
        Intent intent = new Intent();
        String packageName = getPackageName();
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(packageName) || true){
            MinimaLogger.log("Battery Optimise : "+packageName);
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
        }
    }

    public void openBatteryOptimisation(){
        Intent intent = new Intent();
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Unbind from the service..
        if(mMinima != null){
            unbindService(this);
        }
    }

    @Override
    protected void onActivityResult (int requestCode,
                                     int resultCode,
                                     Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Was anything returned
        if(data == null){
            return;
        }

        //Get the file URI
        Uri fileuri = data.getData();
        MinimaLogger.log("FILE CHOSEN : "+data.getDataString());

        if(requestCode == REQUEST_INSTALLMINI){
            //Create an Installer Handler
            InstallMiniDAPP install = new InstallMiniDAPP(fileuri,this);

            Thread inst = new Thread(install);
            inst.start();
        }else if(requestCode == REQUEST_RESTORE){
            //Create an Installer Handler
            RestoreBackup restore = new RestoreBackup(fileuri,this);

            Thread inst = new Thread(restore);
            inst.start();
        }
    }

    // Function to check and request permission
    public boolean checkPermission(String permission, int requestCode){
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { permission }, requestCode);
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //Was this from our MDS open File..
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //Access granted Open the File manager..
            openFile(requestCode);
        }else{
            Toast.makeText(MainActivity.this, "File Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }

    public void openFile(int zRequest) {

        //Are we connected..
        if(mMinima == null){
            Toast.makeText(MainActivity.this, "Minima not initialised yet..", Toast.LENGTH_SHORT).show();
            return;
        }

        //Check for permission
        if(!checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, zRequest)){
            return;
        }

        //The type of file we are looking for
        String mimeType = "application/*";

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mimeType);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // special intent for Samsung file manager
        Intent sIntent = new Intent("com.sec.android.app.myfiles.PICK_DATA");
        sIntent.putExtra("CONTENT_TYPE", mimeType);
        sIntent.addCategory(Intent.CATEGORY_DEFAULT);

        Intent chooserIntent;
        if (getPackageManager().resolveActivity(sIntent, 0) != null){
            // it is device with Samsung file manager
            chooserIntent = Intent.createChooser(sIntent, "Open file");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { intent});
        } else {
            chooserIntent = Intent.createChooser(intent, "Open file");
        }

        try {
            startActivityForResult(chooserIntent, zRequest);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getApplicationContext(), "No suitable File Manager was found.", Toast.LENGTH_SHORT).show();
        }
    }
}