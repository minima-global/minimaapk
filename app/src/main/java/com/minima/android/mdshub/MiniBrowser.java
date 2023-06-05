package com.minima.android.mdshub;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.minima.android.R;

import org.minima.utils.MinimaLogger;

import java.util.Arrays;

public class MiniBrowser extends AppCompatActivity {

    //Have we asked if the SSL is ok
    boolean mHaveCheckedSSL;

    //Main WebView
    WebView mWebView;

    //The BASE URL
    String mBaseURL;

    //File open Ops..
    ValueCallback<Uri[]> mFileCheckPath;

    //The ChromeView Client
    MiniChromViewClient mChromeClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_browser);

        Toolbar mToolBar = findViewById(R.id.minidapp_toolbar);
        setSupportActionBar(mToolBar);

        setTitle("Minima Browser");

        //Get the Base URL
        mBaseURL = getIntent().getStringExtra("url");
        //MinimaLogger.log("BASE URL : "+mBaseURL);

        //Get the WebView
        mWebView = (WebView) findViewById(R.id.mds_webview);

        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setLoadWithOverviewMode(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowContentAccess(true);
        //settings.setBuiltInZoomControls(true);
        settings.setSupportMultipleWindows(true);
        settings.setUserAgentString("Minima Browser v2.0");

        //Set the Clients..
        mWebView.setWebViewClient(new MiniWebViewClient(this));

        mChromeClient = new MiniChromViewClient(this);
        mWebView.setWebChromeClient(mChromeClient);

        //Set a Download Listener..
        mWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String useragent, String contentdisposition, String mimetype, long contentlength) {

                String newurl  = "https://www.spartacusrex.com/images/spincube_promo.jpg";
                String newmime = "image/*";

                MinimaLogger.log("DURL:"+url+"**");
                MinimaLogger.log("DUSERAGENT:"+useragent+"**");
                MinimaLogger.log("DCONTENT:"+contentdisposition+"**");
                MinimaLogger.log("DMIME:"+mimetype+"**");
                MinimaLogger.log("DLEN:"+contentlength+"**");

                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(newurl));

                request.setMimeType(newmime);
                String cookies = CookieManager.getInstance().getCookie(newurl);
                request.addRequestHeader("cookie", cookies);
                request.addRequestHeader("User-Agent", useragent);
                request.setDescription("Downloading File...");

                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Filename");
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(request);

                Toast.makeText(getApplicationContext(), "Downloading File", //To notify the Client that the file is being downloaded
                        Toast.LENGTH_LONG).show();
            }
        });

        //And load the page
        mWebView.loadUrl(mBaseURL);

        //Get Files Permission
        String[] perms = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        checkPermission(perms,99);
    }

    // Function to check and request permission
    public void checkPermission(String[] permissions, int requestCode){
        // Checking if permission is not granted
        ActivityCompat.requestPermissions(this, permissions , requestCode);

//        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[] { permission }, requestCode);
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.minibrowser, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_mdsrefresh:
                //And load the page
                mWebView.loadUrl(mBaseURL);
                return true;

            case R.id.action_mdsexit:
                finish();
                return true;

            case R.id.action_mdsconsole:

                Intent console = new Intent(this, ConsoleActivity.class);
                console.putExtra("consoletext",mChromeClient.getConsoleMessages());
                startActivity(console);

                return true;

            case R.id.action_mdsopen:

                Intent browser = new Intent(Intent.ACTION_VIEW);
                browser.setData(Uri.parse(mBaseURL));
                startActivity(browser);

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if(mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            mWebView.loadUrl("");
            super.onBackPressed();
        }
    }

    public void openFile(String [] zMimeTypes, ValueCallback<Uri[]> zFilePathCallback) {

        //MinimaLogger.log("OPEN FILE!");

        //Store for later
        mFileCheckPath = zFilePathCallback;

        //Open a file
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
//        if(zMimeTypes.length>0) {
//            MinimaLogger.log("Set Mime Types : "+ Arrays.toString(zMimeTypes)+" "+zMimeTypes.length);
//            intent.putExtra(Intent.EXTRA_MIME_TYPES, zMimeTypes);
//        }
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // special intent for Samsung file manager
        Intent sIntent = new Intent("com.sec.android.app.myfiles.PICK_DATA");
        sIntent.putExtra("CONTENT_TYPE", "*/*");
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
            startActivityForResult(chooserIntent, 98);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getApplicationContext(), "No suitable File Manager was found.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult (int requestCode,
                                     int resultCode,
                                     Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_CANCELED){
            mFileCheckPath.onReceiveValue(null);
        }else if(resultCode == RESULT_OK){
            mFileCheckPath.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
        }

        //Was anything returned
        if(data == null){
            return;
        }

        MinimaLogger.log("OPENFILE : "+data.getData().toString());
    }
}
