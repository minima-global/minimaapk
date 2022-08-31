package com.minima.android.ui.backup;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.minima.android.R;
import com.minima.android.service.MinimaService;
import com.minima.android.ui.maxima.contacts.Contact;
import com.minima.android.ui.maxima.contacts.ContactAdapter;

import org.minima.utils.BIP39;
import org.minima.utils.MinimaLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class Bip39Activity extends AppCompatActivity implements ServiceConnection {

//    MinimaService mMinima;

    AutoCompleteTextView mSeedphrase;
    ArrayList<String> mWordListArray;

    TextView mFinalWordlist;

    TextView mWordCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seedkeyboard);

        Toolbar tb  = findViewById(R.id.contact_toolbar);
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

        Button delete = findViewById(R.id.seed_delete);
        delete.setOnClickListener(new View.OnClickListener() {
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

        //Bind to the Minima Service..
//        Intent minimaintent = new Intent(this, MinimaService.class);
//        bindService(minimaintent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.contact_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle item selection
//        switch (item.getItemId()) {
//            case R.id.menu_delete:
//
//                //Only once
//                if(mDeleting){
//                    return true;
//                }
//
//                new AlertDialog.Builder(this)
//                        .setTitle("Delete Contact")
//                        .setMessage("Are you sure ?\n\nThis will also remove you from their contact list..")
//                        .setIcon(android.R.drawable.ic_dialog_alert)
//                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int whichButton) {
//                                deleteContact();
//                            }})
//                        .setNegativeButton(android.R.string.no, null).show();
//
//                return true;
//
//            default:
//                return super.onOptionsItemSelected(item);
//        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        MinimaLogger.log("BIP39 CONNECTED TO SERVICE");
//        MinimaService.MyBinder binder = (MinimaService.MyBinder)iBinder;
//        mMinima = binder.getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        MinimaLogger.log("BIP39  DISCONNECTED TO SERVICE");
//        mMinima = null;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
//        if(mMinima != null){
//            unbindService(this);
//        }
    }
}
