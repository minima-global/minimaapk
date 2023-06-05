package com.minima.android.mdshub;

import android.content.Context;
import android.os.Environment;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import org.minima.objects.base.MiniData;
import org.minima.utils.MiniFile;
import org.minima.utils.MinimaLogger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;

public class MiniBrowserBlobDownload {

    private Context mContext;

    public MiniBrowserBlobDownload(Context zContext){
        mContext = zContext;
    }

    @JavascriptInterface
    public void blobDownload(String zMdsfile, String zHexData){

        //Create a MiniData object
        MiniData data = new MiniData(zHexData);

        //Save to Downloads..
        File downloads      = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File mindownloads   = new File(downloads,"Minima");

        //And save..
        String filename = null;
        try {
            //Create the file..
            filename = Paths.get(new URI(zMdsfile).getPath()).getFileName().toString();

            //Now create the full file..
            File fullfile = new File(mindownloads,filename);

            //Write data to file..
            MiniFile.writeDataToFile(fullfile,data.getBytes());

        } catch (Exception e) {
            MinimaLogger.log(e);

            //Small message
            Toast.makeText(mContext,"Error saving file..",Toast.LENGTH_SHORT).show();

            return;
        }

        //Small message
        Toast.makeText(mContext,"File saved : "+filename,Toast.LENGTH_SHORT).show();
    }
}
