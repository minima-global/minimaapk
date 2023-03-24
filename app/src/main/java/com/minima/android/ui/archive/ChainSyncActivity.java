package com.minima.android.ui.archive;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.minima.android.MainActivity;
import com.minima.android.R;
import com.minima.android.service.MinimaService;

import org.minima.utils.BIP39;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;
import org.minima.utils.json.parser.ParseException;

import java.util.ArrayList;
import java.util.Arrays;

public class ChainSyncActivity extends AppCompatActivity implements ServiceConnection, ArchiveListener {

    MinimaService mMinima;

    String mArchiveNode = "";

    //Loader while connecting to Minima
    ProgressDialog mLoader = null;

    public static int updatecounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chainsync);

        Toolbar tb  = findViewById(R.id.archive_chainsync_toolbar);
        setSupportActionBar(tb);

        Button startarchive = findViewById(R.id.chainsync_start);
        startarchive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Start the process..
                showArchiveHostDialog();
            }
        });

        //Bind to the Minima Service..
        Intent minimaintent = new Intent(this, MinimaService.class);
        bindService(minimaintent, this, Context.BIND_AUTO_CREATE);
    }

    public void showArchiveHostDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Archive Node Host");

        // Set up the input
        final EditText input = new EditText(this);
        input.setText("auto");

        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mArchiveNode = input.getText().toString().trim();
                if(mArchiveNode.equals("")){
                    return;
                }

                Toast.makeText(ChainSyncActivity.this,"Resyncing.. please wait..", Toast.LENGTH_LONG).show();

                //Wait for Minima to fully start up..
                mLoader = new ProgressDialog(ChainSyncActivity.this);
                mLoader.setTitle("Syncing..");
                mLoader.setMessage("Please wait..");
                mLoader.setCanceledOnTouchOutside(false);
                mLoader.setCancelable(false);
                mLoader.show();

                //Get the seedphrase
                runArchiveSync();
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
    public void updateArchiveStatus(String zStatus) {
        updatecounter++;

        if(updatecounter % 10 == 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mLoader != null && mLoader.isShowing()) {
                        mLoader.setMessage(zStatus);
                    }
                }
            });
        }
    }

    public void runArchiveSync(){
        updatecounter = 0;

        Runnable sync = new Runnable() {
            @Override
            public void run() {

                MinimaLogger.log("Starting Archive process..");

                try {
                    String result = mMinima.getMinima().runMinimaCMD("archive action:resync host:" + mArchiveNode);

                    MinimaLogger.log("Archive : "+result);

                    MinimaLogger.log("Ending Archive process.. ");

                    //Parse the result..
                    JSONObject json = (JSONObject) new JSONParser().parse(result);
                    if((boolean)json.get("status")){
                        //Remove loader
                        mLoader.cancel();

                        //Close both
                        ChainSyncActivity.this.finishAffinity();
                        MainActivity.getMainActivity().shutdown();

                    }else{
                        ChainSyncActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //Remove loader
                                mLoader.cancel();

                                Toast.makeText(ChainSyncActivity.this, "Could not connect to host! "+mArchiveNode, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                }catch(Exception exc){
                    MinimaLogger.log("Error while processing Archive "+exc);
                }
            }
        };

        Thread tt = new Thread(sync);
        tt.start();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        MinimaLogger.log("CHAINSYNC CONNECTED TO SERVICE");
        MinimaService.MyBinder binder = (MinimaService.MyBinder)iBinder;
        mMinima = binder.getService();

        //Set the listener
        mMinima.mArchiveListener = this;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        MinimaLogger.log("CHAINSYNC  DISCONNECTED TO SERVICE");
        mMinima.mArchiveListener = null;
        mMinima = null;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(mMinima != null){
            unbindService(this);
        }

        if(mLoader != null && mLoader.isShowing()){
            mLoader.cancel();
        }
    }
}
