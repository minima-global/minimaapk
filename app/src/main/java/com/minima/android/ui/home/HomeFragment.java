package com.minima.android.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.minima.android.MainActivity;
import com.minima.android.R;
import com.minima.android.databinding.FragmentHomeBinding;

import org.minima.Minima;
import org.minima.system.Main;
import org.minima.system.network.maxima.MaximaManager;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;

public class HomeFragment extends Fragment {

    MainActivity mMain;
    View mRoot;
    boolean mRunningUpdate = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_home, container, false);

        //Get the Main Activity
        mMain = (MainActivity)getActivity();

        mRoot = root;

        setStatus();

        return root;
    }

    public void setStatus(){

        if(mRunningUpdate){
            return;
        }
        mRunningUpdate = true;

        Runnable status = new Runnable() {
            @Override
            public void run() {

                try{
                    //Get Minima
                    Minima minima = mMain.getMinima();
                    while(minima == null){
                        Thread.sleep(1000);
                        minima = mMain.getMinima();
                    }

                    //Wait for Maxima..
                    MaximaManager max = Main.getInstance().getMaxima();
                    while(max == null || !max.isInited()) {
                        Thread.sleep(1000);
                        max = Main.getInstance().getMaxima();
                    }

                    //Run Status..
                    String status = minima.runMinimaCMD("status",false);

                    //Make a JSON
                    JSONObject json = (JSONObject) new JSONParser().parse(status);

                    //Get the status..
                    while(!(boolean)json.get("status")){
                        Thread.sleep(1000);

                        //Run Status..
                        status = minima.runMinimaCMD("status");

                        //Make a JSON
                        json = (JSONObject) new JSONParser().parse(status);
                    }

                    JSONObject response = (JSONObject) json.get("response");

                    updateUI(response);

                }catch(Exception exc){
                    MinimaLogger.log("ERROR update status : "+exc);
                }
            }
        };

        Thread checker = new Thread(status);
        checker.setDaemon(true);
        checker.start();
   }

   public void updateUI(JSONObject zStatusJSON){
        mMain.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                //Get the Header details..
                JSONObject memory = (JSONObject)zStatusJSON.get("memory");
                JSONObject chain  = (JSONObject)zStatusJSON.get("chain");

                //Set it..
                ((TextView)mRoot.findViewById(R.id.text_home_time)).setText(chain.getString("time"));
                ((TextView)mRoot.findViewById(R.id.text_home_block)).setText(""+chain.get("block"));
                ((TextView)mRoot.findViewById(R.id.text_home_version)).setText(zStatusJSON.getString("version"));
                ((TextView)mRoot.findViewById(R.id.text_home_ram)).setText(memory.getString("ram"));
                ((TextView)mRoot.findViewById(R.id.text_home_devices)).setText(zStatusJSON.getString("devices"));

                //Finished the update
                mRunningUpdate = false;
            }
        });
   }

    @Override
    public void onResume() {
        super.onResume();
        setStatus();
    }
}