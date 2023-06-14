package com.minima.android.ui.mds;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.minima.android.MainActivity;
import com.minima.android.R;
import com.minima.android.mdshub.MiniBrowser;
import com.minima.android.ui.mds.pending.MDSPendingActivity;

import org.bouncycastle.pqc.crypto.rainbow.Layer;
import org.minima.Minima;
import org.minima.system.params.GlobalParams;
import org.minima.utils.MiniFormat;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;
import org.minima.utils.json.parser.ParseException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MDSFragment extends Fragment {

    MainActivity mMain;

    ListView mMainList;

    JSONObject[] mMDS;

    boolean mMinimaReady = false;
    TextView mBadgeCount = null;

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

                   //Open the new Browwer
                   String minipage = "https://127.0.0.1:9003/"+uid+"/index.html?uid="+sessionid;
                   Intent intent = new Intent(mMain, MiniBrowser.class);
                   intent.putExtra("url",minipage);
                   startActivity(intent);

//                   //Open internally..
//                   Intent intent = new Intent(mMain, MDSBrowser.class);
//                   intent.putExtra("name", conf.getString("name"));
//                   intent.putExtra("uid", uid);
//                   startActivity(intent);

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

        //Register for Context menu
        registerForContextMenu(mMainList);

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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add("Details");

        menu.add("Update");

        SubMenu sub =  menu.addSubMenu("Permission");
            sub.add("READ");
            sub.add("WRITE");

        menu.add("Delete");

//        menu.add("NOTIFY");
//        menu.add("CANCEL");
    }

    int mPreviousPos=0;

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        //Get menu item info
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if(info != null){
            mPreviousPos = info.position;
        }

        //Is it a simple delete
        if(item.getTitle().equals("Details")){

            new AlertDialog.Builder(mMain)
                    .setTitle("MiniDAPP")
                    .setMessage(MiniFormat.JSONPretty(mMDS[mPreviousPos]))
                    .setIcon(R.drawable.ic_minima)
                    .setNegativeButton("Close", null)
                    .show();

        }else if(item.getTitle().equals("Update")){
            //Get the MiniDAPP
            JSONObject mindapp = mMDS[mPreviousPos];

            //What is the UID
            String uid = mindapp.getString("uid");

            //Open a file and Update
            mMain.openFile(uid, MainActivity.REQUEST_UPDATEMINI);

        }else if(item.getTitle().equals("Delete")){

            new AlertDialog.Builder(mMain)
                        .setTitle("Delete MiniDAPP")
                        .setMessage("Are you sure ?\n\nThis will remove all data..")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //Get the MiniDAPP
                                JSONObject mindapp = mMDS[mPreviousPos];

                                //What is the UID
                                String uid = mindapp.getString("uid");

                                //Get the Selected MiniDAPP
                                deleteMiniDAPP(uid);
                            }})
                        .setNegativeButton(android.R.string.no, null).show();

        }else if(item.getTitle().equals("READ") || item.getTitle().equals("WRITE")){

            String trust = "read";
            if(item.getTitle().equals("WRITE")){
                trust = "write";
            }

            //Get the MiniDAPP
            JSONObject mindapp = mMDS[mPreviousPos];

            //What is the UID
            String uid = mindapp.getString("uid");

            //Run a simple function to update the trust level
            String commandstr = mMain.getMinima().runMinimaCMD("mds action:permission uid:"+uid+" trust:"+trust,false);

            //And set it here
            JSONObject conf = (JSONObject) mMDS[mPreviousPos].get("conf");
            conf.put("permission",trust);

            Toast.makeText(mMain, "Permissions updated to "+trust, Toast.LENGTH_SHORT).show();

        }else if(item.getTitle().equals("NOTIFY")){

            //Get the MiniDAPP
            JSONObject mds  = mMDS[mPreviousPos];
            JSONObject conf = (JSONObject) mds.get("conf");

            //What is the UID
            String uid  = mds.getString("uid");
            String name = conf.getString("name");

            //Test Notification
            mMain.getMinimaService().createMiniDAPPNotification(uid, name,"the text");

        }else if(item.getTitle().equals("CANCEL")){
            //Get the MiniDAPP
            JSONObject mds  = mMDS[mPreviousPos];
            String uid      = mds.getString("uid");

            mMain.getMinimaService().cancelNotification(uid);
        }

        return true;
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

    @Override
    public void onResume() {
        super.onResume();

        setBadgeCount();
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

        //Sort Alphabetically..
        Collections.sort(allmdsjson, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject o1, JSONObject o2) {
                JSONObject conf1 = (JSONObject) o1.get("conf");
                JSONObject conf2 = (JSONObject) o2.get("conf");

                return conf1.getString("name").compareTo(conf2.getString("name"));
            }
        });

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

        mMinimaReady = true;

        setBadgeCount();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.mds, menu);

        View count          = menu.findItem(R.id.action_mds_pending).getActionView();
        FrameLayout fram    = count.findViewById(R.id.mds_pending_badge);
        fram.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mMain, MDSPendingActivity.class);
                startActivity(intent);
            }
        });

        mBadgeCount = count.findViewById(R.id.badge_count);
        setBadgeCount();

        super.onCreateOptionsMenu(menu, inflater);
    }

    public void setBadgeCount(){

        if(!mMinimaReady || mBadgeCount==null){
            return;
        }

        //Get the number of pending..
        String pendingstr = mMain.getMinima().runMinimaCMD("mds action:pending", false);

        try {
            JSONObject json     = (JSONObject) new JSONParser().parse(pendingstr);
            JSONObject response = (JSONObject) json.get("response");
            JSONArray pending   = (JSONArray) response.get("pending");

            int num = pending.size();

            if(num == 0){
                mBadgeCount.setVisibility(View.GONE);
            }else{
                mBadgeCount.setVisibility(View.VISIBLE);
            }

            mBadgeCount.setText(""+num);

        }catch(Exception exc) {
            MinimaLogger.log(exc);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {

//            case R.id.action_mds_pending:
//                Intent intent = new Intent(mMain, MDSPendingActivity.class);
//                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}