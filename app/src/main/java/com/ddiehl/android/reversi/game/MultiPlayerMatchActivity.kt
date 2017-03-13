package com.ddiehl.android.reversi.game

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.Toolbar
import butterknife.bindView
import com.ddiehl.android.reversi.R
import com.ddiehl.android.reversi.toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.games.Games
import com.google.android.gms.games.multiplayer.Multiplayer
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch
import com.google.example.games.basegameutils.BaseGameActivity
import com.google.example.games.basegameutils.BaseGameUtils
import timber.log.Timber

class MultiPlayerMatchActivity : BaseGameActivity(), SpinnerView,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    companion object {
        private val PREF_AUTO_SIGN_IN = "PREF_AUTO_SIGN_IN"

        private val RC_SIGN_IN = 9000
        private val RC_RESOLVE_ERROR = 1001
        private val RC_START_MATCH = 1002
        private val RC_NORMAL = 1003
    }

    private var mResolvingConnectionFailure = false
    private var mAutoStartSignInFlow = true
    private var mSignInClicked = false

    internal val mGoogleApiClient: GoogleApiClient by lazy {
        GoogleApiClient.Builder(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build()
    }

    private var mMatchReceived: TurnBasedMatch? = null

    internal val mToolbar by bindView<Toolbar>(R.id.toolbar)

    private val mStartMatchOnStart = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.match_activity)

        setSupportActionBar(mToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val fm = supportFragmentManager
        var fragment: Fragment? = fm.findFragmentById(R.id.fragment_container)

        if (fragment == null) {
            fragment = MatchFragment.newInstance(true)
            fm.beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .commit()
        }
    }

    override fun onStart() {
        super.onStart()

        connectGoogleApiClient()
    }

    public override fun onStop() {
        if (mGoogleApiClient.isConnected) {
            mGoogleApiClient.disconnect()
        }

        super.onStop()
    }

    private fun connectGoogleApiClient() {
        // Check if Google Play Services are available
        val result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        if (result != ConnectionResult.SUCCESS) {
            showErrorDialog(result)
            return
        }

        mGoogleApiClient.connect()
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
        toast("Connected to GPGS")
        dismissSpinner()
        // The player is signed in. Hide the sign-in button and allow the
        // player to proceed.

        if (bundle != null && bundle.containsKey(Multiplayer.EXTRA_TURN_BASED_MATCH)) {
            mMatchReceived = bundle.getParcelable<TurnBasedMatch>(Multiplayer.EXTRA_TURN_BASED_MATCH)
        }

        if (mStartMatchOnStart && mMatchReceived != null) {
            // TODO: Start received match
        }
    }

    override fun onConnectionSuspended(i: Int) {
        Timber.d("onConnectionSuspended")
        connectGoogleApiClient()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            RC_RESOLVE_ERROR -> {
                mResolvingConnectionFailure = false
                if (resultCode == Activity.RESULT_OK) {
                    if (!mGoogleApiClient.isConnecting && !mGoogleApiClient.isConnected) {
                        connectGoogleApiClient()
                    }
                }
            }
        }
    }

    /* Creates a dialog for an error message */
    private fun showErrorDialog(errorCode: Int) {
        val dialog = GoogleApiAvailability.getInstance().getErrorDialog(this, errorCode, RC_RESOLVE_ERROR)
        dialog.setOnDismissListener { mResolvingConnectionFailure = false }
        dialog.show()
    }


    //region SpinnerView

    private val mProgressBar: ProgressDialog by lazy {
        ProgressDialog(this, R.style.ProgressDialog).apply {
            setCancelable(true)
            setProgressStyle(ProgressDialog.STYLE_SPINNER)
            setMessage(getString(R.string.connecting))
        }
    }

    override fun showSpinner() {
        mProgressBar.show()
    }

    override fun dismissSpinner() {
        mProgressBar.dismiss()
    }

    //endregion
}
