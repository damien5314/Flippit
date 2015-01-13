package com.ddiehl.android.reversi.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ddiehl.android.reversi.R;
import com.ddiehl.android.reversi.game.Board;
import com.ddiehl.android.reversi.game.BoardIterator;
import com.ddiehl.android.reversi.game.BoardSpace;
import com.ddiehl.android.reversi.game.ComputerAI;
import com.ddiehl.android.reversi.game.ReversiColor;
import com.ddiehl.android.reversi.game.ReversiPlayer;


public class SinglePlayerMatchActivity extends MatchActivity {
	private static final String TAG = SinglePlayerMatchActivity.class.getSimpleName();
	private static final String PREF_PLAYER_NAME = "pref_player_name";
	private static final String PREF_AI_DIFFICULTY = "pref_ai_difficulty";
    private static final String PREF_MATCH_STATE = "key_gamePrefs";
    private static final String PREF_CURRENT_PLAYER = "pref_currentPlayer";
    private static final String PREF_FIRST_TURN = "pref_firstTurn";
    private static final String PREF_BOARD_STATE = "pref_boardState";
    private Context ctx;
	private ReversiPlayer p1, p2, currentPlayer, firstTurn;
    private Board mBoard;
    private boolean matchInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reversi);
		// Hide select game panel for single player
		findViewById(R.id.board_panel_select_game).setVisibility(View.GONE);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        ctx = this;

		p1 = new ReversiPlayer(ReversiColor.Light, getString(R.string.player1_label_default));
		p2 = new ReversiPlayer(ReversiColor.Dark, getString(R.string.player2_label));
        p1.isCPU(getResources().getBoolean(R.bool.p1_cpu));
        p2.isCPU(getResources().getBoolean(R.bool.p2_cpu));

		mBoard = new Board(this);

        if (getSavedMatch()) {
            mBoard.displayBoard(this);
            updateScoreDisplay();
            matchInProgress = true;
        } else {
            matchInProgress = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        p1.setName(getPlayerName());
		((TextView)findViewById(R.id.label_p1)).setText(p1.getName());
		((TextView)findViewById(R.id.label_p2)).setText(p2.getName());
        if (matchInProgress && currentPlayer.isCPU())
            new ExecuteCPUMove().execute();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (matchInProgress)
            saveMatchToPrefs();
    }

    public boolean getSavedMatch() {
        SharedPreferences sp = getSharedPreferences(PREF_MATCH_STATE, 0);
        if (sp.contains(PREF_CURRENT_PLAYER)
                && sp.contains(PREF_FIRST_TURN)
                && sp.contains(PREF_BOARD_STATE)) {
            currentPlayer = (sp.getBoolean(PREF_CURRENT_PLAYER, true) ? p1 : p2);
            firstTurn = (sp.getBoolean(PREF_FIRST_TURN, true) ? p1 : p2);

			// TODO Need to convert to new game data format for backward compatibility
			String savedData = sp.getString(PREF_BOARD_STATE, "");
            mBoard.deserialize(savedData);
            return true;
        }
        return false;
    }

    public void saveMatchToPrefs() {
        SharedPreferences sp = getSharedPreferences(PREF_MATCH_STATE, 0);
        SharedPreferences.Editor e = sp.edit();
        e.putBoolean(PREF_CURRENT_PLAYER, (currentPlayer == p1));
        e.putBoolean(PREF_FIRST_TURN, (firstTurn == p1));
		byte[] bytes = mBoard.serialize();
		StringBuilder out = new StringBuilder();
		for (byte b : bytes)
			out.append(b);
        e.putString(PREF_BOARD_STATE, out.toString());
        e.apply();
    }

	private String getPlayerName() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		return prefs.getString(PREF_PLAYER_NAME, getString(R.string.player1_label_default));
	}

	public void selectMatch(View v) {
		// Hidden onCreate of Activity
	}

    public void startNewMatch(View v) {
        mBoard.reset();
        mBoard.displayBoard(this);
        switchFirstTurn();
        updateScoreDisplay();
        matchInProgress = true;

        // CPU takes first move if it has turn
        if (currentPlayer.isCPU())
            new ExecuteCPUMove().execute();
    }

	private void switchFirstTurn() {
		if (firstTurn == null)
			firstTurn = p1;
        else
    		firstTurn = (firstTurn == p1) ? p2 : p1;
		currentPlayer = firstTurn;
	}

    public void claim(final BoardSpace s) {
		if (s.isOwned() || currentPlayer.isCPU())
			return;

		if (mBoard.spacesCapturedWithMove(s, currentPlayer.getColor()) > 0) {
			mBoard.commitPiece(s, currentPlayer.getColor());
			calculateMatchState();
		} else
			Toast.makeText(ctx, R.string.bad_move, Toast.LENGTH_SHORT).show();
    }

    public void calculateMatchState() {
		ReversiPlayer opponent = (currentPlayer == p1) ? p2 : p1;
		if (mBoard.hasMove(opponent.getColor())) { // If opponent can make a move, it's his turn
            currentPlayer = opponent;
        } else if (mBoard.hasMove(currentPlayer.getColor())) { // Opponent has no move, keep turn
            Toast.makeText(this, getString(R.string.no_moves) + opponent.getName(), Toast.LENGTH_SHORT).show();
        } else { // No moves remaining, end of match
            updateScoreDisplay();
            endMatch();
            return;
        }
        updateScoreDisplay();
        if (currentPlayer.isCPU())
            new ExecuteCPUMove().execute();
    }

    private class ExecuteCPUMove extends AsyncTask<Void, Void, BoardSpace> {
		final long startTime = System.currentTimeMillis();

        @Override
        protected BoardSpace doInBackground(Void... voids) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
            int difficulty = Integer.valueOf(prefs.getString(PREF_AI_DIFFICULTY, "0"));
            BoardSpace move;
            switch (difficulty) {
                case 1:
                    move = ComputerAI.getBestMove_d1(mBoard, currentPlayer);
                    break;
                case 2:
                    move = ComputerAI.getBestMove_d3(mBoard, currentPlayer, (currentPlayer == p1) ? p2 : p1);
                    break;
                default:
                    move = null;
            }

            return move;
        }

        @Override
        protected void onPostExecute(final BoardSpace space) {
            long tt = System.currentTimeMillis() - startTime;
            // Add delay to 1 second if calculation takes less
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBoard.commitPiece(space, currentPlayer.getColor());
                    calculateMatchState();
                }
            }, Math.max(0, getResources().getInteger(R.integer.cpu_turn_delay) - tt));
        }
    }

    public void updateScoreDisplay() {
        int p1c = 0;
        int p2c = 0;
		BoardIterator i = new BoardIterator(mBoard);
		while (i.hasNext()) {
			BoardSpace s = i.next();
			if (s.isOwned()) {
				if (s.getColor() == ReversiColor.Light)
					p1c++;
				else
					p2c++;
			}
		}
        p1.setScore(p1c);
        p2.setScore(p2c);
		updateScoreForPlayer(p1);
		updateScoreForPlayer(p2);
        ((ImageView)findViewById(R.id.turn_indicator)).setImageResource(
                (currentPlayer == p1) ? R.drawable.ic_turn_indicator_p1 : R.drawable.ic_turn_indicator_p2);
    }

	public void updateScoreForPlayer(ReversiPlayer p) {
		TextView vScore;
		if (p == p1) vScore = (TextView) findViewById(R.id.score_p1);
		else vScore = (TextView) findViewById(R.id.score_p2);
		vScore.setText(String.valueOf(p.getScore()));
	}

	public void endMatch() {
		ReversiPlayer winner;
		if (p1.getScore() != p2.getScore()) {
			winner = (p1.getScore() > p2.getScore()) ? p1 : p2;
			showWinningToast(winner);
			int diff = 64 - p1.getScore() - p2.getScore();
			winner.setScore(winner.getScore() + diff);
			updateScoreForPlayer(winner);
		} else {
			showWinningToast(null);
		}
        switchFirstTurn();
        getSharedPreferences(PREF_MATCH_STATE, 0).edit().clear().apply();
        matchInProgress = false;
	}

	public void showWinningToast(ReversiPlayer winner) {
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
        getMenuInflater().inflate(R.menu.single_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (item.getItemId()) {
			case R.id.action_new_match:
				startNewMatch(findViewById(id));
				return true;
			case R.id.action_settings:
				Intent settings = new Intent(this, SettingsActivity.class);
				settings.putExtra(SettingsActivity.EXTRA_SETTINGS_MODE, SettingsActivity.SETTINGS_MODE_SINGLE_PLAYER);
				startActivity(settings);
				return true;
            case R.id.action_how_to_play:
                Intent htp = new Intent(this, HowToPlayActivity.class);
                startActivity(htp);
                return true;
		}
        return super.onOptionsItemSelected(item);
    }
}
