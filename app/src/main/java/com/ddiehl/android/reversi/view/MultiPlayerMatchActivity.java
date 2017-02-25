package com.ddiehl.android.reversi.view;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.ddiehl.android.reversi.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.example.games.basegameutils.BaseGameActivity;
import com.google.example.games.basegameutils.BaseGameUtils;

import timber.log.Timber;

public class MultiPlayerMatchActivity extends BaseGameActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private boolean mResolvingConnectionFailure = false;
    private boolean mAutoStartSignInFlow = true;
    private boolean mSignInClicked = false;

    private static final String PREF_AUTO_SIGN_IN = "pref_auto_sign_in";

    private static final int RC_SIGN_IN = 9000;
    private static final int RC_RESOLVE_ERROR = 1001;
    private static final int RC_START_MATCH = 1002;
    private static final int RC_NORMAL = 1003;

    private GoogleApiClient mGoogleApiClient;
    private TurnBasedMatch mMatchReceived;

    private ProgressDialog mProgressBar;
    private Dialog mSignInPrompt;

//    private boolean mSignInOnStart = false;
//    private boolean mResolvingError = false;
    private boolean mStartMatchOnStart = false;

    private QueuedAction mQueuedAction;
    private enum QueuedAction {
        StartMultiplayer, StartMultiplayerMatch
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);

        if (fragment == null) {
            fragment = new MultiPlayerMatchFragment();
            fm.beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .commit();
        }

        mGoogleApiClient = setupApiClient();
    }

    GoogleApiClient setupApiClient() {
        return new GoogleApiClient.Builder(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

//    private void displaySignInPrompt() {
//        if (mSignInPrompt != null && mSignInPrompt.isShowing()) {
//            // Dialog is already showing
//        } else {
//            mSignInPrompt = new AlertDialog.Builder(getActivity())
//                    .setTitle(getString(R.string.dialog_sign_in_title))
//                    .setMessage(getString(R.string.dialog_sign_in_message))
//                    .setPositiveButton(getString(R.string.dialog_sign_in_confirm), new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int id) {
//                            setAutoConnectPreference(true);
//                            connectGoogleApiClient();
//                        }
//                    })
//                    .setNegativeButton(getString(R.string.dialog_sign_in_cancel), new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int id) {
//                            // User canceled
//                        }
//                    })
//                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
//                        @Override
//                        public void onCancel(DialogInterface dialog) {
//                            mQueuedAction = null;
//                        }
//                    })
//                    .create();
//            mSignInPrompt.show();
//        }
//    }

    @Override
    protected void onStart() {
        super.onStart();

        connectGoogleApiClient();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    private void connectGoogleApiClient() {
        // Check if Google Play Services are available
        int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (result != ConnectionResult.SUCCESS) {
            showErrorDialog(result);
            return;
        }

        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (mResolvingConnectionFailure) {
            // Already resolving
            return;
        }

        // If the sign in button was clicked or if auto sign-in is enabled,
        // launch the sign-in flow
        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = true;

            // Attempt to resolve the connection failure using BaseGameUtils.
            // The R.string.signin_other_error value should reference a generic
            // error string in your strings.xml file, such as "There was
            // an issue with sign in, please try again later."
            if (!BaseGameUtils.resolveConnectionFailure(
                    this, mGoogleApiClient, connectionResult, RC_SIGN_IN,
                    getString(R.string.signin_other_error))) {
                mResolvingConnectionFailure = false;
            }
        }

        // Put code here to display the sign-in button
    }

    @Override
    public void onSignInFailed() {
        Timber.d("Sign In FAILED: %s", getGameHelper().getSignInError().toString());
    }

    @Override
    public void onSignInSucceeded() {
        Timber.d("Sign In SUCCESS");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Timber.d("onConnected");
        // The player is signed in. Hide the sign-in button and allow the
        // player to proceed.

//        dismissSpinner();
//        Toast.makeText(this, "Connected to Google Play", Toast.LENGTH_SHORT).show();

        if (bundle != null && bundle.containsKey(Multiplayer.EXTRA_TURN_BASED_MATCH)) {
            mMatchReceived = bundle.getParcelable(Multiplayer.EXTRA_TURN_BASED_MATCH);
        }

        if ((mStartMatchOnStart && mMatchReceived != null) || mQueuedAction == QueuedAction.StartMultiplayerMatch) {
            mQueuedAction = null;
            start(mMatchReceived);
        }

        if (mQueuedAction == QueuedAction.StartMultiplayer) {
            mQueuedAction = null;
            start(null);
        }
    }

    private void start(TurnBasedMatch match) {

    }

    @Override
    public void onConnectionSuspended(int i) {
        Timber.d("onConnectionSuspended");
        connectGoogleApiClient();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RC_RESOLVE_ERROR:
                mResolvingConnectionFailure = false;
                if (resultCode == Activity.RESULT_OK) {
                    if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                        connectGoogleApiClient();
                    }
                }
                break;
//            case RC_NORMAL:
//            case RC_START_MATCH:
//                if (resultCode == SettingsActivity.RESULT_SIGN_OUT) {
//                    setAutoConnectPreference(false);
//                }
//                break;
        }
    }

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(errorCode, this, RC_RESOLVE_ERROR);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mResolvingConnectionFailure = false;
            }
        });
        dialog.show();
    }

//    private void showSpinner() {
//        if (mProgressBar == null) {
//            mProgressBar = new ProgressDialog(this, R.style.ProgressDialog);
//            mProgressBar.setCancelable(true);
//            mProgressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//            mProgressBar.setMessage(getString(R.string.connecting));
//        }
//        mProgressBar.show();
//    }
//
//    private void dismissSpinner() {
//        mProgressBar.dismiss();
//    }
}
