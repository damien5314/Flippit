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
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.ddiehl.android.reversi.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.plus.Plus;

public class LauncherActivity extends Activity
		implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
	public static final String TAG = LauncherActivity.class.getSimpleName();

	private static final String PREF_AUTO_SIGN_IN = "pref_auto_sign_in";
	private static final String DIALOG_ERROR = "dialog_error";

	public static final int RC_RESOLVE_ERROR = 1001;

	private GoogleApiClient mGoogleApiClient;
	private ProgressDialog mProgressBar;
	private boolean mSignInOnStart = false;
	private boolean mResolvingError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_launcher);
		initializeGoogleApiClient();
		mSignInOnStart = getAutoConnectPreference();
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

	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, "LauncherActivity - onStart");

		if (mSignInOnStart) {
			connectGoogleApiClient();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d(TAG, "LauncherActivity - onPause");
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.d(TAG, "LauncherActivity - onStop");
		setAutoConnectPreference(mSignInOnStart);
		if (mGoogleApiClient.isConnected())
			mGoogleApiClient.disconnect();
	}

	@Override
	public void onConnected(Bundle bundle) {
		dismissSpinner();
        Log.d(TAG, "Connected to Google Play Services");
		Toast.makeText(this, "Connected to Google Play", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onConnectionSuspended(int i) {
		connectGoogleApiClient();
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
        Log.d(TAG, "Failed to connect to Google Play - Error: " + result.getErrorCode());
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
				connectGoogleApiClient();
			}
		} else {
			Log.d(TAG, "Unresolvable error (ErrorCode: " + result.getErrorCode() + ")");
			showErrorDialog(result.getErrorCode());
			mResolvingError = true;
		}
	}

	private void displaySignInPrompt() {
		new AlertDialog.Builder(this)
				.setTitle(getString(R.string.dialog_signin_title))
				.setMessage(getString(R.string.dialog_signin_message))
				.setPositiveButton(getString(R.string.dialog_signin_confirm), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						mSignInOnStart = true;
						setAutoConnectPreference(true);
						connectGoogleApiClient();
					}
				})
				.setNegativeButton(getString(R.string.dialog_signin_cancel), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// User canceled
					}
				})
				.show();
	}

	private void connectGoogleApiClient() {
		Log.d(TAG, "LauncherActivity - connectGoogleApiClient()");
		mSignInOnStart = true;

		// Check if Google Play Services are available
		int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (result != ConnectionResult.SUCCESS) {
			showErrorDialog(result);
			return;
		}

		// Show spinner
		if (mProgressBar != null && mProgressBar.isShowing())
			dismissSpinner();
		showSpinner();

		// Initialize client if null
		if (mGoogleApiClient == null)
			initializeGoogleApiClient();

		// Call connect() on GoogleApiClient
		mGoogleApiClient.connect();
	}

	public void startSinglePlayer(View view) {
		startActivity(new Intent(this, SinglePlayerMatchActivity.class));
	}

	public void startMultiPlayer(View view) {
		if (mGoogleApiClient.isConnecting()) return;
		if (mGoogleApiClient.isConnected()) {
			startActivity(new Intent(this, MultiPlayerMatchActivity.class));
		} else {
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
