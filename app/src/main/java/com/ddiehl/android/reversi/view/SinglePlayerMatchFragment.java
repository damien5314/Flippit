package com.ddiehl.android.reversi.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.ddiehl.android.reversi.R;
import com.ddiehl.android.reversi.game.Board;
import com.ddiehl.android.reversi.game.BoardSpace;
import com.ddiehl.android.reversi.game.ComputerAI;
import com.ddiehl.android.reversi.game.ReversiPlayer;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.schedulers.Schedulers;

import static com.ddiehl.android.reversi.game.ReversiColor.Dark;
import static com.ddiehl.android.reversi.game.ReversiColor.Light;
import static com.ddiehl.android.reversi.view.SettingsActivity.EXTRA_SETTINGS_MODE;
import static com.ddiehl.android.reversi.view.SettingsActivity.SETTINGS_MODE_SINGLE_PLAYER;

public class SinglePlayerMatchFragment extends MatchFragment {
    private static final String TAG = SinglePlayerMatchFragment.class.getSimpleName();

    private static final String PREF_PLAYER_NAME = "pref_player_name";
    private static final String PREF_AI_DIFFICULTY = "pref_ai_difficulty";
    private static final String PREF_CURRENT_PLAYER = "pref_currentPlayer";
    private static final String PREF_FIRST_TURN = "pref_firstTurn";
    private static final String PREF_BOARD_STATE = "pref_boardState";

    private ReversiPlayer mP1, mP2;
    private ReversiPlayer mCurrentPlayer, mPlayerWithFirstTurn;
    private boolean mMatchInProgress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);

        mP1 = new ReversiPlayer(Light, getString(R.string.player1_label_default));
        mP2 = new ReversiPlayer(Dark, getString(R.string.player2_label));
        mP1.isCPU(getResources().getBoolean(R.bool.p1_cpu));
        mP2.isCPU(getResources().getBoolean(R.bool.p2_cpu));

        mBoard = new Board(8, 8);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        // Hide select match panel for single player
        mSelectMatchButton.setVisibility(View.GONE);

        if (getSavedMatch()) {
            displayBoard();
            updateScoreDisplay();
            mMatchInProgress = true;
        } else {
            mMatchInProgress = false;
        }

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        mP1.setName(getPlayerName());
        mPlayerOneLabelTextView.setText(mP1.getName());
        mPlayerTwoLabelTextView.setText(mP2.getName());
        if (mMatchInProgress && mCurrentPlayer.isCPU()) {
            executeCpuMove();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mMatchInProgress) {
            saveMatchToPrefs();
        }
    }

    public boolean getSavedMatch() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (sp.contains(PREF_CURRENT_PLAYER)
                && sp.contains(PREF_FIRST_TURN)
                && sp.contains(PREF_BOARD_STATE)) {
            mCurrentPlayer = (sp.getBoolean(PREF_CURRENT_PLAYER, true) ? mP1 : mP2);
            mPlayerWithFirstTurn = (sp.getBoolean(PREF_FIRST_TURN, true) ? mP1 : mP2);

            String savedData = sp.getString(PREF_BOARD_STATE, "");
            mBoard = new Board(mBoard.height(), mBoard.width(), savedData);
            updateBoardUi(false);
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
                .putBoolean(PREF_CURRENT_PLAYER, (mCurrentPlayer == mP1))
                .putBoolean(PREF_FIRST_TURN, (mPlayerWithFirstTurn == mP1))
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
        mMatchInProgress = true;

        // CPU takes first move if it has turn
        if (mCurrentPlayer.isCPU()) {
            executeCpuMove();
        }
    }

    @Override
    public void selectMatch() {
        // Button is hidden
    }

    @Override
    void handleSpaceClick(int row, int col) {
        if (mCurrentPlayer.isCPU()) {
            // do nothing, this isn't a valid state
        } else {
            mBoard.requestClaimSpace(row, col, mCurrentPlayer.getColor())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean success) {
                            updateBoardUi(true);
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

    private void updateBoardUi(boolean animate) {
        for (int i = 0; i < mMatchGridView.getChildCount(); i++) {
            ViewGroup row = (ViewGroup) mMatchGridView.getChildAt(i);
            for (int j = 0; j < row.getChildCount(); j++) {
                View space = row.getChildAt(j);
                updateSpace(space, mBoard, i, j, animate);
            }
        }
    }

    private void updateSpace(final View view, final Board board, final int row, final int col, final boolean animate) {
        BoardSpace space = board.spaceAt(row, col);
        if (space.getColor() == null) {
            if (view.getTag() == null || (int) view.getTag() != 0) {
                view.setTag(0);
                if (animate) {
                    animateBackgroundChange(view, R.drawable.board_space_neutral);
                } else {
                    view.setBackgroundResource(R.drawable.board_space_neutral);
                }
            }
        } else {
            switch (space.getColor()) {
                case Light:
                    if (view.getTag() == null || (int) view.getTag() != 1) {
                        view.setTag(1);
                        if (animate) {
                            animateBackgroundChange(view, R.drawable.board_space_p1);
                        } else {
                            view.setBackgroundResource(R.drawable.board_space_p1);
                        }
                    }
                    break;
                case Dark:
                    if (view.getTag() == null || (int) view.getTag() != 2) {
                        view.setTag(2);
                        if (animate) {
                            animateBackgroundChange(view, R.drawable.board_space_p2);
                        } else {
                            view.setBackgroundResource(R.drawable.board_space_p2);
                        }
                    }
                    break;
            }
        }
    }

    private void animateBackgroundChange(final @NonNull View view, final @DrawableRes int resId) {
        final Animation fadeOut = AnimationUtils.loadAnimation(getContext(), R.anim.playermove_fadeout);
        final Animation fadeIn = AnimationUtils.loadAnimation(getContext(), R.anim.playermove_fadein);

        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }

            @Override
            public void onAnimationRepeat(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setBackgroundResource(resId);
                view.startAnimation(fadeIn);
            }
        });

        view.startAnimation(fadeOut);
    }

    private void switchFirstTurn() {
        if (mPlayerWithFirstTurn == null) {
            mPlayerWithFirstTurn = mP1;
        } else {
            mPlayerWithFirstTurn = (mPlayerWithFirstTurn == mP1) ? mP2 : mP1;
        }
        mCurrentPlayer = mPlayerWithFirstTurn;
    }

    public void calculateMatchState() {
        ReversiPlayer opponent = (mCurrentPlayer == mP1) ? mP2 : mP1;
        if (mBoard.hasMove(opponent.getColor())) { // If opponent can make a move, it's his turn
            mCurrentPlayer = opponent;
        } else if (mBoard.hasMove(mCurrentPlayer.getColor())) { // Opponent has no move, keep turn
            Toast.makeText(getActivity(), getString(R.string.no_moves) + opponent.getName(), Toast.LENGTH_SHORT).show();
        } else { // No moves remaining, end of match
            updateScoreDisplay();
            endMatch();
            return;
        }
        updateScoreDisplay();
        if (mCurrentPlayer.isCPU()) {
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
                        move = ComputerAI.getBestMove_d1(mBoard, mCurrentPlayer);
                        break;
                    case "2":
                        move = ComputerAI.getBestMove_d3(mBoard, mCurrentPlayer, (mCurrentPlayer == mP1) ? mP2 : mP1);
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
                                mBoard.commitPiece(space, mCurrentPlayer.getColor());
                                updateBoardUi(true);
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
                if (s.getColor() == Light)
                    p1c++;
                else
                    p2c++;
            }
        }
        mP1.setScore(p1c);
        mP2.setScore(p2c);
        updateScoreForPlayer(mP1);
        updateScoreForPlayer(mP2);
        mTurnIndicator.setImageResource(
                (mCurrentPlayer == mP1) ? R.drawable.ic_turn_indicator_p1 : R.drawable.ic_turn_indicator_p2);
    }

    public void updateScoreForPlayer(ReversiPlayer p) {
        (p == mP1 ? mPlayerOneScoreTextView : mPlayerTwoScoreTextView)
                .setText(String.valueOf(p.getScore()));
    }

    public void endMatch() {
        ReversiPlayer winner;
        if (mP1.getScore() != mP2.getScore()) {
            winner = (mP1.getScore() > mP2.getScore()) ? mP1 : mP2;
            showWinningToast(winner);
            int diff = 64 - mP1.getScore() - mP2.getScore();
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
        mMatchInProgress = false;
    }

    public void displayBoard() {
        mBoardPanelView.setVisibility(View.GONE);
        mMatchGridView.setVisibility(View.VISIBLE);
        updateBoardUi(false);
    }

    public void showWinningToast(ReversiPlayer winner) {
        if (winner != null) {
            Toast toast;
            if (winner == mP1) {
                toast = Toast.makeText(getActivity(), getString(R.string.winner_p1), Toast.LENGTH_LONG);
            } else {
                toast = Toast.makeText(getActivity(), getString(R.string.winner_cpu), Toast.LENGTH_LONG);
            }
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        } else { // You tied
            Toast t = Toast.makeText(getActivity(), getString(R.string.winner_none), Toast.LENGTH_LONG);
            t.setGravity(Gravity.CENTER, 0, 0);
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
        switch (item.getItemId()) {
            case R.id.action_new_match:
                startNewMatch();
                return true;
            case R.id.action_settings:
                Intent settings = new Intent(getActivity(), SettingsActivity.class);
                settings.putExtra(EXTRA_SETTINGS_MODE, SETTINGS_MODE_SINGLE_PLAYER);
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
