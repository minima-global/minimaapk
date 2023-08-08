package com.minima.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.minima.android.service.MinimaService;

import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;
import org.minima.utils.json.parser.ParseException;

public class PeersActivity extends AppCompatActivity implements ServiceConnection {

    /**
     * Main Minmia Service
     */
    MinimaService mMinima = null;

    TextView mMyPeers;

    EditText mEditPeers;

    boolean mHideMyPeers = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_peers);

        Toolbar mToolBar = findViewById(R.id.minidapp_toolbar);
        setSupportActionBar(mToolBar);

        setTitle("Minima Peers");

        mHideMyPeers = getIntent().getBooleanExtra("hidemypeers",false);

        TextView mpt = findViewById(R.id.peers_mypeerstext);
        mMyPeers = findViewById(R.id.peers_mypeers);

        if(mHideMyPeers){
            mpt.setVisibility(View.GONE);
            mMyPeers.setVisibility(View.GONE);
        }

        mEditPeers = findViewById(R.id.peers_editpeers);

        Button addpeersbut = findViewById(R.id.peers_addpeers);
        addpeersbut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addPeers();
            }
        });

        //Connect to the Minima Service..
        Intent minimaintent = new Intent(getBaseContext(), MinimaService.class);
        startForegroundService(minimaintent);
        bindService(minimaintent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(!mHideMyPeers){
            getMenuInflater().inflate(R.menu.consolemenu, menu);
            return true;
        }

        getMenuInflater().inflate(R.menu.closemenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.console_share:

                //Share the text
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Minima Peers");
                if(mHideMyPeers){
                    sendIntent.putExtra(Intent.EXTRA_TEXT,"");
                }else{
                    sendIntent.putExtra(Intent.EXTRA_TEXT, mMyPeers.getText());
                }
                sendIntent.setType("text/plain");

                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);

                return true;

            case R.id.menu_close:
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        MinimaService.MyBinder binder = (MinimaService.MyBinder)iBinder;
        mMinima = binder.getService();

        //Set the peers list
        if(!mHideMyPeers) {
            setPeersList();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mMinima = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Unbind from the service..
        if(mMinima != null) {
            unbindService(this);
        }
    }

    public void setPeersList(){
        //Run a command to get the peers
        String peersfunc = mMinima.getMinima().runMinimaCMD("peers max:10");

        try {
            JSONObject json     = (JSONObject) new JSONParser().parse(peersfunc);
            JSONObject resp     = (JSONObject) json.get("response");
            String peerslist   = resp.getString("peerslist").trim();

            if(peerslist.equals("")){
                mMyPeers.setText("No peers found..");
            }else{
                mMyPeers.setText(peerslist);
            }

        } catch (Exception e) {
            MinimaLogger.log(e);
        }
    }

    public void addPeers(){
        String peers = mEditPeers.getText().toString().trim();

        if(peers.equals("")){
            Toast.makeText(this, "No peers specified", Toast.LENGTH_SHORT).show();
            return;
        }

        //Run a command to add the peers
        String peersfunc = mMinima.getMinima().runMinimaCMD("peers action:addpeers peerslist:"+peers);

        try {
            JSONObject json     = (JSONObject) new JSONParser().parse(peersfunc);
            boolean status      = (boolean) json.get("status");

            if(status){
                //Shutdown..
                Toast.makeText(this, "Peers added to your node!", Toast.LENGTH_SHORT).show();

                //Shut down this activity
                finish();

            }else{
                //Shutdown..
                Toast.makeText(this, json.getString("Error running command.."), Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            MinimaLogger.log(e);
        }
    }
}