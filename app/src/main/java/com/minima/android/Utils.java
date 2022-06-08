package com.minima.android;

import org.minima.utils.MinimaLogger;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class Utils {

    public static byte[] loadFile(InputStream zInput){

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int l,total=0;
        try{
            while( (l = zInput.read(data,0,1024))!=-1){
                total += l;
                baos.write(data,0,l);
            }

            MinimaLogger.log("File read size "+total);
        }catch(Exception exc){
            MinimaLogger.log("Load file : "+exc);
        }

        return baos.toByteArray();
    }
}
