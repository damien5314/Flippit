package com.ddiehl.android.reversi.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.ddiehl.android.reversi.R;

public class LauncherActivity extends Activity {
	public static final String TAG = LauncherActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_launcher);

		Log.d(TAG, "Intent Action: " + getIntent().getAction());
		for (String category : getIntent().getCategories())
			Log.d(TAG, "Intent Category: " + category);
		Log.d(TAG, "Data String: " + getIntent().getDataString());
		Log.d(TAG, "Flags: " + getIntent().getFlags());

		if (getIntent().getExtras() != null) {
			for (String key : getIntent().getExtras().keySet())
				Log.d(TAG, "INTENT EXTRA: " + key);
		}

		if (savedInstanceState != null) {
			for (String key : savedInstanceState.keySet())
				Log.d(TAG, "SAVEDINSTANCE EXTRA: " + key);
		}
    }

    public void startSinglePlayer(View view) {
        startActivity(new Intent(this, SinglePlayerMatchActivity.class));
    }

    public void startMultiPlayer(View view) {
		startActivity(new Intent(this, MultiPlayerMatchActivity.class));
    }

}
