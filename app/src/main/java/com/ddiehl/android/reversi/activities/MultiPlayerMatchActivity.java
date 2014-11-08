package com.ddiehl.android.reversi.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
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
import com.ddiehl.android.reversi.game.Board;
import com.ddiehl.android.reversi.game.BoardIterator;
import com.ddiehl.android.reversi.game.BoardSpace;
import com.ddiehl.android.reversi.game.GameStorage;
import com.ddiehl.android.reversi.game.ReversiColor;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer;
import com.google.android.gms.plus.Plus;

import java.util.ArrayList;

public class MultiPlayerMatchActivity extends Activity
            implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
	private final static String TAG = MultiPlayerMatchActivity.class.getSimpleName();

    private static final int RC_RESOLVE_ERROR = 1001;
	private static final int RC_VIEW_MATCHES = 1002;
	private static final int RC_SELECT_PLAYERS = 1003;

	private AlertDialog mAlertDialog;

	private static final String KEY_RESOLVING_ERROR = "resolving_error";
    private static final String DIALOG_ERROR = "dialog_error";

    // Client used to interact with Google APIs
    private GoogleApiClient mGoogleApiClient;

	private TurnBasedMatch mMatch;
	private Board board;
	private byte[] mGameData;

    // Are we currently resolving a connection failure?
    private boolean mResolvingError = false;

	// Should I be showing the turn API?
	public boolean isDoingTurn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_multiplayer_game_selection);
		setContentView(R.layout.activity_reversi);
		board = Board.getInstance(this);

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
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart(): connecting");
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop(): disconnecting");
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Connected to Google Play Services");
		Toast.makeText(this, "Connected to Google Play", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended(): attempting to connect");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Failed to connect to Google Play services");
        if (mResolvingError) {
            return; // Already attempting to resolve an error.
        } else if (result.hasResolution()) {
			Log.d(TAG, "Attempting to resolve error (ErrorCode: " + result.getErrorCode() + ")");
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, RC_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
				Log.e(TAG, "Unable to start resolution intent; Exception: " + e.getMessage());
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
			Log.d(TAG, "Unresolvable error (ErrorCode: " + result.getErrorCode() + ")");
            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }
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

    public void onDialogDismissed() {
		mResolvingError = false;
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() { }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(KEY_RESOLVING_ERROR);
            return GooglePlayServicesUtil.getErrorDialog(errorCode,
                    this.getActivity(), RC_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((MultiPlayerMatchActivity) getActivity()).onDialogDismissed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.multi_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
		switch(id) {
			case R.id.findNewMatch:
				findNewMatch();
				return true;
			case R.id.selectMatch:
				selectMatch();
				return true;
		}
        return super.onOptionsItemSelected(item);
    }

	public void findNewMatch() {
		if (!mGoogleApiClient.isConnected()) {
			Toast.makeText(this, "Error: GoogleApiClient not connected", Toast.LENGTH_SHORT).show();
			return;
		}
		Intent intent = Games.TurnBasedMultiplayer.getSelectOpponentsIntent(mGoogleApiClient, 1, 1, true);
		startActivityForResult(intent, RC_SELECT_PLAYERS);
	}

	public void selectMatch() {
		if (!mGoogleApiClient.isConnected()) {
			Toast.makeText(this, "Error: GoogleApiClient not connected", Toast.LENGTH_SHORT).show();
			return;
		}
		Intent intent = Games.TurnBasedMultiplayer.getInboxIntent(mGoogleApiClient);
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
							mGoogleApiClient.connect();
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
				else
					autoMatchCriteria = null;

				TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
						.addInvitedPlayers(invitees)
						.setAutoMatchCriteria(autoMatchCriteria)
						.build();

				// Start the match
				Games.TurnBasedMultiplayer.createMatch(mGoogleApiClient, tbmc).setResultCallback(
						new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {
							@Override
							public void onResult(TurnBasedMultiplayer.InitiateMatchResult result) {
								Log.d(TAG, "TurnBasedMultiplayer match created");
								processResult(result);
							}
						});
				showSpinner();
				break;
		}
	}

	private void processResult(TurnBasedMultiplayer.InitiateMatchResult result) {
		TurnBasedMatch match = result.getMatch();
		dismissSpinner();

		if (!checkStatusCode(match, result.getStatus().getStatusCode())) {
			return;
		}

		if (match.getData() != null) {
			// This is a game that has already started, just update
			updateMatch(match);
			return;
		}

		startMatch(match);
	}

	public void processResult(TurnBasedMultiplayer.UpdateMatchResult result) {
		TurnBasedMatch match = result.getMatch();
		dismissSpinner();
		if (!checkStatusCode(match, result.getStatus().getStatusCode())) {
			return;
		}

		if (match.canRematch()) {
//			askForRematch();
		}

		isDoingTurn = (match.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN);

		if (isDoingTurn) {
			updateMatch(match);
			return;
		}

//		setViewVisibility();
	}

	// startMatch() happens in response to the createTurnBasedMatch()
	// above. This is only called on success, so we should have a
	// valid match object. We're taking this opportunity to setup the
	// game, saving our initial state. Calling takeTurn() will
	// callback to OnTurnBasedMatchUpdated(), which will show the game UI.
	public void startMatch(TurnBasedMatch match) {
		mMatch = match;
		board.reset();
		displayBoard();
		updateScoreDisplay();

		String playerId = Games.Players.getCurrentPlayerId(mGoogleApiClient);
		String myParticipantId = mMatch.getParticipantId(playerId);

		showSpinner();

		Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, match.getMatchId(),
				GameStorage.serialize(board), myParticipantId).setResultCallback(
				new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
					@Override
					public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
						processResult(result);
					}
				});
	}

	// This is the main function that gets called when players choose a match
	// from the inbox, or else create a match and want to start it.
	public void updateMatch(TurnBasedMatch match) {
		mMatch = match;
		mGameData = match.getData();
		updateScoreDisplay();
		updatePlayerNameDisplay();
		int status = match.getStatus();
		int turnStatus = match.getTurnStatus();

		switch (status) {
			case TurnBasedMatch.MATCH_STATUS_CANCELED:
				showWarning("Canceled!", "This game was canceled!");
				return;
			case TurnBasedMatch.MATCH_STATUS_EXPIRED:
				showWarning("Expired!", "This game is expired. So sad!");
				return;
			case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
				showWarning("Waiting for auto-match...", "We're still waiting for an automatch partner.");
				return;
			case TurnBasedMatch.MATCH_STATUS_COMPLETE:
				if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE) {
					showWarning(
							"Complete!",
							"This game is over; someone finished it, and so did you!  There is nothing to be done.");
					break;
				}

				// Note that in this state, you must still call "Finish" yourself,
				// so we allow this to continue.
				showWarning("Complete!", "This game is over; someone finished it!  You can only finish it now.");
		}

		// OK, it's active. Check on turn status.
		switch (turnStatus) {
			case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN:
				GameStorage.deserialize(this, mMatch.getData());
				displayBoard();
				return;
			case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN:
				// Should return results.
				showWarning("Alas...", "It's not your turn.");
				break;
			case TurnBasedMatch.MATCH_TURN_STATUS_INVITED:
				showWarning("Good inititative!", "Still waiting for invitations.\n\nBe patient!");
		}

//		board = null;
	}

	public void updatePlayerNameDisplay() {
		((TextView) findViewById(R.id.p1_label)).setText(Games.Players.getCurrentPlayer(mGoogleApiClient).getDisplayName());
		Participant opponent = mMatch.getDescriptionParticipant();
		if (opponent != null)
			((TextView) findViewById(R.id.p2_label)).setText(opponent.getDisplayName());
		else // Autopick player has not yet been found
			((TextView) findViewById(R.id.p2_label)).setText(R.string.unknown_player);
	}

	public void updateScoreDisplay() {
		int p1c = 0;
		int p2c = 0;
		BoardIterator i = new BoardIterator(board);
		while (i.hasNext()) {
			BoardSpace s = i.next();
			if (s.isOwned()) {
				if (s.getColor() == ReversiColor.White)
					p1c++;
				else
					p2c++;
			}
		}
		((TextView) findViewById(R.id.p1score)).setText(String.valueOf(p1c));
		((TextView) findViewById(R.id.p2score)).setText(String.valueOf(p2c));
		ImageView ti = (ImageView)findViewById(R.id.turnIndicator);
		switch (mMatch.getTurnStatus()) {
			case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN:
				ti.setImageResource(R.drawable.ic_turn_indicator_p1);
				break;
			case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN:
				ti.setImageResource(R.drawable.ic_turn_indicator_p2);
				break;
			case TurnBasedMatch.MATCH_TURN_STATUS_INVITED:
				ti.setImageResource(android.R.color.transparent);
				break;
			case TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE:
				ti.setImageResource(android.R.color.transparent);
				break;
		}
	}

	private ProgressDialog progressBar;
	public void showSpinner() {
		if (progressBar == null) {
			progressBar = new ProgressDialog(this, R.style.ProgressDialog);
			progressBar.setCancelable(false);
			progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressBar.setMessage(getString(R.string.loading_match));
		}
		progressBar.show();
	}

	public void dismissSpinner() {
		progressBar.dismiss();
	}

	// Generic warning/info dialog
	public void showWarning(String title, String message) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

		// Set title
		alertDialogBuilder.setTitle(title).setMessage(message);

		// Set dialog message
		alertDialogBuilder.setCancelable(false).setPositiveButton("OK",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						// if this button is clicked, close
						// current activity
					}
				});

		// Create alert dialog
		mAlertDialog = alertDialogBuilder.create();

		// Show it
		mAlertDialog.show();
	}

	public void showErrorMessage(TurnBasedMatch match, int statusCode, int stringId) {
		showWarning("Warning", getResources().getString(stringId));
	}

	// Returns false if something went wrong, probably. This should handle
	// more cases, and probably report more accurate results.
	private boolean checkStatusCode(TurnBasedMatch match, int statusCode) {
		switch (statusCode) {
			case GamesStatusCodes.STATUS_OK:
				return true;
			case GamesStatusCodes.STATUS_NETWORK_ERROR_OPERATION_DEFERRED:
				// This is OK; the action is stored by Google Play Services and will
				// be dealt with later.
				Toast.makeText(
						this,
						"Stored action for later. (Please remove this toast before release.)",
						Toast.LENGTH_SHORT).show();
				// NOTE: This toast is for informative reasons only; please remove
				// it from your final application.
				return true;
			case GamesStatusCodes.STATUS_MULTIPLAYER_ERROR_NOT_TRUSTED_TESTER:
				showErrorMessage(match, statusCode, R.string.status_multiplayer_error_not_trusted_tester);
				break;
			case GamesStatusCodes.STATUS_MATCH_ERROR_ALREADY_REMATCHED:
				showErrorMessage(match, statusCode, R.string.match_error_already_rematched);
				break;
			case GamesStatusCodes.STATUS_NETWORK_ERROR_OPERATION_FAILED:
				showErrorMessage(match, statusCode, R.string.network_error_operation_failed);
				break;
			case GamesStatusCodes.STATUS_CLIENT_RECONNECT_REQUIRED:
				showErrorMessage(match, statusCode, R.string.client_reconnect_required);
				break;
			case GamesStatusCodes.STATUS_INTERNAL_ERROR:
				showErrorMessage(match, statusCode, R.string.internal_error);
				break;
			case GamesStatusCodes.STATUS_MATCH_ERROR_INACTIVE_MATCH:
				showErrorMessage(match, statusCode, R.string.match_error_inactive_match);
				break;
			case GamesStatusCodes.STATUS_MATCH_ERROR_LOCALLY_MODIFIED:
				showErrorMessage(match, statusCode, R.string.match_error_locally_modified);
				break;
			default:
				showErrorMessage(match, statusCode, R.string.unexpected_status);
				Log.d(TAG, "Did not have warning or string to deal with: " + statusCode);
		}

		return false;
	}

	// This is duplicated between the single player activity and multiplayer activity
	// Consider refactoring this to a static method in another class to decrease code duplication
	private void displayBoard() {
		TableLayout grid = (TableLayout) findViewById(R.id.GameGrid);
		grid.setVisibility(View.GONE); // Hide the view until we finish adding children
		grid.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
		grid.removeAllViews();

		int bHeight = (int) getResources().getDimension(R.dimen.space_row_height);
		int bMargin = (int) getResources().getDimension(R.dimen.space_padding);

		for (int y = 0; y < board.height(); y++) {
			TableRow row = new TableRow(this);
			row.setWeightSum(board.width());
			for (int x = 0; x < board.width(); x++) {
				BoardSpace space = board.getSpaceAt(x, y);
				TableRow.LayoutParams params = new TableRow.LayoutParams(0, bHeight, 1.0f);
				params.setMargins(bMargin, bMargin, bMargin, bMargin);
				space.setLayoutParams(params);
				space.setOnClickListener(claim(space)); // Need to change this to a method for multiplayer turn taking
				row.addView(space);
			}
			grid.addView(row);
		}
		grid.setVisibility(View.VISIBLE);
	}

    boolean evaluatingMove = false;
	public View.OnClickListener claim(final BoardSpace s) {
		return new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (mGoogleApiClient.isConnected()) {
                    if (evaluatingMove) {
                        Log.d(TAG, "Error: Still evaluating last move");
                        return;
                    }

					if (s.isOwned())
						return;

                    Log.d(TAG, "Turn Status: " + mMatch.getTurnStatus() + " (My Turn Status = " + TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN + ")");
					if (mMatch.getTurnStatus() != TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
						Toast.makeText(view.getContext(), "Not your turn!", Toast.LENGTH_SHORT).show();
						return;
					}

					ReversiColor playerColor = ReversiColor.White; // Player is always White for now

					if (board.spacesCapturedWithMove(s, playerColor) > 0) {
                        evaluatingMove = true;
						board.commitPiece(s, playerColor);
						calculateGameState();
					} else {
						Toast.makeText(view.getContext(), R.string.bad_move, Toast.LENGTH_SHORT).show();
					}
				}
                else
                    Log.d(TAG, "GoogleApiClient not connected");
			}
		};
	}

	Participant player, opponent;
	public void calculateGameState() {
		player = mMatch.getParticipant(mMatch.getParticipantId(Games.Players.getCurrentPlayerId(mGoogleApiClient)));
		opponent = mMatch.getDescriptionParticipant();
		if (board.hasMove(ReversiColor.Black)) { // If opponent can make a move, it's his turn
			// TakeTurn for opponent
            String pId = (opponent == null) ? null : opponent.getParticipantId();
			Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, mMatch.getMatchId(), GameStorage.serialize(board),
					pId).setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
				@Override
				public void onResult(TurnBasedMultiplayer.UpdateMatchResult updateMatchResult) {
					Log.d(TAG, "Turn updated, Next action for opponent. Result: " + updateMatchResult.getStatus());
                    mMatch = updateMatchResult.getMatch();
                    evaluatingMove = false;
				}
			});
		} else if (board.hasMove(ReversiColor.White)) { // Opponent has no move, keep turn
			Toast.makeText(this, getString(R.string.no_moves) + opponent.getDisplayName(), Toast.LENGTH_SHORT).show();
			// TakeTurn for player
			Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, mMatch.getMatchId(), GameStorage.serialize(board),
					player.getParticipantId()).setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
				@Override
				public void onResult(TurnBasedMultiplayer.UpdateMatchResult updateMatchResult) {
					Log.d(TAG, "Turn updated, Next action for opponent. Result: " + updateMatchResult.getStatus());
                    mMatch = updateMatchResult.getMatch();
                    evaluatingMove = false;
				}
			});
		} else { // No moves remaining, end of game
            evaluatingMove = false;
			updateScoreDisplay();
			endGame();
			return;
		}
		updateScoreDisplay();
	}

	public void endGame() {
		int lightCount = 0;
		int darkCount = 0;
		BoardIterator i = new BoardIterator(board);
		while (i.hasNext()) {
			BoardSpace s = i.next();
			if (s.isOwned()) {
				if (s.getColor() == ReversiColor.White)
					lightCount++;
				else
					darkCount++;
			}
		}
		Participant winner = null;
		if (lightCount != darkCount)
			winner = (lightCount > darkCount) ? player : opponent;
		submitWinner(winner);
//		int diff = 64 - lightCount - darkCount;
//		winner.setScore(winner.getScore() + diff);
//		updateScoreForPlayer(winner);
//		gameInProgress = false;
	}

	private void submitWinner(Participant player) {

	}
}
