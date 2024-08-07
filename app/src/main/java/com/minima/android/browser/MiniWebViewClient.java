package com.minima.android.browser;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Build;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AlertDialog;

import org.minima.objects.base.MiniData;
import org.minima.utils.MinimaLogger;
import org.minima.utils.ssl.SSLManager;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public class MiniWebViewClient extends WebViewClient  {

    private boolean ERROR_LOGS = false;

    //The Minima SSL Certificate
    Certificate mMinimaSSLCert = null;

    //Have we checked the Cert
    boolean mHaveCheckedSSL;

    //The Main Context
    MiniBrowser mMainContext;

    //The Shared Prefs Name
    String SHARED_PREFS         = "minima_ssl_checker_prefs";
    String SHARED_PREFS_VALUE   = "have_checked_ssl";

    //Main Constructor
    public MiniWebViewClient(MiniBrowser zContext){
        mMainContext = zContext;

        //Get the Cert
        mMinimaSSLCert = mMainContext.getMinimaSSLCert();
//        try{
//            mMinimaSSLCert = SSLManager.getSSLKeyStore().getCertificate("MINIMA_NODE");
//        }catch(Exception exc){
//            //Something wrong..
//            MinimaLogger.log(exc);
//        }

        //Have we checked it..
        SharedPreferences prefs = mMainContext.getSharedPreferences(SHARED_PREFS,Context.MODE_PRIVATE);
        mHaveCheckedSSL         = prefs.getBoolean(SHARED_PREFS_VALUE,false);
    }


    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        view.loadUrl(url);
        return false;
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError rerr) {
        if(rerr.getErrorCode() == ERROR_CONNECT){
            if(ERROR_LOGS){
                MinimaLogger.log("onReceivedError :"+req.getUrl().toString());
            }

            //Only show no connect if it's an HTML page..
            if (req.getUrl().toString().toLowerCase().contains(".html")) {
                showNoConnectErrorPage(view);
            }else{
                mMainContext.getConsoleClient().addConsoleMessage(req.getUrl().toString());
            }
        }
    }

    @Override
    public void onReceivedHttpError (WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
        if(request.getUrl()!=null) {
            if(ERROR_LOGS){
                MinimaLogger.log("onReceivedHttpError :"+request.getUrl().toString());
            }

            if (request.getUrl().toString().toLowerCase().contains(".html")) {
                showMainHTTPErrorPage(view);
            }
        }
    }

    private void showMainHTTPErrorPage(WebView zView){
        mMainContext.showToolbar();
        zView.loadUrl("file:///android_asset/httperror.html");
    }

    private void showNoConnectErrorPage(WebView zView){
        mMainContext.showToolbar();
        zView.loadUrl("file:///android_asset/noconnect.html");
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

            //Check the same
            if(cert1.isEqual(cert2)){
                //All good
                handler.proceed();
            }else{

                //Try once to re-load..
                try{
                    mMinimaSSLCert = SSLManager.getSSLKeyStore().getCertificate("MINIMA_NODE");
                    cert2 = new MiniData(mMinimaSSLCert.getPublicKey().getEncoded());
                    if(cert1.isEqual(cert2)){
                        //All good
                        handler.proceed();
                    }

                    return;
                }catch(Exception exc){
                    //Something wrong..
                    MinimaLogger.log(exc);
                }

                MinimaLogger.log("INCORRECT SSL!");
                handler.cancel();
            }

        }else{

            //We have asked already
            if(mHaveCheckedSSL) {

                //Ask the User if they are ok
                final AlertDialog.Builder builder = new AlertDialog.Builder(mMainContext);
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
                builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        //Store this
                        SharedPreferences.Editor edit =  mMainContext.getSharedPreferences(SHARED_PREFS,Context.MODE_PRIVATE).edit();
                        edit.putBoolean(SHARED_PREFS_VALUE, true);
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
}
