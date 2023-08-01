package com.minima.android.browser;

import android.Manifest;
import android.app.DownloadManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.LayoutInflater;
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
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.minima.android.R;
import com.minima.android.StartMinimaActivity;
import com.minima.android.files.FilesActivity;
import com.minima.android.service.MinimaService;

import org.minima.objects.base.MiniData;
import org.minima.system.Main;
import org.minima.system.mds.MDSManager;
import org.minima.system.params.ParamConfigurer;
import org.minima.utils.MiniFile;
import org.minima.utils.MinimaLogger;
import org.minima.utils.ssl.SSLManager;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Base64;

public class MiniBrowser extends AppCompatActivity {

    //Have we asked if the SSL is ok
    boolean mHaveCheckedSSL;

    //Main WebView
    protected WebView mWebView;

    //The BASE URL
    String mBaseURL;

    //File open Ops..
    ValueCallback<Uri[]> mFileCheckPath;

    //The ChromeView Client
    MiniChromViewClient mChromeClient;

    //The ToolBar
    Toolbar mToolBar;

    //Is this the MiniHUB
    boolean mIsMiniHUB = false;

    //Are we hidin the bar..
    boolean mHidingBar = false;

    //Are we in shutdown mode..
    public static boolean mShutDownMode = false;

    //Static ref to the SSL Cert
    static private Certificate mMinimaSSLCert = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_browser);

        //Is the Cert NULL
        if(mMinimaSSLCert == null){
            //Get the Cert
            try{
                mMinimaSSLCert = SSLManager.getSSLKeyStore().getCertificate("MINIMA_NODE");
            }catch(Exception exc){
                //Something wrong..
                MinimaLogger.log(exc);
            }
        }

        //Get the Base URL
        mBaseURL = getIntent().getStringExtra("url");

        //Is this the MiniHUB
        mIsMiniHUB = getIntent().getBooleanExtra("ishub",false);

        //Set Our Toolbar
        mToolBar = findViewById(R.id.minidapp_toolbar);
        setSupportActionBar(mToolBar);
        getSupportActionBar().hide();

        //Blank the title
        setTitle("");

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

        //Browser Web Settings..
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
        mWebView.addJavascriptInterface(new MiniBrowserJSInterface(this),"Android");

        //Set the Clients..
        mWebView.setWebViewClient(new MiniWebViewClient(this));

        //Chrome client gives extra things
        mChromeClient = new MiniChromViewClient(this);
        mWebView.setWebChromeClient(mChromeClient);

        //Set a Download Listener..
        mWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String useragent, String contentdisposition, String mimetype, long contentlength) {

                try{
                    String filename = URLUtil.guessFileName( url, contentdisposition, mimetype);

                    //Can't do blobs - USE JavaScriptInterface instead
                    if(url.startsWith("blob")){
                        Toast.makeText(getApplicationContext(), "Cannot download Blobs..", Toast.LENGTH_LONG).show();
                        return;
                    }

                    //Get the Cookies
                    String cookies = CookieManager.getInstance().getCookie(url);

                    //Create a Download request
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setMimeType(mimetype);
                    request.addRequestHeader("cookie", cookies);
                    request.addRequestHeader("User-Agent", useragent);
                    request.setDescription("Downloading File...");
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);

                    //And send to Download Manager
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
        String[] perms = new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        checkPermission(perms,99);
    }

    public Certificate getMinimaSSLCert(){
        return mMinimaSSLCert;
    }

    //Register a Context Menu - long press image download
    public void registerDefaultContextMenu(){
        registerForContextMenu(mWebView);
    }

    public void unregisterDefaultContextMenu(){
        unregisterForContextMenu(mWebView);
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo){
        super.onCreateContextMenu(contextMenu, view, contextMenuInfo);

        final WebView.HitTestResult webViewHitTestResult = mWebView.getHitTestResult();

        //Is it an image
        if (    webViewHitTestResult.getType() == WebView.HitTestResult.IMAGE_TYPE ||
                webViewHitTestResult.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {

            //contextMenu.setHeaderTitle("Download Image");

            contextMenu.add(0, 1, 0, "Save Image").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {

                    //What are we downloading
                    String DownloadImageURL = webViewHitTestResult.getExtra();

                    //Is it a blob
                    if(DownloadImageURL.startsWith("data:")){

                        //Convert to HEX
                        downloadImageDataURL(DownloadImageURL);

                    }else{
                        if(URLUtil.isValidUrl(DownloadImageURL)){

                            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(DownloadImageURL));
                            request.allowScanningByMediaScanner();
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                            downloadManager.enqueue(request);

                            Toast.makeText(MiniBrowser.this,"Downloading image..",Toast.LENGTH_SHORT).show();

                        }else {
                            Toast.makeText(MiniBrowser.this,"Sorry.. Something Went Wrong.",Toast.LENGTH_SHORT).show();
                        }
                    }

                    return true;
                }
            });
        }
    }

    public void downloadImageDataURL(String zURL){

        try{
            //First get the type..
            int indexstart  = zURL.indexOf(":");
            int indexend    = zURL.indexOf(";");
            int indexdata   = zURL.indexOf(",");

            //Get the type and base64 data
            String type     =  zURL.substring(indexstart+1,indexend);
            String data     =  zURL.substring(indexdata+1);

            //Convert Base64 to Hex
            byte[] hexdata = Base64.getDecoder().decode(data);

            //And pick a filename..
            String filename = MiniData.getRandomData(8).to0xString();
            if(type.equals("image/jpeg")){
                filename = filename+".jpeg";
            }else if(type.equals("image/png")){
                filename = filename+".png";
            }else if(type.equals("image/gif")){
                filename = filename+".gif";
            }else if(type.equals("image/svg+xml")){
                filename = filename+".svg";
            }else if(type.equals("image/ico")){
                filename = filename+".ico";
            }

            //And finally save the data
            saveFile(filename,hexdata);

        }catch(Exception exc){
            MinimaLogger.log(exc);
        }
    }

    public void shutWindow(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.loadUrl("");
                MiniBrowser.super.onBackPressed();
            }
        });
    }

    public void shutdownMinima(){

        //Now in shutdopwn mode..
        mShutDownMode = true;
        MinimaLogger.log("MINIBROWSER SHUTDOWN MODE STARTED");

        //Stop the service..
        Intent minimaintent = new Intent(getBaseContext(), MinimaService.class);
        stopService(minimaintent);

        //And close all windows
        showShutdownmessage();
        //finishAffinity();
    }

    private void showShutdownmessage(){
        if(mIsMiniHUB){
            Toast.makeText(this, "Minima is shutting down..", Toast.LENGTH_SHORT).show();
        }

        //And shutdown..
        finishAffinity();
    }

    @Override
    public void onResume(){
        super.onResume();

        //Are we in shutdown mode..
        if(mShutDownMode){
            MinimaLogger.log("MINIBROWSER ON RESUME.. SHUTDOWN MODE "+mShutDownMode);

            //finishAffinity();
            showShutdownmessage();
        }
    }

    public void showToolbar(){
        //Only if hidden
        if(!getSupportActionBar().isShowing() && !mHidingBar) {
            getSupportActionBar().show();

            //And hide it after a delay
            hideToolBar();
        }
    }

    private void hideToolBar(){

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

        //Check all the requested permissions
        boolean allok = true;
        for(String perm : permissions){
            if(ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED){
                allok = false;
                break;
            }
        }

        //Ask for all the permissions
        if(!allok){
            ActivityCompat.requestPermissions(this, permissions , requestCode);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        //Only works on certain version..
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            menu.setGroupDividerEnabled(true);
        }

        //Different Menu for Main MiniHUB
        if(mIsMiniHUB){
            getMenuInflater().inflate(R.menu.minibrowserhub, menu);
        }else{
            getMenuInflater().inflate(R.menu.minibrowser, menu);
        }

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

            case R.id.action_mdsfiles:

                Intent files = new Intent(this, FilesActivity.class);
                startActivity(files);

                return true;

            case R.id.action_mdsopen:

                Intent browser = new Intent(Intent.ACTION_VIEW);
                browser.setData(Uri.parse(mBaseURL));
                startActivity(browser);

                return true;

            case R.id.action_mdsparams:

                //Show the Initial extra params
                showExtraInitialParams();

                return true;

            case R.id.action_mdsshare:

                //Create share Intent
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=com.minima.android");
                sendIntent.setType("text/plain");

                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);

                return true;

            case R.id.action_mdsshutdown:

                //Show the Initial extra params
                shutdownMinima();

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showExtraInitialParams(){

        AlertDialog.Builder pbuilder = new AlertDialog.Builder(this);
        pbuilder.setTitle("Minima Startup Params");

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.params_view, null);
        pbuilder.setView(dialogView);

        // Set up the input
        final EditText input = dialogView.findViewById(R.id.params_initial);

        //Load the current prefs..
        SharedPreferences pref  = getSharedPreferences("startup_params",MODE_PRIVATE);
        String prefstring       = pref.getString("extra_params","");
        input.setText(prefstring);

        pbuilder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                //Get the text
                String text = input.getText().toString().trim();

                //Check It
                boolean validparams = ParamConfigurer.checkParams(text);

                if(validparams) {
                    //Save to Prefs..
                    SharedPreferences.Editor edit = pref.edit();
                    edit.putString("extra_params", text);
                    edit.apply();

                    Toast.makeText(MiniBrowser.this, "Extra startup params set..", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(MiniBrowser.this, "Invalid Params!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        pbuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        pbuilder.show();
    }

    @Override
    public void onBackPressed() {
        if(mWebView.canGoBack()) {
            mWebView.goBack();

            // And show the toolbar
            showToolbar();

        } else {

            //Reset Page
            mWebView.loadUrl("");

            //Leave MiniDAPP
            super.onBackPressed();
        }
    }

    public void openFile(String [] zMimeTypes, ValueCallback<Uri[]> zFilePathCallback) {

        //Store for later
        mFileCheckPath = zFilePathCallback;

        //Open a file
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        /*if(zMimeTypes.length>0) {
            MinimaLogger.log("Set Mime Types : "+ Arrays.toString(zMimeTypes)+" "+zMimeTypes.length);
            intent.putExtra(Intent.EXTRA_MIME_TYPES, zMimeTypes);
        }*/
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
    }

    //Save HEX data to a file..
    public void saveFile(String zFilename, byte[] zHexData){

        //Create a MiniData object
        MiniData data = new MiniData(zHexData);

        //Save to Downloads..
        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File mindownloads = new File(downloads, "Minima");

        //And save..
        try {

            //Now create the full file..
            File fullfile = new File(mindownloads, zFilename);

            //Write data to file..
            MiniFile.writeDataToFile(fullfile, data.getBytes());

        } catch (Exception e) {
            MinimaLogger.log(e);

            //Small message
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MiniBrowser.this, "Error saving file..", Toast.LENGTH_SHORT).show();
                }
            });

            return;
        }

        //Small message
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MiniBrowser.this, "File saved to Minima folder : " + zFilename, Toast.LENGTH_SHORT).show();
            }
        });
    }
}