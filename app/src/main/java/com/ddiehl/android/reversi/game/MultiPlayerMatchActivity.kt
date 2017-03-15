package com.ddiehl.android.reversi.game

import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.widget.Toast
import com.ddiehl.android.reversi.AUTOMATED_MULTIPLAYER
import com.ddiehl.android.reversi.R
import com.ddiehl.android.reversi.delay
import com.ddiehl.android.reversi.model.BoardSpace
import com.ddiehl.android.reversi.model.ComputerAI
import com.ddiehl.android.reversi.model.ReversiColor
import com.ddiehl.android.reversi.settings.SettingsActivity
import com.ddiehl.android.reversi.toast
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.games.Games
import com.google.android.gms.games.GamesStatusCodes
import com.google.android.gms.games.multiplayer.Participant
import com.google.android.gms.games.multiplayer.ParticipantResult
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer
import com.google.example.games.basegameutils.GameHelper
import timber.log.Timber
import java.util.*

class MultiPlayerMatchActivity : BaseMatchActivity(),
        MatchView, GameHelper.GameHelperListener {

    companion object {
        private @LayoutRes val LAYOUT_RES_ID = R.layout.match_activity

        private val PREF_AUTO_SIGN_IN = "PREF_AUTO_SIGN_IN"

        private val RC_SIGN_IN = 9000
        private val RC_NORMAL = 1000
        private val RC_RESOLVE_ERROR = 1001
        private val RC_START_MATCH = 1002
        private val RC_VIEW_MATCHES = 1003
        private val RC_SELECT_PLAYERS = 1004
        private val RC_SHOW_ACHIEVEMENTS = 1005
        private val RC_SETTINGS = 1006
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

        mHelper.onStop()
        super.onStop()
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

    private fun displaySignInPrompt() {
        AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_sign_in_title))
                .setMessage(getString(R.string.dialog_sign_in_message))
                .setPositiveButton(getString(R.string.dialog_sign_in_confirm), onSignInConfirm())
                .setNegativeButton(getString(R.string.dialog_sign_in_cancel), { _, _ -> })
                .show()
    }

    fun onSignInConfirm() = DialogInterface.OnClickListener { _, _ -> beginUserInitiatedSignIn() }


    //region MatchView

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

    override fun handleSpaceClick(row: Int, col: Int) {
        Timber.d("Piece clicked @ $row $col")
        if (mUpdatingMatch || !mQueuedMoves.isEmpty()) {
            Timber.d("Error: Still evaluating last move")
            return
        }

        if (mMatch!!.status != TurnBasedMatch.MATCH_STATUS_ACTIVE ||
                mMatch!!.turnStatus != TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
            return
        }

        val s = mBoard.spaceAt(row, col)

        if (s.isOwned)
            return

        if (!getApiClient().isConnected) {
            displaySignInPrompt()
            return
        }

        val playerColor = currentPlayerColor

        if (mBoard.spacesCapturedWithMove(s, playerColor) == 0) {
            toast(R.string.bad_move)
            return
        }

        mUpdatingMatch = true
        showSpinner()
        mBoard.commitPiece(s, playerColor) // FIXME: requestClaimSpace?
        saveMatchData()

        // Add selected piece to the end of mMatchData array
        // 0 [Light's Board] 64 [Dark's Moves] 100 [Dark's Board] 164 [Light's Moves]
        var nextIndex = if (mPlayer == mLightPlayer) 164 else 64
        // Increase index til we run into an unfilled index
        while (mMatchData!![nextIndex].toInt() != 0) {
            nextIndex += 1
        }
        mMatchData!![nextIndex] = mBoard.getSpaceNumber(s)

        updateMatchState()
    }

    //endregion


    //region Options menu actions

    override fun onStartNewMatchClicked() {
        if (!getApiClient().isConnected) {
            displaySignInPrompt()
        } else {
            val intent: Intent = Games.TurnBasedMultiplayer
                    .getSelectOpponentsIntent(getApiClient(), 1, 1, true)
            startActivityForResult(intent, RC_SELECT_PLAYERS)
        }
    }

    override fun onSelectMatchClicked() {
        if (!getApiClient().isConnected) {
            displaySignInPrompt()
        } else {
            val intent = Games.TurnBasedMultiplayer.getInboxIntent(getApiClient())
            startActivityForResult(intent, RC_VIEW_MATCHES)
        }
    }

    override fun clearBoard() {
        mMatch = null
        mMatchFragment.clearBoard()
    }

    override fun forfeitMatchSelected() {
        if (mMatch == null) {
            toast(R.string.no_match_selected, Toast.LENGTH_LONG)
            return
        }

        if (!getApiClient().isConnected) {
            displaySignInPrompt()
            return
        }

        when (mMatch!!.status) {
            TurnBasedMatch.MATCH_STATUS_COMPLETE,
            TurnBasedMatch.MATCH_STATUS_CANCELED,
            TurnBasedMatch.MATCH_STATUS_EXPIRED -> {
                toast(R.string.match_inactive)
            }
            TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING -> showLeaveMatchDialog()
            TurnBasedMatch.MATCH_STATUS_ACTIVE -> {
                if (mMatch!!.turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
                    if (mOpponent == null) {
                        showLeaveMatchDialog()
                    } else {
                        if (mOpponent!!.status == Participant.STATUS_JOINED) {
                            showForfeitMatchDialog()
                        } else {
                            showCancelMatchDialog()
                        }
                    }
                } else {
                    showForfeitMatchForbiddenAlert()
                }
            }
        }
    }

    override fun showAchievements() {
        if (!getApiClient().isConnected) {
            displaySignInPrompt()
            return
        }

        val intent = Games.Achievements.getAchievementsIntent(getApiClient())
        startActivityForResult(intent, RC_SHOW_ACHIEVEMENTS)
    }

    override fun settingsSelected() {
        val settings = Intent(this, SettingsActivity::class.java)
        settings.putExtra(SettingsActivity.EXTRA_SETTINGS_MODE, SettingsActivity.SETTINGS_MODE_MULTI_PLAYER)
        val isSignedIn = getApiClient().isConnected
        settings.putExtra(SettingsActivity.EXTRA_IS_SIGNED_IN, isSignedIn)
        val accountName = ""
        settings.putExtra(SettingsActivity.EXTRA_SIGNED_IN_ACCOUNT, accountName)
        startActivityForResult(settings, RC_SETTINGS)
    }

    //endregion


    private fun showCancelMatchDialog() {
        AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_cancel_match_title))
                .setMessage(getString(R.string.dialog_cancel_match_message))
                .setPositiveButton(getString(R.string.dialog_cancel_match_confirm), onCancelMatchConfirm())
                .setNegativeButton(getString(R.string.dialog_cancel_match_cancel)) { _, _ -> }
                .setCancelable(true)
                .show()
    }

    private fun onCancelMatchConfirm() = DialogInterface.OnClickListener { _, _ ->
        if (!getApiClient().isConnected) {
            displaySignInPrompt()
        } else {
            Games.TurnBasedMultiplayer.cancelMatch(getApiClient(), mMatch!!.matchId)
                    .setResultCallback { result -> processResult(result) }
        }
    }

    private fun showForfeitMatchDialog() {
        AlertDialog.Builder(this)
                .setTitle(R.string.dialog_forfeit_match_title)
                .setMessage(R.string.dialog_forfeit_match_message)
                .setPositiveButton(R.string.dialog_forfeit_match_confirm, onForfeitMatchConfirm())
                .setNegativeButton(R.string.dialog_forfeit_match_cancel) { _, _ -> }
                .setCancelable(true)
                .show()
    }

    private fun onForfeitMatchConfirm() = DialogInterface.OnClickListener { _, _ ->
        if (!getApiClient().isConnected) {
            displaySignInPrompt()
            return@OnClickListener
        }

        val winnerResult = ParticipantResult(
                mOpponent!!.participantId,
                ParticipantResult.MATCH_RESULT_WIN,
                ParticipantResult.PLACING_UNINITIALIZED
        )
        val loserResult = ParticipantResult(
                mPlayer!!.participantId,
                ParticipantResult.MATCH_RESULT_LOSS,
                ParticipantResult.PLACING_UNINITIALIZED
        )
        // Give win to other player
        Games.TurnBasedMultiplayer.finishMatch(
                getApiClient(), mMatch!!.matchId, mMatchData, winnerResult, loserResult
        )
                .setResultCallback { result ->
                    if (result.status.isSuccess) {
                        toast(R.string.forfeit_success, Toast.LENGTH_LONG)
                        updateMatch(result.match)
                    } else {
                        toast(R.string.forfeit_fail, Toast.LENGTH_LONG)
                    }
                }
    }

    private fun showForfeitMatchForbiddenAlert() {
        AlertDialog.Builder(this)
                .setTitle(R.string.dialog_forfeit_match_forbidden_title)
                .setMessage(R.string.dialog_forfeit_match_forbidden_message)
                .setPositiveButton(R.string.dialog_forfeit_match_forbidden_confirm) { _, _ -> }
                .setCancelable(true)
                .show()
    }

    private fun showLeaveMatchDialog() {
        AlertDialog.Builder(this)
                .setTitle(R.string.dialog_leave_match_title)
                .setMessage(R.string.dialog_leave_match_message)
                .setPositiveButton(R.string.dialog_leave_match_confirm, onLeaveMatchConfirm())
                .setNegativeButton(R.string.dialog_leave_match_cancel, { _, _ -> })
                .setCancelable(true)
                .show()
    }

    private fun onLeaveMatchConfirm() = DialogInterface.OnClickListener { _, _ ->
        if (!getApiClient().isConnected) {
            displaySignInPrompt()
            return@OnClickListener
        }

        if (mMatch!!.turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
            Games.TurnBasedMultiplayer.leaveMatchDuringTurn(getApiClient(), mMatch!!.matchId, null)
                    .setResultCallback { processResultLeaveMatch(it) }
        } else {
            Games.TurnBasedMultiplayer.leaveMatch(getApiClient(), mMatch!!.matchId)
                    .setResultCallback { processResultLeaveMatch(it) }
        }
    }

    private fun processResultFinishMatch(result: TurnBasedMultiplayer.UpdateMatchResult) {
        mUpdatingMatch = false
        dismissSpinner()
        if (checkStatusCode(result.status.statusCode)) {
            mMatch = result.match
            mPlayer = currentPlayer
            // Update achievements
            if (mPlayer!!.result.result == ParticipantResult.MATCH_RESULT_WIN) {
                mAchievementManager.unlock(Achievements.FIRST_WIN)
                val maxScore = mBoard.width * mBoard.height
                if (playerScore == maxScore) {
                    mAchievementManager.unlock(Achievements.PERFECT_WIN)
                }
            } else if (mPlayer!!.result.result == ParticipantResult.MATCH_RESULT_TIE) {
                mAchievementManager.unlock(Achievements.TIE_GAME)
            }
            mAchievementManager.increment(Achievements.TEN_MATCHES, 1)
            mAchievementManager.increment(Achievements.HUNDRED_MATCHES, 1)

            if (mMatch!!.canRematch()) {
                askForRematch()
            }
        }
    }


    private fun checkStatusCode(statusCode: Int): Boolean {
        if (statusCode == GamesStatusCodes.STATUS_OK ||
                statusCode == GamesStatusCodes.STATUS_NETWORK_ERROR_OPERATION_DEFERRED) {
            return true
        }

        clearBoard()
        dismissSpinner()

        when (statusCode) {
            GamesStatusCodes.STATUS_MULTIPLAYER_ERROR_NOT_TRUSTED_TESTER -> {
                showAlertDialog(
                        getString(R.string.dialog_error_title),
                        getString(R.string.dialog_error_tester_untrusted)
                )
            }
            GamesStatusCodes.STATUS_MATCH_ERROR_ALREADY_REMATCHED -> {
                showAlertDialog(
                        getString(R.string.dialog_error_title),
                        getString(R.string.dialog_error_already_rematched)
                )
            }
            GamesStatusCodes.STATUS_NETWORK_ERROR_OPERATION_FAILED -> {
                showAlertDialog(
                        getString(R.string.dialog_error_title),
                        getString(R.string.dialog_error_network_operation_failed)
                )
            }
            GamesStatusCodes.STATUS_CLIENT_RECONNECT_REQUIRED -> {
                showAlertDialog(
                        getString(R.string.dialog_error_title),
                        getString(R.string.dialog_error_reconnect_required)
                )
            }
            GamesStatusCodes.STATUS_INTERNAL_ERROR -> {
                showAlertDialog(
                        getString(R.string.dialog_error_title),
                        getString(R.string.dialog_error_internal_error)
                )
            }
            GamesStatusCodes.STATUS_MATCH_ERROR_INACTIVE_MATCH -> {
                showAlertDialog(
                        getString(R.string.dialog_error_title),
                        getString(R.string.dialog_error_inactive_match)
                )
            }
            GamesStatusCodes.STATUS_MATCH_ERROR_LOCALLY_MODIFIED -> {
                showAlertDialog(
                        getString(R.string.dialog_error_title),
                        getString(R.string.dialog_error_locally_modified)
                )
            }
            else -> {
                showAlertDialog(
                        getString(R.string.dialog_error_title),
                        getString(R.string.dialog_error_message_default)
                )
            }
        }

        return false
    }

    // Generic warning/info dialog
    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.dialog_error_confirm)) { _, _ -> }
                .show()
    }

    /* Creates a dialog for an error message */
    private fun showErrorDialog(errorCode: Int) {
        val dialog = GooglePlayServicesUtil.getErrorDialog(errorCode, this, RC_RESOLVE_ERROR)
        if (dialog != null) {
            dialog.setOnDismissListener { mResolvingError = false }
            dialog.show()
        }
    }

    private fun askForRematch() {
        AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_rematch_title))
                .setMessage(getString(R.string.dialog_rematch_message))
                .setPositiveButton(getString(R.string.dialog_rematch_confirm), onRematchConfirm())
                .setNegativeButton(getString(R.string.dialog_rematch_cancel), onRematchCancel())
                .setIcon(ContextCompat.getDrawable(this, R.drawable.ic_av_replay_blue))
                .create()
    }

    private fun onRematchConfirm() =
            DialogInterface.OnClickListener { _, _ ->
                if (!getApiClient().isConnected) {
                    mMatchView.showSpinner()
                    Games.TurnBasedMultiplayer.rematch(getApiClient(), mMatch!!.matchId)
                            .setResultCallback { result -> processResult(result) }
                    mMatch = null
                } else {
                    displaySignInPrompt()
                }
            }

    private fun onRematchCancel() = DialogInterface.OnClickListener { _, _ -> }

    private fun processResultLeaveMatch(result: TurnBasedMultiplayer.LeaveMatchResult) {
        if (result.status.isSuccess) {
            toast(R.string.match_canceled_toast)
            clearBoard()
        } else {
            toast(R.string.cancel_fail)
        }
    }

    private fun processResult(result: TurnBasedMultiplayer.CancelMatchResult) {
        if (result.status.isSuccess) {
            toast(R.string.match_canceled_toast)
            clearBoard()
        } else {
            toast(R.string.cancel_fail)
        }
    }

    private val playerScore: Int
        get() = if (mPlayer === mLightPlayer) mLightScore else mDarkScore

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
