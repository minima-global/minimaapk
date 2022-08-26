package com.minima.android.dependencies.backupSync.providers.drive;

import android.content.Context;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.minima.android.dependencies.backupSync.BackupSyncProvider;
import com.minima.android.dependencies.backupSync.model.BackupUserStateCallback;
import com.minima.android.dependencies.backupSync.providers.drive.model.GoogleDriveServiceCallback;
import com.minima.android.dependencies.backupSync.providers.drive.model.fileModel.FolderCallback;
import com.minima.android.dependencies.backupSync.providers.drive.model.userModel.GoogleDriveUserNotSignedInYet;
import com.minima.android.dependencies.backupSync.providers.drive.model.userModel.GoogleDriveUserSignedInModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

import javax.annotation.Nullable;

public class GoogleDriveProvider extends BackupSyncProvider {
    final GoogleSignInClient googleSignInClient;

    private GoogleDriveProvider(Context context) {
        this.googleSignInClient = GoogleSignIn.getClient(
                context,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build()
        );
    }

    private static GoogleDriveProvider instance;

    public static GoogleDriveProvider getInstance(Context context) {
        if (instance == null) {
            instance = new GoogleDriveProvider(context);
        }

        return instance;
    }

    @Override
    public void auth(Context context, @NonNull ActivityResultLauncher<Intent> oAuth2ResultLauncher) {
        Intent intent = createSignInIntent();
        oAuth2ResultLauncher.launch(intent);
    }

    @Override
    public void getUserState(Context context, BackupUserStateCallback backupUserStateCallback) {
        final GoogleSignInAccount lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(context);
        if (lastSignedInAccount != null && !lastSignedInAccount.isExpired()) {
            // send user already there
            backupUserStateCallback.onUserState(
                    new GoogleDriveUserSignedInModel(lastSignedInAccount)
            );
        } else {
            // silent sign in
            Task<GoogleSignInAccount> googleSignInAccountTask = googleSignInClient.silentSignIn();
            googleSignInAccountTask
                    .addOnSuccessListener(googleSignInAccount -> backupUserStateCallback.onUserState(
                            new GoogleDriveUserSignedInModel(googleSignInAccountTask.getResult())
                    ))
                    .addOnFailureListener(e -> backupUserStateCallback.onUserState(
                            new GoogleDriveUserNotSignedInYet()
                    ));
        }
    }

    @Override
    public void uploadBackup(Context context, java.io.File fileToUpload, @Nullable ActivityResultLauncher<Intent> retry) {
        getDriveService(context, driveService -> {
            if (driveService != null) {
                createFolderIfNeeded(context, retry, folderId -> {
                    // Specify file
                    final File gDriveFile = new File();
                    final FileContent gDriveFileContent = new FileContent("application/gzip", fileToUpload);
                    gDriveFile.setName(fileToUpload.getName());
                    gDriveFile.setParents(new ArrayList<String>() {{
                        add(folderId);
                    }});

                    // Take it to another thread
                    new Thread(() -> {
                        try {
                            // Find existing backup if needed
                            Optional<File> fileFound = driveService.files().list()
                                    .setQ("mimeType='application/gzip' and trashed=false")
                                    .setSpaces("drive")
                                    .setKey(context.getApplicationInfo().packageName)
                                    .execute()
                                    .getFiles()
                                    .stream()
                                    .findFirst();

                            final File id;
                            if (fileFound.isPresent()) {
                                // Update File
                                File file = fileFound.get();
                                file.setParents(new ArrayList<String>() {{
                                    add(folderId);
                                }});
                                id = driveService
                                        .files()
                                        .update(
                                                file.getId(),
                                                null,
                                                gDriveFileContent
                                        )
                                        .execute();
                            } else {
                                // Create file
                                id = driveService
                                        .files()
                                        .create(gDriveFile, gDriveFileContent)
                                        .setFields("id")
                                        .setKey(context.getApplicationInfo().packageName)
                                        .execute();
                            }
                        } catch (UserRecoverableAuthIOException e) {
                            if (retry != null) {
                                retry.launch(e.getIntent());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                });
            }
        });
    }

    private void getDriveService(Context context, GoogleDriveServiceCallback googleDriveServiceCallback) {
        getUserState(context, backupUserStateModel -> {
            @Nullable final GoogleSignInAccount googleAccount;
            if (backupUserStateModel instanceof GoogleDriveUserSignedInModel) {
                googleAccount = ((GoogleDriveUserSignedInModel) backupUserStateModel).googleSignInAccount;
            } else {
                googleAccount = null;
            }

            if (googleAccount != null) {
                GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(context, new ArrayList<String>() {{
                    add(DriveScopes.DRIVE_FILE);
                }});

                credential.setSelectedAccount(googleAccount.getAccount());

                Drive.Builder builder = new Drive.Builder(
                        new NetHttpTransport(),
                        new JacksonFactory(),
                        credential
                );
                builder.setApplicationName(applicationName);
                googleDriveServiceCallback.onGoogleDriveServiceAvailable(builder.build());
            } else {
                googleDriveServiceCallback.onGoogleDriveServiceAvailable(null);
            }
        });
    }

    private void createFolderIfNeeded(Context context, ActivityResultLauncher<Intent> retry, FolderCallback folderCallback) {
        getDriveService(context, driveService -> {
            if (driveService != null) {
                new Thread(() -> {
                    try {
                        // Find the previous folder we created
                        Optional<File> folderFound = driveService.files().list()
                                .setQ("mimeType='application/vnd.google-apps.folder' and trashed=false")
                                .setSpaces("drive")
                                .setKey(context.getApplicationInfo().packageName)
                                .execute()
                                .getFiles()
                                .stream()
                                .findFirst();

                        if (folderFound.isPresent()) {
                            // Present this to the file creator
                            folderCallback.folderCreated(folderFound.get().getId());
                        } else {
                            // Create folder
                            final File gDriveFolder = new File();
                            gDriveFolder.setMimeType("application/vnd.google-apps.folder");
                            gDriveFolder.setName(folderBackupName);

                            File folderCreated = driveService
                                    .files()
                                    .create(gDriveFolder)
                                    .setFields("id")
                                    .setKey(context.getApplicationInfo().packageName)
                                    .execute();
                            folderCallback.folderCreated(folderCreated.getId());
                        }
                    } catch (UserRecoverableAuthIOException e) {
                        retry.launch(e.getIntent());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        });
    }

    @NonNull
    private Intent createSignInIntent() {
        return googleSignInClient.getSignInIntent();
    }

    private static final String applicationName = "Minima GDrive";
    private static final String folderBackupName = "Minima Backup";
}
