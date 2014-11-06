package com.ddiehl.android.reversi.activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ddiehl.android.reversi.R;
import com.ddiehl.android.reversi.adapters.MatchSelectionAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer;
import com.google.android.gms.plus.Plus;

import java.util.ArrayList;

public class MultiplayerGameSelectionActivity extends Activity
            implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
	private final static String TAG = MultiplayerGameSelectionActivity.class.getSimpleName();
	private ListView mListView;
	private MatchSelectionAdapter mListAdapter;
	private ArrayList<String> mMatchList;

    private static final int REQUEST_RESOLVE_ERROR = 1001;
	private static final int REQUEST_LOOK_AT_MATCHES = 1002;
	private static final int RC_SELECT_PLAYERS = 1003;

	private static final String STATE_RESOLVING_ERROR = "resolving_error";
    private static final String DIALOG_ERROR = "dialog_error";

    // Client used to interact with Google APIs
    private GoogleApiClient mGoogleApiClient;

    // Are we currently resolving a connection failure?
    private boolean mResolvingError = false;

	// Is there a match already loaded?
	private boolean mMatchLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_multiplayer_game_selection);
		setContentView(R.layout.activity_reversi);

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

//		mMatchList = new ArrayList<String>();
//
//		mListView = (ListView) findViewById(R.id.matchList);
//		mListAdapter = new MatchSelectionAdapter(this, R.layout.activity_multiplayer_game_selection_item, mMatchList);
//		mListView.setAdapter(mListAdapter);
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

		((TextView) findViewById(R.id.p1_label)).setText(
				Games.Players.getCurrentPlayer(mGoogleApiClient).getDisplayName());

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended(): attempting to connect");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Failed to connect to Google Play services.");
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
			Log.d(TAG, "Attempting to resolve error (ErrorCode: " + result.getErrorCode() + ")");
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
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
        dialogFragment.show(getFragmentManager(), "errordialog");
    }

    public void onDialogDismissed() {
        mResolvingError = false;
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() {

        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GooglePlayServicesUtil.getErrorDialog(errorCode,
                    this.getActivity(), REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((MultiplayerGameSelectionActivity) getActivity()).onDialogDismissed();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.multiplayer_game_selection, menu);
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
		if (mGoogleApiClient.isConnected()) {
			Intent intent = Games.TurnBasedMultiplayer.getSelectOpponentsIntent(mGoogleApiClient, 1, 1, true);
			startActivityForResult(intent, RC_SELECT_PLAYERS);
		} else
			Toast.makeText(this, "Error: GoogleApiClient not connected", Toast.LENGTH_SHORT).show();
	}

	public void selectMatch() {
		Intent intent = Games.TurnBasedMultiplayer.getInboxIntent(mGoogleApiClient);
		startActivityForResult(intent, REQUEST_LOOK_AT_MATCHES);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {

			case REQUEST_RESOLVE_ERROR:
				mResolvingError = false;
				switch (resultCode) {
					case RESULT_OK:
						if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
							mGoogleApiClient.connect();
						}
						break;
				}
				break;

			case REQUEST_LOOK_AT_MATCHES: // Returned from the 'Select Match' dialog
				if (resultCode != Activity.RESULT_OK)// User canceled
					break;

				TurnBasedMatch match = data.getParcelableExtra(Multiplayer.EXTRA_TURN_BASED_MATCH);
				if (match != null) {
					Log.d(TAG, "Selected match: " + match.getMatchId());
					updateMatch(match);
				}
				break;

			case RC_SELECT_PLAYERS: // Returned from 'Select players to Invite' dialog
				if (resultCode != Activity.RESULT_OK) {
					// user canceled
					return;
				}

				// Get the invitee list
				final ArrayList<String> invitees = data
						.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);

				// Get automatch criteria
				Bundle autoMatchCriteria = null;

				int minAutoMatchPlayers = data.getIntExtra(
						Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
				int maxAutoMatchPlayers = data.getIntExtra(
						Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);

				if (minAutoMatchPlayers > 0) {
					autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
							minAutoMatchPlayers, maxAutoMatchPlayers, 0);
				} else {
					autoMatchCriteria = null;
				}

				TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
						.addInvitedPlayers(invitees)
						.setAutoMatchCriteria(autoMatchCriteria).build();

				// Start the match
				Games.TurnBasedMultiplayer.createMatch(mGoogleApiClient, tbmc).setResultCallback(
						new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {
							@Override
							public void onResult(TurnBasedMultiplayer.InitiateMatchResult result) {
								Log.d(TAG, "TurnBasedMultiplayer match created");
//								processResult(result);
							}
						});
//				showSpinner();
				break;
		}
	}

	// This is the main function that gets called when players choose a match
	// from the inbox, or else create a match and want to start it.
	public void updateMatch(TurnBasedMatch match) {
		/*mMatch = match;

		int status = match.getStatus();
		int turnStatus = match.getTurnStatus();

		switch (status) {
			case TurnBasedMatch.MATCH_STATUS_CANCELED:
				showWarning("Canceled!", "This game was canceled!");
				return;
			case TurnBasedMatch.MATCH_STATUS_EXPIRED:
				showWarning("Expired!", "This game is expired.  So sad!");
				return;
			case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
				showWarning("Waiting for auto-match...",
						"We're still waiting for an automatch partner.");
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
				showWarning("Complete!",
						"This game is over; someone finished it!  You can only finish it now.");
		}

		// OK, it's active. Check on turn status.
		switch (turnStatus) {
			case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN:
				mTurnData = SkeletonTurn.unpersist(mMatch.getData());
				setGameplayUI();
				return;
			case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN:
				// Should return results.
				showWarning("Alas...", "It's not your turn.");
				break;
			case TurnBasedMatch.MATCH_TURN_STATUS_INVITED:
				showWarning("Good inititative!",
						"Still waiting for invitations.\n\nBe patient!");
		}

		mTurnData = null;

		setViewVisibility();*/
	}

}
