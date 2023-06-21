package com.minima.android.browser;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.ConsoleMessage;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import org.minima.utils.MinimaLogger;

public class MiniChromViewClient extends WebChromeClient {

    //When Opening files..
    ValueCallback<Uri[]> mFilePathCallback;

    //The Main Activity
    MiniBrowser mMiniBrowser;

    //The Console messages
    String mConsoleMessages = new String();

    public MiniChromViewClient(MiniBrowser zActivity){
        mMiniBrowser = zActivity;
    }

    public String getConsoleMessages(){
        return mConsoleMessages;
    }

    @Override
    public void onReceivedTitle(WebView view, String title) {
        super.onReceivedTitle(view, title);
        if (!TextUtils.isEmpty(title)) {
            mMiniBrowser.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(title.equalsIgnoreCase("minihub")){
                        mMiniBrowser.setTitle("Minima");
                    }else{
                        mMiniBrowser.setTitle(title);
                    }
                }
            });
        }
    }

//    @Override
//    public void onReceivedIcon(WebView view, Bitmap icon){
//
//        int height = mMiniBrowser.getToolBar().getHeight();
//        height = (int)((float)height * 0.8);
//
//        Bitmap bitmapResized = Bitmap.createScaledBitmap(icon, height, height, true);
//        Drawable dicon = new BitmapDrawable( mMiniBrowser.getResources(), bitmapResized);
//
//        mMiniBrowser.getToolBar().setNavigationIcon(dicon);
//    }

    @Override
    public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, android.os.Message resultMsg) {

        WebView newWebView = new WebView(view.getContext());
        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
        transport.setWebView(newWebView);
        resultMsg.sendToTarget();
        newWebView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url != null && !url.isEmpty()) {

                    //Open a new window..
                    Intent intent = new Intent(view.getContext(), MiniBrowser.class);
                    intent.putExtra("url",url);
                    view.getContext().startActivity(intent);
                }else{
                    MinimaLogger.log("BLANK NEW Window!!");
                }

                return false;
            }
        });
        return true;


//        WebView.HitTestResult result    = view.getHitTestResult();
//        String data                     = result.getExtra();
//        Context context                 = view.getContext();
//
//        Message href = view.getHandler().obtainMessage();
//        //MinimaLogger.log("New Window Message : "+href.toString());
//
//        view.requestFocusNodeHref(href);
//        var url = href.getData().getString("url");
//
////        MinimaLogger.log("New Window Data : "+data);
////        MinimaLogger.log("New Window url : "+url);
//
//        Intent intent = new Intent(context, MiniBrowser.class);
//        intent.putExtra("url",url);
//        context.startActivity(intent);
//        return false;
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {

        //Add a line to the Console output
        mConsoleMessages += "<b>Line "+consoleMessage.lineNumber()+"</b> - "+consoleMessage.message()+"<br><br>";

        return true;
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
    public boolean onJsPrompt (WebView view, String url, String message, String defaultValue, JsPromptResult result){

        final EditText inputfield = new EditText(mMiniBrowser);
        inputfield.setText(defaultValue);

        new AlertDialog.Builder(view.getContext())
                .setTitle(message)
                .setView(inputfield)
                .setPositiveButton(android.R.string.ok,new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which){

                        //Get the Input
                        String input = inputfield.getText().toString().trim();

                        //And respond..
                        result.confirm(input);
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

        //MinimaLogger.log("OPEN FILES MIME TYPES "+Arrays.toString(types));

        //Open a file..
        mMiniBrowser.openFile(types,filePathCallback);

        return true;
    }
}
