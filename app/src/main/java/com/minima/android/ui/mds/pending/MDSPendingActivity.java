package com.minima.android.ui.mds.pending;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mds_pending);

        Toolbar tb  = findViewById(R.id.pending_toolbar);
        setSupportActionBar(tb);

        mMainList = findViewById(R.id.mds_pending_list);

        //If it's Empty
        mMainList.setEmptyView(findViewById(R.id.mds_empty_list_item));

        //Bind to the Minima Service..
        Intent minimaintent = new Intent(this, MinimaService.class);
        bindService(minimaintent, this, Context.BIND_AUTO_CREATE);
    }

    public void updateList(){

        //Run the command
        String pendingstr = mMinima.getMinima().runMinimaCMD("mds action:pending",false);

        try {
            String fake = "{" +
                    "  \"command\":\"mds\"," +
                    "  \"params\":{" +
                    "    \"action\":\"pending\"" +
                    "  }," +
                    "  \"status\":true," +
                    "  \"pending\":false," +
                    "  \"response\":{" +
                    "    \"pending\":[{" +
                    "      \"uid\":\"0x192ED8B39A685B6F624BAFC4BE961241\"," +
                    "      \"minidapp\":{\n" +
                    "        \"uid\":\"0xF78972A572162F8BDF322EF8D9C6488B\"," +
                    "        \"conf\":{" +
                    "          \"name\":\"Terminal\"," +
                    "          \"icon\":\"terminal.png\"," +
                    "          \"version\":\"1.91\"," +
                    "          \"description\":\"Terminal CLI for Minima\"" +
                    "        }" +
                    "      }," +
                    "      \"command\":\"send amount:1 adress:0xFF\"" +
                    "    }," +
                    "    {" +
                    "      \"uid\":\"0x42EF1DE453183BEB87C7DCD32B33A94B\"," +
                    "      \"minidapp\":{" +
                    "        \"uid\":\"0xF78972A572162F8BDF322EF8D9C6488B\"," +
                    "        \"conf\":{" +
                    "          \"name\":\"Terminal\"," +
                    "          \"icon\":\"terminal.png\"," +
                    "          \"version\":\"1.91\"," +
                    "          \"description\":\"Terminal CLI for Minima\"" +
                    "        }" +
                    "      }," +
                    "      \"command\":\"send amount:2 adress:0xFF\"" +
                    "    }]" +
                    "  }" +
                    "}";

            JSONObject json     = (JSONObject)new JSONParser().parse(fake);
            JSONObject response = (JSONObject)json.get("response");
            JSONArray pending   = (JSONArray) response.get("pending");

            ArrayList<JSONObject> mdspending = new ArrayList<>();

            //Convert these..
            for(Object obj : pending){
                JSONObject jconmds = (JSONObject)obj;
                mdspending.add(jconmds);
            }

            //Get the array list
            JSONObject[] allmds = mdspending.toArray(new JSONObject[0]);

//            //Keep for later
//            mMDS = allmds;

            //Create the custom arrayadapter
            MDSPendingAdapter mdsadap = new MDSPendingAdapter(this, R.layout.mds_view, allmds);
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
