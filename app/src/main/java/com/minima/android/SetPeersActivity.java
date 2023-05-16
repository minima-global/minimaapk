package com.minima.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.minima.android.service.MinimaService;

import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;
import org.minima.utils.json.parser.ParseException;

public class SetPeersActivity extends AppCompatActivity implements ServiceConnection{

    MinimaService mMinima;

    EditText mPeersInput;

    boolean mFromBoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set the View..
        setContentView(R.layout.activity_setpeers);

        //Is this from Boot
        mFromBoot = getIntent().getBooleanExtra("FROMBOOT",true);

        Toolbar tb  = findViewById(R.id.network_toolbar);
        setSupportActionBar(tb);

        //Bind to the Minima Service..
        Intent minimaintent = new Intent(this, MinimaService.class);
        bindService(minimaintent, this, Context.BIND_AUTO_CREATE);

        mPeersInput = findViewById(R.id.peers_input);

        Button btn = (Button) findViewById(R.id.peers_enter);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Get the list
                String peers = mPeersInput.getText().toString().trim();

                if(!peers.equals("")) {
                    //Import these peers..
                    String addpeer = mMinima.getMinima().runMinimaCMD("peers action:addpeers peerslist:" + peers, false);
                    MinimaLogger.log(addpeer);

                    try {
                        JSONObject jsonpeers = (JSONObject) new JSONParser().parse(addpeer);
                        if(!(boolean)jsonpeers.get("status")){
                            Toast.makeText(SetPeersActivity.this, "Invalid format!", Toast.LENGTH_SHORT).show();
                            //mPeersInput.setText("");
                            return;
                        }else{
                            Toast.makeText(SetPeersActivity.this, "Peers Imported - pls wait..", Toast.LENGTH_LONG).show();
                        }
                    } catch (ParseException e) {}

                }else{
                    Toast.makeText(SetPeersActivity.this, "NO Peers Imported", Toast.LENGTH_SHORT).show();
                }

                if(mFromBoot) {
                    SharedPreferences.Editor sharedPreferencesEditor = PreferenceManager.getDefaultSharedPreferences(SetPeersActivity.this).edit();
                    sharedPreferencesEditor.putBoolean("FIRST_RUN", false);
                    sharedPreferencesEditor.apply();

                    Intent intent = new Intent(SetPeersActivity.this, MainActivity.class);
                    view.getContext().startActivity(intent);
                }

                //Close the old app..
                finish();
            }
        });
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        MinimaService.MyBinder binder = (MinimaService.MyBinder)iBinder;
        mMinima = binder.getService();

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
