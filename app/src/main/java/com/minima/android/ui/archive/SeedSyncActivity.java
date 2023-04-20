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
import android.view.inputmethod.EditorInfo;
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

import org.minima.database.wallet.Wallet;
import org.minima.system.Main;
import org.minima.utils.BIP39;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;
import org.minima.utils.json.parser.ParseException;

import java.util.ArrayList;
import java.util.Arrays;

public class SeedSyncActivity extends AppCompatActivity implements ServiceConnection, ArchiveListener {

    MinimaService mMinima;

    AutoCompleteTextView mSeedphrase;
    ArrayList<String> mWordListArray;

    TextView mFinalWordlist;

    TextView mWordCount;

    String mArchiveNode = "";

    Button mDelete;
    Button mComplete;

    EditText mMaxKeys;
    int mNumberKeys = 0;

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

        mMaxKeys = findViewById(R.id.seed_maxkeys);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, bip39);
        mSeedphrase.setAdapter(adapter);
        mSeedphrase.setMaxLines(1);

        mSeedphrase.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    checkSeedWord();
                    return true;
                }
                return false;
            }
        });

//        mSeedphrase.setOnKeyListener(new View.OnKeyListener() {
//            @Override
//            public boolean onKey(View view, int i, KeyEvent keyEvent) {
//                int key     = keyEvent.getKeyCode();
//                int action  = keyEvent.getAction();
//
//                if(key == KeyEvent.KEYCODE_ENTER){
//                    if(action==KeyEvent.ACTION_DOWN){
//                        checkSeedWord();
//                    }
//                    return  true;
//                }
//
//                return false;
//            }
//        });

//        mSeedphrase.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//                checkSeedWord(true);
//            }
//        });

        mFinalWordlist  = findViewById(R.id.seed_wordlist);
        mFinalWordlist.setText("");

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

                if(words!=24){
                    Toast.makeText(SeedSyncActivity.this,"Seed phrase MUST contain 24 words..", Toast.LENGTH_LONG).show();
                    return;
                }

                showArchiveHostDialog();
            }
        });

        //Bind to the Minima Service..
        Intent minimaintent = new Intent(this, MinimaService.class);
        bindService(minimaintent, this, Context.BIND_AUTO_CREATE);
    }

    public void checkSeedWord(){
        String word = mSeedphrase.getText().toString().trim().toLowerCase();

        //remove whitespace
        word = word.replaceAll("\\s+","");

        //Check at least 4 characters
        if(word.length()<3){
            return;
        }

        if(mWordListArray.contains(word)){

            //Add to our Main List!
            String text = mFinalWordlist.getText().toString();
            if(text.equals("")){
                mFinalWordlist.append(word.toUpperCase()+" ");

            }else if(text.endsWith(" ")){
                mFinalWordlist.append(word.toUpperCase()+" ");

            }else{
                mFinalWordlist.append(" "+word.toUpperCase()+" ");
            }

            //And clear again
            mSeedphrase.setText("");

            //Set the wordcount
            String fwords = mFinalWordlist.getText().toString().trim();
            int words = fwords.split("\\s+").length;
            mWordCount.setText("Word Count : "+words+" / 24");
        }
    }

    public void showArchiveHostDialog(){

        //Are all the keys created..?
        if(!Main.getInstance().getAllKeysCreated()){
            String current = "Currently ("+Main.getInstance().getAllDefaultKeysSize()+"/"+ Wallet.NUMBER_GETADDRESS_KEYS+")";
            new AlertDialog.Builder(this)
                    .setTitle("MiniDAPP")
                    .setMessage("Please wait for ALL your Minima keys to be created\n\n" +
                            "This process can take up to 5 mins\n\n" +
                            "Once that is done you can resync!\n\n" +current)
                    .setIcon(R.drawable.ic_minima)
                    .setNegativeButton("Close", null)
                    .show();
            return;
        }

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

                //How many keys..
                String keys = mMaxKeys.getText().toString();
                mNumberKeys = Integer.parseInt(keys);
                if(mNumberKeys<1 || mNumberKeys>200000){
                    Toast.makeText(SeedSyncActivity.this,"Invalid Max Keys.. ", Toast.LENGTH_SHORT).show();
                    return;
                }

                mSeedphrase.setEnabled(false);
                mDelete.setEnabled(false);
                mComplete.setEnabled(false);

                Toast.makeText(SeedSyncActivity.this,"Resyncing.. please wait..", Toast.LENGTH_LONG).show();

                //Wait for Minima to fully start up..
                mLoader = new ProgressDialog(SeedSyncActivity.this);
                mLoader.setTitle("Syncing..");
                mLoader.setMessage("Please wait..");
                mLoader.setCanceledOnTouchOutside(false);
                mLoader.setCancelable(false);
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

    public static int updatecounter = 0;

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

    public void runArchiveSync(String zSeedPhrase){
        updatecounter = 0;

        Runnable sync = new Runnable() {
            @Override
            public void run() {

                MinimaLogger.log("Starting Archive process..");

                try {

                    String result = null;
                    if (zSeedPhrase.equals("")) {
                        result = mMinima.getMinima().runMinimaCMD("archive action:resync host:" + mArchiveNode);
                    } else {
                        result = mMinima.getMinima().runMinimaCMD("archive keyuses:"+mNumberKeys+" action:resync host:" + mArchiveNode + " phrase:\"" + zSeedPhrase + "\"");
                    }

                    MinimaLogger.log("Ending Archive process.. ");

                    //Parse the result..
                    JSONObject json = (JSONObject) new JSONParser().parse(result);
                    if((boolean)json.get("status")){
                        //It worked!
                        //Close both
                        SeedSyncActivity.this.finishAffinity();
                        if(MainActivity.getMainActivity() != null){
                            MainActivity.getMainActivity().shutdown();
                        }

                    }else{
                        SeedSyncActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mSeedphrase.setEnabled(true);
                                mDelete.setEnabled(true);
                                mComplete.setEnabled(true);

                                mLoader.cancel();

                                Toast.makeText(SeedSyncActivity.this, "Could not connect to host! "+mArchiveNode, Toast.LENGTH_SHORT).show();
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
        MinimaLogger.log("SEEDSYNC CONNECTED TO SERVICE");
        MinimaService.MyBinder binder = (MinimaService.MyBinder)iBinder;
        mMinima = binder.getService();

        //Set the listener
        mMinima.mArchiveListener = this;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        MinimaLogger.log("SEEDSYNC  DISCONNECTED TO SERVICE");
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
