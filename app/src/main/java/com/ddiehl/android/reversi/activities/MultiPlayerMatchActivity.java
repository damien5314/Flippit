package com.ddiehl.android.reversi.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.ddiehl.android.reversi.R;
import com.ddiehl.android.reversi.fragments.ErrorDialogFragment;
import com.ddiehl.android.reversi.game.Board;
import com.ddiehl.android.reversi.game.BoardIterator;
import com.ddiehl.android.reversi.game.BoardSpace;
import com.ddiehl.android.reversi.game.ReversiColor;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
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

public class MultiPlayerMatchActivity extends MatchActivity implements GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener, OnTurnBasedMatchUpdateReceivedListener {
	private final static String TAG = MultiPlayerMatchActivity.class.getSimpleName();

	public static final int RC_RESOLVE_ERROR = 1001;
	public static final int RC_VIEW_MATCHES = 1002;
	public static final int RC_SELECT_PLAYERS = 1003;

	private static final String DIALOG_ERROR = "dialog_error";

    private ProgressDialog progressBar;

    private GoogleApiClient mGoogleApiClient;

	private TurnBasedMatch mMatch;
	private Board pBoard; // Saves old Board in case of a connection failure
	private Board mBoard;
	private Participant mPlayer, mOpponent;
	private Participant mLightPlayer, mDarkPlayer;
	private byte[] mMatchData;

    private boolean resolvingError = false;
	private boolean updatingMatch = false;

	private Handler mHandler;
	private List<BoardSpace> mQueuedMoves;

	// Variables used for waiting animation
	private ImageView mWaiting1, mWaiting2;
	private Animation mLeftFadeOut, mLeftFadeIn, mRightFadeOut, mRightFadeIn;
	private boolean mWaitingLeftColor = false;
	private boolean mWaitingRightColor = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_reversi);
		mBoard = new Board(this);

		// Clear player names in score overlay
		((TextView) findViewById(R.id.p1_label)).setText(R.string.unknown_player);
		((TextView) findViewById(R.id.p2_label)).setText(R.string.unknown_player);

        // Create the Google API Client with access to Plus and Games
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();
		mHandler = new Handler();
		mQueuedMoves = new ArrayList<BoardSpace>();
		initializeWaitingAnimations();
    }

    private void connectGoogleApiClient() {
        if (progressBar != null && progressBar.isShowing())
            dismissSpinner();
        showSpinner(3);
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStart() {
        super.onStart();
//        Log.d(TAG, "onStart(): connecting");
		int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
//		Log.d(TAG, "IsGooglePlayServicesAvailable = " + result);
		if (result != ConnectionResult.SUCCESS) {
			GooglePlayServicesUtil.getErrorDialog(result, this, RC_RESOLVE_ERROR).show();
			return;
		} else connectGoogleApiClient();
    }

    @Override
    protected void onStop() {
        super.onStop();
//        Log.d(TAG, "onStop(): disconnecting");
        if (mGoogleApiClient.isConnected()) {
//            Log.d(TAG, "Unregistering match update listener");
            registerMatchUpdateListener(false);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        dismissSpinner();
//        Log.d(TAG, "Connected to Google Play Services");
		Toast.makeText(this, "Connected to Google Play", Toast.LENGTH_SHORT).show();
        registerMatchUpdateListener(true);
		if (mMatch != null)
			updateMatch(mMatch);
    }

    @Override
    public void onConnectionSuspended(int i) {
//        Log.d(TAG, "onConnectionSuspended(): attempting to connect");
        connectGoogleApiClient();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
//        Log.i(TAG, "Failed to connect to Google Play services");
		dismissSpinner();
        if (resolvingError) {
            return; // Already attempting to resolve an error
        } else if (result.hasResolution()) {
			Log.d(TAG, "Attempting to resolve error (ErrorCode: " + result.getErrorCode() + ")");
            try {
                resolvingError = true;
                result.startResolutionForResult(this, RC_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
				Log.e(TAG, "Unable to start resolution intent; Exception: " + e.getMessage());
                connectGoogleApiClient();
            }
        } else {
			Log.d(TAG, "Unresolvable error (ErrorCode: " + result.getErrorCode() + ")");
            showErrorDialog(result.getErrorCode());
            resolvingError = true;
        }
    }

    private void registerMatchUpdateListener(boolean b) {
        // Unregister any existing listener
        Games.TurnBasedMultiplayer.unregisterMatchUpdateListener(mGoogleApiClient);
        if (b) { // Register update listener to replace notifications when a match is open
//            Log.d(TAG, "Registering match update listener");
            Games.TurnBasedMultiplayer.registerMatchUpdateListener(mGoogleApiClient, this);
        }
    }

    public void onDialogDismissed() {
		resolvingError = false;
    }

	public void startNewMatch(View v) {
		Intent intent;
		if (!mGoogleApiClient.isConnected()) {
			Toast.makeText(this, R.string.google_play_not_connected, Toast.LENGTH_SHORT).show();
			return;
		}
		intent = Games.TurnBasedMultiplayer.getSelectOpponentsIntent(mGoogleApiClient, 1, 1, true);
		startActivityForResult(intent, RC_SELECT_PLAYERS);
	}

	public void selectMatch(View v) {
		Intent intent;
		if (!mGoogleApiClient.isConnected()) {
			Toast.makeText(this, R.string.google_play_not_connected, Toast.LENGTH_SHORT).show();
			return;
		}
		intent = Games.TurnBasedMultiplayer.getInboxIntent(mGoogleApiClient);
		startActivityForResult(intent, RC_VIEW_MATCHES);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {

			case RC_RESOLVE_ERROR:
				resolvingError = false;
				switch (resultCode) {
					case RESULT_OK:
						if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                            connectGoogleApiClient();
						}
						break;
				}
				break;

			case RC_VIEW_MATCHES: // Returned from the 'Select Match' dialog
				if (resultCode != Activity.RESULT_OK) // User canceled
					break;

				TurnBasedMatch match = data.getParcelableExtra(Multiplayer.EXTRA_TURN_BASED_MATCH);
				if (match != null) {
					Log.d(TAG, "Selected match: " + match.getMatchId());
					updateMatch(match);
				}
				break;

			case RC_SELECT_PLAYERS: // Returned from 'Select players to Invite' dialog
				if (resultCode != Activity.RESULT_OK) // User canceled
					return;

				final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
				Bundle autoMatchCriteria;

				int minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
				int maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);

				if (minAutoMatchPlayers > 0)
					autoMatchCriteria = RoomConfig.createAutoMatchCriteria(minAutoMatchPlayers, maxAutoMatchPlayers, 0);
				else autoMatchCriteria = null;

				TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
						.addInvitedPlayers(invitees)
						.setAutoMatchCriteria(autoMatchCriteria)
						.build();

				showSpinner(1);
				Games.TurnBasedMultiplayer.createMatch(mGoogleApiClient, tbmc).setResultCallback(
						new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {
							@Override
							public void onResult(TurnBasedMultiplayer.InitiateMatchResult result) {
//								Log.d(TAG, "TurnBasedMultiplayer match created");
                                processResult(result);
							}
						});
				break;
		}
	}

	private void processResult(TurnBasedMultiplayer.InitiateMatchResult result) {
		TurnBasedMatch match = result.getMatch();
		mMatchData = null;

        if (match.getData() == null)
            mBoard.reset();

		saveMatchData();

		if (!checkStatusCode(match, result.getStatus().getStatusCode())) {
			return;
		}

		if (match.getData() != null) { // This is a match that has already started, just update
			Log.d(TAG, bytesToString(match.getData()));
			updateMatch(match);
			return;
		}

		startMatch(match);
	}

	private void startMatch(TurnBasedMatch match) {
		mMatch = match;
		mBoard.reset();
		saveMatchData();
		displayBoard();
		updateScoreDisplay();

		String playerId = Games.Players.getCurrentPlayerId(mGoogleApiClient);
		String myParticipantId = mMatch.getParticipantId(playerId);

		Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, match.getMatchId(),
				mMatchData, myParticipantId).setResultCallback(
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

		if (!checkStatusCode(mMatch, result.getStatus().getStatusCode())) {
			Log.d(TAG, "Failure status code: " + result.getStatus().getStatusCode());
			showAlertDialog("Not connected", "Try move again");
			mBoard = pBoard;
			displayBoard();
			return;
		}

		updatingMatch = false;

		if (mMatch.canRematch()) {
//			askForRematch();
		}

		updateMatch(mMatch);
	}

	private void updateMatch(TurnBasedMatch match) {
		updatingMatch = true;
		mMatch = match;
		mPlayer = getCurrentPlayer();
		mOpponent = getOpponent();
		mLightPlayer = getLightPlayer();
		mDarkPlayer = getDarkPlayer();
		mMatchData = match.getData();

		// Grab the appropriate segment from mMatchData based on player's color
		int startIndex = (getCurrentPlayer() == getLightPlayer()) ? 0 : 100;
		byte[] playerData = Arrays.copyOfRange(mMatchData, startIndex, startIndex+64);

        mBoard.deserialize(playerData);
		findViewById(R.id.board_panels).setVisibility(View.GONE);
        displayBoard();
		updateScoreDisplay();
		updatePlayerNameDisplay();
		dismissSpinner();

		// Commit opponent's moves to the deserialized Board object
		// 0 [Light's Board] 64 [Dark's Moves] 100 [Dark's Board] 164 [Light's Moves]
		startIndex += 64;
		while (mMatchData[startIndex] != 0) {
			BoardSpace s = mBoard.getBoardSpaceFromNum(mMatchData[startIndex++]);
			Log.d(TAG, "Opponent moved @(" + s.x + " " + s.y + ")");
			mQueuedMoves.add(s);
		}

		updatingMatch = false;
		if (!mQueuedMoves.isEmpty())
			processReceivedTurns();

		int status = match.getStatus();
		int turnStatus = match.getTurnStatus();

		switch (status) {
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
				displayMessage(getString(R.string.match_complete));
				Games.TurnBasedMultiplayer.finishMatch(mGoogleApiClient, mMatch.getMatchId());
				return;
		}

		// OK, it's active. Check on turn status.
		switch (turnStatus) {
			case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN:
				dismissMessage();
				return;
			case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN: // Should return results.
				displayMessage(getString(R.string.match_opponent_turn));
				break;
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
			// Clear out the first 16 nodes following (which were the other player's previous moves)
			for (int clearIndex = startIndex+64; clearIndex < startIndex+64+16; clearIndex++)
				mMatchData[clearIndex] = 0;
		}

		Log.d(TAG, "Player's match data saved: " + bytesToString(mMatchData));
	}

	private void processReceivedTurns() {
		updatingMatch = true;
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				mBoard.commitPiece(mQueuedMoves.remove(0), getOpponentColor());
				updateScoreDisplay();
				saveMatchData();
				if (!mQueuedMoves.isEmpty())
					processReceivedTurns();
				else
					updatingMatch = false;
			}
		}, getResources().getInteger(R.integer.cpu_turn_delay));
	}

    @Override
    public void onTurnBasedMatchReceived(TurnBasedMatch match) {
		if (mMatch != null) {
			if (mMatch.getMatchId().equals(match.getMatchId()))
				updateMatch(match);
		}
    }

    @Override
    public void onTurnBasedMatchRemoved(String matchId) {
        // Don't think I actually need to implement this
		Log.d(TAG, "Match removed: " + matchId);
    }

	public void claim(final BoardSpace s) {
		if (mGoogleApiClient.isConnected()) {
			if (updatingMatch || !mQueuedMoves.isEmpty()) {
				Log.d(TAG, "Error: Still evaluating last move");
				return;
			}

			if (s.isOwned())
				return;

			Log.d(TAG, "Turn Status: " + mMatch.getTurnStatus() + " (My Turn Status = "
					+ TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN + ")");
			if (mMatch.getTurnStatus() != TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
				Toast.makeText(this, "Not your turn!", Toast.LENGTH_SHORT).show();
				return;
			}

			ReversiColor playerColor = getCurrentPlayerColor();

			if (mBoard.spacesCapturedWithMove(s, playerColor) > 0) {
				updatingMatch = true;
				showSpinner(2);
				pBoard = mBoard.copy();
				mBoard.commitPiece(s, playerColor);
				saveMatchData();

				// Add selected piece to the end of mMatchData array
				// 0 [White's Board] 64 [Dark's Moves] 100 [Dark's Board] 164 [White's Moves]
				int nextIndex = (mPlayer == mLightPlayer) ? 164 : 64;
				while (mMatchData[nextIndex] != 0)
					nextIndex++; // Increase index til we run into an unfilled index
				mMatchData[nextIndex] = mBoard.getSpaceNumber(s);
				Log.d(TAG, "Queued move for opponent's Board");
				Log.d(TAG, bytesToString(mMatchData));

				updateMatchState();
			} else {
				Toast.makeText(this, R.string.bad_move, Toast.LENGTH_SHORT).show();
			}
		}
		else // TODO Change this to display a sign-in dialog
			Log.d(TAG, "GoogleApiClient not connected");
	}

	private void updateMatchState() {
		if (mBoard.hasMove(getOpponentColor())) { // If opponent can make a move, it's his turn
            String pId = (mOpponent == null) ? null : mOpponent.getParticipantId();
			Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, mMatch.getMatchId(), mMatchData, pId)
                    .setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
						@Override
						public void onResult(TurnBasedMultiplayer.UpdateMatchResult updateMatchResult) {
							Log.d(TAG, "Turn updated, opponent receives turn. Result: " + updateMatchResult.getStatus());
							processResult(updateMatchResult);
						}
					});
		} else if (mBoard.hasMove(getCurrentPlayerColor())) { // Opponent has no move, keep turn
			Toast.makeText(this, getString(R.string.no_moves) + mOpponent.getDisplayName(), Toast.LENGTH_SHORT).show();
			Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, mMatch.getMatchId(), mMatchData, mPlayer.getParticipantId())
                    .setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
						@Override
						public void onResult(TurnBasedMultiplayer.UpdateMatchResult updateMatchResult) {
							Log.d(TAG, "Turn updated, player keeps turn. Result: " + updateMatchResult.getStatus());
							processResult(updateMatchResult);
						}
					});
		} else { // No moves remaining, end of match
            updatingMatch = false;
            dismissSpinner();
			updateScoreDisplay();
			endMatch();
			return;
		}
		updateScoreDisplay();
	}

	private void endMatch() {
		// Calculate score for each piece
		int lightCount = 0;
		int darkCount = 0;
		BoardIterator i = new BoardIterator(mBoard);

		while (i.hasNext()) {
			BoardSpace s = i.next();
			if (s.isOwned()) {
				if (s.getColor() == ReversiColor.White)
					lightCount++;
				else
					darkCount++;
			}
		}

		// Add remaining spaces to winning count as per Reversi rules
		addRemainingSpacesToWinningCount(lightCount, darkCount);

		// Generate ParticipantResult objects for passing to finishMatch()
		ParticipantResult winnerResult, loserResult;
		if (lightCount != darkCount) {
			if (lightCount > darkCount) {
				winnerResult = new ParticipantResult(mLightPlayer.getParticipantId(), ParticipantResult.MATCH_RESULT_WIN,
						ParticipantResult.PLACING_UNINITIALIZED);
				loserResult = new ParticipantResult(mDarkPlayer.getParticipantId(), ParticipantResult.MATCH_RESULT_LOSS,
						ParticipantResult.PLACING_UNINITIALIZED);
				showAlertDialog(getString(R.string.game_over), getString(R.string.winner_light));
			} else {
				winnerResult = new ParticipantResult(mDarkPlayer.getParticipantId(), ParticipantResult.MATCH_RESULT_WIN,
						ParticipantResult.PLACING_UNINITIALIZED);
				loserResult = new ParticipantResult(mLightPlayer.getParticipantId(), ParticipantResult.MATCH_RESULT_LOSS,
						ParticipantResult.PLACING_UNINITIALIZED);
				showAlertDialog(getString(R.string.game_over), getString(R.string.winner_dark));
			}
		} else {
			winnerResult = new ParticipantResult(mDarkPlayer.getParticipantId(), ParticipantResult.MATCH_RESULT_TIE,
					ParticipantResult.PLACING_UNINITIALIZED);
			loserResult = new ParticipantResult(mLightPlayer.getParticipantId(), ParticipantResult.MATCH_RESULT_TIE,
					ParticipantResult.PLACING_UNINITIALIZED);
			showAlertDialog(getString(R.string.game_over), getString(R.string.winner_tie));
		}

		// Call finishMatch() with results
		Games.TurnBasedMultiplayer.finishMatch(mGoogleApiClient, mMatch.getMatchId(), mMatchData, winnerResult, loserResult);
	}

	private void addRemainingSpacesToWinningCount(int lightCount, int darkCount) {
		int diff = 64 - lightCount - darkCount;
		if (lightCount > darkCount)
			((TextView) findViewById(R.id.p1score)).setText(String.valueOf(lightCount+diff));
		else if (darkCount > lightCount)
			((TextView) findViewById(R.id.p2score)).setText(String.valueOf(darkCount+diff));
	}

	private Participant getCurrentPlayer() {
		Participant lightPlayer = getLightPlayer();
		Participant darkPlayer = getDarkPlayer();
		if (mMatch.getParticipant(mMatch.getParticipantId(
				Games.Players.getCurrentPlayerId(mGoogleApiClient))) == lightPlayer)
			return lightPlayer;
		else return darkPlayer;
	}

	private Participant getOpponent() {
		return mMatch.getDescriptionParticipant();
	}

	private ReversiColor getCurrentPlayerColor() {
		if (mPlayer == mLightPlayer)
			return ReversiColor.White;
		else return ReversiColor.Black;
	}

	private ReversiColor getOpponentColor() {
		if (mOpponent == mLightPlayer)
			return ReversiColor.White;
		else return ReversiColor.Black;
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

	// This is duplicated between the single player activity and multiplayer activity
	// Consider refactoring this to a static method in another class to decrease code duplication
	private void displayBoard() {
		TableLayout grid = (TableLayout) findViewById(R.id.MatchGrid);
		grid.setVisibility(View.GONE); // Hide the view until we finish adding children
		grid.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		grid.removeAllViews();

		int bHeight = (int) getResources().getDimension(R.dimen.space_row_height);
		int bMargin = (int) getResources().getDimension(R.dimen.space_padding);

		for (int y = 0; y < mBoard.height(); y++) {
			TableRow row = new TableRow(this);
			row.setWeightSum(mBoard.width());
			for (int x = 0; x < mBoard.width(); x++) {
				BoardSpace space = mBoard.getSpaceAt(x, y);
				TableRow.LayoutParams params = new TableRow.LayoutParams(0, bHeight, 1.0f);
				params.setMargins(bMargin, bMargin, bMargin, bMargin);
				space.setLayoutParams(params);
				space.setOnClickListener(
						new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								claim((BoardSpace) v);
							}
						});
				row.addView(space);
			}
			grid.addView(row);
		}
		grid.setVisibility(View.VISIBLE);
	}

	private void updatePlayerNameDisplay() {
		if (mLightPlayer != null) {
			((TextView) findViewById(R.id.p1_label)).setText(mLightPlayer.getDisplayName());
		} else {
			((TextView) findViewById(R.id.p1_label)).setText(R.string.unknown_player);
		}

		if (mDarkPlayer != null) {
			((TextView) findViewById(R.id.p2_label)).setText(mDarkPlayer.getDisplayName());
		} else {
			((TextView) findViewById(R.id.p2_label)).setText(R.string.unknown_player);
		}
	}

	private void updateScoreDisplay() {
		int p1c = 0, p2c = 0;
		BoardIterator i = new BoardIterator(mBoard);
		while (i.hasNext()) {
			BoardSpace s = i.next();
			if (s.isOwned()) {
				if (s.getColor() == ReversiColor.White) p1c++;
				else p2c++;
			}
		}
		((TextView) findViewById(R.id.p1score)).setText(String.valueOf(p1c));
		((TextView) findViewById(R.id.p2score)).setText(String.valueOf(p2c));

		// Update turn indicator
		ImageView turnIndicator = (ImageView) findViewById(R.id.turnIndicator);
		int myTurnResource = (mPlayer == mLightPlayer) ?
				R.drawable.ic_turn_indicator_p1 : R.drawable.ic_turn_indicator_p2;
		int oppTurnResource = (myTurnResource == R.drawable.ic_turn_indicator_p1) ?
				R.drawable.ic_turn_indicator_p2 : R.drawable.ic_turn_indicator_p1;
		switch (mMatch.getTurnStatus()) {
			case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN:
				turnIndicator.setImageResource(myTurnResource);
				break;
			case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN:
				turnIndicator.setImageResource(oppTurnResource);
				break;
			case TurnBasedMatch.MATCH_TURN_STATUS_INVITED:
				turnIndicator.setImageResource(R.drawable.ic_turn_neutral);
				break;
			case TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE:
				turnIndicator.setImageResource(R.drawable.ic_turn_neutral);
				break;
		}
	}

	// Returns false if something went wrong, probably. This should handle
	// more cases, and probably report more accurate results.
	private boolean checkStatusCode(TurnBasedMatch match, int statusCode) {
		switch (statusCode) {
			case GamesStatusCodes.STATUS_OK:
				return true;
			case GamesStatusCodes.STATUS_NETWORK_ERROR_OPERATION_DEFERRED:
				// Action deferred on Google Play until later
				return true;
			case GamesStatusCodes.STATUS_MULTIPLAYER_ERROR_NOT_TRUSTED_TESTER:
				showAlertDialog(getString(R.string.dialog_warning), getString(R.string.status_multiplayer_error_not_trusted_tester));
				break;
			case GamesStatusCodes.STATUS_MATCH_ERROR_ALREADY_REMATCHED:
				showAlertDialog(getString(R.string.dialog_warning), getString(R.string.match_error_already_rematched));
				break;
			case GamesStatusCodes.STATUS_NETWORK_ERROR_OPERATION_FAILED:
				showAlertDialog(getString(R.string.dialog_warning), getString(R.string.network_error_operation_failed));
				break;
			case GamesStatusCodes.STATUS_CLIENT_RECONNECT_REQUIRED:
				showAlertDialog(getString(R.string.dialog_warning), getString(R.string.client_reconnect_required));
				break;
			case GamesStatusCodes.STATUS_INTERNAL_ERROR:
				showAlertDialog(getString(R.string.dialog_warning), getString(R.string.internal_error));
				break;
			case GamesStatusCodes.STATUS_MATCH_ERROR_INACTIVE_MATCH:
				showAlertDialog(getString(R.string.dialog_warning), getString(R.string.match_error_inactive_match));
				break;
			case GamesStatusCodes.STATUS_MATCH_ERROR_LOCALLY_MODIFIED:
				showAlertDialog(getString(R.string.dialog_warning), getString(R.string.match_error_locally_modified));
				break;
			default:
				showAlertDialog(getString(R.string.dialog_warning), getString(R.string.unexpected_status));
				Log.d(TAG, "Did not have warning or string to deal with: " + statusCode);
		}

		return false;
	}

	private void displayMessage(String matchMsg) {
		((TextView) findViewById(R.id.matchMessageText)).setText(matchMsg);
		findViewById(R.id.matchMessage).setVisibility(View.VISIBLE);

		// Start animations for side icons
		if (mLeftFadeOut != null && mRightFadeOut != null
				&& !mLeftFadeOut.hasStarted() && !mRightFadeOut.hasStarted()) {
			mWaiting1.startAnimation(mLeftFadeOut);
			mWaiting2.startAnimation(mRightFadeOut);
		}
	}

	private void dismissMessage() {
		findViewById(R.id.matchMessage).setVisibility(View.GONE);
		((TextView) findViewById(R.id.matchMessageText)).setText("");
		mLeftFadeOut.cancel();
		mRightFadeOut.cancel();
		mLeftFadeIn.cancel();
		mRightFadeIn.cancel();
	}

	private void initializeWaitingAnimations() {
		mLeftFadeIn = AnimationUtils.loadAnimation(this, R.anim.waitingmessage_fadein);
		mLeftFadeOut = AnimationUtils.loadAnimation(this, R.anim.waitingmessage_fadeout);
		mRightFadeIn = AnimationUtils.loadAnimation(this, R.anim.waitingmessage_fadein);
		mRightFadeOut = AnimationUtils.loadAnimation(this, R.anim.waitingmessage_fadeout);

		mWaiting1 = (ImageView) findViewById(R.id.waiting_icon1);
		mWaiting2 = (ImageView) findViewById(R.id.waiting_icon2);
		mWaiting1.setBackgroundResource(R.drawable.player_icon_p1);
		mWaiting2.setBackgroundResource(R.drawable.player_icon_p2);

		mRightFadeOut.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				// Flip background resources
				mWaiting2.setBackgroundResource(mWaitingRightColor ? R.drawable.player_icon_p1 : R.drawable.player_icon_p2);
				mWaitingRightColor = !mWaitingRightColor;
				// Start animations
				mWaiting2.startAnimation(mRightFadeIn);
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
				// Flip background resources
				mWaiting1.setBackgroundResource(mWaitingLeftColor ? R.drawable.player_icon_p1 : R.drawable.player_icon_p2);
				mWaitingLeftColor = !mWaitingLeftColor;
				// Start animations
				mWaiting1.startAnimation(mLeftFadeIn);
			}
		});

		mLeftFadeIn.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						mWaiting1.startAnimation(mLeftFadeOut);
						mWaiting2.startAnimation(mRightFadeOut);
					}
				}, getResources().getInteger(R.integer.waitingMessageFadeDelay));
			}
		});
	}

	private void showSpinner(int spinnerMsg) {
		if (progressBar == null) {
			progressBar = new ProgressDialog(this, R.style.ProgressDialog);
			progressBar.setCancelable(false);
			progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		}
		switch (spinnerMsg) {
			case 1: progressBar.setMessage(getString(R.string.loading_match)); break;
			case 2: progressBar.setMessage(getString(R.string.submitting_move)); break;
			case 3: progressBar.setMessage(getString(R.string.connecting)); break;
			default: progressBar.setMessage(getString(R.string.please_wait));
		}
		progressBar.show();
	}

	private void dismissSpinner() {
		progressBar.dismiss();
	}

	// Generic warning/info dialog
	private void showAlertDialog(String title, String message) {
		new AlertDialog.Builder(this)
				.setTitle(title)
				.setMessage(message)
				.setCancelable(false)
				.setPositiveButton("OK",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						// if this button is clicked, close current activity
					}
				})
				.create().show();
	}

	/* Creates a dialog for an error message */
	private void showErrorDialog(int errorCode) {
		// Create a fragment for the error dialog
		ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
		// Pass the error that should be displayed
		Bundle args = new Bundle();
		args.putInt(DIALOG_ERROR, errorCode);
		dialogFragment.setArguments(args);
		dialogFragment.show(getFragmentManager(), DIALOG_ERROR);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.multi_player, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch(id) {
			case R.id.findNewMatch:
				startNewMatch(findViewById(id));
				return true;
			case R.id.selectMatch:
				selectMatch(findViewById(id));
				return true;
			case R.id.action_howtoplay:
				Intent htp = new Intent(this, HowToPlayActivity.class);
				startActivity(htp);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	// Used for converting Board to debugging text String only
	private String bytesToString(byte[] in) {
		// Converting mMatchData to String for debugging
		StringBuffer buf = new StringBuffer();
		for (byte b : in)
			buf.append(String.valueOf(b));
		return buf.toString();
	}
}
