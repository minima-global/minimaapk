package com.minima.android.ui.store;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.minima.android.R;

import org.minima.objects.base.MiniData;
import org.minima.system.Main;
import org.minima.utils.MinimaLogger;
import org.minima.utils.ssl.SSLManager;

import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public class StoreBrowser extends AppCompatActivity {

    WebView mWebView;

    String mUID;
    String mSessionID;

    boolean mHaveCheckedSSL = false;

    Certificate mMinimaSSLCert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_browser);

        Toolbar tb  = findViewById(R.id.minidapp_toolbar);
        setSupportActionBar(tb);

        String name = getIntent().getStringExtra("name");
        mUID        = getIntent().getStringExtra("uid");
        mSessionID  = Main.getInstance().getMDSManager().convertMiniDAPPID(mUID);

        setTitle(name);

        //Check the Shared Prefs..
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        mHaveCheckedSSL         = prefs.getBoolean("have_checked_ssl",false);

        //Get Our SSL Cert
        try {
            mMinimaSSLCert = SSLManager.getSSLKeyStore().getCertificate("MINIMA_NODE");
        } catch (KeyStoreException e) {
            MinimaLogger.log(e);
        }

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

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return false;
            }

            @Override
            public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {

                // Get the Certificate
                SslCertificate serverCertificate = error.getCertificate();

                //Are we able to check properly
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    //Get the X509 Cert in full
                    X509Certificate cert = serverCertificate.getX509Certificate();

                    //Check they are the same..
                    MiniData cert1 = new MiniData(cert.getPublicKey().getEncoded());
                    MiniData cert2 = new MiniData(mMinimaSSLCert.getPublicKey().getEncoded());

                    if(cert1.isEqual(cert2)){
                        //All good
                        handler.proceed();
                    }else{
                        handler.cancel();
                    }

                }else{

                    //We have asked already
                    if(mHaveCheckedSSL) {

                        //Ask the User if they are ok
                        final AlertDialog.Builder builder = new AlertDialog.Builder(StoreBrowser.this);
                        String message = "";
                        //                    switch (error.getPrimaryError()) {
                        //                        case SslError.SSL_UNTRUSTED:
                        //                            message = "The certificate authority is not trusted.";
                        //                            break;
                        //                        case SslError.SSL_EXPIRED:
                        //                            message = "The certificate has expired.";
                        //                            break;
                        //                        case SslError.SSL_IDMISMATCH:
                        //                            message = "The certificate Hostname mismatch.";
                        //                            break;
                        //                        case SslError.SSL_NOTYETVALID:
                        //                            message = "The certificate is not yet valid.";
                        //                            break;
                        //                    }
                        message += "Minima uses a self-signed certificate.\n\nDo you wish to continue anyway?";

                        builder.setTitle("SSL Certificate Warning");
                        builder.setMessage(message);
                        builder.setPositiveButton("continue", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                //Store this
                                SharedPreferences.Editor edit = getPreferences(Context.MODE_PRIVATE).edit();
                                edit.putBoolean("have_checked_ssl", true);
                                mHaveCheckedSSL = true;

                                //Proceed
                                handler.proceed();
                            }
                        });
                        builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                handler.cancel();
                            }
                        });
                        final AlertDialog dialog = builder.create();
                        dialog.show();
                    }else{
                        //Already checked..
                        handler.proceed();
                    }
                }
            }
        });

        loadInit();
    }

    public String getIndexURL(){
        return "https://127.0.0.1:9003/"+mUID+"/index.html?uid="+mSessionID;
    }

    public void loadInit(){
        mWebView.clearHistory();
        mWebView.clearCache(true);
        mWebView.loadUrl(getIndexURL());
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
}
