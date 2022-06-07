package com.minima.android.ui.mds;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.minima.android.MainActivity;
import com.minima.android.R;
import com.minima.android.databinding.FragmentGalleryBinding;
import com.minima.android.ui.maxima.Contact;
import com.minima.android.ui.maxima.ContactAdapter;

import org.minima.Minima;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;
import org.minima.utils.json.parser.ParseException;

import java.io.File;
import java.util.ArrayList;

public class MDSFragment extends Fragment {

    MainActivity mMain;

    ListView mMainList;

    JSONObject[] mMDS;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_mds, container, false);

        mMainList = root.findViewById(R.id.mds_list);

        //Get the Main Activity
        mMain = (MainActivity)getActivity();

        //If it's Empty
        mMainList.setEmptyView(root.findViewById(R.id.mds_empty_list_item));

        FloatingActionButton fab = root.findViewById(R.id.fab_mds);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMain.openFile();
            }
        });

        updateMDSList();

        return root;
    }

    public void updateMDSList(){

        ArrayList<JSONObject> mds = new ArrayList<>();

        //Get Minima..
        Minima minima = mMain.getMinima();
        if(minima == null){
            Toast.makeText(mMain,"Minima not initialised yet", Toast.LENGTH_SHORT).show();
            return;
        }

        //Do the RPC call..
        String mdscommand = minima.runMinimaCMD("mds",false);

        //Convert JSON
        JSONObject json         = null;
        try {
            json = (JSONObject)new JSONParser().parse(mdscommand);
        } catch (ParseException e) {
            MinimaLogger.log(e);
            return;
        }

        JSONObject response     = (JSONObject)json.get("response");
        JSONArray allmdsjson    = (JSONArray) response.get("minidapps");

        //Convert these..
        for(Object obj : allmdsjson){
            JSONObject jconmds = (JSONObject)obj;
            mds.add(jconmds);
        }

        //Get the array list
        JSONObject[] allmds = mds.toArray(new JSONObject[0]);

        //Keep for later
        mMDS = allmds;

        //Create the custom arrayadapter
        MDSAdapter mdsadap = new MDSAdapter(mMain, R.layout.mds_view, allmds);
        mMainList.setAdapter(mdsadap);
    }
}