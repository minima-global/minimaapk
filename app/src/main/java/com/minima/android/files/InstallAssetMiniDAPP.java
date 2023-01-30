package com.minima.android.files;

import android.net.Uri;

import com.minima.android.MainActivity;
import com.minima.android.Utils;

import org.minima.utils.MiniFile;
import org.minima.utils.MinimaLogger;

import java.io.File;
import java.io.InputStream;

public class InstallAssetMiniDAPP implements Runnable {

    String mAsset;

    MainActivity mMain;

    public InstallAssetMiniDAPP(String zAssetName, MainActivity zMain){
        mAsset   = zAssetName;
        mMain    = zMain;
    }

    @Override
    public void run() {
        //Get the Input Stream..
        try {
            InputStream is = mMain.getAssets().open(mAsset);
            byte[] data = Utils.loadFile(is);

            //Get a file..
            File dapp = new File(mMain.getFilesDir(),"dapp.zip");
            if(dapp.exists()){
                dapp.delete();
            }

            //Now save to da file..
            MiniFile.writeDataToFile(dapp,data);

            //Now load that..
            String result = mMain.getMinima().runMinimaCMD("mds action:install file:\""+dapp.getAbsolutePath()+"\"",false);

            //Delete the MiniDAPP
            dapp.delete();

            if(mMain.mMDSFragment != null){
                mMain.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mMain.mMDSFragment.updateMDSList();
                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
