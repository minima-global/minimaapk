package com.minima.android.dependencies.backupSync.providers.drive.model.userModel;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

public class GoogleDriveUserSignedInModel extends GoogleStateUserModel {
    final public GoogleSignInAccount googleSignInAccount;

    public GoogleDriveUserSignedInModel(GoogleSignInAccount googleSignInAccount) {
        this.googleSignInAccount = googleSignInAccount;
    }

    public String getEmail() {
        return googleSignInAccount.getEmail();
    }
}
