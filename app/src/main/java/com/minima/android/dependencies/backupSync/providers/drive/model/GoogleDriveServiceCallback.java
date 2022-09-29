package com.minima.android.dependencies.backupSync.providers.drive.model;

import com.google.api.services.drive.Drive;

import javax.annotation.Nullable;

public interface GoogleDriveServiceCallback {
    void onGoogleDriveServiceAvailable(@Nullable Drive drive);
}
