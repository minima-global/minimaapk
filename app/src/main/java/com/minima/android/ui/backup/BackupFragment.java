package com.minima.android.ui.backup;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.minima.android.MainActivity;
import com.minima.android.R;
import com.minima.android.dependencies.backupSync.BackupSyncProvider;
import com.minima.android.dependencies.backupSync.minima.MinimaBackupUtils;
import com.minima.android.dependencies.backupSync.providers.drive.model.userModel.GoogleDriveUserNotSignedInYet;
import com.minima.android.dependencies.backupSync.providers.drive.model.userModel.GoogleDriveUserSignedInModel;
import com.minima.android.dependencies.backupSync.providers.drive.model.userModel.GoogleStateUserModel;

import java.io.File;

public class BackupFragment extends Fragment {

    MainActivity mMain;

    private TextView gDriveText;
    private Button gDriveButton;

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
                Toast.makeText(mMain,"Creating a backup.. pls wait",Toast.LENGTH_SHORT).show();
                makeBackup();
            }
        });

        Button restore = root.findViewById(R.id.backup_restore);
        restore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mMain, "Restoring Minima.. pls wait", Toast.LENGTH_SHORT).show();
                mMain.openFile(MainActivity.REQUEST_RESTORE);
            }
        });

        gDriveText = root.findViewById(R.id.text_gdrive);
        gDriveButton = root.findViewById(R.id.backup_gdrive);
        gDriveText.setVisibility(View.GONE);
        gDriveButton.setVisibility(View.GONE);

        updateGDriveTexts(root.getContext());

        return root;
    }

    private void updateGDriveTexts(Context context) {
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
        BackupSyncProvider.getGoogleDriveProvider(context).uploadBackup(context, MinimaBackupUtils.createBackup(mMain), authResultLauncher);
    }

    public void makeBackup() {

        Runnable bak = new Runnable() {
            @Override
            public void run() {
                File backupFile = MinimaBackupUtils.createBackup(mMain);
                if (backupFile != null) {
                    Uri backupUri = FileProvider.getUriForFile(mMain, minimaProviderAuthority, backupFile);
                    //Now share that file..
                    Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                    intentShareFile.setType("application/zip");
                    intentShareFile.putExtra(Intent.EXTRA_STREAM, backupUri);
                    intentShareFile.putExtra(Intent.EXTRA_SUBJECT, "Minima backup");
                    intentShareFile.putExtra(Intent.EXTRA_TEXT, "Here is my Minima backup");
                    startActivity(Intent.createChooser(intentShareFile, "Store your Minima Backup"));

                } else {
                    mMain.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mMain, "Error creating backup..", Toast.LENGTH_LONG).show();
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

    private static final String minimaProviderAuthority = "com.minima.android.provider";

}