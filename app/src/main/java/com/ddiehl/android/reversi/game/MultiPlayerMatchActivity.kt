package com.ddiehl.android.reversi.game

import android.app.ProgressDialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.Toolbar
import butterknife.bindView
import com.ddiehl.android.reversi.R
import com.ddiehl.android.reversi.toast
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch
import com.google.example.games.basegameutils.BaseGameActivity
import timber.log.Timber

class MultiPlayerMatchActivity : BaseGameActivity(), SpinnerView {

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

    fun getGoogleApiClient(): GoogleApiClient = mHelper.apiClient

    private var mMatchReceived: TurnBasedMatch? = null

    internal val mToolbar by bindView<Toolbar>(R.id.toolbar)

    private val mStartMatchOnStart = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.match_activity)

        setSupportActionBar(mToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // Debugging: Show error dialogs
        mHelper.setShowErrorDialogs(true)

        val fm = supportFragmentManager
        var fragment: Fragment? = fm.findFragmentById(R.id.fragment_container)

        if (fragment == null) {
            fragment = MatchFragment.newInstance(true)
            fm.beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .commit()
        }
    }


    //region GameHelper.Listener

    override fun onSignInSucceeded() {
        Timber.d("Sign In SUCCESS")
        toast("Connected to Games Services")
        dismissSpinner()
    }

    override fun onSignInFailed() {
        dismissSpinner()

        if (gameHelper.hasSignInError()) {
            toast("Sign in failed: " + gameHelper.signInError.toString())
        }
    }

    //endregion


//    override fun onConnected(bundle: Bundle?) {
//        toast("Connected to GPGS")
//        dismissSpinner()
//        // The player is signed in. Hide the sign-in button and allow the
//        // player to proceed.
//
//        if (bundle != null && bundle.containsKey(Multiplayer.EXTRA_TURN_BASED_MATCH)) {
//            mMatchReceived = bundle.getParcelable<TurnBasedMatch>(Multiplayer.EXTRA_TURN_BASED_MATCH)
//        }
//
//        if (mStartMatchOnStart && mMatchReceived != null) {
//            // TODO: Start received match
//        }
//    }


    //region SpinnerView

    private val mProgressBar: ProgressDialog by lazy {
        ProgressDialog(this, R.style.ProgressDialog).apply {
            setCancelable(true)
            setProgressStyle(ProgressDialog.STYLE_SPINNER)
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
