package com.minima.android.ui.mds.pending;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.minima.android.R;
import com.minima.android.service.MinimaService;
import com.minima.android.ui.maxima.contacts.Contact;
import com.minima.android.ui.maxima.contacts.ContactAdapter;
import com.minima.android.ui.mds.MDSAdapter;

import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;
import org.minima.utils.json.parser.ParseException;

import java.util.ArrayList;
import java.util.Date;

public class MDSPendingActivity extends AppCompatActivity implements ServiceConnection {

    MinimaService mMinima;

    ListView mMainList;

    JSONObject[] mAllPending = new JSONObject[0];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mds_pending);

        Toolbar tb  = findViewById(R.id.pending_toolbar);
        setSupportActionBar(tb);

        mMainList = findViewById(R.id.mds_pending_list);

        //If it's Empty
        mMainList.setEmptyView(findViewById(R.id.mds_empty_list_item));

        //Register for Context menu
        registerForContextMenu(mMainList);

        //Bind to the Minima Service..
        Intent minimaintent = new Intent(this, MinimaService.class);
        bindService(minimaintent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add("Accept");
        menu.add("Deny");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        //Get menu item info
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        //Get the Pending Item..
        JSONObject pendingitem = mAllPending[info.position];

        //What is the UID
        String uid = pendingitem.getString("uid");

        boolean accept = false;
        if(item.getTitle().equals("Accept")){
            accept = true;
        }

        if(accept){
            new AlertDialog.Builder(this)
                    .setTitle("Accept Command")
                    .setMessage("Are you sure ?\n\nThis will run the command..\n\n"+pendingitem.getString("command"))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            acceptCommand(uid);
                        }})
                    .setNegativeButton(android.R.string.no, null).show();
        }else{

            //Delete this Command
            String commandstr = mMinima.getMinima().runMinimaCMD("mds action:deny uid:"+uid,true);

            //And reset the list
            updateList();
        }

        return true;
    }

    public void acceptCommand(String zUID){

        //Show a little message
        Toast.makeText(this, "Running command.. Please wait", Toast.LENGTH_SHORT).show();

        Runnable rr = new Runnable() {
            @Override
            public void run() {
                //Run the command..
                String commandstr = mMinima.getMinima().runMinimaCMD("mds action:accept uid:"+zUID,true);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // The TextView to show your Text
                        TextView showText = new TextView(MDSPendingActivity.this);
                        showText.setText(commandstr);
                        showText.setTextIsSelectable(true);

                        //Show the results
                        new AlertDialog.Builder(MDSPendingActivity.this)
                                .setTitle("Command Output")
                                .setView(showText)
//                                .setMessage(commandstr)
                                .setIcon(R.drawable.ic_minima)
                                .setCancelable(true)
                                .setNegativeButton("Close", null)
                                .show();

                        //And reset the list
                        updateList();
                    }
                });
            }
        };

        Thread runner = new Thread(rr);
        runner.start();
    }

    public void updateList(){

        //Run the command
        String pendingstr = mMinima.getMinima().runMinimaCMD("mds action:pending",false);

        try {

            JSONObject json     = (JSONObject)new JSONParser().parse(pendingstr);
            JSONObject response = (JSONObject)json.get("response");
            JSONArray pending   = (JSONArray) response.get("pending");

            ArrayList<JSONObject> mdspending = new ArrayList<>();

            //Convert these..
            for(Object obj : pending){
                JSONObject jconmds = (JSONObject)obj;
                mdspending.add(jconmds);
            }

            //Get the array list
            mAllPending = mdspending.toArray(new JSONObject[0]);

            //Create the custom arrayadapter
            MDSPendingAdapter mdsadap = new MDSPendingAdapter(this, R.layout.mds_view, mAllPending);
            mMainList.setAdapter(mdsadap);

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        MinimaService.MyBinder binder = (MinimaService.MyBinder)iBinder;
        mMinima = binder.getService();

        updateList();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mMinima = null;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(mMinima != null){
            unbindService(this);
        }
    }
}
