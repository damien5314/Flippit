package com.ddiehl.android.flippit.activities;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Gravity;
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

		p1 = new Player(ReversiColor.White, getString(R.string.player1_label));
		p2 = new Player(ReversiColor.Black, getString(R.string.player2_label));
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

		b.getSpaceAt(3, 3).setClickable(false);
		b.getSpaceAt(4, 3).setClickable(false);
		b.getSpaceAt(3, 4).setClickable(false);
		b.getSpaceAt(4, 4).setClickable(false);
    }

    public View.OnClickListener attemptToPlace(final BoardSpace s) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (b.setPieceOn(s.x, s.y, currentPlayer.getColor())) {
					s.setClickable(false);
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

		p1.setScore(p1c);
		p2.setScore(p2c);

        ((TextView) findViewById(R.id.p1score)).setText(String.valueOf( p1.getScore() ));
        ((TextView) findViewById(R.id.p2score)).setText(String.valueOf( p2.getScore() ));
    }

    public void changePlayerTurn() {
		Player opponent = (currentPlayer == p1) ? p2 : p1;

		if (b.hasMove(opponent))
			currentPlayer = opponent;
		else if (b.hasMove(currentPlayer))
			Toast.makeText(this, "No moves for " + opponent.getName(), Toast.LENGTH_LONG).show();
		else
			endGame();

        findViewById(R.id.turnIndicator).setBackgroundResource(
                (currentPlayer == p1) ? R.drawable.ic_turn_indicator_p1 : R.drawable.ic_turn_indicator_p2);
    }

	public void endGame() {
		Player winner = null;

		if (p1.getScore() != p2.getScore())
			winner = (p1.getScore() > p2.getScore()) ? p1 : p2;

		if (winner != null) {
			Toast t = Toast.makeText(this, winner.getName() + " wins.", Toast.LENGTH_LONG);
			t.setGravity(Gravity.CENTER|Gravity.CENTER, 0, 0);
			t.show();
		} else { // You tied
			Toast t = Toast.makeText(this, getString(R.string.no_winner), Toast.LENGTH_LONG);
			t.setGravity(Gravity.CENTER|Gravity.CENTER, 0, 0);
			t.show();
		}

		// Remove on click functionality from all buttons

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
