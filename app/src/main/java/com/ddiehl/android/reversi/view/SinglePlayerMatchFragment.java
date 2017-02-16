package com.ddiehl.android.reversi.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ddiehl.android.reversi.R;
import com.ddiehl.android.reversi.game.Board;
import com.ddiehl.android.reversi.game.BoardSpace;
import com.ddiehl.android.reversi.game.ComputerAI;
import com.ddiehl.android.reversi.game.ReversiColor;
import com.ddiehl.android.reversi.game.ReversiPlayer;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.schedulers.Schedulers;

public class SinglePlayerMatchFragment extends MatchFragment {
    private static final String TAG = SinglePlayerMatchFragment.class.getSimpleName();

    private static final String PREF_PLAYER_NAME = "pref_player_name";
    private static final String PREF_AI_DIFFICULTY = "pref_ai_difficulty";
    private static final String PREF_CURRENT_PLAYER = "pref_currentPlayer";
    private static final String PREF_FIRST_TURN = "pref_firstTurn";
    private static final String PREF_BOARD_STATE = "pref_boardState";

    private ReversiPlayer p1, p2, currentPlayer, firstTurn;
    private boolean matchInProgress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);

        p1 = new ReversiPlayer(ReversiColor.Light, getString(R.string.player1_label_default));
        p2 = new ReversiPlayer(ReversiColor.Dark, getString(R.string.player2_label));
        p1.isCPU(getResources().getBoolean(R.bool.p1_cpu));
        p2.isCPU(getResources().getBoolean(R.bool.p2_cpu));

        mBoard = new Board();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        mStartNewMatchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNewMatch();
            }
        });

        // Hide select game panel for single player
        mSelectMatchButton.setVisibility(View.GONE);

        if (getSavedMatch()) {
            displayBoard();
            updateScoreDisplay();
            matchInProgress = true;
        } else {
            matchInProgress = false;
        }

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        p1.setName(getPlayerName());
        mPlayerOneLabelTextView.setText(p1.getName());
        mPlayerTwoLabelTextView.setText(p2.getName());
        if (matchInProgress && currentPlayer.isCPU()) {
            executeCpuMove();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (matchInProgress) {
            saveMatchToPrefs();
        }
    }

    public boolean getSavedMatch() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (sp.contains(PREF_CURRENT_PLAYER)
                && sp.contains(PREF_FIRST_TURN)
                && sp.contains(PREF_BOARD_STATE)) {
            currentPlayer = (sp.getBoolean(PREF_CURRENT_PLAYER, true) ? p1 : p2);
            firstTurn = (sp.getBoolean(PREF_FIRST_TURN, true) ? p1 : p2);

            String savedData = sp.getString(PREF_BOARD_STATE, "");
            mBoard.deserialize(savedData);
            updateBoardUi();
            return true;
        }
        return false;
    }

    public void saveMatchToPrefs() {
        byte[] bytes = mBoard.serialize();
        StringBuilder out = new StringBuilder();
        for (byte b : bytes)
            out.append(b);

        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                .putBoolean(PREF_CURRENT_PLAYER, (currentPlayer == p1))
                .putBoolean(PREF_FIRST_TURN, (firstTurn == p1))
                .putString(PREF_BOARD_STATE, out.toString())
                .apply();
    }

    private String getPlayerName() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        return prefs.getString(PREF_PLAYER_NAME, getString(R.string.player1_label_default));
    }

    @Override
    public void startNewMatch() {
        mBoard.reset();
        displayBoard();
        switchFirstTurn();
        updateScoreDisplay();
        matchInProgress = true;

        // CPU takes first move if it has turn
        if (currentPlayer.isCPU()) {
            executeCpuMove();
        }
    }

    @Override
    public void selectMatch() {
        // Button is hidden
    }

    @Override
    void handleSpaceClick(int row, int col) {
        if (currentPlayer.isCPU()) {
            // do nothing, this isn't a valid state
        } else {
            mBoard.requestClaimSpace(row, col, currentPlayer.getColor())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean success) {
                            updateBoardUi();
                            calculateMatchState();
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Toast.makeText(getActivity(), throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void updateBoardUi() {
        for (int i = 0; i < mMatchGridView.getChildCount(); i++) {
            ViewGroup row = (ViewGroup) mMatchGridView.getChildAt(i);
            for (int j = 0; j < row.getChildCount(); j++) {
                View space = row.getChildAt(j);
                updateSpace(space, mBoard, i, j);
            }
        }
    }

    private void updateSpace(final View view, final Board board, final int row, final int col) {
        BoardSpace space = board.spaceAt(row, col);
        if (space.getColor() == null) {
            view.setBackgroundResource(R.drawable.board_space_neutral);
        } else {
            switch (space.getColor()) {
                case Light:
                    view.setBackgroundResource(R.drawable.board_space_p1);
                    break;
                case Dark:
                    view.setBackgroundResource(R.drawable.board_space_p2);
                    break;
            }
        }
    }

    private void switchFirstTurn() {
        if (firstTurn == null) {
            firstTurn = p1;
        } else {
            firstTurn = (firstTurn == p1) ? p2 : p1;
        }
        currentPlayer = firstTurn;
    }

    public void calculateMatchState() {
        ReversiPlayer opponent = (currentPlayer == p1) ? p2 : p1;
        if (mBoard.hasMove(opponent.getColor())) { // If opponent can make a move, it's his turn
            currentPlayer = opponent;
        } else if (mBoard.hasMove(currentPlayer.getColor())) { // Opponent has no move, keep turn
            Toast.makeText(getActivity(), getString(R.string.no_moves) + opponent.getName(), Toast.LENGTH_SHORT).show();
        } else { // No moves remaining, end of match
            updateScoreDisplay();
            endMatch();
            return;
        }
        updateScoreDisplay();
        if (currentPlayer.isCPU()) {
            executeCpuMove();
        }
    }

    void executeCpuMove() {
        Observable.defer(new Func0<Observable<BoardSpace>>() {
            @Override
            public Observable<BoardSpace> call() {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                String difficulty = prefs.getString(PREF_AI_DIFFICULTY, "");
                BoardSpace move;
                switch (difficulty) {
                    case "1":
                        move = ComputerAI.getBestMove_d1(mBoard, currentPlayer);
                        break;
                    case "2":
                        move = ComputerAI.getBestMove_d3(mBoard, currentPlayer, (currentPlayer == p1) ? p2 : p1);
                        break;
                    default:
                        move = null;
                }

                return Observable.just(move);
            }
        })
                .delay(getResources().getInteger(R.integer.cpu_turn_delay), TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Action1<BoardSpace>() {
                            @Override
                            public void call(BoardSpace space) {
                                Log.d(TAG, "CPU move updated");
                                mBoard.commitPiece(space, currentPlayer.getColor());
                                updateBoardUi();
                                calculateMatchState();
                            }
                        }
                );
    }

    public void updateScoreDisplay() {
        int p1c = 0;
        int p2c = 0;
        Iterator<BoardSpace> i = mBoard.iterator();
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
        mTurnIndicator.setImageResource(
                (currentPlayer == p1) ? R.drawable.ic_turn_indicator_p1 : R.drawable.ic_turn_indicator_p2);
    }

    public void updateScoreForPlayer(ReversiPlayer p) {
        (p == p1 ? mPlayerOneScoreTextView : mPlayerTwoScoreTextView)
                .setText(String.valueOf(p.getScore()));
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
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .edit()
                .remove(PREF_CURRENT_PLAYER)
                .remove(PREF_FIRST_TURN)
                .remove(PREF_BOARD_STATE)
                .apply();
        matchInProgress = false;
    }

    public void displayBoard() {
        mBoardPanelView.setVisibility(View.GONE);
        mMatchGridView.setVisibility(View.VISIBLE);
    }

    public void showWinningToast(ReversiPlayer winner) {
        if (winner != null) {
            Toast t;
            if (winner == p1) {
                t = Toast.makeText(getActivity(), getString(R.string.winner_p1), Toast.LENGTH_LONG);
            } else {
                t = Toast.makeText(getActivity(), getString(R.string.winner_cpu), Toast.LENGTH_LONG);
            }
            t.setGravity(Gravity.CENTER | Gravity.CENTER, 0, 0);
            t.show();
        } else { // You tied
            Toast t = Toast.makeText(getActivity(), getString(R.string.winner_none), Toast.LENGTH_LONG);
            t.setGravity(Gravity.CENTER | Gravity.CENTER, 0, 0);
            t.show();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.single_player, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (item.getItemId()) {
            case R.id.action_new_match:
                startNewMatch();
                return true;
            case R.id.action_settings:
                Intent settings = new Intent(getActivity(), SettingsActivity.class);
                settings.putExtra(SettingsActivity.EXTRA_SETTINGS_MODE, SettingsActivity.SETTINGS_MODE_SINGLE_PLAYER);
                startActivity(settings);
                return true;
            case R.id.action_how_to_play:
                Intent htp = new Intent(getActivity(), HowToPlayActivity.class);
                startActivity(htp);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
