package com.minima.android.ui.mds;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.minima.android.R;

import org.minima.utils.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MDSAdapter extends ArrayAdapter<JSONObject> {

    Context mContext;

    JSONObject[] mMDS;

    public static final SimpleDateFormat DATEFORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH );

    public MDSAdapter(@NonNull Context zContext, int resource, @NonNull JSONObject[] objects) {
        super(zContext, resource, objects);
        mContext    = zContext;
        mMDS        = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.mds_view, null);
        }

        JSONObject mds = getItem(position);

        TextView name        = v.findViewById(R.id.mds_name);
        TextView description = v.findViewById(R.id.mds_description);

        name.setText(mds.getString("name"));
        description.setText(mds.getString("description"));

        //Now the image
        File rootfile  = mContext.getFilesDir();
        File mdsroot   = new File(rootfile,"mds");
        File webroot   = new File(mdsroot,"web");
        File dapproot  = new File(webroot,mds.getString("uid"));
        File image     = new File(dapproot,mds.getString("icon"));

        ImageView iv = v.findViewById(R.id.mds_image);

        if(image.exists()){
            Bitmap myBitmap = BitmapFactory.decodeFile(image.getAbsolutePath());
            iv.setImageBitmap(myBitmap);
        }

        return v;
    }
}
