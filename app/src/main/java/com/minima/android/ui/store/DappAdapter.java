package com.minima.android.ui.store;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.minima.android.R;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import org.minima.utils.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;

import jp.wasabeef.picasso.transformations.RoundedCornersTransformation;

public class DappAdapter extends ArrayAdapter<JSONObject> {

    Context mContext;

    JSONObject[] mStores;

    public static final SimpleDateFormat DATEFORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH );

    public DappAdapter(@NonNull Context zContext, int resource, @NonNull JSONObject[] objects) {
        super(zContext, resource, objects);
        mContext    = zContext;
        mStores     = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.store_view, null);
        }

        JSONObject store    = getItem(position);

        TextView name           = v.findViewById(R.id.mds_name);
        TextView description    = v.findViewById(R.id.mds_description);
        TextView version        = v.findViewById(R.id.mds_version);

        name.setText(store.getString("name"));
        description.setText(store.getString("description"));
        version.setText(store.getString("version"));

        //The Image..
        ImageView iv = v.findViewById(R.id.mds_image);

        final Transformation transformation = new RoundedCornersTransformation(20, 0);

        Picasso.get()
                .load(store.getString("icon"))
                .resize(200, 200)
                .transform(transformation)
                .centerCrop()
                .into(iv);

        return v;
    }
}
