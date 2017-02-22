package com.ddiehl.android.reversi.view;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

public class MultiPlayerMatchActivity extends BaseActivity {

    @Override
    protected Fragment createFragment() {
        return new MultiPlayerMatchFragment();
    }

    @Override
    public void onSignInFailed() {

    }

    @Override
    public void onSignInSucceeded() {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }
}
