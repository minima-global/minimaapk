package com.minima.android.mdshub;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Build;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AlertDialog;

import com.minima.android.ui.mds.MDSBrowser;

import org.minima.objects.base.MiniData;
import org.minima.utils.MinimaLogger;
import org.minima.utils.ssl.SSLManager;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public class MiniWebViewClient extends WebViewClient  {

    //The Minima SSL Certificate
    Certificate mMinimaSSLCert = null;

    //Have we checked the Cert
    boolean mHaveCheckedSSL;

    //The Main Context
    Context mMainContext;

    //The Shared Prefs Name
    String SHARED_PREFS         = "minima_ssl_checker_prefs";
    String SHARED_PREFS_VALUE   = "have_checked_ssl";

    //Main Constructor
    public MiniWebViewClient(Context zContext){
        mMainContext = zContext;

        //Get the Cert
        try{
            mMinimaSSLCert = SSLManager.getSSLKeyStore().getCertificate("MINIMA_NODE");
        }catch(Exception exc){
            //Something wrong..
            MinimaLogger.log(exc);
        }

        //Have we checked it..
        SharedPreferences prefs = mMainContext.getSharedPreferences(SHARED_PREFS,Context.MODE_PRIVATE);
        mHaveCheckedSSL         = prefs.getBoolean(SHARED_PREFS_VALUE,false);

        //MinimaLogger.log("Have Checked SSL : "+mHaveCheckedSSL);
    }


    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        view.loadUrl(request.getUrl().toString());
        return false;
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description,String failingUrl){
        MinimaLogger.log("WEB ERROR : "+description);
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
                builder.setPositiveButton("continue", new DialogInterface.OnClickListener() {
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
