package com.minima.android.browser;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.JsResult;
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

import com.minima.android.R;

import org.minima.utils.MinimaLogger;

import java.security.cert.Certificate;
import java.util.Arrays;

public class MDSBrowserTest extends MiniBrowser {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadBasePage();
    }

    private void loadBasePage(){
        String summary =
                "<html><body>" +
                        "<input type=\"file\"\n" +
                        "       id=\"avatar\" name=\"avatar\"\n" +
                        "       accept=\"image/png, image/jpeg\"><br><br>" +
                        "<input type=\"button\" value=\"Show Alert\" onClick=\"alert('Works!');\" />\n" +
                        "<br><br>" +
                        "<input type=\"button\" value=\"Show Confirm\" onClick=\"confirm('Works!');\" />\n" +
                        "<br><br>" +
                        "<input type=\"button\" value=\"Close Window\" onClick=\"Android.closeWindow();\" />\n" +
                        "<br><br>" +
                        "<input type=\"button\" value=\"Shut Down\" onClick=\"Android.shutdownMinima();\" />\n" +
                        "<br><br>" +
                        "<input type=\"button\" value=\"External Window\" onClick=\"Android.openExternalBrowser('http://www.google.com','google');\" />\n" +
                        "<br><br>" +
                        "<input type=\"button\" value=\"Share Text\" onClick=\"Android.shareText('Hello you!');\" />\n" +
                        "<br><br>" +
                        "<input type=\"button\" value=\"Share File\" onClick=\"Android.shareFile('/file', 'application/zip');\" />\n" +
                        "<br><br>" +
                        "<a href=\"http://hello.com/somepage.html\">OPEN SAME WINDOW</a><br><br>" +
                        "<a href=\"http://hello.com/somepage.html\" target='_blank'>OPEN NEW WINDOW</a><br><br>" +
                        "</body></html>";

        mWebView.loadData(summary, "text/html; charset=utf-8", "utf-8");
    }
}
