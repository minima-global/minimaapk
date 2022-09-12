package com.minima.android.ui.backup;

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
import com.minima.android.ui.maxima.contacts.Contact;
import com.minima.android.ui.maxima.contacts.ContactAdapter;

import org.minima.utils.BIP39;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;
import org.minima.utils.json.parser.ParseException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class Bip39Activity extends AppCompatActivity implements ServiceConnection {

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

        setContentView(R.layout.activity_seedkeyboard);

        Toolbar tb  = findViewById(R.id.seed_toolbar);
        setSupportActionBar(tb);

        //Use a special dictionary
        mSeedphrase = findViewById(R.id.seed_entry);
        mWordListArray = new ArrayList<>(Arrays.asList(BIP39.WORD_LIST));
        String[] bip39 = BIP39.WORD_LIST;

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, bip39);
        mSeedphrase.setAdapter(adapter);
        mSeedphrase.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                String word = mSeedphrase.getText().toString().trim().toLowerCase();
                MinimaLogger.log("Word : "+word);
                if(mWordListArray.contains(word)){

                    //Remove and add to our Main List!
                    mFinalWordlist.append(word.toUpperCase()+" ");

                    //And clear again
                    mSeedphrase.setText("");

                    //Set the wordcount
                    String fwords = mFinalWordlist.getText().toString().trim();
                    int words = fwords.split("\\s+").length;
                    mWordCount.setText("Word Count : "+words+" / 24");
                }
            }
        });

        mFinalWordlist  = findViewById(R.id.seed_wordlist);

        mWordCount      = findViewById(R.id.seed_wordcount);
        mWordCount.setText("Word Count : 0 / 24");

        mDelete = findViewById(R.id.seed_delete);
        mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = mFinalWordlist.getText().toString().trim();
                int index   = text.lastIndexOf(" ");
                if(index != -1){
                    String deltext = text.substring(0,index).trim();
                    mFinalWordlist.setText(deltext);
                    int words = deltext.split("\\s+").length;
                    mWordCount.setText("Word Count : "+words+" / 24");
                }else{
                    mFinalWordlist.setText("");
                    mWordCount.setText("Word Count : 0 / 24");
                }
            }
        });

        mComplete = findViewById(R.id.seed_phrasecomplete);
        mComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String fwords = mFinalWordlist.getText().toString().trim();
                int words     = 0;
                if(fwords.equals("")){
                    words     = 0;
                }else{
                    words     = fwords.split("\\s+").length;
                }

                if(words!=0 && words!=24){
                    Toast.makeText(Bip39Activity.this,"Seed phrase MUST either be blank OR contain 24 words..", Toast.LENGTH_LONG).show();
                    return;
                }

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

                Toast.makeText(Bip39Activity.this,"Resyncing.. please wait..", Toast.LENGTH_LONG).show();

                //Wait for Minima to fully start up..
                mLoader = new ProgressDialog(Bip39Activity.this);
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
                        Bip39Activity.this.finishAffinity();
                        MainActivity.getMainActivity().shutdown();

                    }else{
                        Bip39Activity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mSeedphrase.setEnabled(true);
                                mDelete.setEnabled(true);
                                mComplete.setEnabled(true);

                                mLoader.cancel();

                                Toast.makeText(Bip39Activity.this, "Could not connect to host! "+mArchiveNode, Toast.LENGTH_SHORT).show();
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
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.archive_share:

                String seedphrase = "";
                String vault = mMinima.getMinima().runMinimaCMD("vault");
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
        mMinima.mArchiveListener = this;
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
