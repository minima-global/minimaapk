package com.minima.android.ui.vault;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.minima.android.MainActivity;
import com.minima.android.R;

import org.minima.Minima;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;

public class VaultFragment extends Fragment {

    MainActivity mMain;
    View mRoot;

    String mPassword;
    boolean mLocked;

    TextView mLockStatus;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        View root = inflater.inflate(R.layout.fragment_vault, container, false);

        //Get the Main Activity
        mMain = (MainActivity)getActivity();
        mMain.mVaultFragment = this;

        mRoot = root;

        Button lock = root.findViewById(R.id.vault_lock);
        lock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputDialog(true);
            }
        });

        Button unlock = root.findViewById(R.id.vault_unlock);
        unlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputDialog(false);
            }
        });

        mLockStatus = root.findViewById(R.id.text_vaultstatus);

        updateUI();

        return root;
    }

    public void showInputDialog(boolean zBackup){
        AlertDialog.Builder builder = new AlertDialog.Builder(mMain);
        if(zBackup){
            builder.setTitle("Choose Password");
        }else{
            builder.setTitle("Set Password");
        }

        // Set up the input
        final EditText input = new EditText(mMain);

        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPassword = input.getText().toString().trim();
                if(mPassword.equals("")){
                    Toast.makeText(mMain,"Cannot have a blank password", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(zBackup){
                    lockDB();
                }else{
                    unlockDB();
                }
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
    }

    public void updateUI(){

        //Check we are connected..
        Minima minima = mMain.getMinima();
        if(minima == null){
            Toast.makeText(mMain,"Minima not initialised yet", Toast.LENGTH_SHORT).show();
            return;
        }

        Runnable status = new Runnable() {
            @Override
            public void run() {

                try{
                    //Get Minima
                    Minima minima = mMain.getMinima();

                    //Run Status..
                    String status = minima.runMinimaCMD("vault",false);

                    //Make a JSON
                    JSONObject json = (JSONObject) new JSONParser().parse(status);
                    JSONObject response = (JSONObject) json.get("response");

                    //Are we locked..
                    mLocked = (boolean)response.get("locked");

                    mMain.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(mLocked){
                                mLockStatus.setText(Html.fromHtml(" Status : <b>LOCKED</b> ", Html.FROM_HTML_MODE_COMPACT) );
                            }else{
                                mLockStatus.setText(Html.fromHtml(" Status : <b>UNLOCKED</b> ", Html.FROM_HTML_MODE_COMPACT) );
                            }
                        }
                    });

                }catch(Exception exc){
                    MinimaLogger.log("ERROR update vault : "+exc);
                }
            }
        };

        Thread checker = new Thread(status);
        checker.setDaemon(true);
        checker.start();
   }

    private void lockDB(){

        //Check we are connected..
        Minima minima = mMain.getMinima();
        if(minima == null){
            Toast.makeText(mMain,"Minima not initialised yet", Toast.LENGTH_SHORT).show();
            return;
        }

        Runnable status = new Runnable() {
            @Override
            public void run() {

                try{
                    //Get Minima
                    Minima minima = mMain.getMinima();

                    //Run Status..
                    String passlock = minima.runMinimaCMD("vault action:passwordlock password:"+mPassword,false);

                    MinimaLogger.log("passlock : "+passlock);

                    //Make a JSON
                    JSONObject json = (JSONObject) new JSONParser().parse(passlock);

                    //Did it work..
                    boolean status = (boolean)json.get("status");

                    if(!status){
                      final String errormsg = json.getString("error");
                      mMain.runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              MinimaLogger.log("ERROR : "+errormsg);
                              Toast.makeText(mMain,errormsg, Toast.LENGTH_LONG).show();
                          }
                      });

                      return;
                    }else{
                        mMain.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mMain,"Private Keys Encrypted!", Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    //Update
                    updateUI();

                }catch(Exception exc){
                    MinimaLogger.log("ERROR update vault : "+exc);
                }
            }
        };

        Thread checker = new Thread(status);
        checker.setDaemon(true);
        checker.start();
    }

    private void unlockDB(){

        //Check we are connected..
        Minima minima = mMain.getMinima();
        if(minima == null){
            Toast.makeText(mMain,"Minima not initialised yet", Toast.LENGTH_SHORT).show();
            return;
        }

        Runnable status = new Runnable() {
            @Override
            public void run() {

                try{
                    //Get Minima
                    Minima minima = mMain.getMinima();

                    //Run Status..
                    String passlock = minima.runMinimaCMD("vault action:passwordunlock password:"+mPassword,false);

                    //Make a JSON
                    JSONObject json = (JSONObject) new JSONParser().parse(passlock);

                    //Did it work..
                    boolean status = (boolean)json.get("status");

                    if(!status){
                        final String errormsg = json.getString("error");
                        mMain.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mMain,errormsg, Toast.LENGTH_LONG).show();
                            }
                        });

                        return;
                    }else{
                        mMain.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mMain,"Private Keys Decrypted!", Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    //Update
                    updateUI();

                }catch(Exception exc){
                    MinimaLogger.log("ERROR update vault : "+exc);
                }
            }
        };

        Thread checker = new Thread(status);
        checker.setDaemon(true);
        checker.start();
    }

}