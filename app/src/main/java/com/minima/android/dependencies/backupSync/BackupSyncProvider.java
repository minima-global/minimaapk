package com.minima.android.dependencies.backupSync;

import android.content.Context;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.minima.android.dependencies.backupSync.model.BackupUserStateCallback;
import com.minima.android.dependencies.backupSync.providers.drive.GoogleDriveProvider;

public abstract class BackupSyncProvider {
    /**
     * @param context              Context of the application (resources wise)
     * @param oAuth2ResultLauncher [ActivityResultLauncher] that will handle the
     *                             result back when logged in.
     */
    public abstract void auth(Context context, @NonNull ActivityResultLauncher<Intent> oAuth2ResultLauncher);

    /**
     * @param context Context of the application (resources wise)
     * @return user's state and possible info depending on the provider injected
     */
    public abstract void getUserState(Context context, BackupUserStateCallback backupUserStateCallback);

    /**
     * @param context             Context of the application (resources wise)
     * @param fileToUpload        File from the file system to upload.
     * @param retryResultLauncher [ActivityResultLauncher] that will handle the
     *                            and launch in case the user needs to do any
     *                            further operation.
     *                            <p>
     *                            Example: when the user denies the access of this
     *                            app to Google Drive once accepted before.
     *                            <p>
     *                            This parameter can be null, which means that it
     *                            won't try to retry any OAuth2 further action that
     *                            can be need, and it's usually null when we can to
     *                            do a silent upload.
     */
    public abstract void uploadBackup(Context context, java.io.File fileToUpload, @Nullable ActivityResultLauncher<Intent> retryResultLauncher);

    /**
     * @param context      Context of the application (resources wise)
     * @param fileToUpload File from the file system to upload.
     */
    public void uploadBackup(Context context, java.io.File fileToUpload) {
        uploadBackup(context, fileToUpload, null);
    }

    // Providers
    public static BackupSyncProvider getGoogleDriveProvider(Context context) {
        return GoogleDriveProvider.getInstance(context);
    }
}
