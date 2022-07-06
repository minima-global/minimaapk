package com.minima.android.ui.store;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.minima.android.MainActivity;
import com.minima.android.R;
import com.minima.android.ui.mds.MDSAdapter;

import org.minima.utils.MinimaLogger;
import org.minima.utils.RPCClient;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;
import org.minima.utils.json.parser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;

public class StoreFragment extends Fragment {

    MainActivity mMain;

    ListView mMainList;

    JSONObject[] mAllStores;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        View root = inflater.inflate(R.layout.fragment_store, container, false);

        mMainList = root.findViewById(R.id.store_list);

        //Get the Main Activity
        mMain = (MainActivity)getActivity();

        FloatingActionButton fab = root.findViewById(R.id.fab_store);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mMain, "HELLO!", Toast.LENGTH_SHORT).show();
            }
        });

        loadStores();

        return root;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.store, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {

            case R.id.action_store_refresh:
                loadStores();
                Toast.makeText(mMain,"Reloading Stores",Toast.LENGTH_SHORT).show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    HashSet<String> allstores = new HashSet<>();
    public void loadStores(){

        //Load the list from the Preferences..
        allstores.clear();
        allstores.add("http://10.0.2.2/mysites/dappstore/dappstore.txt");

        Runnable rr = new Runnable() {
            @Override
            public void run() {

                //The final list
                ArrayList<JSONObject> jsonstores = new ArrayList<>();

                //First load ther store..
                Iterator<String> stores = allstores.iterator();
                while(stores.hasNext()){
                    String store = stores.next();

                    //Load it..
                    String storedata = null;
                    try {
                        storedata = RPCClient.sendGET(store);
                    } catch (IOException e) {
                        MinimaLogger.log("Could not download store : "+store+" "+e);
                        storedata = "";
                    }

                    //Did it work..
                    if(!storedata.equals("")){

                        //Convert to JSON
                        JSONObject jsonstore = null;
                        try {
                            jsonstore = (JSONObject) new JSONParser().parse(storedata);
                        } catch (ParseException e) {
                            MinimaLogger.log("Could not parse store : "+storedata+" "+e);
                        }

                        //Did it work..
                        if(jsonstore != null){
                            jsonstores.add(jsonstore);
                        }
                    }
               }

                //Convert to a JSON array
                mAllStores = jsonstores.toArray(new JSONObject[0]);

                mMain.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        StoreAdapter stadap = new StoreAdapter(mMain, R.layout.mds_view, mAllStores);
                        mMainList.setAdapter(stadap);
                    }
                });
            }
        };

        Thread tt = new Thread(rr);
        tt.start();
    }

}