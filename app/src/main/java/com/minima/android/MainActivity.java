package com.minima.android;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.minima.android.databinding.ActivityMainBinding;
import com.minima.android.service.MinimaService;
import com.minima.android.ui.maxima.MyDetailsActivity;
import com.minima.android.ui.mds.MDSFragment;

import org.minima.Minima;
import org.minima.system.Main;
import org.minima.system.network.maxima.MaximaManager;
import org.minima.system.params.GeneralParams;
import org.minima.utils.MiniFile;
import org.minima.utils.MinimaLogger;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity  implements ServiceConnection {

    MinimaService mMinima;

    public MDSFragment mMDSFragment = null;

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
                R.id.nav_home, R.id.nav_mds, R.id.nav_maxima)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        //Start the Minima Service..
        Intent minimaintent = new Intent(getBaseContext(), MinimaService.class);
        startForegroundService(minimaintent);
        bindService(minimaintent, this, Context.BIND_AUTO_CREATE);

//        File rootfile = getFilesDir();
//        File[] files = rootfile.listFiles();
//        MinimaLogger.log(getFilesDir().getAbsolutePath());
//        MinimaLogger.log(GeneralParams.DATA_FOLDER);
//        MinimaLogger.log(Arrays.toString(files));

//        requestBatteryCheck(false);
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

    public Minima getMinima(){
        if(mMinima == null){
            return null;
        }

        return mMinima.getMinima();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        MinimaLogger.log("CONNECTED TO SERVICE");
        MinimaService.MyBinder binder = (MinimaService.MyBinder)iBinder;
        mMinima = binder.getService();
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
                .setIcon(android.R.drawable.ic_dialog_info)
                .setNegativeButton(android.R.string.no, null)
                .show();

    }

    /**
     * Show a messgae requesting access to battery settings
     */
    public void requestBatteryCheck(boolean zForce){
        String packageName = getPackageName();
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        boolean ignoring = pm.isIgnoringBatteryOptimizations(packageName);
        MinimaLogger.log("Battery Is Ignored : "+ignoring);
        if (!ignoring || zForce) {
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
        }
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
//        String packageName = getPackageName();
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        //if (pm.isIgnoringBatteryOptimizations(packageName))
        intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
//        else {
//            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
//            intent.setData(Uri.parse("package:" + packageName));
//        }

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

        MinimaLogger.log("FILE OPEN RESULTCODE "+resultCode);
        if(data == null){
            return;
        }

        //Get the file URI
        final Uri fileuri = data.getData();
        MinimaLogger.log("FILE CHOSEN : "+data.getDataString());

        Runnable installer = new Runnable() {
                @Override
                public void run() {
                    //Get the Input Stream..
                    try {
                        InputStream is = getContentResolver().openInputStream(fileuri);
                        byte[] data = Utils.loadFile(is);

                        //Get a file..
                        File dapp = new File(getFilesDir(),"dapp.zip");
                        if(dapp.exists()){
                            dapp.delete();
                        }

                        //Now save to da file..
                        MiniFile.writeDataToFile(dapp,data);

                        //Now load that..
                        String result = mMinima.getMinima().runMinimaCMD("mds action:install file:\""+dapp.getAbsolutePath()+"\"",false);
                        MinimaLogger.log(result);

                        if(mMDSFragment != null){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mMDSFragment.updateMDSList();
                                }
                            });
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

            Thread inst = new Thread(installer);
            inst.start();
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
        if(requestCode == 42){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Access granted Open the File manager..
                openFile();
            }else{
                Toast.makeText(MainActivity.this, "File Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void openFile() {

        //Ask for permission
        if(!checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, 42)){
            return;
        }

        String mimeType = "application/*";

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mimeType);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // special intent for Samsung file manager
        Intent sIntent = new Intent("com.sec.android.app.myfiles.PICK_DATA");
        // if you want any file type, you can skip next line
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
            startActivityForResult(chooserIntent, 99);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getApplicationContext(), "No suitable File Manager was found.", Toast.LENGTH_SHORT).show();
        }
    }
}