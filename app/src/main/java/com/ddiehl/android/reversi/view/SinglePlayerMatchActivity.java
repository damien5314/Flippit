package com.ddiehl.android.reversi.view;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;


public class SinglePlayerMatchActivity extends BaseActivity {

    @Override
    protected Fragment createFragment() {
        return new SinglePlayerMatchFragment();
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
