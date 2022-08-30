package com.minima.android.ui.backup;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
import com.minima.android.ui.maxima.MyDetailsActivity;

import org.minima.utils.MinimaLogger;

import java.io.File;
import java.util.List;

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
                Toast.makeText(mMain,"Creating a backup.. pls wait",Toast.LENGTH_SHORT).show();
                makeBackup();
            }
        });

        Button restore = root.findViewById(R.id.backup_restore);
        restore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mMain,"Restoring Minima.. pls wait",Toast.LENGTH_SHORT).show();
                mMain.openFile(MainActivity.REQUEST_RESTORE);
            }
        });

        Button archive = root.findViewById(R.id.backup_archive);
        archive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mMain, Bip39Activity.class);
                startActivity(intent);
            }
        });

        return root;
    }

    public void makeBackup(){

        Runnable bak = new Runnable() {
            @Override
            public void run() {
                //Where are we going to store the file
                String filename = "minima-backup.bak.gz";
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
                    intentShareFile.setType("application/gz");
                    intentShareFile.putExtra(Intent.EXTRA_STREAM, backupuri);
                    intentShareFile.putExtra(Intent.EXTRA_SUBJECT,"Minima_Backup_"+System.currentTimeMillis());
                    intentShareFile.putExtra(Intent.EXTRA_TEXT, "Here is my Minima backup");
                    intentShareFile.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    Intent chooser = Intent.createChooser(intentShareFile, "Share File");

                    List<ResolveInfo> resInfoList = mMain.getPackageManager().queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);
                    for (ResolveInfo resolveInfo : resInfoList) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        mMain.grantUriPermission(packageName, backupuri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }

                    startActivity(chooser);

                    //startActivity(Intent.createChooser(intentShareFile, "Store your Minima Backup"));
                    //startActivity(intentShareFile);

                }else{
                    mMain.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mMain,"Error creating backup..",Toast.LENGTH_LONG).show();
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