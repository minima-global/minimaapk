package com.minima.android.browser;

import android.os.Environment;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import org.minima.objects.base.MiniData;
import org.minima.utils.MiniFile;
import org.minima.utils.MinimaLogger;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;

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

        MinimaLogger.log("MiniBrowser JS - Close Window");

        //Close this WebView
        mMiniBrowser.shutWindow();
    }

    @JavascriptInterface
    public void shutdownMinima() {

        MinimaLogger.log("MiniBrowser JS - ShutDown Minima");

        //Close this WebView
        mMiniBrowser.finishAffinity();
    }

    @JavascriptInterface
    public void showTitleBar() {

        MinimaLogger.log("MiniBrowser JS - Show Title Bar");

        mMiniBrowser.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Show the Title bar
                mMiniBrowser.showToolbar();
            }
        });

    }
}
