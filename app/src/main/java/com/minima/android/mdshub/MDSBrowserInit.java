package com.minima.android.mdshub;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.minima.android.MainActivity;
import com.minima.android.R;
import com.minima.android.files.CopyFile;
import com.minima.android.files.InstallMiniDAPP;
import com.minima.android.files.RestoreBackup;
import com.minima.android.files.UpdateMiniDAPP;

import org.minima.Minima;
import org.minima.objects.base.MiniData;
import org.minima.utils.MinimaLogger;
import org.minima.utils.ssl.SSLManager;

import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public class MDSBrowserInit extends AppCompatActivity {

    WebView mWebView;

    String mUID;
    String mSessionID;

    boolean mHaveCheckedSSL = false;

    Certificate mMinimaSSLCert;

    ValueCallback<Uri[]> mFilePathCallback = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_browser);

        Toolbar tb  = findViewById(R.id.minidapp_toolbar);
        setSupportActionBar(tb);

        setTitle("MiniHUB");

        //Check the Shared Prefs..
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        mHaveCheckedSSL         = prefs.getBoolean("have_checked_ssl",false);

        //Get Our SSL Cert
//        try {
//            mMinimaSSLCert = SSLManager.getSSLKeyStore().getCertificate("MINIMA_NODE");
//        } catch (Exception e) {
//            MinimaLogger.log(e);
//        }

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
        settings.setBuiltInZoomControls(true);

        //settings.setBuiltInZoomControls(true);
        //mWebView.addJavascriptInterface(new WebAppInterface(this), "");

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return false;
            }
        });

        //mWebView.setWebViewClient(new WebViewClient() {
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, final android.webkit.JsResult result)
            {
                new AlertDialog.Builder(MDSBrowserInit.this)
                        .setTitle("javaScript dialog")
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok,
                                new AlertDialog.OnClickListener()
                                {
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        result.confirm();
                                    }
                                })
                        .setCancelable(false)
                        .create()
                        .show();

                return true;
            };

            @Override
            public boolean onShowFileChooser (WebView webView, ValueCallback<Uri[]> filePathCallback,
                                              WebChromeClient.FileChooserParams fileChooserParams){
                MinimaLogger.log("onFileChooser");
                mFilePathCallback = filePathCallback;

                String[] types = fileChooserParams.getAcceptTypes();
                MinimaLogger.log(Arrays.toString(types));

                openFile();
                return true;
            }

//            @Override
//            public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
//
//                // Get the Certificate
//                SslCertificate serverCertificate = error.getCertificate();
//
//                //Are we able to check properly
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                    //Get the X509 Cert in full
//                    X509Certificate cert = serverCertificate.getX509Certificate();
//
//                    //Check they are the same..
//                    MiniData cert1 = new MiniData(cert.getPublicKey().getEncoded());
//                    MiniData cert2 = new MiniData(mMinimaSSLCert.getPublicKey().getEncoded());
//
//                    if(cert1.isEqual(cert2)){
//                        //All good
//                        handler.proceed();
//                    }else{
//                        handler.cancel();
//                    }
//
//                }else{
//
//                    //We have asked already
//                    if(mHaveCheckedSSL) {
//
//                        //Ask the User if they are ok
//                        final AlertDialog.Builder builder = new AlertDialog.Builder(MDSBrowserInit.this);
//                        String message = "";
//                        //                    switch (error.getPrimaryError()) {
//                        //                        case SslError.SSL_UNTRUSTED:
//                        //                            message = "The certificate authority is not trusted.";
//                        //                            break;
//                        //                        case SslError.SSL_EXPIRED:
//                        //                            message = "The certificate has expired.";
//                        //                            break;
//                        //                        case SslError.SSL_IDMISMATCH:
//                        //                            message = "The certificate Hostname mismatch.";
//                        //                            break;
//                        //                        case SslError.SSL_NOTYETVALID:
//                        //                            message = "The certificate is not yet valid.";
//                        //                            break;
//                        //                    }
//                        message += "Minima uses a self-signed certificate.\n\nDo you wish to continue anyway?";
//
//                        builder.setTitle("SSL Certificate Warning");
//                        builder.setMessage(message);
//                        builder.setPositiveButton("continue", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//
//                                //Store this
//                                SharedPreferences.Editor edit = getPreferences(Context.MODE_PRIVATE).edit();
//                                edit.putBoolean("have_checked_ssl", true);
//                                mHaveCheckedSSL = true;
//
//                                //Proceed
//                                handler.proceed();
//                            }
//                        });
//                        builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                handler.cancel();
//                            }
//                        });
//                        final AlertDialog dialog = builder.create();
//                        dialog.show();
//                    }else{
//                        //Already checked..
//                        handler.proceed();
//                    }
//                }
//            }
        });

        loadInit();
    }

    public String getIndexURL(){
        return "https://127.0.0.1:9003/";
    }

    public void loadInit(){
        mWebView.clearHistory();
        mWebView.clearCache(true);

        String summary =
                "<html><body>" +
                "You scored <b>192</b> points." +
                "<input type=\"file\"\n" +
                        "       id=\"avatar\" name=\"avatar\"\n" +
                        "       accept=\"image/png, image/jpeg\">" +
                "<input type=\"button\" value=\"Say hello\" onClick=\"showAndroidToast('Hello Android!')\" />\n" +
                        "\n" +
                        "<script type=\"text/javascript\">\n" +
                        "    function showAndroidToast(toast) {\n" +
                        "        alert(toast);\n" +
                        "    }\n" +
                        "</script>" +
                "</body></html>";

        mWebView.loadData(summary, "text/html; charset=utf-8", "utf-8");

        //mWebView.loadUrl(getIndexURL());
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

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mdsbrowser, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_mdsrefresh:
                loadInit();
                return true;

            case R.id.action_mdsexit:
                finish();
                return true;

            case R.id.action_mdsopen:

                String url = getIndexURL();
                Intent browser = new Intent(Intent.ACTION_VIEW);
                browser.setData(Uri.parse(url));
                startActivity(browser);

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Function to check and request permission
    public boolean checkPermission(String permission, int requestCode){
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(MDSBrowserInit.this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MDSBrowserInit.this, new String[] { permission }, requestCode);
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
            openFile();
        }else{
            Toast.makeText(MDSBrowserInit.this, "File Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }

    public void openFile() {

        //Check for permission
        if(!checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, 99)){
            return;
        }

        //The type of file we are looking for
        String mimeType = "image/png";

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        String [] mimeTypes = {"image/png", "image/jpeg"};
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
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
            mFilePathCallback.onReceiveValue(null);
        }else if(resultCode == RESULT_OK){
            mFilePathCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
        }

        //Was anything returned
        if(data == null){
            return;
        }

        //Get the file URI
        Uri fileuri = data.getData();

        MinimaLogger.log("OPENFILE : "+fileuri.toString());
    }
}
