package com.minima.android.ui.logs;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.minima.android.MainActivity;
import com.minima.android.R;
import com.minima.android.ui.maxima.MyDetailsActivity;

import org.minima.Minima;
import org.minima.objects.base.MiniData;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;

import java.util.ArrayList;

public class LogsFragment extends Fragment {

    MainActivity mMain;
    View mRoot;

    TextView mLogs;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        View root = inflater.inflate(R.layout.fragment_logs, container, false);

        //Get the Main Activity
        mMain = (MainActivity)getActivity();
        mMain.mLogsFragment = this;

        mLogs = root.findViewById(R.id.logs_list);
        mLogs.setMovementMethod(new ScrollingMovementMethod());

        mRoot = root;

        setLog(mMain.getFullLogs());

        return root;
    }

    public void setLog(String zLogs){
        mLogs.setText(zLogs);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.logs, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {

            case R.id.action_logs_share:

                String text = mLogs.getText().toString();

                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Minima Logs");
                sendIntent.putExtra(Intent.EXTRA_TEXT, text);
                sendIntent.setType("text/plain");

                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);

            case R.id.action_logs_refresh:
                setLog(mMain.getFullLogs());
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setLog(mMain.getFullLogs());
    }
}