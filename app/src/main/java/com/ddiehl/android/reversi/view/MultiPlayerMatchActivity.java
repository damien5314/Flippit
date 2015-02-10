package com.ddiehl.android.reversi.view;

import android.support.v4.app.Fragment;

public class MultiPlayerMatchActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {
        return new MultiPlayerMatchFragment();
    }
}
