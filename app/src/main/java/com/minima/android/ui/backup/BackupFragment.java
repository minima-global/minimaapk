package com.minima.android.ui.backup;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.minima.android.BuildConfig;
import com.minima.android.MainActivity;
import com.minima.android.R;

import org.minima.utils.MinimaLogger;

import java.io.File;

public class BackupFragment extends Fragment {

    MainActivity mMain;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        mMain = (MainActivity)getActivity();

        View root = inflater.inflate(R.layout.fragment_backup, container, false);

        Button backup = root.findViewById(R.id.backup_makebackup);
        backup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                makeBackup();
            }
        });

        Button restore = root.findViewById(R.id.backup_restore);
        restore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMain.openFile(MainActivity.REQUEST_RESTORE);
            }
        });

        return root;
    }

    public void makeBackup(){

        Runnable bak = new Runnable() {
            @Override
            public void run() {
                //Where are we going to store the file
                String filename = "minima-backup.gz.bak";
                File backup = new File(mMain.getFilesDir(),filename);
                if(backup.exists()){
                    backup.delete();
                }

                //First run a command On Minima..
                String result = mMain.getMinima().runMinimaCMD("backup file:"+backup.getAbsolutePath());

                //Now share this..
                MinimaLogger.log("Backup : "+result);

                if(backup.exists()) {
                    //Get the URi
                    Uri backupuri = FileProvider.getUriForFile(mMain,"com.minima.android.provider",backup);

                    //Now share that file..
                    Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                    intentShareFile.setType("application/*");
                    intentShareFile.putExtra(Intent.EXTRA_STREAM, backupuri);
                    intentShareFile.putExtra(Intent.EXTRA_SUBJECT,"Minima backup");
                    intentShareFile.putExtra(Intent.EXTRA_TEXT, "Here is my Minima backup");
                    startActivity(Intent.createChooser(intentShareFile, "Store your Minima Backup"));

                }else{
                    mMain.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mMain,"Error cretaing backup..",Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        };

        Thread rr = new Thread(bak);
        rr.start();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
    }
}