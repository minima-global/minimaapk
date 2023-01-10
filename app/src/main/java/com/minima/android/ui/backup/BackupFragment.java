package com.minima.android.ui.backup;

import android.content.DialogInterface;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
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

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.minima.android.MainActivity;
import com.minima.android.R;
import com.minima.android.dependencies.backupSync.BackupSyncProvider;
import com.minima.android.dependencies.backupSync.minima.MinimaBackupUtils;
import com.minima.android.dependencies.backupSync.providers.drive.model.userModel.GoogleDriveUserNotSignedInYet;
import com.minima.android.dependencies.backupSync.providers.drive.model.userModel.GoogleDriveUserSignedInModel;
import com.minima.android.dependencies.backupSync.providers.drive.model.userModel.GoogleStateUserModel;

import org.minima.utils.MinimaLogger;

import java.io.File;
import java.util.Date;
import java.util.List;

public class BackupFragment extends Fragment {

    MainActivity mMain;

    String mPassword = null;

    private TextView gDriveText;
    private Button gDriveButton;

    EditText mInput1;
    EditText mInput2;

    ActivityResultLauncher<Intent> authResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            (ActivityResultCallback<ActivityResult>) result -> {
                if (result.getResultCode() == Activity.RESULT_OK && getContext() != null) {
                    updateGDriveTexts(getContext());
                    backup(getContext());
                }
            }
    );

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        mMain = (MainActivity) getActivity();

        View root = inflater.inflate(R.layout.fragment_backup, container, false);

        Button backup = root.findViewById(R.id.backup_makebackup);
        backup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputDialog(true);
            }
        });

        Button restore = root.findViewById(R.id.backup_restore);
        restore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputDialog(false);
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

        gDriveText = root.findViewById(R.id.text_gdrive);
        gDriveButton = root.findViewById(R.id.backup_gdrive);
        gDriveText.setVisibility(View.GONE);
        gDriveButton.setVisibility(View.GONE);

        updateGDriveTexts(root.getContext());

        return root;
    }

    public void showInputDialog(boolean zBackup){
        AlertDialog.Builder builder = new AlertDialog.Builder(mMain);
        builder.setTitle("Password Entry");

        if(zBackup) {
            LayoutInflater inflater = mMain.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.password_view, null);

            // Set up the input
            mInput1 = dialogView.findViewById(R.id.passowrd_try1);
            mInput2 = dialogView.findViewById(R.id.passowrd_try2);

            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            builder.setView(dialogView);
        }else{

            //Just one input..
            mInput1 = new EditText(mMain);
            mInput1.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

            builder.setView(mInput1);
        }

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                mPassword = mInput1.getText().toString().trim();

                if(zBackup) {
                    String passcheck = mInput2.getText().toString().trim();

                    //MUST be the same
                    if (!passcheck.equals(mPassword)) {
                        Toast.makeText(mMain, "Passwords do NOT match!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if(mPassword.equals("")){
                        Toast.makeText(mMain,"Cannot have a blank password", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }else{
                    if(mPassword.equals("")){
                        mPassword = "minima";
                    }
                }

                if(zBackup){
                    Toast.makeText(mMain,"Creating a backup.. pls wait",Toast.LENGTH_SHORT).show();
                    makeBackup();
                }else{
                    Toast.makeText(mMain,"Restoring Minima.. pls wait",Toast.LENGTH_SHORT).show();
                    mMain.openFile(mPassword, MainActivity.REQUEST_RESTORE);
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

//    public void showInputDialog(boolean zBackup){
//        AlertDialog.Builder builder = new AlertDialog.Builder(mMain);
//        if(zBackup){
//            builder.setTitle("Choose Password");
//        }else{
//            builder.setTitle("Set Password");
//        }
//
//        // Set up the input
//        final EditText input = new EditText(mMain);
//
//        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
//        input.setInputType(InputType.TYPE_CLASS_TEXT);
//        builder.setView(input);
//
//        // Set up the buttons
//        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                mPassword = input.getText().toString().trim();
//                if(zBackup && mPassword.equals("")){
//                    Toast.makeText(mMain,"Cannot have a blank password", Toast.LENGTH_SHORT).show();
//                    return;
//                }else if(mPassword.equals("")){
//                    mPassword = "minima";
//                }
//
//                if(zBackup){
//                    Toast.makeText(mMain,"Creating a backup.. pls wait",Toast.LENGTH_SHORT).show();
//                    makeBackup();
//                }else{
//                    Toast.makeText(mMain,"Restoring Minima.. pls wait",Toast.LENGTH_SHORT).show();
//                    mMain.openFile(mPassword, MainActivity.REQUEST_RESTORE);
//                }
//            }
//        });
//        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.cancel();
//            }
//        });
//
//        builder.show();
//    }

    public void makeBackup(){

        Runnable bak = new Runnable() {
            @Override
            public void run() {
                //Where are we going to store the file
                String filename = "minima-backup-custom.bak";
                File backup = new File(mMain.getFilesDir(),filename);
                if(backup.exists()){
                    backup.delete();
                }

                //First run a command On Minima..
                String result = mMain.getMinima().runMinimaCMD("backup file:"+backup.getAbsolutePath()
                        +" password:\""+mPassword+"\"",false);

                if(backup.exists()) {
                    //Get the URi
                    Uri backupuri = FileProvider.getUriForFile(mMain,"com.minima.android.provider",backup);

                    //Now share that file..
                    Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                    intentShareFile.setType("application/zip");
                    intentShareFile.putExtra(Intent.EXTRA_STREAM, backupuri);
                    intentShareFile.putExtra(Intent.EXTRA_SUBJECT,"Minima_Backup_"+System.currentTimeMillis()+".bak");
//                    intentShareFile.putExtra(Intent.EXTRA_SUBJECT,filename);
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

    private void updateGDriveTexts(Context context) {

        //Cut this out for now..
        if(true){
            return;
        }

        BackupSyncProvider
                .getGoogleDriveProvider(context)
                .getUserState(context, backupUserStateModel -> {
                    GoogleStateUserModel googleStateUserModel = (GoogleStateUserModel) backupUserStateModel;

                    if (gDriveText != null && gDriveButton != null) {
                        gDriveText.setVisibility(View.VISIBLE);
                        gDriveButton.setVisibility(View.VISIBLE);

                        if (googleStateUserModel instanceof GoogleDriveUserNotSignedInYet) {
                            gDriveText.setText(getString(R.string.minima_gdrive_backup_explanation_not_signed_in));
                            gDriveButton.setText(getString(R.string.minima_gdrive_backup_button_not_signed_in));
                            gDriveButton.setOnClickListener(
                                    view -> BackupSyncProvider.getGoogleDriveProvider(context).auth(view.getContext(), authResultLauncher)
                            );
                        } else if (googleStateUserModel instanceof GoogleDriveUserSignedInModel) {

//                            //Store this time..
//                            SharedPreferences pref = context.getSharedPreferences("gdrive",Context.MODE_PRIVATE);
//                            long lastbackup = pref.getLong("lastgdrive",0);
//                            String backuptime = null;
//                            if(lastbackup == 0){
//                                backuptime = "None yet..";
//                            }else{
//                                backuptime = new Date(lastbackup).toString();
//                            }
//
//                            MinimaLogger.log("LAST GDrive found : "+lastbackup);

                            gDriveText.setText(
                                    getString(
                                            R.string.minima_gdrive_backup_explanation_signed_in,
                                            ((GoogleDriveUserSignedInModel) googleStateUserModel).getEmail()
                                    )
                            );
                            gDriveButton.setText(getString(R.string.minima_gdrive_backup_button_signed_in));
                            gDriveButton.setOnClickListener(view -> backup(view.getContext()));
                        }
                    }
                });

    }

    private void backup(Context context) {
        Toast.makeText(context, "Saving Minima backup to GDrive", Toast.LENGTH_SHORT).show();

        BackupSyncProvider.getGoogleDriveProvider(context).uploadBackup(context, MinimaBackupUtils.createBackup(mMain), authResultLauncher);

        MinimaLogger.log("Finished Backup.. ");
    }

    private static final String minimaProviderAuthority = "com.minima.android.provider";
}