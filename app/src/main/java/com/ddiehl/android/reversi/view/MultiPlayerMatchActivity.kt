package com.ddiehl.android.reversi.view

import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager

import com.ddiehl.android.reversi.R
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.games.Games
import com.google.android.gms.games.multiplayer.Multiplayer
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch
import com.google.example.games.basegameutils.BaseGameActivity
import com.google.example.games.basegameutils.BaseGameUtils

import timber.log.Timber

class MultiPlayerMatchActivity : BaseGameActivity(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private var mResolvingConnectionFailure = false
    private var mAutoStartSignInFlow = true
    private var mSignInClicked = false

    private var mGoogleApiClient: GoogleApiClient? = null
    private var mMatchReceived: TurnBasedMatch? = null

    private val mProgressBar: ProgressDialog? = null
    private val mSignInPrompt: Dialog? = null

    //    private boolean mSignInOnStart = false;
    //    private boolean mResolvingError = false;
    private val mStartMatchOnStart = false

    private var mQueuedAction: QueuedAction? = null

    private enum class QueuedAction {
        StartMultiplayer, StartMultiplayerMatch
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment)

        val fm = supportFragmentManager
        var fragment: Fragment? = fm.findFragmentById(R.id.fragment_container)

        if (fragment == null) {
            fragment = MultiPlayerMatchFragment()
            fm.beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .commit()
        }

        mGoogleApiClient = setupApiClient()
    }

    internal fun setupApiClient(): GoogleApiClient {
        return GoogleApiClient.Builder(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build()
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

    override fun onStart() {
        super.onStart()

        connectGoogleApiClient()
    }

    public override fun onStop() {
        super.onStop()

        if (mGoogleApiClient!!.isConnected) {
            mGoogleApiClient!!.disconnect()
        }
    }

    private fun connectGoogleApiClient() {
        // Check if Google Play Services are available
        val result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)
        if (result != ConnectionResult.SUCCESS) {
            showErrorDialog(result)
            return
        }

        mGoogleApiClient!!.connect()
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        if (mResolvingConnectionFailure) {
            // Already resolving
            return
        }

        // If the sign in button was clicked or if auto sign-in is enabled,
        // launch the sign-in flow
        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false
            mSignInClicked = false
            mResolvingConnectionFailure = true

            // Attempt to resolve the connection failure using BaseGameUtils.
            // The R.string.signin_other_error value should reference a generic
            // error string in your strings.xml file, such as "There was
            // an issue with sign in, please try again later."
            if (!BaseGameUtils.resolveConnectionFailure(
                    this, mGoogleApiClient, connectionResult, RC_SIGN_IN,
                    getString(R.string.signin_other_error))) {
                mResolvingConnectionFailure = false
            }
        }

        // Put code here to display the sign-in button
    }

    override fun onSignInFailed() {
        Timber.d("Sign In FAILED: %s", gameHelper.signInError.toString())
    }

    override fun onSignInSucceeded() {
        Timber.d("Sign In SUCCESS")
    }

    override fun onConnected(bundle: Bundle?) {
        Timber.d("onConnected")
        // The player is signed in. Hide the sign-in button and allow the
        // player to proceed.

        //        dismissSpinner();
        //        Toast.makeText(this, "Connected to Google Play", Toast.LENGTH_SHORT).show();

        if (bundle != null && bundle.containsKey(Multiplayer.EXTRA_TURN_BASED_MATCH)) {
            mMatchReceived = bundle.getParcelable<TurnBasedMatch>(Multiplayer.EXTRA_TURN_BASED_MATCH)
        }

        if (mStartMatchOnStart && mMatchReceived != null || mQueuedAction == QueuedAction.StartMultiplayerMatch) {
            mQueuedAction = null
            start(mMatchReceived)
        }

        if (mQueuedAction == QueuedAction.StartMultiplayer) {
            mQueuedAction = null
            start(null)
        }
    }

    private fun start(match: TurnBasedMatch?) {

    }

    override fun onConnectionSuspended(i: Int) {
        Timber.d("onConnectionSuspended")
        connectGoogleApiClient()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            RC_RESOLVE_ERROR -> {
                mResolvingConnectionFailure = false
                if (resultCode == Activity.RESULT_OK) {
                    if (!mGoogleApiClient!!.isConnecting && !mGoogleApiClient!!.isConnected) {
                        connectGoogleApiClient()
                    }
                }
            }
        }//            case RC_NORMAL:
        //            case RC_START_MATCH:
        //                if (resultCode == SettingsActivity.RESULT_SIGN_OUT) {
        //                    setAutoConnectPreference(false);
        //                }
        //                break;
    }

    /* Creates a dialog for an error message */
    private fun showErrorDialog(errorCode: Int) {
        val dialog = GooglePlayServicesUtil.getErrorDialog(errorCode, this, RC_RESOLVE_ERROR)
        dialog.setOnDismissListener { mResolvingConnectionFailure = false }
        dialog.show()
    }

    companion object {

        private val PREF_AUTO_SIGN_IN = "pref_auto_sign_in"

        private val RC_SIGN_IN = 9000
        private val RC_RESOLVE_ERROR = 1001
        private val RC_START_MATCH = 1002
        private val RC_NORMAL = 1003
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
