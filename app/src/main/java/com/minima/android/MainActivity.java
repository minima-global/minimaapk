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
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
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
import com.minima.android.dynamite.OnboardingOne;
import com.minima.android.files.CopyFile;
import com.minima.android.files.InstallAssetMiniDAPP;
import com.minima.android.files.InstallMiniDAPP;
import com.minima.android.files.RestoreBackup;
import com.minima.android.files.UpdateMiniDAPP;
import com.minima.android.service.MinimaService;
import com.minima.android.ui.archive.ArchiveListener;
import com.minima.android.ui.archive.ChainSyncActivity;
import com.minima.android.ui.files.FilesFragment;
import com.minima.android.ui.home.HomeFragment;
import com.minima.android.ui.logs.LogsFragment;
import com.minima.android.ui.maxima.MaximaFragment;
import com.minima.android.ui.maxima.MyDetailsActivity;
import com.minima.android.ui.mds.MDSFragment;
import com.minima.android.ui.vault.VaultFragment;

import org.minima.Minima;
import org.minima.system.Main;
import org.minima.system.network.maxima.MaximaManager;
import org.minima.system.params.GlobalParams;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;
import org.minima.utils.json.parser.ParseException;

public class MainActivity extends AppCompatActivity  implements ServiceConnection, ArchiveListener {

    /**
     * Open File operations
     */
    public static int REQUEST_INSTALLMINI   = 42;
    public static int REQUEST_RESTORE       = 43;
    public static int REQUEST_UPDATEMINI    = 44;

    public static int REQUEST_WRITEPERMISSIONS          = 45;
    public static int REQUEST_COPYFILE_TO_INTERNAL      = 46;

    /**
     * The MiniDAPP we are trying to update
     */
    String mExtraFileData   = "";

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
    public VaultFragment mVaultFragment     = null;
    public FilesFragment mFileFragment      = null;
    public LogsFragment mLogsFragment      = null;

    /**
     * The DAPP Stores..
     */
    JSONObject[] mAllDappStores = null;

    //Loader while connecting to Minima
    ProgressDialog mLoader = null;

    //Loader to show SYNC status
    ProgressDialog mSyncLoader = null;

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
                R.id.nav_home,
                R.id.nav_mds,
                R.id.nav_maxima,
                R.id.nav_store,
                R.id.nav_backup,
                R.id.nav_vault,
                R.id.nav_archive,
                R.id.nav_files,
                R.id.nav_logs,
                R.id.nav_help)
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
        mLoader.setCancelable(false);
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

            case R.id.action_intro:

                Intent introintent = new Intent(this, OnboardingOne.class);
                introintent.putExtra("FROMBOOT", false);
                startActivity(introintent);

                return true;

            case R.id.action_recalcip:

                String recalcip = mMinima.getMinima().runMinimaCMD("network action:recalculateip",false);

                Toast.makeText(this,"NEW Host IP Set",Toast.LENGTH_SHORT).show();

                //Refresh..
                try{
                    if(mHomeFragment != null){
                        mHomeFragment.updateUI();
                    }
                }catch(Exception exc){
                    MinimaLogger.log(exc);
                }

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

            case R.id.action_sharepeers:

                //Run peers..
                String peers = mMinima.getMinima().runMinimaCMD("peers max:20",false);

                //Get just the peers
                String list = "[]";
                try {
                    JSONObject jsonpeers  = (JSONObject) new JSONParser().parse(peers);
                    JSONObject resp       = (JSONObject) jsonpeers.get("response");
                    MinimaLogger.log("response:"+resp.toString());

                    JSONArray  peersarray = (JSONArray) resp.get("peers-list");
                    list = peersarray.toString();

                } catch (ParseException e) {
                    e.printStackTrace();
                    return true;
                }

                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, list);
                startActivity(Intent.createChooser(share, "Share your peers"));

                return true;

            case R.id.action_importpeers:

                AlertDialog.Builder pbuilder = new AlertDialog.Builder(this);
                pbuilder.setTitle("Import Peers");

                // Set up the input
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                pbuilder.setView(input);

                pbuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        //Get the peers list
                        String peers = input.getText().toString().trim();

                        //Import these peers..
                        String addpeer = mMinima.getMinima().runMinimaCMD("peers action:addpeers peerslist:"+peers,false);

                        MinimaLogger.log(addpeer);

                        Toast.makeText(MainActivity.this, "Peers Imported", Toast.LENGTH_SHORT).show();
                    }
                });
                pbuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                pbuilder.show();

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
                        shutdown(true);
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
        shutdown(false);
    }


    boolean mShuttingDown = false;
    public void shutdown(boolean zCompact){

        //Check not called twice..
        if(!mShuttingDown){
            mShuttingDown = true;
        }else{
            return;
        }

        MinimaLogger.log("SHUTDOWN REQUESTED");

        Runnable close = new Runnable() {
            @Override
            public void run() {
                if(mMinima != null){
                    if(zCompact){
                        String res = mMinima.getMinima().runMinimaCMD("quit compact:true");
                        MinimaLogger.log(res);
                    }else{
                        String res = mMinima.getMinima().runMinimaCMD("quit");
                        MinimaLogger.log(res);
                    }
                }

                //Stop the service
                Intent minimaintent = new Intent(getBaseContext(), MinimaService.class);
                stopService(minimaintent);

                MinimaLogger.log("SHUTDOWN FINISHED");

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

    public MinimaService getMinimaService(){
        if(mMinima == null){
            return null;
        }

        return mMinima;
    }

    public String getFullLogs(){
        if(mMinima == null){
            return "Not connected to Service yet..";
        }

        return mMinima.getFullLogs();
    }

    public void setMaximaFragment(MaximaFragment zMaxima){
        mMaximaFragment         = zMaxima;
        mMinima.mContactsFrag   = zMaxima;
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

                    //Are we restoring
                    if(Main.getInstance().isRestoring()){

                        //Are we restoring..
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //Hide the Loader
                                try {
                                    if(mLoader != null && mLoader.isShowing()){
                                        mLoader.cancel();
                                    }

                                    //New Loader
                                    mLoader = new ProgressDialog(MainActivity.this);
                                    mLoader.setTitle("Syncing..");
                                    mLoader.setMessage("Please wait..");
                                    mLoader.setCanceledOnTouchOutside(false);
                                    mLoader.setCancelable(false);
                                    mLoader.show();

                                    //Tell service to send messages here
                                    getMinimaService().mArchiveListener = MainActivity.this;

                                } catch (Exception exc) {
                                    MinimaLogger.log(exc);
                                }
                            }
                        });

                        return;
                    }

                    //Wait for Maxima..
                    MaximaManager max = Main.getInstance().getMaxima();
                    while(max == null || !max.isInited()) {
                        Thread.sleep(2000);
                        max = Main.getInstance().getMaxima();
                        MinimaLogger.log("Waiting for Maxima.. ");
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

                    //Install the MiniDApps..
                    installMiniDAPPs();

                    //OK - Lets update the views..
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Hide the Loader
                            try{
                                if(mLoader != null && mLoader.isShowing()){
                                    mLoader.cancel();
                                }
                            }catch(Exception exc){
                                MinimaLogger.log(exc);
                            }

                            //Update fragments
                            try{
                                MinimaLogger.log("Update MDS");
                                if(mMDSFragment != null){
                                    mMDSFragment.updateMDSList();
                                }
                            }catch(Exception exc){
                                MinimaLogger.log(exc);
                            }

                            try{
                                MinimaLogger.log("Update Home");
                                if(mHomeFragment != null){
                                    mHomeFragment.updateUI();
                                }
                            }catch(Exception exc){
                                MinimaLogger.log(exc);
                            }

                            try{
                                MinimaLogger.log("Update Maxima");
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

            MinimaLogger.log("Installing MiniDAPPs first time");

            //Install them..
            new InstallAssetMiniDAPP("block-0.1.5.mds.zip", MainActivity.this).run();
            new InstallAssetMiniDAPP("chatter-1.0.0.mds.zip", MainActivity.this).run();
            new InstallAssetMiniDAPP("docs_1.1.3.mds.zip", MainActivity.this).run();
            new InstallAssetMiniDAPP("futurecash_1.6.0.mds.zip", MainActivity.this).run();
            new InstallAssetMiniDAPP("maxcontacts-1.3.4.mds.zip", MainActivity.this).run();
            new InstallAssetMiniDAPP("maxsolo-2.3.7.mds.zip", MainActivity.this).run();
            new InstallAssetMiniDAPP("news-2.0.mds.zip", MainActivity.this).run();
            new InstallAssetMiniDAPP("scriptide-2.0.mds.zip", MainActivity.this).run();
            new InstallAssetMiniDAPP("terminal-2.03.mds.zip", MainActivity.this).run();
            new InstallAssetMiniDAPP("wallet-2.17.1.mds.zip", MainActivity.this).run();

            //And that's that
            SharedPreferences.Editor edit = pref.edit();
            edit.putBoolean(minidapppref, true);
            edit.apply();
        }else{
            MinimaLogger.log("MiniDAPPs already installed");
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        MinimaLogger.log("DISCONNECTED TO SERVICE");
        mMinima.mContactsFrag = null;
        mMinima = null;
    }

    public void runStatus(){

        if(mMinima == null){
            Toast.makeText(this,"Not connected yet",Toast.LENGTH_SHORT).show();
            return;
        }

        //Run Status Command
        String status = mMinima.getMinima().runMinimaCMD("status complete:true");

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

        mStaticLink = null;

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

        }else if(requestCode == REQUEST_COPYFILE_TO_INTERNAL){
            CopyFile cf = new CopyFile(fileuri,getFileName(fileuri),this);
            Thread inst = new Thread(cf);
            inst.start();
        }
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int row = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    result = cursor.getString(row);
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
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
            if(requestCode != REQUEST_WRITEPERMISSIONS) {
                //Access granted Open the File manager..
                openFile(requestCode);
            }
        }else{
            Toast.makeText(MainActivity.this, "File Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }

    public void openFile(int zRequest) {
        openFile(mExtraFileData,zRequest);
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

    int updatecounter = -1;
    @Override
    public void updateArchiveStatus(String zStatus) {

        updatecounter++;

        if(updatecounter % 10 == 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mLoader != null && mLoader.isShowing()) {
                        mLoader.setMessage(zStatus);
                    }
                }
            });
        }
    }
}