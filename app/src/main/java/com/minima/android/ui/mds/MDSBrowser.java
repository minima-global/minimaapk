package com.minima.android.ui.mds;

import android.content.Intent;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.minima.android.MainActivity;
import com.minima.android.R;
import com.minima.android.ui.maxima.MyDetailsActivity;

import org.minima.system.Main;
import org.minima.system.network.maxima.MaximaManager;

import java.io.File;

public class MDSBrowser extends AppCompatActivity {

    WebView mWebView;

    String mUID;
    String mSessionID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_browser);

        String name = getIntent().getStringExtra("name");
        mUID        = getIntent().getStringExtra("uid");
        mSessionID  = Main.getInstance().getMDSManager().convertMiniDAPPID(mUID);

        setTitle(name);

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

                // Checks Embedded certificates
                SslCertificate serverCertificate = error.getCertificate();

                //Check it..
                String sslCertificate = serverCertificate.toString();
//                String mySslCertificate = new SslCertificate(cert).toString();

                handler.proceed();
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
