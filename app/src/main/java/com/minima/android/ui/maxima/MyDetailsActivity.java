package com.minima.android.ui.maxima;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.minima.android.R;
import com.minima.android.service.MinimaService;

import org.minima.utils.MinimaLogger;
import org.minima.utils.RPCClient;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;

public class MyDetailsActivity extends AppCompatActivity implements ServiceConnection {

    MinimaService mMinima;

    EditText mName;
    String mNameString = "";

    TextView mContact;
    String mContactString = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_mydetails);

        mName       = findViewById(R.id.mydetails_name);

        Button updatename     = findViewById(R.id.mydetails_updatename);
        updatename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check if no view has focus:
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                updateName();
            }
        });

        mContact    = findViewById(R.id.mydetails_contact);

        //Bind to the Minima Service..
        Intent minimaintent = new Intent(getBaseContext(), MinimaService.class);
        bindService(minimaintent, this, Context.BIND_AUTO_CREATE);
    }

    public void updateName(){

        Toast.makeText(this,"Updating Maxima Name..",Toast.LENGTH_SHORT).show();

        //Get the new name..
        String name = mName.getText().toString().trim();

        //Do the RPC call..
        mMinima.getMinima().runMinimaCMD("maxima action:setname name:\""+name+"\"",false);
    }

    public void setDetails(){

        //Call Maxima..
        try{
            //Do the RPC call..
            String maxdetails = mMinima.getMinima().runMinimaCMD("maxima",false);

            JSONObject json         = (JSONObject)new JSONParser().parse(maxdetails);
            JSONObject response     = (JSONObject)json.get("response");

            mNameString             = (String) response.get("name");
            mNameString = mNameString.replace("\"", "");
            mNameString = mNameString.replace("'", "");
            mNameString = mNameString.replace(";", "");

            mContactString          = (String) response.get("contact");

            mName.setText(mNameString);
            mContact.setText(mContactString);

        }catch(Exception exc){
            MinimaLogger.log(exc);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mydetails, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_share:

                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, mContactString);
                sendIntent.setType("text/plain");

                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(mMinima != null){
            unbindService(this);
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        MinimaLogger.log("MYDETAILS CONNECTED TO SERVICE");
        MinimaService.MyBinder binder = (MinimaService.MyBinder)iBinder;
        mMinima = binder.getService();

        //Now you can set the details..
        setDetails();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        MinimaLogger.log("MYDETAILS  DISCONNECTED TO SERVICE");
        mMinima = null;
    }
}
