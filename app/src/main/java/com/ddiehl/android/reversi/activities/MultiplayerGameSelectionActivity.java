package com.ddiehl.android.reversi.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.ddiehl.android.reversi.R;
import com.ddiehl.android.reversi.adapters.MatchSelectionAdapter;

import java.util.ArrayList;

public class MultiplayerGameSelectionActivity extends Activity {
	private final static String TAG = MultiplayerGameSelectionActivity.class.getSimpleName();
	private ListView mListView;
	private MatchSelectionAdapter mListAdapter;
	private ArrayList<String> mMatchList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_multiplayer_game_selection);

		mMatchList = new ArrayList<String>();

		mListView = (ListView) findViewById(R.id.matchList);
		mListAdapter = new MatchSelectionAdapter(this, R.layout.activity_multiplayer_game_selection_item, mMatchList);
		mListView.setAdapter(mListAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.multiplayer_game_selection, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
		switch(id) {
			case R.id.findNewMatch:
				return true;
		}
        return super.onOptionsItemSelected(item);
    }




}
