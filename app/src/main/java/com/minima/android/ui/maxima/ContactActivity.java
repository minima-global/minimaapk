package com.minima.android.ui.maxima;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.minima.android.R;
import com.minima.android.service.MinimaService;

import org.minima.utils.MinimaLogger;
import org.minima.utils.RPCClient;

import java.util.Date;

public class ContactActivity extends AppCompatActivity implements ServiceConnection {

    MinimaService mMinima;

    Contact mContact;

    boolean mDeleting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        //Get the contact
        mContact = (Contact) getIntent().getSerializableExtra("contact");

        //Set the name
        TextView name = findViewById(R.id.cname);
        name.setText(mContact.mName);

        //publickey
        TextView pubkey = findViewById(R.id.cpublickey);
        pubkey.setText( mContact.mPublicKey );

        //Address
        TextView address = findViewById(R.id.caddress);
        address.setText( mContact.mAddress );

        //My Address
        TextView myaddress = findViewById(R.id.cmyaddress);
        myaddress.setText( mContact.mMyAddress );

        //Last seen
        TextView lseen = findViewById(R.id.clastseen);
        lseen.setText( ContactAdapter.DATEFORMAT.format(new Date(mContact.mLastSeen)));

        //Same Chain
        TextView samec = findViewById(R.id.csamechain);
        samec.setText( new String(""+mContact.mSameChain).toUpperCase()  );

        //Bind to the Minima Service..
        Intent minimaintent = new Intent(this, MinimaService.class);
        bindService(minimaintent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.contact_menu, menu);
        return true;
    }

    private void deleteContact(){

        //Only once
        if(mDeleting){
            return;
        }
        mDeleting = true;

        //Delete Contact
        Runnable del = new Runnable() {
            @Override
            public void run() {

                //Tell Minima..
                try{
                    //Delete the contact
                    mMinima.getMinima().runMinimaCMD("maxcontacts action:remove id:"+mContact.mID);

                    //Pause to let it update..
                    Thread.sleep(3000);

                }catch(Exception exc){
                    MinimaLogger.log(exc);
                }

                //And exit with code 99
                ContactActivity.this.finish();
            }
        };

        Toast.makeText(this,"Deleting Contact", Toast.LENGTH_LONG).show();

        //Now start her up..
        Thread tt = new Thread(del);
        tt.start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_delete:

                //Only once
                if(mDeleting){
                    return true;
                }

                new AlertDialog.Builder(this)
                        .setTitle("Delete Contact")
                        .setMessage("Are you sure ?\n\nThis will also remove you from their contact list..")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                deleteContact();
                            }})
                        .setNegativeButton(android.R.string.no, null).show();

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        MinimaLogger.log("CONTACT CONNECTED TO SERVICE");
        MinimaService.MyBinder binder = (MinimaService.MyBinder)iBinder;
        mMinima = binder.getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        MinimaLogger.log("CONTACT  DISCONNECTED TO SERVICE");
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
