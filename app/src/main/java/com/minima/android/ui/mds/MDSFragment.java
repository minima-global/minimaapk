package com.minima.android.ui.mds;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.minima.android.MainActivity;
import com.minima.android.R;
import com.minima.android.ui.mds.pending.MDSPendingActivity;

import org.minima.Minima;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;
import org.minima.utils.json.parser.ParseException;

import java.util.ArrayList;

public class MDSFragment extends Fragment {

    MainActivity mMain;

    ListView mMainList;

    JSONObject[] mMDS;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        View root = inflater.inflate(R.layout.fragment_mds, container, false);

        mMainList = root.findViewById(R.id.mds_list);

        //Get the Main Activity
        mMain = (MainActivity)getActivity();
        mMain.mMDSFragment = this;

        //If it's Empty
        mMainList.setEmptyView(root.findViewById(R.id.mds_empty_list_item));

        mMainList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int zPosition, long l) {

                //Get the Selected MiniDAPP
                JSONObject mds  = mMDS[zPosition];
                JSONObject conf = (JSONObject) mds.get("conf");

                //The MiniDAPP UID
                String uid = mds.getString("uid");

                //Get the sessionid
                String sessionid = mMain.getMinima().getMain().getMDSManager().convertMiniDAPPID(uid);

                //Is this an internal or external MiniDAPP..
                String browsertype = conf.getString("browser","internal");

               if(browsertype.equals("internal")){

                   //Open internally..
                   Intent intent = new Intent(mMain, MDSBrowser.class);
                   intent.putExtra("name", conf.getString("name"));
                   intent.putExtra("uid", uid);
                   startActivity(intent);

               }else{

                   //The URL page
                   Uri minipage = Uri.parse("https://127.0.0.1:9003/"+uid+"/index.html?uid="+sessionid);

                   //Start the browser
                   Intent browser = new Intent(Intent.ACTION_VIEW);
                   browser.putExtra(Browser.EXTRA_APPLICATION_ID, "MiniDAPP_"+uid);
                   browser.setData(minipage);
                   startActivity(browser);
               }
            }
        });

        //Delete Apps..
        mMainList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int zPosition, long l) {

                new AlertDialog.Builder(mMain)
                        .setTitle("Delete MiniDAPP")
                        .setMessage("Are you sure ?\n\nThis will remove all data..")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //Get the Selected MiniDAPP
                                JSONObject mds = mMDS[zPosition];

                                String uid = mds.getString("uid");

                                deleteMiniDAPP(uid);
                            }})
                        .setNegativeButton(android.R.string.no, null).show();

                return true;
            }
        });

        FloatingActionButton fab = root.findViewById(R.id.fab_mds);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMain.openFile(MainActivity.REQUEST_INSTALLMINI);
            }
        });

        updateMDSList();

        return root;
    }

    public void deleteMiniDAPP(String zUID){
        Runnable delete = new Runnable() {
            @Override
            public void run() {
                //Delete the app
                mMain.getMinima().runMinimaCMD("mds action:uninstall uid:"+zUID);

                //Update the UI..
                mMain.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateMDSList();
                    }
                });
            }
        };

        Thread tt = new Thread(delete);
        tt.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMain.mMDSFragment = null;
    }

    public void updateMDSList(){

        ArrayList<JSONObject> mds = new ArrayList<>();

        //Get Minima..
        Minima minima = mMain.getMinima();
        if(minima == null){
            //Toast.makeText(mMain,"Minima not initialised yet", Toast.LENGTH_SHORT).show();
            return;
        }

        //Do the RPC call..
        String mdscommand = minima.runMinimaCMD("mds",false);

        //Convert JSON
        JSONObject json         = null;
        try {
            json = (JSONObject)new JSONParser().parse(mdscommand);
        } catch (ParseException e) {
            Toast.makeText(mMain,"Error parsing mds function", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.mds, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {

            case R.id.action_mds_pending:

                //Show your details
                Intent intent = new Intent(mMain, MDSPendingActivity.class);
                startActivity(intent);

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}