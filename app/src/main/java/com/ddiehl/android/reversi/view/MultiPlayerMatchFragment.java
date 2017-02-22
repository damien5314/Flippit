package com.ddiehl.android.reversi.view;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.ddiehl.android.reversi.R;
import com.ddiehl.android.reversi.game.Board;
import com.ddiehl.android.reversi.game.BoardSpace;
import com.ddiehl.android.reversi.game.ComputerAI;
import com.ddiehl.android.reversi.game.ReversiColor;
import com.ddiehl.android.reversi.game.ReversiPlayer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.ParticipantResult;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchUpdateReceivedListener;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer;
import com.google.android.gms.plus.Plus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiPlayerMatchFragment extends MatchFragment implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, OnTurnBasedMatchUpdateReceivedListener {
    private final static String TAG = MultiPlayerMatchFragment.class.getSimpleName();

    private static final int RC_RESOLVE_ERROR = 1001;
    private static final int RC_VIEW_MATCHES = 1002;
    private static final int RC_SELECT_PLAYERS = 1003;
    private static final int RC_SHOW_ACHIEVEMENTS = 1004;
    private static final int RC_SETTINGS = 1005;
    private static final int MAXIMUM_SCORE = 64;

    private static final String PREF_AUTO_SIGN_IN = "pref_auto_sign_in";

    private ProgressDialog mProgressBar;

    private GoogleApiClient mGoogleApiClient;

    private TurnBasedMatch mMatch;
    private Participant mPlayer, mOpponent;
    private Participant mLightPlayer, mDarkPlayer;
    private byte[] mMatchData;
    private int mLightScore, mDarkScore;

    private boolean mSignInOnStart = true;
    private boolean mSignOutOnConnect = false;
    private boolean mResolvingError = false;
    private boolean mUpdatingMatch = false;
    private boolean mIsSignedIn = false;

    private Handler mHandler;
    private List<BoardSpace> mQueuedMoves;

    private QueuedAction mQueuedAction;

    private enum QueuedAction {
        NewMatch, SelectMatch, ShowAchievements, ForfeitMatch
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBoard = new Board(8, 8);
        mSignInOnStart = getAutoConnectPreference();
        mHandler = new Handler();
        mQueuedMoves = new ArrayList<>();
        mGoogleApiClient = buildGoogleApiClient();

        if (getActivity().getIntent().hasExtra(Multiplayer.EXTRA_TURN_BASED_MATCH)) {
            mMatch = getActivity().getIntent().getParcelableExtra(Multiplayer.EXTRA_TURN_BASED_MATCH);
        }
    }

    // Create the Google API Client with access to Plus and Games
    GoogleApiClient buildGoogleApiClient() {
        return new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeWaitingAnimations();
    }

    private void connectGoogleApiClient() {
        // Check if Google Play Services are available
        int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());
        if (result != ConnectionResult.SUCCESS) {
            setAutoConnectPreference(false);
            showErrorDialog(result);
            return;
        }

        setAutoConnectPreference(true);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = buildGoogleApiClient();
        }

        showSpinner(3);
        mGoogleApiClient.connect();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mSignInOnStart) {
            connectGoogleApiClient();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mQueuedAction = null;
        if (mGoogleApiClient.isConnected()) {
            registerMatchUpdateListener(false);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        dismissSpinner();
        mIsSignedIn = true;

        if (mSignOutOnConnect) {
            signOutFromGooglePlay();
            return;
        }

        if (mQueuedAction != null) {
            switch (mQueuedAction) {
                case NewMatch:
                    mQueuedAction = null;
                    startNewMatchSelected();
                    return;
                case SelectMatch:
                    mQueuedAction = null;
                    selectMatchSelected();
                    return;
                case ForfeitMatch:
                    mQueuedAction = null;
                    forfeitMatchSelected();
                    return;
                case ShowAchievements:
                    mQueuedAction = null;
                    showAchievements();
                    return;
            }
        }

        registerMatchUpdateListener(true);

        if (mMatch != null) {
            if (mMatch.getData() == null) {
                startMatch(mMatch);
            } else {
                updateMatch(mMatch);
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        connectGoogleApiClient();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
//        Log.d(TAG, "Failed to connect to Google Play - Error: " + result.getErrorCode());
        dismissSpinner();

        if (mResolvingError) {
            return; // Already attempting to resolve an error
        }

        if (result.hasResolution()) {
//            Log.d(TAG, "Attempting to resolve error (ErrorCode: " + result.getErrorCode() + ")");
            try {
                mResolvingError = true;
                result.startResolutionForResult(getActivity(), RC_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                connectGoogleApiClient();
            }
        } else { // Unresolvable error
            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }
    }

    private void registerMatchUpdateListener(boolean b) {
        Games.TurnBasedMultiplayer.unregisterMatchUpdateListener(mGoogleApiClient);
        if (b) {
            Games.TurnBasedMultiplayer.registerMatchUpdateListener(mGoogleApiClient, this);
        }
    }

    private void displaySignInPrompt() {
        showDialog(
                new AlertDialog.Builder(getContext())
                        .setTitle(getString(R.string.dialog_sign_in_title))
                        .setMessage(getString(R.string.dialog_sign_in_message))
                        .setPositiveButton(getString(R.string.dialog_sign_in_confirm), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                connectGoogleApiClient();
                            }
                        })
                        .setNegativeButton(getString(R.string.dialog_sign_in_cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                mQueuedAction = null;
                            }
                        })
                        .create()
        );
    }

    public void startNewMatch() {
        Intent intent;
        if (!mGoogleApiClient.isConnected()) {
            mQueuedAction = QueuedAction.NewMatch;
            displaySignInPrompt();
            return;
        }
        intent = Games.TurnBasedMultiplayer.getSelectOpponentsIntent(mGoogleApiClient, 1, 1, true);
        startActivityForResult(intent, RC_SELECT_PLAYERS);
    }

    public void selectMatch() {
        if (!mGoogleApiClient.isConnected()) {
            mQueuedAction = QueuedAction.SelectMatch;
            displaySignInPrompt();
            return;
        }
        Intent intent = Games.TurnBasedMultiplayer.getInboxIntent(mGoogleApiClient);
        startActivityForResult(intent, RC_VIEW_MATCHES);
    }

    @Override
    void handleSpaceClick(int row, int col) {
        // TODO
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case RC_RESOLVE_ERROR:
                mResolvingError = false;
                if (resultCode == Activity.RESULT_OK) {
                    if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                        connectGoogleApiClient();
                    }
                }
                break;

            case RC_VIEW_MATCHES: // Returned from the 'Select Match' dialog
                if (resultCode == Activity.RESULT_OK) {
                    TurnBasedMatch match = data.getParcelableExtra(Multiplayer.EXTRA_TURN_BASED_MATCH);
                    if (match != null) {
                        if (match.getData() == null) {
                            startMatch(match);
                        } else {
                            updateMatch(match);
                        }
                    }
                } else if (resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) { // User signed out
                    mIsSignedIn = false;
                    signOutFromGooglePlay();
                } else {
                    showErrorDialog(resultCode);
                }
                break;

            case RC_SELECT_PLAYERS: // Returned from 'Select players to Invite' dialog
                if (resultCode == Activity.RESULT_OK) {
                    final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
                    Bundle autoMatchCriteria;

                    int minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
                    int maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);

                    if (minAutoMatchPlayers > 0) {
                        autoMatchCriteria = RoomConfig.createAutoMatchCriteria(minAutoMatchPlayers, maxAutoMatchPlayers, 0);
                    } else {
                        autoMatchCriteria = null;
                        Games.Achievements.unlock(mGoogleApiClient, getString(R.string.achievement_lets_play_together));
                    }

                    TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
                            .addInvitedPlayers(invitees)
                            .setAutoMatchCriteria(autoMatchCriteria)
                            .build();

                    showSpinner(1);
                    Games.TurnBasedMultiplayer.createMatch(mGoogleApiClient, tbmc).setResultCallback(
                            new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {
                                @Override
                                public void onResult(TurnBasedMultiplayer.InitiateMatchResult result) {
                                    processResult(result);
                                }
                            });
                } else if (resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) { // User signed out
                    mIsSignedIn = false;
                    signOutFromGooglePlay();
                } else {
                    showErrorDialog(resultCode);
                }
                break;

            case RC_SHOW_ACHIEVEMENTS:
                if (resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) { // User signed out
                    mIsSignedIn = false;
                    signOutFromGooglePlay();
                }
                break;

            case RC_SETTINGS:
                switch (resultCode) {
                    case SettingsActivity.RESULT_SIGN_IN:
                        connectGoogleApiClient();
                        break;

                    case SettingsActivity.RESULT_SIGN_OUT:
                        mSignOutOnConnect = true;
                        break;
                }
                break;
        }
    }

    private void signOutFromGooglePlay() {
        Toast.makeText(getActivity(), R.string.sign_out_confirmation, Toast.LENGTH_SHORT).show();

        mSignOutOnConnect = false;
        setAutoConnectPreference(false);
        if (mGoogleApiClient.isConnected() && mIsSignedIn) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            Games.signOut(mGoogleApiClient);
        }
        mIsSignedIn = false;
        mGoogleApiClient.disconnect();

        getActivity().setResult(SettingsActivity.RESULT_SIGN_OUT);
        getActivity().finish();
    }

    private void processResult(TurnBasedMultiplayer.InitiateMatchResult result) {
        TurnBasedMatch match = result.getMatch();
        mMatchData = null;

        if (!checkStatusCode(result.getStatus().getStatusCode())) {
            return;
        }

        if (match.getData() == null) {
            startMatch(match);
        } else {
            updateMatch(match);
        }
    }

    private void startMatch(TurnBasedMatch match) {
        mMatch = match;
        mMatchData = null;
        mBoard.reset();
        saveMatchData();
        displayBoard();
        updateScore();

        String participantId = mMatch.getParticipantId(Games.Players.getCurrentPlayerId(mGoogleApiClient));
        Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, match.getMatchId(),
                mMatchData, participantId).setResultCallback(
                new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                        processResult(result);
                    }
                });
    }

    private void processResult(TurnBasedMultiplayer.UpdateMatchResult result) {
        mMatch = result.getMatch();
        dismissSpinner();

        if (checkStatusCode(result.getStatus().getStatusCode())) {
            updateMatch(mMatch);
        }

        mUpdatingMatch = false;
    }

    private void updateMatch(TurnBasedMatch match) {
        mUpdatingMatch = true;
        mMatch = match;
        mPlayer = getCurrentPlayer();
        mOpponent = getOpponent();
        mLightPlayer = getLightPlayer();
        mDarkPlayer = getDarkPlayer();
        mMatchData = match.getData();

//        Log.d(TAG, "Match ID: " + mMatch.getMatchId());
//        Log.d(TAG, bytesToString(mMatchData));
//
//        Log.d(TAG, "Match Status: " + mMatch.getStatus());
//        Log.d(TAG, "Turn Status: " + mMatch.getTurnStatus());

        // Grab the appropriate segment from mMatchData based on player's color
        int startIndex = (getCurrentPlayer() == getLightPlayer()) ? 0 : 100;
        byte[] playerData = Arrays.copyOfRange(mMatchData, startIndex, startIndex + 64);

        mBoard = new Board(mBoard.height(), mBoard.width(), playerData);
        displayBoard();
        dismissSpinner();

        // Commit opponent's moves to the deserialized Board object
        // 0 [Light's Board] 64 [Dark's Moves] 100 [Dark's Board] 164 [Light's Moves]
        startIndex += 64;
        while (mMatchData[startIndex] != 0) {
            BoardSpace s = mBoard.getBoardSpaceFromNum(mMatchData[startIndex++]);
            mQueuedMoves.add(s);
        }

        mUpdatingMatch = false;
        if (!mQueuedMoves.isEmpty())
            processReceivedTurns();
        updateScore();

        // Check for inactive match states
        switch (mMatch.getStatus()) {
            case TurnBasedMatch.MATCH_STATUS_CANCELED:
                displayMessage(getString(R.string.match_canceled));
                return;
            case TurnBasedMatch.MATCH_STATUS_EXPIRED:
                displayMessage(getString(R.string.match_expired));
                return;
            case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
                displayMessage(getString(R.string.match_finding_partner));
                return;
            case TurnBasedMatch.MATCH_STATUS_COMPLETE:
                endMatch();
                return;
        }

        // OK, it's active. Check on turn status.
        switch (mMatch.getTurnStatus()) {
            case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN:
                dismissMessage();
//                autoplayIfEnabled();
                return;
            case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN:
                displayMessage(getString(R.string.match_opponent_turn));
                return;
            case TurnBasedMatch.MATCH_TURN_STATUS_INVITED:
                displayMessage(getString(R.string.match_invite_pending));
        }
    }

    private void saveMatchData() {
        byte[] playerBoard = mBoard.serialize();

        if (mMatchData == null) {
            mMatchData = new byte[256];
            System.arraycopy(playerBoard, 0, mMatchData, 0, playerBoard.length);
            System.arraycopy(playerBoard, 0, mMatchData, 100, playerBoard.length);
        } else {
            int startIndex = (mPlayer == mLightPlayer) ? 0 : 100;
            // Copy the serialized Board into the appropriate place in match data
            System.arraycopy(playerBoard, 0, mMatchData, startIndex, playerBoard.length);
            // Clear out the first 16 nodes following, which were the other player's previous moves
            for (int clearIndex = startIndex + 64; clearIndex < startIndex + 64 + 16; clearIndex++)
                mMatchData[clearIndex] = 0;
        }
    }

    private void processReceivedTurns() {
        mUpdatingMatch = true;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBoard.commitPiece(mQueuedMoves.remove(0), getOpponentColor());
                saveMatchData();
                if (!mQueuedMoves.isEmpty())
                    processReceivedTurns();
                else {
                    mUpdatingMatch = false;
                    updateScore();
//                    autoplayIfEnabled();
                }
            }
        }, getResources().getInteger(R.integer.cpu_turn_delay));
    }

    @Override
    public void onTurnBasedMatchReceived(TurnBasedMatch match) {
        if (mMatch != null && mMatch.getMatchId().equals(match.getMatchId())) {
            if (match.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN) {
                // Turn still belongs to opponent, wait for another update
                return;
            }
            updateMatch(match);
        }
    }

    @Override
    public void onTurnBasedMatchRemoved(String matchId) {
        if (mMatch != null && mMatch.getMatchId().equals(matchId)) {
            Toast.makeText(getActivity(), R.string.match_removed, Toast.LENGTH_SHORT).show();
            clearBoard();
        }
    }

    public void claim(final BoardSpace s) {
        if (mUpdatingMatch || !mQueuedMoves.isEmpty()) {
//            Log.d(TAG, "Error: Still evaluating last move");
            return;
        }

        if (mMatch.getStatus() != TurnBasedMatch.MATCH_STATUS_ACTIVE ||
                mMatch.getTurnStatus() != TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
            return;
        }

        if (s.isOwned())
            return;

        if (!mGoogleApiClient.isConnected()) {
            displaySignInPrompt();
            return;
        }

        ReversiColor playerColor = getCurrentPlayerColor();
        if (mBoard.spacesCapturedWithMove(s, playerColor) == 0) {
            Toast.makeText(getActivity(), R.string.bad_move, Toast.LENGTH_SHORT).show();
            return;
        }

        mUpdatingMatch = true;
        showSpinner(2);
        mBoard.commitPiece(s, playerColor);
        saveMatchData();

        // Add selected piece to the end of mMatchData array
        // 0 [Light's Board] 64 [Dark's Moves] 100 [Dark's Board] 164 [Light's Moves]
        int nextIndex = (mPlayer == mLightPlayer) ? 164 : 64;
        while (mMatchData[nextIndex] != 0)
            nextIndex++; // Increase index til we run into an unfilled index
        mMatchData[nextIndex] = mBoard.getSpaceNumber(s);

        updateMatchState();
    }

    private void updateMatchState() {
        if (mBoard.hasMove(getOpponentColor())) { // If opponent can make a move, it's his turn
            String pId = (mOpponent == null) ? null : mOpponent.getParticipantId();
            Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, mMatch.getMatchId(), mMatchData, pId)
                    .setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                        @Override
                        public void onResult(TurnBasedMultiplayer.UpdateMatchResult updateMatchResult) {
                            processResult(updateMatchResult);
                        }
                    });
        } else if (mBoard.hasMove(getCurrentPlayerColor())) { // Opponent has no move, keep turn
            Toast.makeText(getActivity(), getString(R.string.no_moves) + mOpponent.getDisplayName(), Toast.LENGTH_SHORT).show();
            Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, mMatch.getMatchId(), mMatchData, mPlayer.getParticipantId())
                    .setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                        @Override
                        public void onResult(TurnBasedMultiplayer.UpdateMatchResult updateMatchResult) {
                            processResult(updateMatchResult);
                        }
                    });
        } else { // No moves remaining, end of match
            endMatch();
            return;
        }
        updateScore();
    }

    private void endMatch() {
        updateScore();
        if (mMatch.getStatus() == TurnBasedMatch.MATCH_STATUS_COMPLETE) {
            switch (mPlayer.getResult().getResult()) {
                case ParticipantResult.MATCH_RESULT_WIN:
                    displayMessage(getString(R.string.winner_you));
                    break;
                case ParticipantResult.MATCH_RESULT_TIE:
                    displayMessage(getString(R.string.winner_tie));
                    break;
                case ParticipantResult.MATCH_RESULT_LOSS:
                    displayMessage(getString((mPlayer == mLightPlayer) ? R.string.winner_dark : R.string.winner_light));
                    break;
                default:
                    displayMessage(getString(R.string.match_complete));
                    break;
            }

            if (mMatch.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
                // Call finishMatch() to close out match for player
                Games.TurnBasedMultiplayer.finishMatch(mGoogleApiClient, mMatch.getMatchId())
                        .setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                            @Override
                            public void onResult(TurnBasedMultiplayer.UpdateMatchResult updateMatchResult) {
                                processResultFinishMatch(updateMatchResult);
                            }
                        });
            }
        } else { // Match is not yet finished
            ParticipantResult winnerResult, loserResult;
            if (mLightScore != mDarkScore) {
                if (mLightScore > mDarkScore) {
                    winnerResult = new ParticipantResult(mLightPlayer.getParticipantId(), ParticipantResult.MATCH_RESULT_WIN,
                            ParticipantResult.PLACING_UNINITIALIZED);
                    loserResult = new ParticipantResult(mDarkPlayer.getParticipantId(), ParticipantResult.MATCH_RESULT_LOSS,
                            ParticipantResult.PLACING_UNINITIALIZED);
                    displayMessage(getString((mPlayer == mLightPlayer) ? R.string.winner_you : R.string.winner_light));
                } else {
                    winnerResult = new ParticipantResult(mDarkPlayer.getParticipantId(), ParticipantResult.MATCH_RESULT_WIN,
                            ParticipantResult.PLACING_UNINITIALIZED);
                    loserResult = new ParticipantResult(mLightPlayer.getParticipantId(), ParticipantResult.MATCH_RESULT_LOSS,
                            ParticipantResult.PLACING_UNINITIALIZED);
                    displayMessage(getString((mPlayer == mDarkPlayer) ? R.string.winner_you : R.string.winner_dark));
                }
            } else {
                winnerResult = new ParticipantResult(mDarkPlayer.getParticipantId(), ParticipantResult.MATCH_RESULT_TIE,
                        ParticipantResult.PLACING_UNINITIALIZED);
                loserResult = new ParticipantResult(mLightPlayer.getParticipantId(), ParticipantResult.MATCH_RESULT_TIE,
                        ParticipantResult.PLACING_UNINITIALIZED);
                displayMessage(getString(R.string.winner_tie));
            }

            // Call finishMatch() with result parameters
            Games.TurnBasedMultiplayer.finishMatch(mGoogleApiClient, mMatch.getMatchId(), mMatchData, winnerResult, loserResult)
                    .setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                        @Override
                        public void onResult(TurnBasedMultiplayer.UpdateMatchResult updateMatchResult) {
                            processResultFinishMatch(updateMatchResult);
                        }
                    });
        }
    }

    private Participant getCurrentPlayer() {
        Participant lightPlayer = getLightPlayer();
        Participant darkPlayer = getDarkPlayer();
        if (mMatch.getParticipant(mMatch.getParticipantId(Games.Players.getCurrentPlayerId(mGoogleApiClient))) == lightPlayer)
            return lightPlayer;
        else return darkPlayer;
    }

    private Participant getOpponent() {
        return mMatch.getDescriptionParticipant();
    }

    private ReversiColor getCurrentPlayerColor() {
        if (mPlayer == mLightPlayer)
            return ReversiColor.Light;
        else return ReversiColor.Dark;
    }

    private ReversiColor getOpponentColor() {
        if (mOpponent == mLightPlayer)
            return ReversiColor.Light;
        else return ReversiColor.Dark;
    }

    private Participant getLightPlayer() {
        if (mMatch != null)
            return mMatch.getParticipant(mMatch.getCreatorId());
        return null;
    }

    private Participant getDarkPlayer() {
        if (mMatch != null) {
            ArrayList<String> participantIds = mMatch.getParticipantIds();
            String lightId = mMatch.getCreatorId();
            String darkId = null;

            for (String id : participantIds) {
                if (id.equals(lightId))
                    continue;
                darkId = id;
            }

            if (darkId != null)
                return mMatch.getParticipant(darkId);
        }
        return null;
    }

    private void displayBoard() {
        mMatchGridView.setVisibility(View.GONE);
        mBoardPanelView.setVisibility(View.VISIBLE);
    }

    private void clearBoard() {
        mMatch = null;
        mMatchGridView.setVisibility(View.GONE);
        mPlayerOneScoreTextView.setText("");
        mPlayerTwoScoreTextView.setText("");
        showWaitingIndicator(false, false);
        mBoardPanelView.setVisibility(View.VISIBLE);
    }

    private void updateScore() {
        mLightScore = mBoard.getNumSpacesForColor(ReversiColor.Light);
        mDarkScore = mBoard.getNumSpacesForColor(ReversiColor.Dark);

        if (mMatch.getStatus() == TurnBasedMatch.MATCH_STATUS_COMPLETE && !mUpdatingMatch) {
            // Add remaining spaces to winning count as per Reversi rules
            if (mLightPlayer.getResult().getResult() == ParticipantResult.MATCH_RESULT_WIN)
                mLightScore += mBoard.getNumberOfEmptySpaces();
            else if (mDarkPlayer.getResult().getResult() == ParticipantResult.MATCH_RESULT_WIN)
                mDarkScore += mBoard.getNumberOfEmptySpaces();
        }

        mPlayerOneScoreTextView.setText(String.valueOf(mLightScore));
        mPlayerTwoScoreTextView.setText(String.valueOf(mDarkScore));

        // Update turn indicator
        switch (mMatch.getTurnStatus()) {
            case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN:
                showWaitingIndicator(false, false);
                break;
            case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN:
                showWaitingIndicator(false, true);
                break;
            case TurnBasedMatch.MATCH_TURN_STATUS_INVITED:
                showWaitingIndicator(false, false);
                break;
            case TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE:
                showWaitingIndicator(false, false);
                break;
        }
    }

    private boolean checkStatusCode(int statusCode) {
        if (statusCode == GamesStatusCodes.STATUS_OK
                || statusCode == GamesStatusCodes.STATUS_NETWORK_ERROR_OPERATION_DEFERRED) {
            return true;
        }

        clearBoard();
        dismissSpinner();

        switch (statusCode) {
            case GamesStatusCodes.STATUS_MULTIPLAYER_ERROR_NOT_TRUSTED_TESTER:
                showAlertDialog(getString(R.string.dialog_error_title), getString(R.string.dialog_error_tester_untrusted));
                break;
            case GamesStatusCodes.STATUS_MATCH_ERROR_ALREADY_REMATCHED:
                showAlertDialog(getString(R.string.dialog_error_title), getString(R.string.dialog_error_already_rematched));
                break;
            case GamesStatusCodes.STATUS_NETWORK_ERROR_OPERATION_FAILED:
                showAlertDialog(getString(R.string.dialog_error_title), getString(R.string.dialog_error_network_operation_failed));
                break;
            case GamesStatusCodes.STATUS_CLIENT_RECONNECT_REQUIRED:
                showAlertDialog(getString(R.string.dialog_error_title), getString(R.string.dialog_error_reconnect_required));
                break;
            case GamesStatusCodes.STATUS_INTERNAL_ERROR:
                showAlertDialog(getString(R.string.dialog_error_title), getString(R.string.dialog_error_internal_error));
                break;
            case GamesStatusCodes.STATUS_MATCH_ERROR_INACTIVE_MATCH:
                showAlertDialog(getString(R.string.dialog_error_title), getString(R.string.dialog_error_inactive_match));
                break;
            case GamesStatusCodes.STATUS_MATCH_ERROR_LOCALLY_MODIFIED:
                showAlertDialog(getString(R.string.dialog_error_title), getString(R.string.dialog_error_locally_modified));
                break;
            default:
                showAlertDialog(getString(R.string.dialog_error_title), getString(R.string.dialog_error_message_default));
//                Log.w(TAG, "Unknown status code: " + statusCode);
                break;
        }

        return false;
    }

    private void displayMessage(String matchMsg) {
        mMatchMessageTextView.setText(matchMsg);
        mMatchMessageView.setVisibility(View.VISIBLE);

        // Start animations for side icons
        if (mLeftFadeOut != null && mRightFadeOut != null
                && !mLeftFadeOut.hasStarted() && !mRightFadeOut.hasStarted()) {
            mMatchMessageIcon1.startAnimation(mLeftFadeOut);
            mMatchMessageIcon2.startAnimation(mRightFadeOut);
        }
    }

    private void dismissMessage() {
        mMatchMessageView.setVisibility(View.INVISIBLE);
        mMatchMessageTextView.setText("");
        mLeftFadeOut.cancel();
        mRightFadeOut.cancel();
        mLeftFadeIn.cancel();
        mRightFadeIn.cancel();
    }

    private void askForRematch() {
        showDialog(
                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.dialog_rematch_title))
                        .setMessage(getString(R.string.dialog_rematch_message))
                        .setPositiveButton(getString(R.string.dialog_rematch_confirm), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (!mGoogleApiClient.isConnected()) {
                                    displaySignInPrompt();
                                    return;
                                }
                                showSpinner(1);
                                Games.TurnBasedMultiplayer.rematch(mGoogleApiClient, mMatch.getMatchId())
                                        .setResultCallback(new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {
                                            @Override
                                            public void onResult(TurnBasedMultiplayer.InitiateMatchResult result) {
                                                processResult(result);
                                            }
                                        });
                                mMatch = null;
                            }
                        })
                        .setNegativeButton(getString(R.string.dialog_rematch_cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // User canceled
                            }
                        })
                        .setIcon(getResources().getDrawable(R.drawable.ic_av_replay_blue))
                        .create()
        );
    }

    private void initializeWaitingAnimations() {
        mLeftFadeIn = AnimationUtils.loadAnimation(getActivity(), R.anim.waitingmessage_fadein);
        mLeftFadeOut = AnimationUtils.loadAnimation(getActivity(), R.anim.waitingmessage_fadeout);
        mRightFadeIn = AnimationUtils.loadAnimation(getActivity(), R.anim.waitingmessage_fadein);
        mRightFadeOut = AnimationUtils.loadAnimation(getActivity(), R.anim.waitingmessage_fadeout);

        mMatchMessageIcon1.setBackgroundResource(R.drawable.player_icon_p1);
        mMatchMessageIcon2.setBackgroundResource(R.drawable.player_icon_p2);

        mRightFadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Flip background resources & start animation
                mMatchMessageIcon2.setBackgroundResource(mMatchMessageIcon2Color
                        ? R.drawable.player_icon_p1 : R.drawable.player_icon_p2);
                mMatchMessageIcon2Color = !mMatchMessageIcon2Color;
                mMatchMessageIcon2.startAnimation(mRightFadeIn);
            }
        });

        mLeftFadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Flip background resources & start animation
                mMatchMessageIcon1.setBackgroundResource(mMatchMessageIcon1Color
                        ? R.drawable.player_icon_p1 : R.drawable.player_icon_p2);
                mMatchMessageIcon1Color = !mMatchMessageIcon1Color;
                mMatchMessageIcon1.startAnimation(mLeftFadeIn);
            }
        });

        mLeftFadeIn.setAnimationListener(
                new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) { }
                    @Override public void onAnimationRepeat(Animation animation) { }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mMatchMessageIcon1.startAnimation(mLeftFadeOut);
                                mMatchMessageIcon2.startAnimation(mRightFadeOut);
                            }
                        }, getResources().getInteger(R.integer.waiting_message_fade_delay));
                    }
                });
    }

    private void showSpinner(int spinnerMsg) {
        if (mProgressBar == null) {
            mProgressBar = new ProgressDialog(getActivity(), R.style.ProgressDialog);
            mProgressBar.setCancelable(false);
            mProgressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        }
        switch (spinnerMsg) {
            case 1:
                mProgressBar.setMessage(getString(R.string.loading_match));
                break;
            case 2:
                mProgressBar.setMessage(getString(R.string.submitting_move));
                break;
            case 3:
                mProgressBar.setMessage(getString(R.string.connecting));
                break;
            default:
                mProgressBar.setMessage(getString(R.string.please_wait));
        }
        mProgressBar.show();
    }

    private void dismissSpinner() {
        mProgressBar.dismiss();
    }

    private void showDialog(Dialog dialog) {
        if (mDisplayedDialog != null && mDisplayedDialog.isShowing()) {
            mDisplayedDialog.dismiss();
        }
        mDisplayedDialog = dialog;
        mDisplayedDialog.show();
    }

    // Generic warning/info dialog
    private void showAlertDialog(String title, String message) {
        showDialog(new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.dialog_error_confirm),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        })
                .create());
    }

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(errorCode, getActivity(), RC_RESOLVE_ERROR);
        if (dialog != null) {
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mResolvingError = false;
                }
            });
            dialog.show();
        }
    }

    private void startNewMatchSelected() {
        startNewMatch();
    }

    private void selectMatchSelected() {
        selectMatch();
    }

    private void settingsSelected() {
        Intent settings = new Intent(getActivity(), SettingsActivity.class);
        settings.putExtra(SettingsActivity.EXTRA_SETTINGS_MODE, SettingsActivity.SETTINGS_MODE_MULTI_PLAYER);
        boolean isSignedIn = mGoogleApiClient.isConnected();
        settings.putExtra(SettingsActivity.EXTRA_IS_SIGNED_IN, isSignedIn);
        settings.putExtra(SettingsActivity.EXTRA_SIGNED_IN_ACCOUNT,
                isSignedIn ? Plus.AccountApi.getAccountName(mGoogleApiClient) : "");
        startActivityForResult(settings, RC_SETTINGS);
    }

    private void forfeitMatchSelected() {
        if (mMatch == null) {
            Toast.makeText(getActivity(), R.string.no_match_selected, Toast.LENGTH_LONG).show();
            return;
        }

        if (!mGoogleApiClient.isConnected()) {
            mQueuedAction = QueuedAction.ForfeitMatch;
            displaySignInPrompt();
            return;
        }

        switch (mMatch.getStatus()) {
            case TurnBasedMatch.MATCH_STATUS_COMPLETE:
            case TurnBasedMatch.MATCH_STATUS_CANCELED:
            case TurnBasedMatch.MATCH_STATUS_EXPIRED:
                Toast.makeText(getActivity(), R.string.match_inactive, Toast.LENGTH_SHORT).show();
                break;
            case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
                showLeaveMatchDialog();
                break;
            case TurnBasedMatch.MATCH_STATUS_ACTIVE:
                if (mMatch.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
                    if (mOpponent == null) {
                        showLeaveMatchDialog();
                    } else {
                        if (mOpponent.getStatus() == Participant.STATUS_JOINED) {
                            showForfeitMatchDialog();
                        } else {
                            showCancelMatchDialog();
                        }
                    }
                } else {
                    showForfeitMatchForbiddenAlert();
                }
                break;
        }

    }

    private void showCancelMatchDialog() {
        showDialog(new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.dialog_cancel_match_title))
                .setMessage(getString(R.string.dialog_cancel_match_message))
                .setPositiveButton(getString(R.string.dialog_cancel_match_confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!mGoogleApiClient.isConnected()) {
                            displaySignInPrompt();
                            return;
                        }
                        Games.TurnBasedMultiplayer.cancelMatch(mGoogleApiClient, mMatch.getMatchId())
                                .setResultCallback(new ResultCallback<TurnBasedMultiplayer.CancelMatchResult>() {
                                    @Override
                                    public void onResult(TurnBasedMultiplayer.CancelMatchResult cancelMatchResult) {
                                        processResult(cancelMatchResult);
                                    }
                                });
                    }
                })
                .setNegativeButton(getString(R.string.dialog_cancel_match_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setCancelable(true)
                .create());
    }

    private void showForfeitMatchDialog() {
        showDialog(new AlertDialog.Builder(getActivity())
                .setTitle(R.string.dialog_forfeit_match_title)
                .setMessage(R.string.dialog_forfeit_match_message)
                .setPositiveButton(R.string.dialog_forfeit_match_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!mGoogleApiClient.isConnected()) {
                            displaySignInPrompt();
                            return;
                        }
                        ParticipantResult winnerResult = new ParticipantResult(mOpponent.getParticipantId(),
                                ParticipantResult.MATCH_RESULT_WIN, ParticipantResult.PLACING_UNINITIALIZED);
                        ParticipantResult loserResult = new ParticipantResult(mPlayer.getParticipantId(),
                                ParticipantResult.MATCH_RESULT_LOSS, ParticipantResult.PLACING_UNINITIALIZED);
                        // Give win to other player
                        Games.TurnBasedMultiplayer.finishMatch(mGoogleApiClient, mMatch.getMatchId(), mMatchData, winnerResult, loserResult)
                                .setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                                    @Override
                                    public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                                        if (result.getStatus().isSuccess()) {
                                            Toast.makeText(getActivity(), getString(R.string.forfeit_success), Toast.LENGTH_LONG).show();
                                            updateMatch(result.getMatch());
                                        } else {
                                            Toast.makeText(getActivity(), getString(R.string.forfeit_fail), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                    }
                })
                .setNegativeButton(R.string.dialog_forfeit_match_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setCancelable(true)
                .create());
    }

    private void showForfeitMatchForbiddenAlert() {
        showDialog(new AlertDialog.Builder(getActivity())
                .setTitle(R.string.dialog_forfeit_match_forbidden_title)
                .setMessage(R.string.dialog_forfeit_match_forbidden_message)
                .setPositiveButton(R.string.dialog_forfeit_match_forbidden_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setCancelable(true)
                .create());
    }

    private void showLeaveMatchDialog() {
        showDialog(new AlertDialog.Builder(getActivity())
                .setTitle(R.string.dialog_leave_match_title)
                .setMessage(R.string.dialog_leave_match_message)
                .setPositiveButton(R.string.dialog_leave_match_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!mGoogleApiClient.isConnected()) {
                            displaySignInPrompt();
                            return;
                        }

                        if (mMatch.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
                            Games.TurnBasedMultiplayer.leaveMatchDuringTurn(mGoogleApiClient, mMatch.getMatchId(), null)
                                    .setResultCallback(new ResultCallback<TurnBasedMultiplayer.LeaveMatchResult>() {
                                        @Override
                                        public void onResult(TurnBasedMultiplayer.LeaveMatchResult result) {
                                            processResultLeaveMatch(result);
                                        }
                                    });
                        } else {
                            Games.TurnBasedMultiplayer.leaveMatch(mGoogleApiClient, mMatch.getMatchId())
                                    .setResultCallback(new ResultCallback<TurnBasedMultiplayer.LeaveMatchResult>() {
                                        @Override
                                        public void onResult(TurnBasedMultiplayer.LeaveMatchResult result) {
                                            processResultLeaveMatch(result);
                                        }
                                    });
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_leave_match_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setCancelable(true)
                .create());
    }

    private void processResultFinishMatch(TurnBasedMultiplayer.UpdateMatchResult result) {
        mUpdatingMatch = false;
        dismissSpinner();
        if (checkStatusCode(result.getStatus().getStatusCode())) {
            mMatch = result.getMatch();
            mPlayer = getCurrentPlayer();
            // Update achievements
            if (mPlayer.getResult().getResult() == ParticipantResult.MATCH_RESULT_WIN) {
                Games.Achievements.unlock(mGoogleApiClient, getString(R.string.achievement_first_win));
                if (getPlayerScore() == MAXIMUM_SCORE) {
                    Games.Achievements.unlock(mGoogleApiClient, getString(R.string.achievement_the_perfect_win));
                }
            } else if (mPlayer.getResult().getResult() == ParticipantResult.MATCH_RESULT_TIE) {
                Games.Achievements.unlock(mGoogleApiClient, getString(R.string.achievement_evenly_matched));
            }
            Games.Achievements.increment(mGoogleApiClient, getString(R.string.achievement_up_coming), 1);
            Games.Achievements.increment(mGoogleApiClient, getString(R.string.achievement_reversi_expert), 1);

            if (mMatch.canRematch()) {
                askForRematch();
            }
        }
    }

    private void processResultLeaveMatch(TurnBasedMultiplayer.LeaveMatchResult result) {
        if (result.getStatus().isSuccess()) {
            Toast.makeText(getActivity(), R.string.match_canceled_toast, Toast.LENGTH_SHORT).show();
            clearBoard();
        } else {
            Toast.makeText(getActivity(), getString(R.string.cancel_fail), Toast.LENGTH_SHORT).show();
        }
    }

    private void processResult(TurnBasedMultiplayer.CancelMatchResult result) {
        if (result.getStatus().isSuccess()) {
            Toast.makeText(getActivity(), R.string.match_canceled_toast, Toast.LENGTH_SHORT).show();
            clearBoard();
        } else {
            Toast.makeText(getActivity(), getString(R.string.cancel_fail), Toast.LENGTH_SHORT).show();
        }
    }

    private int getPlayerScore() {
        return ((mPlayer == mLightPlayer) ? mLightScore : mDarkScore);
    }

    private void showAchievements() {
        if (mGoogleApiClient.isConnected()) {
            startActivityForResult(Games.Achievements.getAchievementsIntent(mGoogleApiClient), RC_SHOW_ACHIEVEMENTS);
        } else {
            mQueuedAction = QueuedAction.ShowAchievements;
            displaySignInPrompt();
        }
    }

    private boolean getAutoConnectPreference() {
        return PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(PREF_AUTO_SIGN_IN, false);
    }

    private void setAutoConnectPreference(boolean b) {
        mSignInOnStart = b;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.edit().putBoolean(PREF_AUTO_SIGN_IN, b).apply();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.multi_player, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_create_match:
                startNewMatchSelected();
                return true;
            case R.id.action_select_match:
                selectMatchSelected();
                return true;
            case R.id.action_how_to_play:
                Intent intent = new Intent(getActivity(), HowToPlayActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_close_match:
                clearBoard();
                return true;
            case R.id.action_forfeit_match:
                forfeitMatchSelected();
                return true;
            case R.id.action_achievements:
                showAchievements();
                return true;
            case R.id.action_settings:
                settingsSelected();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Added for testing full end-to-end multiplayer flow
    private void autoplayIfEnabled() {
        if (!mUpdatingMatch && getResources().getBoolean(R.bool.automated_multiplayer)
                && mMatch.getStatus() == TurnBasedMatch.MATCH_STATUS_ACTIVE
                && mMatch.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ReversiPlayer p1 = new ReversiPlayer((mPlayer == mLightPlayer) ? ReversiColor.Light : ReversiColor.Dark, "");
                    ReversiPlayer p2 = new ReversiPlayer((mPlayer == mLightPlayer) ? ReversiColor.Dark : ReversiColor.Light, "");
                    claim(ComputerAI.getBestMove_d3(mBoard, p1, p2));
//                    claim(ComputerAI.getBestMove_d1(mBoard, p1));
                }
            }, 500);
        }
    }

    // Used for converting Board to debugging text
    private String bytesToString(byte[] in) {
        if (in == null)
            return "";

        StringBuilder buf = new StringBuilder();

        buf.append("\n");
        for (int i = 0; i < 64; i++) {
            buf.append(String.valueOf(in[i]));
        }
        buf.append("\n");
        for (int i = 64; i < 100; i++) {
            buf.append(String.valueOf(in[i])).append(" ");
        }
        buf.append("\n");
        for (int i = 100; i < 164; i++) {
            buf.append(String.valueOf(in[i]));
        }
        buf.append("\n");
        for (int i = 164; i < 200; i++) {
            buf.append(String.valueOf(in[i])).append(" ");
        }

        return buf.toString();
    }
}
