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

public class ChainSyncActivity extends AppCompatActivity implements ServiceConnection {

    MinimaService mMinima;

    AutoCompleteTextView mSeedphrase;
    ArrayList<String> mWordListArray;

    TextView mFinalWordlist;

    TextView mWordCount;

    String mArchiveNode = "";

    Button mDelete;
    Button mComplete;

    //Loader while connecting to Minima
    ProgressDialog mLoader = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chainsync);

        Toolbar tb  = findViewById(R.id.archive_chainsync_toolbar);
        setSupportActionBar(tb);

        //Bind to the Minima Service..
        //Intent minimaintent = new Intent(this, MinimaService.class);
        //bindService(minimaintent, this, Context.BIND_AUTO_CREATE);
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

                mSeedphrase.setEnabled(false);
                mDelete.setEnabled(false);
                mComplete.setEnabled(false);

                Toast.makeText(ChainSyncActivity.this,"Resyncing.. please wait..", Toast.LENGTH_LONG).show();

                //Wait for Minima to fully start up..
                mLoader = new ProgressDialog(ChainSyncActivity.this);
                mLoader.setTitle("Syncing..");
                mLoader.setMessage("Please wait..");
                mLoader.setCanceledOnTouchOutside(false);
                mLoader.show();

                //Get the seedphrase
                String text = mFinalWordlist.getText().toString().trim();
                runArchiveSync(text);
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

    public void updateLoader(String zString){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(mLoader != null && mLoader.isShowing()){
                    mLoader.setMessage(zString);
                }
            }
        });
    }

    public void runArchiveSync(String zSeedPhrase){
        Runnable sync = new Runnable() {
            @Override
            public void run() {

                MinimaLogger.log("Starting Archive process..");

                try {

                    String result = null;
                    if (zSeedPhrase.equals("")) {
                        result = mMinima.getMinima().runMinimaCMD("archive action:resync host:" + mArchiveNode);
                    } else {
                        result = mMinima.getMinima().runMinimaCMD("archive action:resync host:" + mArchiveNode + " phrase:\"" + zSeedPhrase + "\"");
                    }

                    MinimaLogger.log("Ending Archive process.. ");

                    //Parse the result..
                    JSONObject json = (JSONObject) new JSONParser().parse(result);
                    if((boolean)json.get("status")){
                        //It worked!
                        //Close both
                        ChainSyncActivity.this.finishAffinity();
                        MainActivity.getMainActivity().shutdown();

                    }else{
                        ChainSyncActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mSeedphrase.setEnabled(true);
                                mDelete.setEnabled(true);
                                mComplete.setEnabled(true);

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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.archive_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String seedphrase   = "";
        String vault        = "";

        // Handle item selection
        switch (item.getItemId()) {
            case R.id.archive_reveal:

                vault = mMinima.getMinima().runMinimaCMD("vault");
                try {
                    JSONObject json = (JSONObject)new JSONParser().parse(vault);
                    JSONObject resp = (JSONObject)json.get("response");
                    seedphrase      = resp.getString("phrase");

                } catch (ParseException e) {
                    MinimaLogger.log("Error getting seed phrase..");
                    return true;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Seed Phrase");
                builder.setMessage(seedphrase);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.cancel();
                    }
                });

                builder.show();

                return true;

            case R.id.archive_share:

                vault = mMinima.getMinima().runMinimaCMD("vault");
                try {
                    JSONObject json = (JSONObject)new JSONParser().parse(vault);
                    JSONObject resp = (JSONObject)json.get("response");
                    seedphrase      = resp.getString("phrase");

                } catch (ParseException e) {
                    MinimaLogger.log("Error getting seed phrase..");
                    return true;
                }

                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, seedphrase);
                sendIntent.setType("text/plain");

                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        MinimaLogger.log("BIP39 CONNECTED TO SERVICE");
        MinimaService.MyBinder binder = (MinimaService.MyBinder)iBinder;
        mMinima = binder.getService();

        //Set the listener
//        mMinima.mArchiveListener = this;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        MinimaLogger.log("BIP39  DISCONNECTED TO SERVICE");
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
