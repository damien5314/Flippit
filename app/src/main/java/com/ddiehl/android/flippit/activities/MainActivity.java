package com.ddiehl.android.flippit.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.ddiehl.android.flippit.R;
import com.ddiehl.android.flippit.game.Board;
import com.ddiehl.android.flippit.game.Player;
import com.ddiehl.android.flippit.game.ReversiColor;


public class MainActivity extends Activity {
	private Player p1;
	private Player p2;
	private Board b;
	private Player currentPlayer;

	private void startNewGame() {
		b.reset();
		currentPlayer = p1;
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		p1 = new Player(ReversiColor.White);
		p2 = new Player(ReversiColor.Black);
		b = Board.getInstance();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

		switch (id) {

			case R.id.action_new_game:
				startNewGame();
				return true;

			case R.id.action_settings:
				return true;

		}

        return super.onOptionsItemSelected(item);
    }
}
