package com.minima.android.ui.store;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
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
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.minima.android.MainActivity;
import com.minima.android.R;
import com.minima.android.ui.mds.MDSAdapter;
import com.minima.android.ui.mds.MDSBrowser;

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
import java.util.Set;

public class StoreFragment extends Fragment {

    MainActivity mMain;

    ListView mMainList;

    JSONObject[] mAllStores;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        View root = inflater.inflate(R.layout.fragment_store, container, false);

        //Get the Main Activity
        mMain = (MainActivity)getActivity();

        mMainList = root.findViewById(R.id.store_list);
        mMainList.setEmptyView(root.findViewById(R.id.store_empty_list_item));

        //Click Listener..
        mMainList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int zPosition, long l) {

                //Get the Store..
                JSONObject store = mAllStores[zPosition];

                //Convert to String..
                String jsonstr  = store.toString();

                //Open internally..
                Intent intent = new Intent(mMain, StoreBrowser.class);
                intent.putExtra("store", jsonstr);
                intent.putExtra("store_url", store.getString("store_url"));
                startActivity(intent);
            }
        });

        //Delete Apps..
        mMainList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int zPosition, long l) {

                new AlertDialog.Builder(mMain)
                        .setTitle("Delete Store")
                        .setMessage("Are you sure you wish to remove this store ?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //Get the Selected MiniDAPP
                                JSONObject store = mAllStores[zPosition];

                                //Which URL
                                String url = store.getString("store_url");

                                //Get the Prefs..
                                SharedPreferences prefs = mMain.getPreferences(Context.MODE_PRIVATE);
                                Set<String> allstores = prefs.getStringSet("allstores", new HashSet<>());
                                allstores.remove(url);

                                //And save
                                SharedPreferences.Editor edit = prefs.edit();
                                edit.putStringSet("allstores", allstores);
                                edit.apply();

                                Toast.makeText(mMain,"Store removed", Toast.LENGTH_SHORT).show();

                                loadStores();
                            }})
                        .setNegativeButton(android.R.string.no, null).show();

                return true;
            }
        });

        FloatingActionButton fab = root.findViewById(R.id.fab_store);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addStore();
            }
        });

        loadStores();

        return root;
    }

    public void addStore(){
        AlertDialog.Builder builder = new AlertDialog.Builder(mMain);
        builder.setTitle("Add MiniDAPP Store");

        // Set up the input
        final EditText input = new EditText(mMain);

        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Get the store..
                String store = input.getText().toString().trim();

                //Get the Prefs..
                SharedPreferences prefs = mMain.getPreferences(Context.MODE_PRIVATE);
                Set<String> allstores = prefs.getStringSet("allstores", new HashSet<>());

                //Add this store..
                allstores.add(store);

                //And save
                SharedPreferences.Editor edit = prefs.edit();
                edit.putStringSet("allstores", allstores);
                edit.apply();

                Toast.makeText(mMain,"Store added", Toast.LENGTH_SHORT).show();

                loadStores();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
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

    //http://10.0.2.2/mysites/dappstore/dappstore.txt
    Set<String> mStoreSet = null;
    public void loadStores(){

        //Get the stores..
        SharedPreferences prefs = mMain.getPreferences(Context.MODE_PRIVATE);
        mStoreSet = prefs.getStringSet("allstores", new HashSet<>());

        Runnable rr = new Runnable() {
            @Override
            public void run() {

                //The final list
                ArrayList<JSONObject> jsonstores = new ArrayList<>();

                //First load ther store..
                Iterator<String> stores = mStoreSet.iterator();
                while(stores.hasNext()){
                    String storeurl = stores.next();

                    //Load it..
                    String storedata = null;
                    try {
                        storedata = RPCClient.sendGET(storeurl);
                    } catch (IOException e) {
                        MinimaLogger.log("Could not download store : "+storeurl+" "+e);
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

                            //Add the URL
                            jsonstore.put("store_url", storeurl);

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