package com.minima.android.browser;

import org.minima.utils.MiniFile;
import org.minima.utils.MinimaLogger;

import java.io.File;
import java.io.IOException;

public class FileCopy implements Runnable {

    File mOrig;
    File mCopy;

    public FileCopy(File zOrig, File zCopy){
        mOrig = zOrig;
        mCopy = zCopy;
    }

    @Override
    public void run() {
        try {
            //MinimaLogger.log("Start Copy..");
            MiniFile.copyFile(mOrig,mCopy);
            //MinimaLogger.log("End Copy..");
        } catch (Exception e) {
            MinimaLogger.log(e);
        }
    }
}
