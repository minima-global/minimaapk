package com.minima.android.ui.mds;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_browser);

        String name = getIntent().getStringExtra("name");
        mUID  = getIntent().getStringExtra("uid");

        setTitle(name);

        mWebView = (WebView) findViewById(R.id.mds_webview);

        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setLoadWithOverviewMode(true);
        //settings.setBuiltInZoomControls(true);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return false;
            }
        });

        loadInit();
    }

    public void loadInit(){
        mWebView.clearHistory();

        //Now the image
        File rootfile   = getFilesDir();
        File mdsroot    = new File(rootfile,"mds");
        File webroot    = new File(mdsroot,"web");
        File dapproot   = new File(webroot,mUID);
        File index      = new File(dapproot,"index.html");

        mWebView.loadUrl("file://"+index.getAbsolutePath());
    }

    @Override
    public void onBackPressed() {
        if(mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
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

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
