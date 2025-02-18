package com.minima.android.browser;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.Browser;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.minima.objects.base.MiniData;
import org.minima.system.Main;
import org.minima.system.commands.Command;
import org.minima.utils.MiniFile;
import org.minima.utils.MiniFormat;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONArray;
import org.minima.utils.ssl.SSLManager;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;

public class MiniBrowserJSInterface {

    public boolean DEBUG_LOGS = false;

    private MiniBrowser mMiniBrowser;

    public MiniBrowserJSInterface(MiniBrowser zMiniBrowser) {
        mMiniBrowser = zMiniBrowser;
    }

    @JavascriptInterface
    public void blobDownload(String zFilename, String zHexData) {

        if(DEBUG_LOGS){
            MinimaLogger.log("JS BLOBDOWNLOAD "+zFilename+" size:"+zHexData.length());
        }

        //Create a MiniData object
        MiniData data = new MiniData(zHexData);

        mMiniBrowser.saveHexData(zFilename,data.getBytes());
    }

    @JavascriptInterface
    public void fileDownload(String zSessionID, String zMdsfile) {

        if(DEBUG_LOGS){
            MinimaLogger.log("JS FILEDOWNLOAD "+zMdsfile+" "+zSessionID);
        }

        //Now get the MDS file..
        String minidappid = Main.getInstance().getMDSManager().convertSessionID(zSessionID);

        //Get the filepath..
        File root   = Main.getInstance().getMDSManager().getMiniDAPPFileFolder(minidappid);
        File actual = new File(root,zMdsfile);

        //And save..
        mMiniBrowser.saveFile("*/*",actual);
    }

    @JavascriptInterface
    public void disableDefaultContextMenu() {
        if(DEBUG_LOGS) {
            MinimaLogger.log("JS DISABLE CONTEXT MENU");
        }

        //Don't handle long press automatically
        mMiniBrowser.unregisterDefaultContextMenu();
    }

    @JavascriptInterface
    public void enableDefaultContextMenu() {
        if(DEBUG_LOGS) {
            MinimaLogger.log("JS ENABLE CONTEXT MENU");
        }

        //Enable Context menu for long press..
        mMiniBrowser.registerDefaultContextMenu();
    }

    @JavascriptInterface
    public void closeWindow() {
        if(DEBUG_LOGS) {
            MinimaLogger.log("JS CLOSE WINDOW");
        }

        //Close this WebView
        //mMiniBrowser.shutdownMinima();

        //Close this WebView
        mMiniBrowser.shutWindow();
    }

    @JavascriptInterface
    public void shutdownMinima() {
        shutdownMinima(false);
    }

    @JavascriptInterface
    public void shutdownMinima(boolean zCompact) {
        if(DEBUG_LOGS) {
            MinimaLogger.log("JS SHUTDOWN MINIMA COMPACT:"+zCompact);
        }

        //Close this WebView
        MiniBrowser.mShutDownCompact = zCompact;
        mMiniBrowser.shutdownMinima();
    }

    @JavascriptInterface
    public void showTitleBar() {
        if(DEBUG_LOGS) {
            MinimaLogger.log("JS SHOW TITLEBAR");
        }

        mMiniBrowser.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Show the Title bar
                mMiniBrowser.showToolbar();
            }
        });
   }

    @JavascriptInterface
    public void clearCache() {
        if(DEBUG_LOGS) {
            MinimaLogger.log("JS CLEAR CACHE");
        }

        mMiniBrowser.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMiniBrowser.clearCache();
            }
        });
    }

    @JavascriptInterface
    public void openExternalBrowser(String zUrl, String zTarget) {
        if(DEBUG_LOGS) {
            MinimaLogger.log("JS OPENEXTERNAL "+zUrl);
        }
            //The URL page
        Uri minipage = Uri.parse(zUrl);

        //Start the browser
        Intent browser = new Intent(Intent.ACTION_VIEW);
        browser.putExtra(Browser.EXTRA_APPLICATION_ID, zTarget);
        browser.setData(minipage);
        mMiniBrowser.startActivity(browser);
    }

    @JavascriptInterface
    public void shareText(String zText) {
        if(DEBUG_LOGS) {
            MinimaLogger.log("JS SHARETEXT "+zText);
        }

        //Create share Intent
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Minima Text Share");
        sendIntent.putExtra(Intent.EXTRA_TEXT, zText);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        mMiniBrowser.startActivity(shareIntent);
    }

    @JavascriptInterface
    public void shareFile(String zFilePath, String zMimeType) {
        if(DEBUG_LOGS) {
            MinimaLogger.log("JS SHAREFILE "+zFilePath);
        }

        //Create the file..
        File backup = new File(zFilePath);

        //Check exists..
        if(!backup.exists()){

            mMiniBrowser.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mMiniBrowser, "File does not exist", Toast.LENGTH_SHORT).show();
                }
            });

            return;
        }

        String filename = "sharefile";
        try {
            filename = Paths.get(new URI(zFilePath).getPath()).getFileName().toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        //Get the URi
        Uri backupuri = FileProvider.getUriForFile(mMiniBrowser,"com.minima.android.provider",backup);

        //Now share that file..
        Intent intentShareFile = new Intent(Intent.ACTION_SEND);
        intentShareFile.setType(zMimeType);
        intentShareFile.putExtra(Intent.EXTRA_STREAM, backupuri);
        intentShareFile.putExtra(Intent.EXTRA_SUBJECT,filename);
        intentShareFile.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent chooser = Intent.createChooser(intentShareFile, "Share File");

        List<ResolveInfo> resInfoList = mMiniBrowser.getPackageManager().queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            mMiniBrowser.grantUriPermission(packageName, backupuri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        mMiniBrowser.startActivity(chooser);
    }
}