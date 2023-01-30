package com.minima.android.files;

import android.net.Uri;

import com.minima.android.MainActivity;
import com.minima.android.Utils;

import org.minima.utils.MiniFile;
import org.minima.utils.MinimaLogger;

import java.io.File;
import java.io.InputStream;

public class RestoreBackup implements Runnable {

    Uri mFileUri;
    String mPassword;
    MainActivity mMain;

    public RestoreBackup(Uri zFilePath,String zPassword, MainActivity zMain){
        mFileUri   = zFilePath;
        mPassword  = zPassword;
        mMain      = zMain;
    }

    @Override
    public void run() {
        //Get the Input Stream..
        try {
            InputStream is = mMain.getContentResolver().openInputStream(mFileUri);
            byte[] data = Utils.loadFile(is);

            //Get a file..
            File dapp = new File(mMain.getFilesDir(),"restore.bak");
            if(dapp.exists()){
                dapp.delete();
            }

            //Now save to da file..
            MiniFile.writeDataToFile(dapp,data);

            //Now restore from that
            String result = mMain.getMinima().runMinimaCMD("restore password:\""+mPassword+"\" file:\""+dapp.getAbsolutePath()+"\"",false);

            //Now delete the file..
            dapp.delete();

            //Now shut down..
            mMain.shutdown();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
