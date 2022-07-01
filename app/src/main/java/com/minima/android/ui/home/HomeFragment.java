package com.minima.android.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.minima.android.MainActivity;
import com.minima.android.R;

import org.minima.Minima;
import org.minima.system.Main;
import org.minima.system.network.maxima.MaximaManager;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;

public class HomeFragment extends Fragment {

    MainActivity mMain;
    View mRoot;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_home, container, false);

        //Get the Main Activity
        mMain = (MainActivity)getActivity();
        mMain.mHomeFragment = this;

        mRoot = root;

        return root;
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
                    String status = minima.runMinimaCMD("status",false);

                    //Make a JSON
                    JSONObject json = (JSONObject) new JSONParser().parse(status);
                    JSONObject response = (JSONObject) json.get("response");
                    updateMinimaUI(response);

                    //Now the Maxima details
                    String maxcontacts = minima.runMinimaCMD("maxcontacts",false);

                    //Make a JSON
                    json        = (JSONObject) new JSONParser().parse(maxcontacts);
                    response    = (JSONObject) json.get("response");
                    updateMaximaUI(response);

                    //Now the MDS details..
                    String mds = minima.runMinimaCMD("mds",false);

                    //Make a JSON
                    json        = (JSONObject) new JSONParser().parse(mds);
                    response    = (JSONObject) json.get("response");
                    updateMDSUI(response);

                }catch(Exception exc){
                    MinimaLogger.log("ERROR update status : "+exc);
                }
            }
        };

        Thread checker = new Thread(status);
        checker.setDaemon(true);
        checker.start();
   }

   public void updateMinimaUI(JSONObject zStatusJSON){
        mMain.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                //Get the Header details..
                JSONObject memory   = (JSONObject)zStatusJSON.get("memory");
                JSONObject chain    = (JSONObject)zStatusJSON.get("chain");
                JSONObject network  = (JSONObject)zStatusJSON.get("network");

                //Set it..
                ((TextView)mRoot.findViewById(R.id.text_home_time)).setText(chain.getString("time"));
                ((TextView)mRoot.findViewById(R.id.text_home_block)).setText(""+chain.get("block"));
                ((TextView)mRoot.findViewById(R.id.text_home_version)).setText(zStatusJSON.getString("version"));
                ((TextView)mRoot.findViewById(R.id.text_home_ram)).setText(memory.getString("ram"));
                ((TextView)mRoot.findViewById(R.id.text_home_diskspace)).setText(memory.getString("disk"));
                ((TextView)mRoot.findViewById(R.id.text_home_devices)).setText(zStatusJSON.getString("devices"));
                ((TextView)mRoot.findViewById(R.id.text_home_connections)).setText(""+network.get("connected"));

                ((TextView)mRoot.findViewById(R.id.text_home_ip)).setText("https://"+network.getString("host")+":9003");
            }
        });
   }

    public void updateMaximaUI(JSONObject zMaxcontacts){
        mMain.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                //Get the contacts
                JSONArray contacts = (JSONArray) zMaxcontacts.get("contacts");

                //How many are we in sync with
                long acceptable = System.currentTimeMillis() - (1000 * 60 * 30);
                int valid   = 0;
                int net     = 0;
                for(Object obj : contacts){
                    JSONObject contact = (JSONObject) obj;
                    boolean samechain = (boolean) contact.get("samechain");
                    if(samechain){
                        valid++;
                    }

                    long lastseen = (long) contact.get("lastseen");
                    if(lastseen > acceptable){
                        net++;
                    }
                }

                //Set it..
                ((TextView)mRoot.findViewById(R.id.text_home_contacts)).setText(""+contacts.size());
                ((TextView)mRoot.findViewById(R.id.text_home_valid)).setText(""+valid);
                ((TextView)mRoot.findViewById(R.id.text_home_net)).setText(""+net);
            }
        });
    }

    public void updateMDSUI(JSONObject zMDS){
        mMain.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                //Get the contacts
                JSONArray mdsapps = (JSONArray) zMDS.get("minidapps");

                String password   = zMDS.getString("password");

                //Set it..
                ((TextView)mRoot.findViewById(R.id.text_home_dapps)).setText(""+mdsapps.size());
                ((TextView)mRoot.findViewById(R.id.text_mds_password)).setText(password);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }
}