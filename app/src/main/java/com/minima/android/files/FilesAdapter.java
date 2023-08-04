package com.minima.android.files;

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

import org.minima.system.params.GlobalParams;
import org.minima.utils.MiniFormat;
import org.minima.utils.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FilesAdapter extends ArrayAdapter<JSONObject> {

    Context mContext;

    JSONObject[] mFiles;

    public static final SimpleDateFormat DATEFORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH );

    public FilesAdapter(@NonNull Context zContext, int resource, @NonNull JSONObject[] objects) {
        super(zContext, resource, objects);
        mContext        = zContext;
        mFiles          = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.files_view, null);
        }

        JSONObject file  = getItem(position);
        String fname     = (String) file.get("name");

        TextView name           = v.findViewById(R.id.mds_name);
        TextView description    = v.findViewById(R.id.mds_description);
        TextView version        = v.findViewById(R.id.mds_version);
        TextView time           = v.findViewById(R.id.mds_time);

        ImageView iv = v.findViewById(R.id.mds_image);

        if(!(boolean)file.get("isdir")){
            long filesize = (long) file.get("size");
            version.setText(MiniFormat.formatSize(filesize));
            iv.setImageResource(R.drawable.file_icon);

            long filetime = (long) file.get("time");
            time.setText(DATEFORMAT.format(new Date(filetime)));
        }else{
            version.setText("");
            iv.setImageResource(R.drawable.folder_icon);
        }

        name.setText(fname);

        return v;
    }
}