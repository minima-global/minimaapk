package com.minima.android.ui.store;

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

import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;
import org.minima.utils.json.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class StoreFragment extends Fragment {

    MainActivity mMain;

    ListView mMainList;

    Set<String> mPrefsStoreSet = null;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        View root = inflater.inflate(R.layout.fragment_store, container, false);

        //Get the Main Activity
        mMain = (MainActivity)getActivity();

        mMainList = root.findViewById(R.id.store_list);
        //mMainList.setEmptyView(root.findViewById(R.id.store_empty_list_item));

        //Click Listener..
        mMainList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int zPosition, long l) {

                //Get the Store..
                JSONObject store = mMain.getDappStores()[zPosition];

                //Is it valid..
                boolean errorload = (boolean)store.get("error_load");
                if(errorload){
                    Toast.makeText(mMain, "Invalid Store - Try Refreshing", Toast.LENGTH_SHORT).show();
                    return;
                }

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
                                JSONObject store = mMain.getDappStores()[zPosition];

                                //Which URL
                                String url = store.getString("store_url");

                                //Get the Prefs..
                                SharedPreferences prefs = mMain.getApplicationContext().getSharedPreferences("stores", 0);
                                Set<String> allstores = prefs.getStringSet("allstores", new HashSet<>());
                                allstores.remove(url);

                                //And save - Bug in Android
                                SharedPreferences.Editor edit = prefs.edit();
                                edit.remove("allstores");
                                edit.apply();
                                edit.putStringSet("allstores", allstores);
                                edit.apply();

                                Toast.makeText(mMain,"Store removed", Toast.LENGTH_SHORT).show();

                                loadStores(true);
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

        loadStores(false);

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
                SharedPreferences prefs = mMain.getApplicationContext().getSharedPreferences("stores", 0);
                Set<String> allstores   = prefs.getStringSet("allstores", new HashSet<>());

                //Add this store..
                allstores.add(store);

                //And save - Bug in Android..
                SharedPreferences.Editor edit = prefs.edit();
                edit.remove("allstores");
                edit.apply();
                edit.putStringSet("allstores", allstores);
                edit.apply();

                Toast.makeText(mMain,"Store added", Toast.LENGTH_SHORT).show();

                loadStores(true);
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
                loadStores(true);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //http://10.0.2.2/mysites/dappstore/dappstore.txt
    public void loadStores(boolean zForceRefresh){

        //Get the stores..
        SharedPreferences prefs = mMain.getApplicationContext().getSharedPreferences("stores", 0);
        mPrefsStoreSet = prefs.getStringSet("allstores", new HashSet<>());

        MinimaLogger.log("Store set size :"+ mPrefsStoreSet.size());
        if(mPrefsStoreSet.size() == 0){
            mPrefsStoreSet.add("https://raw.githubusercontent.com/minima-global/Minima/dev-spartacus/mds/store/dapps.json");
        }

        //Are we refreshing..
        if(mMain.getDappStores() == null || zForceRefresh) {
            //Little Message
            Toast.makeText(mMain,"Refreshing Stores",Toast.LENGTH_SHORT).show();
        }

        Runnable rr = new Runnable() {
            @Override
            public void run() {

                //Are we refreshing..
                if(mMain.getDappStores() == null || zForceRefresh) {

                    //The final list
                    ArrayList<JSONObject> jsonstores = new ArrayList<>();

                    //First load ther store..
                    Iterator<String> stores = mPrefsStoreSet.iterator();
                    while (stores.hasNext()) {
                        String storeurl = stores.next();

                        //Load it..
                        String storedata = null;
                        try {
                            storedata = sendGET(storeurl);

                            MinimaLogger.log("Store loaded : " + storeurl + " " + storedata);

                        } catch (IOException e) {
                            MinimaLogger.log("Could not download store : " + storeurl + " " + e);
                            storedata = "";
                        }

                        //Did it work..
                        if (!storedata.equals("")) {

                            //Convert to JSON
                            JSONObject jsonstore = null;
                            try {
                                jsonstore = (JSONObject) new JSONParser().parse(storedata);
                            } catch (ParseException e) {
                                MinimaLogger.log("Could not parse store : " + storedata + " " + e);
                            }

                            //Did it work..
                            if (jsonstore != null) {

                                //Add the URL
                                jsonstore.put("error_load", false);
                                jsonstore.put("store_url", storeurl);

                                jsonstores.add(jsonstore);
                            }
                        } else {
                            //Add a filed Store..
                            JSONObject failedstore = new JSONObject();
                            failedstore.put("error_load", true);
                            failedstore.put("store_url", storeurl);

                            failedstore.put("name", "Error Loading..");
                            failedstore.put("description", storeurl);
                            failedstore.put("icon", "");
                            failedstore.put("version", "");
                            jsonstores.add(failedstore);
                        }
                    }

                    //And set..
                    mMain.setDappStores(jsonstores.toArray(new JSONObject[0]));
                }

                //Load the List
                mMain.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        StoreAdapter stadap = new StoreAdapter(mMain, R.layout.mds_view, mMain.getDappStores());
                        mMainList.setAdapter(stadap);
                    }
                });
            }
        };

        Thread tt = new Thread(rr);
        tt.start();
    }

    private String sendGET(String zHost) throws IOException {
        URL obj = new URL(zHost);
        HttpURLConnection con = (HttpURLConnection)obj.openConnection();
        con.setConnectTimeout(5000);
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "MinimaAndroid/1.0");
        con.setRequestProperty("Connection", "close");

        int responseCode = con.getResponseCode();
        StringBuffer response = new StringBuffer();
        if (responseCode == 200) {
            InputStream is = con.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));

            String inputLine;
            while((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close();
            is.close();
        } else {
            System.out.println("GET request not HTTP_OK resp:" + responseCode + " @ " + zHost);
        }

        return response.toString();
    }
}