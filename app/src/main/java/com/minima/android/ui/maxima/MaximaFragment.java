package com.minima.android.ui.maxima;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.minima.android.MainActivity;
import com.minima.android.R;

import org.minima.Minima;
import org.minima.utils.MinimaLogger;
import org.minima.utils.RPCClient;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;
import org.minima.utils.json.parser.ParseException;

import java.util.ArrayList;

public class MaximaFragment extends Fragment {

    MainActivity mMain;

    ListView mMainList;

    ArrayList<Contact> mContacts = new ArrayList<>();

    private String mNewContactAddress = "";

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_maxima, container, false);

        mMainList = root.findViewById(R.id.maxima_list);

        //Get the Main Activity
        mMain = (MainActivity)getActivity();

        //If it's Empty
        mMainList.setEmptyView(root.findViewById(R.id.empty_list_item));

        //What happens on click
        mMainList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                //Get the contact..
                Contact contact = mContacts.get(position);

                //Now open a Contact Activity
                Intent intent = new Intent(mMain, ContactActivity.class);
                intent.putExtra("contact", contact);
                startActivity(intent);
            }
        });

        FloatingActionButton fab = root.findViewById(R.id.fab_maxima);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showNewContactDialog();
            }
        });

        //And Update the List
        updateContactList(false);

        return root;
    }

    public void showNewContactDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(mMain);
        builder.setTitle("Add new contact");

        // Set up the input
        final EditText input = new EditText(mMain);

        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mNewContactAddress = input.getText().toString().trim();
                if(mNewContactAddress.equals("")){
                    return;
                }

                Toast.makeText(mMain,"Adding contact.. please wait..", Toast.LENGTH_LONG).show();

                addContact(mNewContactAddress);
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

    public void addContact(String zAddress){
        Runnable add = new Runnable() {
            @Override
            public void run() {
                //Tell Minima..
                try{
                    //Get Minima..
                    Minima minima = mMain.getMinima();
                    if(minima == null){
                        return;
                    }

                    String result = minima.runMinimaCMD("maxcontacts action:add contact:"+mNewContactAddress,false);
                    MinimaLogger.log(result);

                    //Small pause..
                    Thread.sleep(5000);

                    //And Update the List
                    mMain.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateContactList(false);
                        }
                    });

                }catch(Exception exc){
                    MinimaLogger.log(exc);
                }
            }
        };

        Thread tt = new Thread(add);
        tt.start();
    }

    public void updateContactList(boolean zRefrshAswell){

        ArrayList<Contact> contacts = new ArrayList<>();

        //Get Minima..
        Minima minima = mMain.getMinima();
        if(minima == null){
            Toast.makeText(mMain,"Minima not initialised yet", Toast.LENGTH_SHORT).show();
            return;
        }

//                    //Are we refreshing out contacts
//                    if(zRefrshAswell){
//                        //Run maxima refresh - tell everyone where you are
//                        String resp = RPCClient.sendGET(MINIMA_HOST+"maxima action:refresh");
//                        MinimaLogger.log(resp);
//                    }

        //Do the RPC call..
        String maxcontacts = minima.runMinimaCMD("maxcontacts",false);

        //Convert JSON
        JSONObject json         = null;
        try {
            json = (JSONObject)new JSONParser().parse(maxcontacts);
        } catch (ParseException e) {
            return;
        }

        //Did it work..
        if(!(boolean)json.get("status")){
            //Too soon
            mMain.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mMain,"Maxima NOT initialised yet.. ",Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        JSONObject response     = (JSONObject)json.get("response");
        JSONArray allcontactsjson   = (JSONArray) response.get("contacts");

        //Convert these..
        for(Object obj : allcontactsjson){

            JSONObject jconcontact = (JSONObject)obj;

            Contact newcontact = new Contact(jconcontact);
            contacts.add(newcontact);
        }

        //Get the array list
        Contact[] allcontacts = contacts.toArray(new Contact[0]);

        //Keep for later
        mContacts = contacts;

        //Create the custom arrayadapter
        ContactAdapter adapter = new ContactAdapter(mMain, R.layout.contact_view, allcontacts);

        mMainList.setAdapter(adapter);

    }

    @Override
    public void onResume() {
        super.onResume();

        //And Update the List
        updateContactList(false);
    }
}