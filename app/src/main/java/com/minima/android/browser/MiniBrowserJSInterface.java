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
import org.minima.utils.MiniFile;
import org.minima.utils.MinimaLogger;
import org.minima.utils.ssl.SSLManager;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;

public class MiniBrowserJSInterface {

    private MiniBrowser mMiniBrowser;

    public MiniBrowserJSInterface(MiniBrowser zMiniBrowser) {
        mMiniBrowser = zMiniBrowser;
    }

    @JavascriptInterface
    public void blobDownload(String zMdsfile, String zHexData) {

        //Create a MiniData object
        MiniData data = new MiniData(zHexData);

        //Save to Downloads..
        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File mindownloads = new File(downloads, "Minima");

        //And save..
        String filename = null;
        try {
            //Create the file..
            filename = Paths.get(new URI(zMdsfile).getPath()).getFileName().toString();

            //Now create the full file..
            File fullfile = new File(mindownloads, filename);

            //Write data to file..
            MiniFile.writeDataToFile(fullfile, data.getBytes());

        } catch (Exception e) {
            MinimaLogger.log(e);

            //Small message
            mMiniBrowser.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mMiniBrowser, "Error saving file..", Toast.LENGTH_SHORT).show();
                }
            });

            return;
        }

        //Small message
        final String finalfile = filename;
        mMiniBrowser.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mMiniBrowser, "File saved : " + finalfile, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @JavascriptInterface
    public void closeWindow() {

        //Close this WebView
        mMiniBrowser.shutWindow();
    }

    @JavascriptInterface
    public void shutdownMinima() {

        //Close this WebView
        mMiniBrowser.shutdownMinima();
    }

    @JavascriptInterface
    public void showTitleBar() {

        mMiniBrowser.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Show the Title bar
                mMiniBrowser.showToolbar();
            }
        });
   }

    @JavascriptInterface
    public void openExternalBrowser(String zUrl, String zTarget) {

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

        //Create share Intent
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, zText);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        mMiniBrowser.startActivity(shareIntent);
    }

    @JavascriptInterface
    public void shareFile(String zFilePath, String zMimeType) {

        //Create the file..
        File backup = new File(zFilePath);

        //HACK
        //backup = SSLManager.getKeystoreFile();

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

        //Get the URi
        Uri backupuri = FileProvider.getUriForFile(mMiniBrowser,"com.minima.android.provider",backup);

        //Now share that file..
        Intent intentShareFile = new Intent(Intent.ACTION_SEND);
        intentShareFile.setType(zMimeType);
        intentShareFile.putExtra(Intent.EXTRA_STREAM, backupuri);
        intentShareFile.putExtra(Intent.EXTRA_SUBJECT,"Share file from Minima");
        //intentShareFile.putExtra(Intent.EXTRA_TEXT, "Share file");
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