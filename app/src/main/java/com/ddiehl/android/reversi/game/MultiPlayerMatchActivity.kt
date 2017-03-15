package com.ddiehl.android.reversi.game

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v4.app.ActivityCompat
import com.ddiehl.android.reversi.AUTOMATED_MULTIPLAYER
import com.ddiehl.android.reversi.R
import com.ddiehl.android.reversi.delay
import com.ddiehl.android.reversi.model.BoardSpace
import com.ddiehl.android.reversi.model.ComputerAI
import com.ddiehl.android.reversi.model.ReversiColor
import com.ddiehl.android.reversi.toast
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.games.Games
import com.google.android.gms.games.multiplayer.Participant
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch
import com.google.example.games.basegameutils.GameHelper
import timber.log.Timber
import java.util.*

class MultiPlayerMatchActivity : BaseMatchActivity(),
        MatchView, GameHelper.GameHelperListener {

    companion object {
        private @LayoutRes val LAYOUT_RES_ID = R.layout.match_activity

        private val PREF_AUTO_SIGN_IN = "PREF_AUTO_SIGN_IN"

        private val RC_SIGN_IN = 9000
        private val RC_RESOLVE_ERROR = 1001
        private val RC_START_MATCH = 1002
        private val RC_NORMAL = 1003
    }

    private var mRequestedClients = GameHelper.CLIENT_GAMES
    private val mHelper: GameHelper by lazy {
        GameHelper(this, mRequestedClients)
                .apply { enableDebugLog(true) }
    }

    private var mMatch: TurnBasedMatch? = null
    private var mPlayer: Participant? = null
    private var mOpponent: Participant? = null
    private var mLightPlayer: Participant? = null
    private var mDarkPlayer: Participant? = null
    private var mMatchData: ByteArray? = null
    private var mLightScore: Int = 0
    private var mDarkScore: Int = 0

    private var mSignInOnStart = true
    private var mSignOutOnConnect = false
    private var mResolvingError = false
    private var mUpdatingMatch = false
    private var mIsSignedIn = false
    private val mQueuedMoves: MutableList<BoardSpace> = ArrayList()

    private var mResolvingConnectionFailure = false
    private var mAutoStartSignInFlow = true
    private var mSignInClicked = false

    private lateinit var mAchievementManager: AchievementManager

    private var mMatchReceived: TurnBasedMatch? = null
    private val mStartMatchOnStart = false


    //region BaseGameActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mHelper.setup(this)
        mHelper.setShowErrorDialogs(true)

        setContentView(LAYOUT_RES_ID)

        setSupportActionBar(mToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // Initialize Games API client
        mAchievementManager = AchievementManager.get(getApiClient())
    }

    override fun onStart() {
        super.onStart()
        mHelper.onStart(this)
    }

    override fun onStop() {
        if (getApiClient().isConnected) {
            registerMatchUpdateListener(false)
            getApiClient().disconnect()
        }

        super.onStop()
        mHelper.onStop()
    }

    override fun onActivityResult(request: Int, response: Int, data: Intent) {
        super.onActivityResult(request, response, data)
        mHelper.onActivityResult(request, response, data)
    }

    private fun getApiClient(): GoogleApiClient {
        return mHelper.apiClient
    }

    private fun isSignedIn(): Boolean {
        return mHelper.isSignedIn
    }

    private fun beginUserInitiatedSignIn() {
        mHelper.beginUserInitiatedSignIn()
    }

    private fun signOut() {
        mHelper.signOut()
    }

    private fun enableDebugLog(enabled: Boolean) {
        mHelper.enableDebugLog(enabled)
    }

    private fun getInvitationId(): String {
        return mHelper.invitationId
    }

    private fun reconnectClient() {
        mHelper.reconnectClient()
    }

    private fun hasSignInError(): Boolean {
        return mHelper.hasSignInError()
    }

    private fun getSignInError(): GameHelper.SignInFailureReason {
        return mHelper.signInError
    }

    //endregion


    //region GameHelper.Listener

    override fun onSignInSucceeded() {
        Timber.d("Sign In SUCCESS")
        toast("Connected to Games Services")
        dismissSpinner()
    }

    override fun onSignInFailed() {
        dismissSpinner()

        if (mHelper.hasSignInError()) {
            toast("Sign in failed: " + mHelper.signInError.toString())
        }
    }

    //endregion

    override fun showAchievements() {
        if (!getApiClient().isConnected) {
            displaySignInPrompt()
            return
        }

        val intent = Games.Achievements.getAchievementsIntent(mGoogleApiClient)
        ActivityCompat.startActivityForResult(intent, MatchFragment.RC_SHOW_ACHIEVEMENTS)
    }

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

    // For testing full end-to-end multiplayer flow
    private fun autoplayIfEnabled() {
        if (!mUpdatingMatch && AUTOMATED_MULTIPLAYER
                && mMatch!!.status == TurnBasedMatch.MATCH_STATUS_ACTIVE
                && mMatch!!.turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
            delay(500) {
                val color = if (mPlayer === mLightPlayer) ReversiColor.LIGHT else ReversiColor.DARK
                val bestMove = ComputerAI.getBestMove_d3(mBoard, color)
                handleSpaceClick(bestMove.y, bestMove.x)
            }
        }
    }
}
