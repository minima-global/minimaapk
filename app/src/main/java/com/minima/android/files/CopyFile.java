package com.minima.android.files;

import android.net.Uri;

import com.minima.android.MainActivity;
import com.minima.android.Utils;

import org.minima.utils.MiniFile;
import org.minima.utils.MinimaLogger;

import java.io.File;
import java.io.InputStream;

public class CopyFile implements Runnable {

    Uri mFileUri;
    String mFilename;
    MainActivity mMain;

    public CopyFile(Uri zFilePath,String zFilename, MainActivity zMain){
        mFileUri    = zFilePath;
        mFilename   = zFilename;
        mMain       = zMain;
    }

    @Override
    public void run() {
        //Get the Input Stream..
        try {
            InputStream is = mMain.getContentResolver().openInputStream(mFileUri);
            byte[] data     = Utils.loadFile(is);

            //Get a file..
            File dapp = new File(mMain.getFilesDir(),mFilename);
            if(dapp.exists()){
                dapp.delete();
            }

            //Now save to da file..
            MiniFile.writeDataToFile(dapp,data);

            if(mMain.mFileFragment != null){
                mMain.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mMain.mFileFragment.loadFiles();
                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
