package com.ddiehl.android.flippit.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.ddiehl.android.flippit.R;

public class LauncherActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_launcher);
    }

    public void start1p(View view) {
        Intent i = new Intent(this, ReversiActivity.class);
        startActivity(i);
    }

    public void startMultiplayer(View view) {
        Toast.makeText(this, getString(R.string.coming_soon), Toast.LENGTH_SHORT).show();
    }
}
