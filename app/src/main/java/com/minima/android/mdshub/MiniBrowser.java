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
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
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

    //The ToolBar
    Toolbar mToolBar;

    //Are we hidin the bar..
    boolean mHidingBar = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_browser);

        mToolBar = findViewById(R.id.minidapp_toolbar);
        setSupportActionBar(mToolBar);

        setTitle("Minima Browser");

        //Get the Base URL
        mBaseURL = getIntent().getStringExtra("url");
        //MinimaLogger.log("BASE URL : "+mBaseURL);

        //Get the WebView
        mWebView = (WebView) findViewById(R.id.mds_webview);

        mWebView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View view, int scrollX, int scrolly, int oldScrollX, int oldScrollY) {
                //Show toolbar on scroll
                if(scrolly<oldScrollY){
                    showToolbar();
                }
            }
        });

        WebSettings settings = mWebView.getSettings();

        settings.setUserAgentString("Minima Browser v2.0");
        settings.setDefaultTextEncodingName("utf-8");
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

        //Listen for specific download bloc requests
        mWebView.addJavascriptInterface(new MiniBrowserBlobDownload(this),"Android");

        //Set the Clients..
        mWebView.setWebViewClient(new MiniWebViewClient(this));

        mChromeClient = new MiniChromViewClient(this);
        mWebView.setWebChromeClient(mChromeClient);

        //Set a Download Listener..
        mWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String useragent, String contentdisposition, String mimetype, long contentlength) {

//                MinimaLogger.log("DURL:"+url+"**");
//                MinimaLogger.log("DUSERAGENT:"+useragent+"**");
//                MinimaLogger.log("DCONTENT:"+contentdisposition+"**");
//                MinimaLogger.log("DMIME:"+mimetype+"**");
//                MinimaLogger.log("DLEN:"+contentlength+"**");

                try{
                    String filename = URLUtil.guessFileName( url, contentdisposition, mimetype);

                    //Can't do blobs - USE JavaScriptInterface instead
                    if(url.startsWith("blob")){
                        Toast.makeText(getApplicationContext(), "Cannot download Blobs..", Toast.LENGTH_LONG).show();
                        return;
                    }

                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

                    request.setMimeType(mimetype);
                    String cookies = CookieManager.getInstance().getCookie(url);
                    request.addRequestHeader("cookie", cookies);
                    request.addRequestHeader("User-Agent", useragent);
                    request.setDescription("Downloading File...");

                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    dm.enqueue(request);

                    Toast.makeText(getApplicationContext(), "Downloading File", Toast.LENGTH_LONG).show();
                }catch(Exception exc){
                    MinimaLogger.log(exc);
                }
            }
        });

        //And load the page
        mWebView.loadUrl(mBaseURL);

        //Get Files Permission
        String[] perms = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        checkPermission(perms,99);

        hideToolBar();
    }

    public void showToolbar(){

        //Only if hidden
        if(!getSupportActionBar().isShowing() && !mHidingBar) {
            getSupportActionBar().show();

            //And hide it after a delay
            hideToolBar();
        }
    }

    public void hideToolBar(){

        //We are hiding
        mHidingBar = true;

        //Hide toolbar after a few secs..
        Runnable hider = new Runnable() {
            @Override
            public void run() {

                //Wait 5 secs
                try {Thread.sleep(5000);} catch (InterruptedException e) {}

                //Now hide it..
                MiniBrowser.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mHidingBar = false;
                        MiniBrowser.this.getSupportActionBar().hide();
                    }
                });
            }
        };
        Thread tt = new Thread(hider);
        tt.start();
    }

    public Toolbar getToolBar(){
        return mToolBar;
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
            //Show the toolbar
            showToolbar();

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