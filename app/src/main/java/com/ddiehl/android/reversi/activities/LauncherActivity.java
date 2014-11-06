package com.ddiehl.android.reversi.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.ddiehl.android.reversi.R;

public class LauncherActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_launcher);
    }

    public void start1p(View view) {
        startActivity(new Intent(this, ReversiActivity.class));
    }

    public void startMultiplayer(View view) {
		startActivity(new Intent(this, MultiplayerGameSelectionActivity.class));
    }
}
