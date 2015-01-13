package com.ddiehl.android.reversi.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.ddiehl.android.reversi.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.plus.Plus;

public class LauncherActivity extends Activity
		implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
	private static final String TAG = LauncherActivity.class.getSimpleName();

	private static final String PREF_AUTO_SIGN_IN = "pref_auto_sign_in";

	private static final int RC_RESOLVE_ERROR = 1001;
    private static final int RC_START_MATCH = 1002;
    private static final int RC_NORMAL = 1003;

	private GoogleApiClient mGoogleApiClient;
	private TurnBasedMatch mMatchReceived;

	private ProgressDialog mProgressBar;

	private boolean mSignInOnStart = false;
	private boolean mResolvingError = false;
    private boolean mStartMatchOnStart = false;

	private QueuedAction mQueuedAction;
	private enum QueuedAction {
		StartMultiplayer, StartMultiplayerMatch
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_launcher);
		initializeGoogleApiClient();
		mSignInOnStart = getAutoConnectPreference();
//        Utils.displayMetrics(this);
    }

	@Override
	public void onStart() {
		super.onStart();

		if (mSignInOnStart) {
            mStartMatchOnStart = true;
			connectGoogleApiClient();
		} else mStartMatchOnStart = false;
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
		if (mGoogleApiClient.isConnected())
			mGoogleApiClient.disconnect();
	}

	@Override
	public void onConnected(Bundle bundle) {
		dismissSpinner();
//		Toast.makeText(this, "Connected to Google Play", Toast.LENGTH_SHORT).show();

        if (bundle != null && bundle.containsKey(Multiplayer.EXTRA_TURN_BASED_MATCH))
            mMatchReceived = bundle.getParcelable(Multiplayer.EXTRA_TURN_BASED_MATCH);

        if ((mStartMatchOnStart && mMatchReceived != null) || mQueuedAction == QueuedAction.StartMultiplayerMatch) {
			mQueuedAction = null;
            startMultiplayerMatch(mMatchReceived);
        }

		if (mQueuedAction == QueuedAction.StartMultiplayer) {
			mQueuedAction = null;
			startMultiPlayer(findViewById(R.id.button_start_mp));
		}
	}

	@Override
	public void onConnectionSuspended(int i) {
		connectGoogleApiClient();
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		dismissSpinner();

		if (mResolvingError) {
			return; // Already attempting to resolve an error
		}

		if (result.hasResolution()) {
			try {
				mResolvingError = true;
				result.startResolutionForResult(this, RC_RESOLVE_ERROR);
			} catch (IntentSender.SendIntentException e) {
				connectGoogleApiClient();
			}
		} else {
			showErrorDialog(result.getErrorCode());
			mResolvingError = true;
		}
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RC_RESOLVE_ERROR:
                mResolvingError = false;
                if (resultCode == RESULT_OK) {
                    if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                        connectGoogleApiClient();
                    }
                }
                break;

            case RC_NORMAL:
            case RC_START_MATCH:
                if (resultCode == SettingsActivity.RESULT_SIGN_OUT) {
                    setAutoConnectPreference(false);
                }
                break;
        }
    }

	// Create the Google API Client with access to Plus and Games
	private void initializeGoogleApiClient() {
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
				.addApi(Games.API).addScope(Games.SCOPE_GAMES)
				.build();
	}

    private Dialog mSignInPrompt;
	private void displaySignInPrompt() {
        if (mSignInPrompt != null && mSignInPrompt.isShowing()) {
            // Dialog is already showing
        } else {
            mSignInPrompt = new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.dialog_sign_in_title))
                    .setMessage(getString(R.string.dialog_sign_in_message))
                    .setPositiveButton(getString(R.string.dialog_sign_in_confirm), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            setAutoConnectPreference(true);
                            connectGoogleApiClient();
                        }
                    })
                    .setNegativeButton(getString(R.string.dialog_sign_in_cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            // User canceled
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            mQueuedAction = null;
                        }
                    })
                    .create();
            mSignInPrompt.show();
        }
	}

	private void connectGoogleApiClient() {
		// Check if Google Play Services are available
		int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (result != ConnectionResult.SUCCESS) {
			setAutoConnectPreference(false);
			showErrorDialog(result);
			return;
		}

		setAutoConnectPreference(true);

		if (mGoogleApiClient == null)
			initializeGoogleApiClient();
		showSpinner();
		mGoogleApiClient.connect();
	}

	public void startSinglePlayer(View view) {
		startActivity(new Intent(this, SinglePlayerMatchActivity.class));
	}

	public void startMultiPlayer(View view) {
		if (mGoogleApiClient.isConnecting()) return;
		if (mGoogleApiClient.isConnected()) {
			startActivityForResult(new Intent(this, MultiPlayerMatchActivity.class), RC_NORMAL);
		} else {
			mQueuedAction = QueuedAction.StartMultiplayer;
			displaySignInPrompt();
		}
	}

    private void startMultiplayerMatch(TurnBasedMatch match) {
        if (mGoogleApiClient.isConnected()) {
            Intent intent = new Intent(this, MultiPlayerMatchActivity.class);
            intent.putExtra(Multiplayer.EXTRA_TURN_BASED_MATCH, match);
            mMatchReceived = null;
            startActivityForResult(intent, RC_NORMAL);
        } else {
			mQueuedAction = QueuedAction.StartMultiplayerMatch;
            displaySignInPrompt();
        }
    }

	private void showSpinner() {
		if (mProgressBar == null) {
			mProgressBar = new ProgressDialog(this, R.style.ProgressDialog);
			mProgressBar.setCancelable(false);
			mProgressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mProgressBar.setMessage(getString(R.string.connecting));
		}
		mProgressBar.show();
	}

	private void dismissSpinner() {
		mProgressBar.dismiss();
	}

	private boolean getAutoConnectPreference() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		return prefs.getBoolean(PREF_AUTO_SIGN_IN, false);
	}

	private void setAutoConnectPreference(boolean b) {
		mSignInOnStart = b;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.edit().putBoolean(PREF_AUTO_SIGN_IN, b).apply();
	}

	/* Creates a dialog for an error message */
	private void showErrorDialog(int errorCode) {
		Dialog dialog = GooglePlayServicesUtil.getErrorDialog(errorCode, this, RC_RESOLVE_ERROR);
		dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				mResolvingError = false;
			}
		});
		dialog.show();
	}
}
