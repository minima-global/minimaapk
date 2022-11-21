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
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.minima.android.databinding.ActivityMainBinding;
import com.minima.android.files.InstallAssetMiniDAPP;
import com.minima.android.files.InstallMiniDAPP;
import com.minima.android.files.RestoreBackup;
import com.minima.android.files.UpdateMiniDAPP;
import com.minima.android.service.MinimaService;
import com.minima.android.ui.home.HomeFragment;
import com.minima.android.ui.maxima.MaximaFragment;
import com.minima.android.ui.maxima.MyDetailsActivity;
import com.minima.android.ui.mds.MDSFragment;

import org.minima.Minima;
import org.minima.system.Main;
import org.minima.system.network.maxima.MaximaManager;
import org.minima.system.params.GlobalParams;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;

public class MainActivity extends AppCompatActivity  implements ServiceConnection {

    /**
     * Open File operations
     */
    public static int REQUEST_INSTALLMINI   = 42;
    public static int REQUEST_RESTORE       = 43;
    public static int REQUEST_UPDATEMINI    = 44;

    /**
     * The MiniDAPP we are trying to update
     */
    String mExtraFileData = "";

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

    /**
     * The DAPP Stores..
     */
    JSONObject[] mAllDappStores = null;

    //Loader while connecting to Minima
    ProgressDialog mLoader = null;

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    static MainActivity mStaticLink = null;
    public static MainActivity getMainActivity(){
        return mStaticLink;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStaticLink = this;

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_mds, R.id.nav_maxima, R.id.nav_store, R.id.nav_backup, R.id.nav_help)
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
        MinimaLogger.log("Show initial Loader..");
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
            case R.id.action_shutdown:


                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Shutdown");
                builder.setMessage("Are you sure you want to shutdown Minima ?");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        shutdown();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();

                return true;

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

    public JSONObject[] getDappStores(){
        return mAllDappStores;
    }

    public void setDappStores(JSONObject[] zStores){
        mAllDappStores = zStores;
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
        MinimaLogger.log("MAINACTIVITY - waiting for Minima to StartUp..");

        Runnable checker = new Runnable() {
            @Override
            public void run() {
                try{
//                    //Wait for Maxima..
//                    MaximaManager max = Main.getInstance().getMaxima();
//                    while(max == null || !max.isInited()) {
//                        Thread.sleep(1000);
//                        max = Main.getInstance().getMaxima();
//                    }

                    //Wait a second..
                    Thread.sleep(2000);

                    //Run Status..
                    String status = mMinima.getMinima().runMinimaCMD("status",false);

                    //Make a JSON
                    JSONObject json = (JSONObject) new JSONParser().parse(status);

                    //Get the status..
                    while(!(boolean)json.get("status")){
                        Thread.sleep(2000);

                        //Run Status..
                        status = mMinima.getMinima().runMinimaCMD("status");

                        //Make a JSON
                        json = (JSONObject) new JSONParser().parse(status);

                        MinimaLogger.log("Waiting for Status .. "+json.toString());
                    }

                    //Install the MiniDApps..
                    MinimaLogger.log("Install MiniDAPPs");
                    installMiniDAPPs();

                    //OK - Status returned OK..
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Hide the Loader
                            try{
                                MinimaLogger.log("Remove Loader");
                                if(mLoader != null && mLoader.isShowing()){
                                    mLoader.cancel();
                                }
                            }catch(Exception exc){
                                MinimaLogger.log(exc);
                            }

                            //Update fragments
                            try{
                                if(mMDSFragment != null){
                                    mMDSFragment.updateMDSList();
                                }
                            }catch(Exception exc){
                                MinimaLogger.log(exc);
                            }

                            try{
                                if(mHomeFragment != null){
                                    mHomeFragment.updateUI();
                                }
                            }catch(Exception exc){
                                MinimaLogger.log(exc);
                            }

                            try{
                                if(mMaximaFragment != null){
                                    mMaximaFragment.updateUI();
                                }
                            }catch(Exception exc){
                                MinimaLogger.log(exc);
                            }

                            //And check for Battery..
                            requestBatteryCheck();
                        }
                    });

                }catch(Exception exc) {
                    MinimaLogger.log(exc);
                }

                MinimaLogger.log("MAINACTIVITY - Minima StartUp complete..");
            }
        };

        Thread tt = new Thread(checker);
        tt.start();
    }

    public void installMiniDAPPs(){
        //Which setting is it..
        String minidapppref = "minidapps_installed_"+ GlobalParams.MINIMA_BASE_VERSION;

        //Do we need to install apps..
        SharedPreferences pref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        if(!pref.getBoolean(minidapppref,false)){

            //Install them..
            new InstallAssetMiniDAPP("block-0.1.5.mds.zip", MainActivity.this).run();
            new InstallAssetMiniDAPP("docs_1.1.3.mds.zip", MainActivity.this).run();
            new InstallAssetMiniDAPP("futurecash_1.3.4.mds.zip", MainActivity.this).run();
            new InstallAssetMiniDAPP("gimme20_1.6.mds.zip", MainActivity.this).run();
            new InstallAssetMiniDAPP("ic_1.3.11.mds.zip", MainActivity.this).run();
            new InstallAssetMiniDAPP("maxsolo_2.0.19.mds.zip", MainActivity.this).run();
            new InstallAssetMiniDAPP("news-2.0.mds.zip", MainActivity.this).run();
            new InstallAssetMiniDAPP("scriptide-1.71.mds.zip", MainActivity.this).run();
            new InstallAssetMiniDAPP("terminal-2.03.mds.zip", MainActivity.this).run();
            new InstallAssetMiniDAPP("wallet_1.13.9.mds.zip", MainActivity.this).run();

            //And that's that
            SharedPreferences.Editor edit = pref.edit();
            edit.putBoolean(minidapppref, true);
            edit.apply();
        }
    }

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
                .setNegativeButton("Close", null)
                .show();
    }

    /**
     * Show a messgae requesting access to battery settings
     */
    public boolean requestBatteryCheck(){
        String packageName = getPackageName();
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        boolean ignoring = pm.isIgnoringBatteryOptimizations(packageName);
        MinimaLogger.log("Battery Optimisation is ignored : "+ignoring);
        if (!ignoring) {
            try{
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
            }catch (Exception exc){
                MinimaLogger.log(exc);
            }

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

        MinimaLogger.log("MAINACTIVITY - ONDESTROY");

        if(mLoader != null && mLoader.isShowing()){
            mLoader.cancel();
        }

        //Unbind from the service..
        if(mMinima != null){
            unbindService(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        MinimaLogger.log("MAINACTIVITY - ONRESUME");
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
        //MinimaLogger.log("FILE CHOSEN : "+data.getDataString());

        if(requestCode == REQUEST_INSTALLMINI){
            //Create an Installer Handler
            InstallMiniDAPP install = new InstallMiniDAPP(fileuri,this);

            Thread inst = new Thread(install);
            inst.start();

        }else if(requestCode == REQUEST_UPDATEMINI){
            //Create an Update Handler
            UpdateMiniDAPP upd = new UpdateMiniDAPP(mExtraFileData, fileuri, this);

            Thread inst = new Thread(upd);
            inst.start();

        }else if(requestCode == REQUEST_RESTORE){
            //Create an Installer Handler
            RestoreBackup restore = new RestoreBackup(fileuri,mExtraFileData,this);

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
        openFile("",zRequest);
    }

    public void openFile(String zExtraData, int zRequest) {

        //Are we connected..
        if(mMinima == null){
            Toast.makeText(MainActivity.this, "Minima not initialised yet..", Toast.LENGTH_SHORT).show();
            return;
        }

        //Store for later
        mExtraFileData = zExtraData;

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