package com.ddiehl.android.reversi.view;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import timber.log.Timber;

public class LauncherActivity extends BaseActivity {

    @Override
    protected Fragment createFragment() {
        return new LauncherFragment();
    }

    @Override
    public void onSignInFailed() {
        Timber.d("Sign In FAILED: %s", getGameHelper().getSignInError().toString());
    }

    @Override
    public void onSignInSucceeded() {
        Timber.d("Sign In SUCCESS");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Timber.d("onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Timber.d("onConnectionSuspended");
    }
}
