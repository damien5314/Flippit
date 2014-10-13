package com.ddiehl.android.flippit.activities;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.ddiehl.android.flippit.R;
import com.ddiehl.android.flippit.game.Board;
import com.ddiehl.android.flippit.game.BoardSpace;
import com.ddiehl.android.flippit.game.Player;
import com.ddiehl.android.flippit.game.ReversiColor;


public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getSimpleName();
    private Context c;
	private Player p1;
	private Player p2;
	private Board b;
	private Player currentPlayer = p2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        c = this;

		p1 = new Player(ReversiColor.White);
		p2 = new Player(ReversiColor.Black);
		b = Board.getInstance(this);
    }

    private void startNewGame() {
        b.reset();
        displayBoard();
        updateScoreCounts();
        changePlayerTurn();
    }

    private void displayBoard() {
		TableLayout l = (TableLayout) findViewById(R.id.GameGrid);
        l.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        l.removeAllViews();

        for (int y = 0; y < b.height(); y++) {
			TableRow r = new TableRow(this);
            r.setWeightSum(b.width());
            for (int x = 0; x < b.width(); x++) {
                BoardSpace s = b.getSpaceAt(x, y);
                TableRow.LayoutParams p = new TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, 1.0f);
                p.setMargins(5, 5, 5, 5);
                s.setLayoutParams(p);
                s.setOnClickListener(attemptToPlace(s));
                r.addView(s);
            }
			l.addView(r);
        }
    }

    public View.OnClickListener attemptToPlace(final BoardSpace s) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (b.setPieceOn(s.x, s.y, currentPlayer.getColor())) {
                    changePlayerTurn();
                    updateScoreCounts();
                } else {
                    Toast.makeText(c, R.string.bad_move, Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    public void updateScoreCounts() {
        int p1c = 0;
        int p2c = 0;

        for (int y = 0; y < b.height(); y++) {
            for (int x = 0; x < b.height(); x++) {
                BoardSpace s= b.getSpaceAt(x,y);
                if (s.isOwned()) {
                    if (s.getColor() == ReversiColor.White)
                        p1c++;
                    else
                        p2c++;
                }
            }
        }

        ((TextView) findViewById(R.id.p1score)).setText(String.valueOf(p1c));
        ((TextView) findViewById(R.id.p2score)).setText(String.valueOf(p2c));
    }

    public void changePlayerTurn() {
        if (currentPlayer == p1) {
            if (b.hasMove(p2)) currentPlayer = p2;
            else if (b.hasMove(p1)) {
                currentPlayer = p1;
            } else Toast.makeText(this, "No more moves.", Toast.LENGTH_LONG).show();
        } else {
            if (b.hasMove(p1)) currentPlayer = p1;
            else if (b.hasMove(p2)) {
                currentPlayer = p2;
            } else Toast.makeText(this, "No more moves.", Toast.LENGTH_LONG).show();
        }

        findViewById(R.id.turnIndicator).setBackgroundResource(
                (currentPlayer == p1) ? R.drawable.ic_turn_indicator_p1 : R.drawable.ic_turn_indicator_p2);
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
