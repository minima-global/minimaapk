package com.minima.android.ui.store;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.minima.android.MainActivity;
import com.minima.android.R;

import org.minima.Minima;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;
import org.minima.utils.json.parser.ParseException;

import java.io.File;
import java.sql.Array;
import java.util.ArrayList;

public class StoreBrowser extends AppCompatActivity {

    ListView mDAppList;

    JSONObject mStoreJSON;

    JSONObject[] mAllDapps;

    String mFileDownload;

    String mStoreURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_storebrowser);

        Toolbar tb  = findViewById(R.id.storebrowser_toolbar);
        setSupportActionBar(tb);

        //What is the URL for this store..
        mStoreURL = getIntent().getStringExtra("store_url");

        //Get the complete stroe..
        String storestr = getIntent().getStringExtra("store");

        //Convert to JSON
        try {
            mStoreJSON = (JSONObject) new JSONParser().parse(storestr);
        } catch (ParseException e) {
            MinimaLogger.log("Could not parse store : "+storestr+" "+e);
            finish();
            return;
        }

        //Set the Title
        tb.setTitle(mStoreJSON.getString("name"));

        //Get the List
        mDAppList = findViewById(R.id.storebrowser_list);
        mDAppList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int zPosition, long l) {

                //Get the JSON
                JSONObject dapp = mAllDapps[zPosition];

                new AlertDialog.Builder(StoreBrowser.this)
                        .setTitle("Store DAPP")
                        .setMessage("Would you like to download this dapp ?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //Get the Selected MiniDAPP
                                JSONObject dapp = mAllDapps[zPosition];
                                downloadFile(dapp.getString("file"));
                            }})
                        .setNegativeButton(android.R.string.no, null).show();
            }
        });

        loadDAPPS();
    }

    public void loadDAPPS(){

        //Get the DAPPs
        JSONArray dapps = (JSONArray) mStoreJSON.get("dapps");

        //Now cycle through and add them to the
        ArrayList<JSONObject> alldapps = new ArrayList<>();
        for(Object dappobj : dapps) {
            //Get the DAPP
            alldapps.add((JSONObject) dappobj);
        }

        //Now convert to an array..
        mAllDapps = alldapps.toArray(new JSONObject[0]);

        StoreAdapter stadap = new StoreAdapter(StoreBrowser.this, R.layout.mds_view, mAllDapps);
        mDAppList.setAdapter(stadap);
    }

    public void downloadFile(String zFile){
        mFileDownload = zFile;

        //Check Permission
        if(!checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,88)){
            MinimaLogger.log("Missing Write Permissions.. asking..");
            return;
        }

        //Get the file name
        MinimaLogger.log("Attempt download.. "+zFile);

        Uri uri = Uri.parse(zFile);
        File file   = new File(uri.getPath());
        String name = file.getName();
        try{
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle(name);
            request.setDescription("Downloading");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setVisibleInDownloadsUi(true);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);

            //And Download..
            DownloadManager downloadmanager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            downloadmanager.enqueue(request);

        }catch (Exception exc){
            MinimaLogger.log(exc);
            Toast.makeText(StoreBrowser.this, "Invalid file..", Toast.LENGTH_SHORT).show();
        }
    }

    // Function to check and request permission
    public boolean checkPermission(String permission, int requestCode){
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(StoreBrowser.this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(StoreBrowser.this, new String[] { permission }, requestCode);
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
            downloadFile(mFileDownload);
        }else{
            Toast.makeText(StoreBrowser.this, "File Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.storebrowser, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_store_share:

                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, mStoreURL);
                sendIntent.setType("text/plain");

                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
