package com.ddiehl.android.flippit.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
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
import com.ddiehl.android.flippit.game.ComputerAI;
import com.ddiehl.android.flippit.game.Player;
import com.ddiehl.android.flippit.game.ReversiColor;
import com.ddiehl.android.flippit.utils.BoardIterator;


public class ReversiActivity extends Activity {
	private static final String TAG = ReversiActivity.class.getSimpleName();
	private static final String PREF_PLAYER_NAME = "pref_player_name";
	private static final String PREF_AI_DIFFICULTY = "pref_ai_difficulty";
    private Context ctx;
	protected Player p1, p2, currentPlayer;
    protected Player firstTurn;
    protected Board b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reversi);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        ctx = this;

		p1 = new Player(ReversiColor.White, getString(R.string.player1_label_default));
		p1.isCPU(getResources().getBoolean(R.bool.p1_cpu));
		p2 = new Player(ReversiColor.Black, getString(R.string.player2_label));
		p2.isCPU(true);

		b = Board.getInstance(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        saveGameToPrefs();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getSavedGame()) {
            Log.d(TAG, "Saved game detected, loaded from SharedPreferences.");
            displayBoard();
            updateScoreCounts();
        } else {
            Log.d(TAG, "No saved game detected in SharedPreferences.");
        }
		p1.setName(getPlayerName());
		((TextView)findViewById(R.id.p1_label)).setText(p1.getName());
		if (currentPlayer != null && currentPlayer.isCPU())
            new ExecuteCPUMove().execute();
    }

    private static final String KEY_CURRENT_PLAYER = "key_currentPlayer";
    private static final String KEY_FIRST_TURN = "key_firstTurn";
    private static final String KEY_BOARD_STATE = "key_boardState";

    public boolean getSavedGame() {
        SharedPreferences sp = getSharedPreferences("key_gamePrefs", 0);
        if (sp.contains(KEY_CURRENT_PLAYER)
                && sp.contains(KEY_FIRST_TURN)
                && sp.contains(KEY_BOARD_STATE)) {
            currentPlayer = (sp.getBoolean(KEY_CURRENT_PLAYER, true) ? p1 : p2);
            firstTurn = (sp.getBoolean(KEY_FIRST_TURN, true) ? p1 : p2);
            GameStorage.deserialize(this, sp.getString(KEY_BOARD_STATE, ""));
            Log.d(TAG, "SharedPreferences Contents");
            Log.d(TAG, "Current Player: " + currentPlayer.getName());
            Log.d(TAG, "First Turn: " + firstTurn.getName());
            Log.d(TAG, "Board Status: " + sp.getString(KEY_BOARD_STATE, ""));
            return true;
        }
        return false;
    }

    public void saveGameToPrefs() {
        SharedPreferences sp = getSharedPreferences("key_gamePrefs", 0);
        SharedPreferences.Editor e = sp.edit();
        e.putBoolean(KEY_CURRENT_PLAYER, (currentPlayer == p1));
        e.putBoolean(KEY_FIRST_TURN, (firstTurn == p1));
        e.putString(KEY_BOARD_STATE, GameStorage.serialize(b));
        e.apply();
    }

	private String getPlayerName() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		return prefs.getString(PREF_PLAYER_NAME, getString(R.string.player1_label_default));
	}

    private void startNewGame() {
		switchFirstTurn();
        b.reset();
        displayBoard();
        updateScoreCounts();
        calculateGameState();
    }

	private void switchFirstTurn() {
		if (firstTurn == null)
			firstTurn = p1;
		firstTurn = (firstTurn == p1) ? p2 : p1;
		currentPlayer = firstTurn;
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
                s.setOnClickListener(claim(s));
                r.addView(s);
            }
			l.addView(r);
        }
    }

    public View.OnClickListener claim(final BoardSpace s) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (s.isOwned() || currentPlayer.isCPU())
					return;

				if (b.spacesCapturedWithMove(s, currentPlayer.getColor()) > 0) {
					b.commitPiece(s, currentPlayer.getColor());
                    calculateGameState();
                } else {
                    Toast.makeText(ctx, R.string.bad_move, Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    public void calculateGameState() {
        updateScoreCounts();
		Player opponent = (currentPlayer == p1) ? p2 : p1;
		if (b.hasMove(opponent)) { // If opponent can make a move, it's his turn
            currentPlayer = opponent;
        } else if (b.hasMove(currentPlayer)) { // Opponent has no move, keep turn
            Toast.makeText(this, "No moves for " + opponent.getName(), Toast.LENGTH_SHORT).show();
        } else { // No moves remaining, end of game
            endGame();
            return;
        }

        findViewById(R.id.turnIndicator).setBackgroundResource(
                (currentPlayer == p1) ? R.drawable.ic_turn_indicator_p1 : R.drawable.ic_turn_indicator_p2);

        if (currentPlayer.isCPU()) {
            new ExecuteCPUMove().execute();
        }
    }

    private class ExecuteCPUMove extends AsyncTask<Void, Void, BoardSpace> {
		long startTime = System.currentTimeMillis();

        @Override
        protected BoardSpace doInBackground(Void... voids) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
            int difficulty = Integer.valueOf(prefs.getString(PREF_AI_DIFFICULTY, "0"));
            BoardSpace move;
            switch (difficulty) {
                case 1:
                    move = ComputerAI.getBestMove_d1(b, currentPlayer);
                    break;
                case 2:
                    move = ComputerAI.getBestMove_d3(b, currentPlayer, (currentPlayer == p1) ? p2 : p1);
                    break;
                default:
                    Log.e(TAG, "AI difficulty setting not recognized: " + difficulty);
                    move = null;
            }

            if (move == null) {
                Log.e(TAG, "ERROR: getBestMove did not return a BoardSpace.");
            }
            return move;
        }

        @Override
        protected void onPostExecute(BoardSpace space) {
			try { // Add delay to 1 second if calculation takes less
				long tt = System.currentTimeMillis() - startTime;
//                Log.d(TAG, "Time taken to calculate move: " + tt + "ms");
				Thread.sleep(Math.max(0, getResources().getInteger(R.integer.cpu_turn_delay) - tt));
			} catch (InterruptedException e) { }
            b.commitPiece(space, currentPlayer.getColor());
            calculateGameState();
        }
    }

    public void updateScoreCounts() {
        int p1c = 0;
        int p2c = 0;

		BoardIterator i = new BoardIterator(b);
		while (i.hasNext()) {
			BoardSpace s = i.next();
			if (s.isOwned()) {
				if (s.getColor() == ReversiColor.White)
					p1c++;
				else
					p2c++;
			}
		}

        p1.setScore(p1c);
        p2.setScore(p2c);
		updateScoreForPlayer(p1);
		updateScoreForPlayer(p2);
    }

	public void updateScoreForPlayer(Player p) {
		TextView vScore;
		if (p == p1) vScore = (TextView) findViewById(R.id.p1score);
		else vScore = (TextView) findViewById(R.id.p2score);
		vScore.setText(String.valueOf(p.getScore()));
	}

	public void endGame() {
		Player winner = null;

		if (p1.getScore() != p2.getScore())
			winner = (p1.getScore() > p2.getScore()) ? p1 : p2;
		showWinningToast(winner);
		int diff = 64 - p1.getScore() - p2.getScore();
		winner.setScore(winner.getScore() + diff);
		updateScoreForPlayer(winner);
	}

	public void showWinningToast(Player winner) {
		if (winner != null) {
			Toast t;
			if (winner == p1)
				t = Toast.makeText(this, getString(R.string.winner_p1), Toast.LENGTH_LONG);
			else
				t = Toast.makeText(this, getString(R.string.winner_cpu), Toast.LENGTH_LONG);
			t.setGravity(Gravity.CENTER|Gravity.CENTER, 0, 0);
			t.show();
		} else { // You tied
			Toast t = Toast.makeText(this, getString(R.string.winner_none), Toast.LENGTH_LONG);
			t.setGravity(Gravity.CENTER|Gravity.CENTER, 0, 0);
			t.show();
		}
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_new_game:
				startNewGame();
				return true;
			case R.id.action_settings:
				Intent intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				return true;

		}
        return super.onOptionsItemSelected(item);
    }
}
