package com.ddiehl.android.flippit.activities;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.ddiehl.android.flippit.R;
import com.ddiehl.android.flippit.game.Board;
import com.ddiehl.android.flippit.game.BoardSpace;
import com.ddiehl.android.flippit.game.Player;
import com.ddiehl.android.flippit.game.ReversiColor;


public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getSimpleName();
	private Player p1;
	private Player p2;
	private Board b;
	private Player currentPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		p1 = new Player(ReversiColor.White);
		p2 = new Player(ReversiColor.Black);
		b = Board.getInstance(this);
    }

    private void startNewGame() {
        b.reset();
        displayBoard();
        currentPlayer = p1;
    }

    private void displayBoard() {
		TableLayout l = (TableLayout) findViewById(R.id.GameGrid);
        l.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        for (int y = 0; y < b.height(); y++) {
			TableRow r = new TableRow(this);
            r.setWeightSum(b.width());
            for (int x = 0; x < b.width(); x++) {
                BoardSpace s = b.getSpaceAt(x, y);
                s.setBackgroundResource(R.drawable.board_space);
//                if (s.hasPiece())
//                    s.setBackgroundColor( (s.piece().color() == ReversiColor.Black) ? Color.BLACK : Color.WHITE );
                TableRow.LayoutParams p = new TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.MATCH_PARENT, 1.0f);
                p.setMargins(5, 5, 5, 5);
                s.setLayoutParams(p);
                r.addView(s);
            }
			l.addView(r);
        }
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
