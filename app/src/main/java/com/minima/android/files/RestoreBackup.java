package com.minima.android.files;

import android.net.Uri;
import android.widget.Toast;

import com.minima.android.MainActivity;
import com.minima.android.Utils;

import org.minima.utils.MiniFile;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;

import java.io.File;
import java.io.InputStream;

public class RestoreBackup implements Runnable {

    Uri mFileUri;
    String mPassword;
    MainActivity mMain;

    public RestoreBackup(Uri zFilePath,String zPassword, MainActivity zMain){
        mFileUri   = zFilePath;
        mPassword  = new String(zPassword);
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
            String result = null;
            if(mPassword.trim().equals("")){
                result = mMain.getMinima().runMinimaCMD("restore file:\""+dapp.getAbsolutePath()+"\"",false);
            }else{
                result = mMain.getMinima().runMinimaCMD("restore password:\""+mPassword+"\" file:\""+dapp.getAbsolutePath()+"\"",false);
            }

            //Now delete the file..
            dapp.delete();

            JSONObject res = (JSONObject) new JSONParser().parse(result);
            boolean status = (boolean)res.get("status");
            if(!status){
                //Show a Toast
                mMain.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mMain,"Invalid Password!",Toast.LENGTH_LONG).show();
                    }
                });
            }else{
                //Now shut down..
                mMain.shutdown();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
