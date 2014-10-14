package com.ddiehl.android.flippit.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.ddiehl.android.flippit.R;

public class LauncherActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
    }

    public void start1p(View view) {
        Intent i = new Intent(this, ReversiActivity.class);
        startActivity(i);
    }

    public void startMultiplayer(View view) {

    }
}
