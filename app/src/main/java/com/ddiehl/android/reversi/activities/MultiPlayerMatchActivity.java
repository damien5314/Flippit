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

public class MultiPlayerMatchActivity extends Activity
            implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
                    OnTurnBasedMatchUpdateReceivedListener {
	private final static String TAG = MultiPlayerMatchActivity.class.getSimpleName();

	public static final int RC_RESOLVE_ERROR = 1001;
	private static final int RC_VIEW_MATCHES = 1002;
	private static final int RC_SELECT_PLAYERS = 1003;

    private ProgressDialog progressBar;

    private static final String DIALOG_ERROR = "dialog_error";

    // Client used to interact with Google APIs
    private GoogleApiClient mGoogleApiClient;

	private TurnBasedMatch mMatch;
	private Board mBoard;
	Participant player, opponent;
	private byte[] mGameData;

    private boolean mResolvingError = false;
	boolean evaluatingMove = false;

	Handler mHandler;
	List<BoardSpace> mQueuedMoves;

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
        Log.d(TAG, "onStart(): connecting");
		int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		Log.d(TAG, "IsGooglePlayServicesAvailable = " + result);
		if (result != ConnectionResult.SUCCESS) {
			GooglePlayServicesUtil.getErrorDialog(result, this, RC_RESOLVE_ERROR).show();
			return;
		} else connectGoogleApiClient();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop(): disconnecting");
        if (mGoogleApiClient.isConnected()) {
            Log.d(TAG, "Unregistering match update listener");
            registerMatchUpdateListener(false);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        dismissSpinner();
        Log.d(TAG, "Connected to Google Play Services");
		Toast.makeText(this, "Connected to Google Play", Toast.LENGTH_SHORT).show();
        registerMatchUpdateListener(true);
		if (mMatch != null)
			updateMatch(mMatch);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended(): attempting to connect");
        connectGoogleApiClient();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Failed to connect to Google Play services");
		dismissSpinner();
        if (mResolvingError) {
            return; // Already attempting to resolve an error
        } else if (result.hasResolution()) {
			Log.d(TAG, "Attempting to resolve error (ErrorCode: " + result.getErrorCode() + ")");
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, RC_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
				Log.e(TAG, "Unable to start resolution intent; Exception: " + e.getMessage());
                // There was an error with the resolution intent. Try again.
                connectGoogleApiClient();
            }
        } else {
			Log.d(TAG, "Unresolvable error (ErrorCode: " + result.getErrorCode() + ")");
            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }
    }

    private void registerMatchUpdateListener(boolean b) {
        // Unregister any existing listener
        Games.TurnBasedMultiplayer.unregisterMatchUpdateListener(mGoogleApiClient);

        if (b) { // Register update listener to replace notifications when a match is open
            Log.d(TAG, "Registering match update listener");
            Games.TurnBasedMultiplayer.registerMatchUpdateListener(mGoogleApiClient, this);
        }
    }

    public void onDialogDismissed() {
		mResolvingError = false;
    }

	public void startNewGame(View v) {
		Intent intent;
		if (!mGoogleApiClient.isConnected()) {
			Toast.makeText(this, R.string.google_play_not_connected, Toast.LENGTH_SHORT).show();
			return;
		}
		intent = Games.TurnBasedMultiplayer.getSelectOpponentsIntent(mGoogleApiClient, 1, 1, true);
		startActivityForResult(intent, RC_SELECT_PLAYERS);
	}

	public void selectGame(View v) {
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
				mResolvingError = false;
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

				// Get the invitee list
				final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);

				// Get automatch criteria
				Bundle autoMatchCriteria = null;

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
				// Start the match
				Games.TurnBasedMultiplayer.createMatch(mGoogleApiClient, tbmc).setResultCallback(
						new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {
							@Override
							public void onResult(TurnBasedMultiplayer.InitiateMatchResult result) {
								Log.d(TAG, "TurnBasedMultiplayer match created");
								processResult(result);
							}
						});
				break;
		}
	}

	private void processResult(TurnBasedMultiplayer.InitiateMatchResult result) {
		TurnBasedMatch match = result.getMatch();
		mGameData = null;
		saveGameData();

		if (!checkStatusCode(match, result.getStatus().getStatusCode())) {
			return;
		}

		if (match.getData() != null) { // This is a game that has already started, just update
			Log.d(TAG, "Game already started, data:");
			Log.d(TAG, bytesToString(match.getData()));
			updateMatch(match);
			return;
		}

		startMatch(match);
	}

	private void startMatch(TurnBasedMatch match) {
		mMatch = match;
		mBoard.reset();
		saveGameData();
		displayBoard();
		updateScoreDisplay();

		String playerId = Games.Players.getCurrentPlayerId(mGoogleApiClient);
		String myParticipantId = mMatch.getParticipantId(playerId);

//		showSpinner(1);
		Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, match.getMatchId(),
				mGameData, myParticipantId).setResultCallback(
				new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
					@Override
					public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
						processResult(result);
					}
				});
	}

	private void processResult(TurnBasedMultiplayer.UpdateMatchResult result) {
		mMatch = result.getMatch();
		evaluatingMove = false;
		dismissSpinner();
//		updateScoreDisplay();

		if (!checkStatusCode(mMatch, result.getStatus().getStatusCode())) {
			return;
		}

		if (mMatch.canRematch()) {
//			askForRematch();
		}

		updateMatch(mMatch);
	}

	// This is the main function that gets called when players choose a match
	// from the inbox, or else create a match and want to start it.
	private void updateMatch(TurnBasedMatch match) {
		mMatch = match;
		player = getCurrentPlayer();
		opponent = getOpponent();
		mGameData = match.getData();

		// Grab the appropriate segment from mGameData based on player's color
		int startIndex = (getCurrentPlayer() == getLightPlayer()) ? 0 : 100;
		byte[] playerData = Arrays.copyOfRange(mGameData, startIndex, startIndex+64);

        mBoard.deserialize(playerData);
		findViewById(R.id.board_panels).setVisibility(View.GONE);
        displayBoard();
		updateScoreDisplay();
		updatePlayerNameDisplay();
		dismissSpinner();

		// Commit opponent's moves to the deserialized Board object
		// 0 [Light's Board] 64 [Dark's Moves] 100 [Dark's Board] 164 [Light's Moves]
		startIndex += 64;
		while (mGameData[startIndex] != 0) {
			BoardSpace s = mBoard.getBoardSpaceFromNum(mGameData[startIndex++]);
			Log.d(TAG, "Opponent moved @(" + s.x + " " + s.y + ")");
			mQueuedMoves.add(s);
		}

		if (!mQueuedMoves.isEmpty())
			processReceivedTurns();

		int status = match.getStatus();
		int turnStatus = match.getTurnStatus();

		switch (status) {
			case TurnBasedMatch.MATCH_STATUS_CANCELED:
				displayMessage(getString(R.string.game_canceled));
				return;
			case TurnBasedMatch.MATCH_STATUS_EXPIRED:
				displayMessage(getString(R.string.game_expired));
				return;
			case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
				displayMessage(getString(R.string.game_finding_partner));
				return;
			case TurnBasedMatch.MATCH_STATUS_COMPLETE:
				if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE) {
					displayMessage(getString(R.string.game_complete));
					break;
				}

				// Call endGame() here to trigger finish() method and result display
				endGame();
		}

		// OK, it's active. Check on turn status.
		switch (turnStatus) {
			case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN:
				dismissMessage();
				return;
			case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN: // Should return results.
				displayMessage(getString(R.string.game_opponent_turn));
				break;
			case TurnBasedMatch.MATCH_TURN_STATUS_INVITED:
				displayMessage(getString(R.string.game_invite_pending));
		}
	}

	private void saveGameData() {
		byte[] playerBoard = mBoard.serialize();

		if (mGameData == null) {
			mGameData = new byte[256];
			System.arraycopy(playerBoard, 0, mGameData, 0, playerBoard.length);
			System.arraycopy(playerBoard, 0, mGameData, 100, playerBoard.length);
		} else {
			int startIndex = (getCurrentPlayer() == getLightPlayer()) ? 0 : 100;
			// Copy the serialized Board into the appropriate place in game data
			System.arraycopy(playerBoard, 0, mGameData, startIndex, playerBoard.length);
			// Clear out the first 20 nodes following (which were the other player's previous moves)
			for (int clearIndex = startIndex+64; clearIndex < startIndex+64+16; clearIndex++)
				mGameData[clearIndex] = 0;
		}

		Log.d(TAG, "Player's game data saved: " + bytesToString(mGameData));
	}

	private void processReceivedTurns() {
		evaluatingMove = true;
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				mBoard.commitPiece(mQueuedMoves.remove(0), getOpponentColor());
				updateScoreDisplay();
//				mGameData = GameStorage.serialize(mBoard);
				saveGameData();
				if (!mQueuedMoves.isEmpty())
					processReceivedTurns();
				else
					evaluatingMove = false;
			}
		}, getResources().getInteger(R.integer.cpu_turn_delay));
	}

    @Override
    public void onTurnBasedMatchReceived(TurnBasedMatch match) {
        Log.d(TAG, "Match update received for match: " + match.getMatchId());
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

	private void claim(final BoardSpace s) {
		if (mGoogleApiClient.isConnected()) {
			if (evaluatingMove || !mQueuedMoves.isEmpty()) {
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
				evaluatingMove = true;
				showSpinner(2);
				mBoard.commitPiece(s, playerColor);
				saveGameData();

				// Add selected piece to the end of mGameData array
				// 0 [White's Board] 64 [Dark's Moves] 100 [Dark's Board] 164 [White's Moves]
				int nextIndex = (getCurrentPlayer() == getLightPlayer()) ? 164 : 64;
				while (mGameData[nextIndex] != 0)
					nextIndex++; // Increase index til we run into an unfilled index
				mGameData[nextIndex] = mBoard.getSpaceNumber(s);
				Log.d(TAG, "Queued move for opponent's Board");
				Log.d(TAG, bytesToString(mGameData));

				updateGameState();
			} else {
				Toast.makeText(this, R.string.bad_move, Toast.LENGTH_SHORT).show();
			}
		}
		else // TODO Change this to display a sign-in dialog
			Log.d(TAG, "GoogleApiClient not connected");
	}

	private void updateGameState() {
		if (mBoard.hasMove(getOpponentColor())) { // If opponent can make a move, it's his turn
            String pId = (opponent == null) ? null : opponent.getParticipantId();
			Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, mMatch.getMatchId(), mGameData, pId)
                    .setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
						@Override
						public void onResult(TurnBasedMultiplayer.UpdateMatchResult updateMatchResult) {
							Log.d(TAG, "Turn updated, opponent receives turn. Result: " + updateMatchResult.getStatus());
							processResult(updateMatchResult);
						}
					});
		} else if (mBoard.hasMove(getCurrentPlayerColor())) { // Opponent has no move, keep turn
			Toast.makeText(this, getString(R.string.no_moves) + opponent.getDisplayName(), Toast.LENGTH_SHORT).show();
			Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, mMatch.getMatchId(), mGameData, player.getParticipantId())
                    .setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
						@Override
						public void onResult(TurnBasedMultiplayer.UpdateMatchResult updateMatchResult) {
							Log.d(TAG, "Turn updated, player keeps turn. Result: " + updateMatchResult.getStatus());
							processResult(updateMatchResult);
						}
					});
		} else { // No moves remaining, end of game
            evaluatingMove = false;
            dismissSpinner();
			updateScoreDisplay();
			endGame();
			return;
		}
		updateScoreDisplay();
	}

	private void endGame() {
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
				winnerResult = new ParticipantResult(getLightPlayer().getParticipantId(), ParticipantResult.MATCH_RESULT_WIN,
						ParticipantResult.PLACING_UNINITIALIZED);
				loserResult = new ParticipantResult(getDarkPlayer().getParticipantId(), ParticipantResult.MATCH_RESULT_LOSS,
						ParticipantResult.PLACING_UNINITIALIZED);
			} else {
				winnerResult = new ParticipantResult(getDarkPlayer().getParticipantId(), ParticipantResult.MATCH_RESULT_WIN,
						ParticipantResult.PLACING_UNINITIALIZED);
				loserResult = new ParticipantResult(getLightPlayer().getParticipantId(), ParticipantResult.MATCH_RESULT_LOSS,
						ParticipantResult.PLACING_UNINITIALIZED);
			}
		} else {
			winnerResult = new ParticipantResult(getDarkPlayer().getParticipantId(), ParticipantResult.MATCH_RESULT_TIE,
					ParticipantResult.PLACING_UNINITIALIZED);
			loserResult = new ParticipantResult(getLightPlayer().getParticipantId(), ParticipantResult.MATCH_RESULT_TIE,
					ParticipantResult.PLACING_UNINITIALIZED);
		}

		// Call finishMatch() with results
		Games.TurnBasedMultiplayer.finishMatch(mGoogleApiClient, mMatch.getMatchId(), mGameData, winnerResult, loserResult);
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
		if (getCurrentPlayer() == getLightPlayer())
			return ReversiColor.White;
		else return ReversiColor.Black;
	}

	private ReversiColor getOpponentColor() {
		if (getOpponent() == getLightPlayer())
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
		TableLayout grid = (TableLayout) findViewById(R.id.GameGrid);
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
		Participant light = getLightPlayer();
		Participant dark = getDarkPlayer();

		if (light != null) {
			((TextView) findViewById(R.id.p1_label)).setText(light.getDisplayName());
		} else {
			((TextView) findViewById(R.id.p1_label)).setText(R.string.unknown_player);
		}

		if (dark != null) {
			((TextView) findViewById(R.id.p2_label)).setText(dark.getDisplayName());
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
		int myTurnResource = (getCurrentPlayer() == getLightPlayer()) ?
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
				turnIndicator.setImageResource(android.R.color.transparent);
				break;
			case TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE:
				turnIndicator.setImageResource(android.R.color.transparent);
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
				// TODO Remove this Toast
				Toast.makeText(
						this,
						"Stored action for later. (Please remove this toast before release.)",
						Toast.LENGTH_SHORT).show();
				return true;
			case GamesStatusCodes.STATUS_MULTIPLAYER_ERROR_NOT_TRUSTED_TESTER:
				showErrorMessage(R.string.status_multiplayer_error_not_trusted_tester);
				break;
			case GamesStatusCodes.STATUS_MATCH_ERROR_ALREADY_REMATCHED:
				showErrorMessage(R.string.match_error_already_rematched);
				break;
			case GamesStatusCodes.STATUS_NETWORK_ERROR_OPERATION_FAILED:
				showErrorMessage(R.string.network_error_operation_failed);
				break;
			case GamesStatusCodes.STATUS_CLIENT_RECONNECT_REQUIRED:
				showErrorMessage(R.string.client_reconnect_required);
				break;
			case GamesStatusCodes.STATUS_INTERNAL_ERROR:
				showErrorMessage(R.string.internal_error);
				break;
			case GamesStatusCodes.STATUS_MATCH_ERROR_INACTIVE_MATCH:
				showErrorMessage(R.string.match_error_inactive_match);
				break;
			case GamesStatusCodes.STATUS_MATCH_ERROR_LOCALLY_MODIFIED:
				showErrorMessage(R.string.match_error_locally_modified);
				break;
			default:
				showErrorMessage(R.string.unexpected_status);
				Log.d(TAG, "Did not have warning or string to deal with: " + statusCode);
		}

		return false;
	}

	private void displayMessage(String gameMsg) {
		((TextView) findViewById(R.id.gameMessage)).setText(gameMsg);
	}

	private void dismissMessage() {
		((TextView) findViewById(R.id.gameMessage)).setText("");
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
	private void showWarning(String title, String message) {
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

	private void showErrorMessage(int stringId) {
		showWarning("Warning", getResources().getString(stringId));
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
				startNewGame(findViewById(id));
				return true;
			case R.id.selectMatch:
				selectGame(findViewById(id));
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	// Used for converting Board to debugging text String only
	private String bytesToString(byte[] in) {
		// Converting mGameData to String for debugging
		StringBuffer buf = new StringBuffer();
		for (byte b : in)
			buf.append(String.valueOf(b));
		return buf.toString();
	}
}
