package com.minima.android.mdshub;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Message;
import android.text.TextUtils;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.appcompat.app.AlertDialog;

import org.minima.utils.MinimaLogger;

import java.util.Arrays;

public class MiniChromViewClient extends WebChromeClient {

    //When Opening files..
    ValueCallback<Uri[]> mFilePathCallback;

    //The Main Activity
    MiniBrowser mMiniBrowser;

    public MiniChromViewClient(MiniBrowser zActivity){
        mMiniBrowser = zActivity;
    }

    @Override
    public void onReceivedTitle(WebView view, String title) {
        super.onReceivedTitle(view, title);
        if (!TextUtils.isEmpty(title)) {
            mMiniBrowser.setTitle(title);
        }
    }

    @Override
    public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, android.os.Message resultMsg) {
        WebView.HitTestResult result    = view.getHitTestResult();
        String data                     = result.getExtra();
        Context context                 = view.getContext();

        Message href = view.getHandler().obtainMessage();
        //MinimaLogger.log("New Window Message : "+href.toString());

        view.requestFocusNodeHref(href);
        var url = href.getData().getString("url");

        MinimaLogger.log("New Window Data : "+data);
        MinimaLogger.log("New Window url : "+url);

        Intent intent = new Intent(context, MiniBrowser.class);
        intent.putExtra("url",url);
        context.startActivity(intent);

        return false;
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        MinimaLogger.log(consoleMessage.message()
                + " -- From line "
                + consoleMessage.lineNumber()
                + " of "+ consoleMessage.sourceId());

        return super.onConsoleMessage(consoleMessage);
    }

    @Override
    public boolean onJsAlert(WebView view, String url, String message, final android.webkit.JsResult result)
    {
        new AlertDialog.Builder(view.getContext())
                .setTitle("Alert")
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
    public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
        new AlertDialog.Builder(view.getContext())
                .setTitle("Confirm")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok,new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int which){
                                result.confirm();
                            }
                        })
                .setNegativeButton(android.R.string.cancel,new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int which){
                                result.cancel();
                            }
                        })
                .create()
                .show();

        return true;
    }

    @Override
    public boolean onShowFileChooser (WebView webView, ValueCallback<Uri[]> filePathCallback,
                                      WebChromeClient.FileChooserParams fileChooserParams){

        //Store for later
        mFilePathCallback = filePathCallback;

        //What types do you want..
        String[] types = fileChooserParams.getAcceptTypes();

        MinimaLogger.log("OPEN FILES MIME TYPES "+Arrays.toString(types));

        //Open a file..
        mMiniBrowser.openFile(types,filePathCallback);

        return true;
    }
}
