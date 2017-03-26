package com.ddiehl.android.reversi.game

import android.app.Activity
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.widget.Toast
import com.ddiehl.android.reversi.*
import com.ddiehl.android.reversi.model.BoardSpace
import com.ddiehl.android.reversi.model.ComputerAI
import com.ddiehl.android.reversi.model.ReversiColor
import com.ddiehl.android.reversi.settings.SettingsActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.games.Games
import com.google.android.gms.games.GamesActivityResultCodes
import com.google.android.gms.games.GamesStatusCodes
import com.google.android.gms.games.multiplayer.Multiplayer
import com.google.android.gms.games.multiplayer.Participant
import com.google.android.gms.games.multiplayer.ParticipantResult
import com.google.android.gms.games.multiplayer.realtime.RoomConfig
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchUpdateReceivedListener
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer
import com.google.example.games.basegameutils.GameHelper
import timber.log.Timber
import java.util.*

class MultiPlayerMatchActivity : BaseMatchActivity(),
        IMatchView, GameHelper.GameHelperListener, OnTurnBasedMatchUpdateReceivedListener {

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

    private val mHelper: GameHelper by lazy {
        val clients = GameHelper.CLIENT_GAMES
        GameHelper(this, clients)
                .apply {
                    if (BuildConfig.DEBUG) enableDebugLog(true)
                }
    }

    private var mMatch: TurnBasedMatch? = null
    private var mPlayer: Participant? = null
    private var mOpponent: Participant? = null
    private var mLightPlayer: Participant? = null
    private var mDarkPlayer: Participant? = null
    private var mMatchData: ByteArray? = null
    private var mLightScore: Int = 0
    private var mDarkScore: Int = 0

    private var mSignOutOnConnect = false
    private var mUpdatingMatch = false
    private val mQueuedMoves: MutableList<BoardSpace> = ArrayList()

    private var mResolvingConnectionFailure = false
    private var mAutoStartSignInFlow = true
    private var mSignInClicked = false

    private lateinit var mAchievementManager: AchievementManager

    private var mMatchReceived: TurnBasedMatch? = null
    private var mStartMatchOnStart = false


    //region BaseGameActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mHelper.setup(this)
        mHelper.setShowErrorDialogs(true)

        setContentView(LAYOUT_RES_ID)

        setSupportActionBar(mToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

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
        dismissSpinner()

        if (mSignOutOnConnect) {
            mSignOutOnConnect = false
            signOut()
        }
    }

    override fun onSignInFailed() {
        dismissSpinner()

        if (mHelper.hasSignInError()) {
            toast("Sign in failed: " + mHelper.signInError.toString())
        }
    }

    //endregion


    private val currentPlayer: Participant
        get() {
            val currentPlayerId = Games.Players.getCurrentPlayerId(getApiClient())
            if (mMatch!!.getParticipant(mMatch!!.getParticipantId(currentPlayerId)) === lightPlayer) {
                return lightPlayer!!
            } else {
                return darkPlayer!!
            }
        }

    private val opponent: Participant?
        get() = mMatch!!.descriptionParticipant

    private val currentPlayerColor: ReversiColor
        get() {
            if (mPlayer === mLightPlayer) {
                return ReversiColor.LIGHT
            } else {
                return ReversiColor.DARK
            }
        }

    private val opponentColor: ReversiColor
        get() {
            if (mOpponent === mLightPlayer)
                return ReversiColor.LIGHT
            else
                return ReversiColor.DARK
        }

    private val lightPlayer: Participant?
        get() {
            if (mMatch != null) {
                return mMatch!!.getParticipant(mMatch!!.creatorId)
            }
            return null
        }

    private val darkPlayer: Participant?
        get() {
            if (mMatch != null) {
                val participantIds = mMatch!!.participantIds
                val lightId = mMatch!!.creatorId

                val darkId: String? = participantIds.lastOrNull { it != lightId }

                if (darkId != null) {
                    return mMatch!!.getParticipant(darkId)
                }
            }
            return null
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

    override fun onSpaceClick(row: Int, col: Int) {
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

    override fun endMatch() {
        updateScore()

        if (mMatch!!.status == TurnBasedMatch.MATCH_STATUS_COMPLETE) {
            when (mPlayer!!.result.result) {
                ParticipantResult.MATCH_RESULT_WIN -> {
                    mMatchFragment.displayMessage(getString(R.string.winner_you))
                }
                ParticipantResult.MATCH_RESULT_TIE -> {
                    mMatchFragment.displayMessage(getString(R.string.winner_tie))
                }
                ParticipantResult.MATCH_RESULT_LOSS -> {
                    val msg = if (mPlayer === mLightPlayer) R.string.winner_dark else R.string.winner_light
                    mMatchFragment.displayMessage(getString(msg))
                }
                else -> {
                    mMatchFragment.displayMessage(getString(R.string.match_complete))
                }
            }

            if (mMatch!!.turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
                // Call finishMatch() to close out match for player
                Games.TurnBasedMultiplayer.finishMatch(getApiClient(), mMatch!!.matchId)
                        .setResultCallback {
                            updateMatchResult -> processResultFinishMatch(updateMatchResult)
                        }
            }
        } else { // Match is not yet finished
            val winnerResult: ParticipantResult
            val loserResult: ParticipantResult
            if (mLightScore != mDarkScore) {
                if (mLightScore > mDarkScore) {
                    winnerResult = ParticipantResult(
                            mLightPlayer!!.participantId,
                            ParticipantResult.MATCH_RESULT_WIN,
                            ParticipantResult.PLACING_UNINITIALIZED
                    )
                    loserResult = ParticipantResult(
                            mDarkPlayer!!.participantId,
                            ParticipantResult.MATCH_RESULT_LOSS,
                            ParticipantResult.PLACING_UNINITIALIZED
                    )
                    val msg = if (mPlayer === mLightPlayer) R.string.winner_you else R.string.winner_light
                    mMatchFragment.displayMessage(getString(msg))
                } else {
                    winnerResult = ParticipantResult(
                            mDarkPlayer!!.participantId,
                            ParticipantResult.MATCH_RESULT_WIN,
                            ParticipantResult.PLACING_UNINITIALIZED
                    )
                    loserResult = ParticipantResult(
                            mLightPlayer!!.participantId,
                            ParticipantResult.MATCH_RESULT_LOSS,
                            ParticipantResult.PLACING_UNINITIALIZED
                    )
                    val msg = if (mPlayer === mDarkPlayer) R.string.winner_you else R.string.winner_dark
                    mMatchFragment.displayMessage(getString(msg))
                }
            } else {
                winnerResult = ParticipantResult(
                        mDarkPlayer!!.participantId,
                        ParticipantResult.MATCH_RESULT_TIE,
                        ParticipantResult.PLACING_UNINITIALIZED
                )
                loserResult = ParticipantResult(
                        mLightPlayer!!.participantId,
                        ParticipantResult.MATCH_RESULT_TIE,
                        ParticipantResult.PLACING_UNINITIALIZED
                )
                mMatchFragment.displayMessage(getString(R.string.winner_tie))
            }

            // Call finishMatch() with result parameters
            Games.TurnBasedMultiplayer.finishMatch(
                    getApiClient(), mMatch!!.matchId, mMatchData, winnerResult, loserResult
            )
                    .setResultCallback { updateMatchResult ->
                        processResultFinishMatch(updateMatchResult)
                    }
        }
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

    private fun showErrorDialog(errorCode: Int) {
        GoogleApiAvailability.getInstance()
                .getErrorDialog(this, errorCode, RC_RESOLVE_ERROR)
                .show()
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
                    showSpinner()
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

    private fun updateMatchState() {
        if (mBoard.hasMove(opponentColor)) { // If opponent can make a move, it's his turn
            val pId = if (mOpponent == null) null else mOpponent!!.participantId
            Games.TurnBasedMultiplayer.takeTurn(getApiClient(), mMatch!!.matchId, mMatchData, pId)
                    .setResultCallback { updateMatchResult -> processResult(updateMatchResult) }
        } else if (mBoard.hasMove(currentPlayerColor)) { // Opponent has no move, keep turn
            val msg = getString(R.string.no_moves, mOpponent!!.displayName)
            toast(msg)
            Games.TurnBasedMultiplayer.takeTurn(
                    getApiClient(), mMatch!!.matchId, mMatchData,
                    mPlayer!!.participantId
            )
                    .setResultCallback { updateMatchResult -> processResult(updateMatchResult) }
        } else { // No moves remaining, end of match
            endMatch()
            return
        }
        updateScore()
    }

    private val playerScore: Int
        get() = if (mPlayer === mLightPlayer) mLightScore else mDarkScore


    private fun updateScore() {
        mLightScore = mBoard.getNumSpacesForColor(ReversiColor.LIGHT)
        mDarkScore = mBoard.getNumSpacesForColor(ReversiColor.DARK)

        if (mMatch!!.status == TurnBasedMatch.MATCH_STATUS_COMPLETE && !mUpdatingMatch) {
            // Add remaining spaces to winning count as per Reversi rules
            if (mLightPlayer!!.result.result == ParticipantResult.MATCH_RESULT_WIN) {
                mLightScore += mBoard.numberOfEmptySpaces
            } else if (mDarkPlayer!!.result.result == ParticipantResult.MATCH_RESULT_WIN) {
                mDarkScore += mBoard.numberOfEmptySpaces
            }
        }

        mMatchFragment.showScore(mLightScore, mDarkScore)
    }

    private fun registerMatchUpdateListener(shouldRegister: Boolean) {
        Games.TurnBasedMultiplayer.unregisterMatchUpdateListener(getApiClient())
        if (shouldRegister) {
            Games.TurnBasedMultiplayer.registerMatchUpdateListener(getApiClient(), this)
        }
    }

    private fun connectGoogleApiClient() {
        // Check if Google Play Services are available
        val result = GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(this)
        if (result != ConnectionResult.SUCCESS) {
            showErrorDialog(result)
        } else {
            showSpinner()
            getApiClient().connect()
        }
    }

    override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        mHelper.onActivityResult(request, result, data)

        when (request) {
        // Resolving error with Play Games
            RC_RESOLVE_ERROR -> handleError(result)

        // Returned from the 'Select Match' dialog
            RC_VIEW_MATCHES -> handleSelectMatchResult(result, data)

        // Returned from 'Select players to Invite' dialog
            RC_SELECT_PLAYERS -> handleSelectPlayersResult(result, data)

        // Returned from achievements screen
            RC_SHOW_ACHIEVEMENTS -> handleShowAchievementsResult(result)

        // Returned from settings screen
            RC_SETTINGS -> handleSettingsResult(result)
        }
    }

    private fun handleError(resultCode: Int) {
        if (resultCode == Activity.RESULT_OK) {
            if (!getApiClient().isConnecting && !getApiClient().isConnected) {
                connectGoogleApiClient()
            }
        }
    }

    private fun handleSelectMatchResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val match = data!!.getParcelableExtra<TurnBasedMatch>(Multiplayer.EXTRA_TURN_BASED_MATCH)
            if (match != null) {
                if (match.data == null) {
                    startMatch(match)
                } else {
                    updateMatch(match)
                }
            }
        } else if (resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
            // User signed out
            signOut()
        } else {
            showErrorDialog(resultCode)
        }
    }

    private fun handleSelectPlayersResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            val invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS)

            val minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0)
            val maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0)

            val autoMatchCriteria =
                    if (minAutoMatchPlayers > 0) {
                        RoomConfig.createAutoMatchCriteria(
                                minAutoMatchPlayers, maxAutoMatchPlayers, 0
                        )
                    } else {
                        mAchievementManager.unlock(Achievements.PLAY_WITH_FRIEND)
                        null
                    }

            val matchConfig = TurnBasedMatchConfig.builder()
                    .addInvitedPlayers(invitees)
                    .setAutoMatchCriteria(autoMatchCriteria)
                    .build()

            showSpinner()
            Games.TurnBasedMultiplayer.createMatch(getApiClient(), matchConfig)
                    .setResultCallback { processResult(it) }
        } else if (resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
            // User signed out
            signOut()
        } else {
            showErrorDialog(resultCode)
        }
    }

    private fun handleShowAchievementsResult(resultCode: Int) {
        if (resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
            // User signed out
            signOut()
        }
    }

    private fun handleSettingsResult(resultCode: Int) {
        when (resultCode) {
            SettingsActivity.RESULT_SIGN_OUT -> {
                mSignOutOnConnect = true

                // User signed out
                signOut()
            }
        }
    }

    private fun processResult(result: TurnBasedMultiplayer.InitiateMatchResult) {
        val match = result.match
        mMatchData = null

        if (checkStatusCode(result.status.statusCode)) {
            if (match.data == null) {
                startMatch(match)
            } else {
                updateMatch(match)
            }
        }
    }

    private fun startMatch(match: TurnBasedMatch) {
        mMatch = match
        mMatchData = null
        mBoard.reset()
        saveMatchData()
        mMatchFragment.displayBoard(mBoard)
        updateScore()

        val playerId = Games.Players.getCurrentPlayerId(getApiClient())
        val participantId = mMatch!!.getParticipantId(playerId)
        Games.TurnBasedMultiplayer.takeTurn(
                getApiClient(), match.matchId, mMatchData, participantId
        ).setResultCallback { processResult(it) }
    }

    private fun processResult(result: TurnBasedMultiplayer.UpdateMatchResult) {
        mMatch = result.match
        dismissSpinner()

        if (checkStatusCode(result.status.statusCode)) {
            updateMatch(mMatch!!)
        }

        mUpdatingMatch = false
    }

    private fun updateMatch(match: TurnBasedMatch) {
        mUpdatingMatch = true
        mMatch = match
        mPlayer = currentPlayer
        mOpponent = opponent
        mLightPlayer = lightPlayer
        mDarkPlayer = darkPlayer
        mMatchData = match.data

//        Timber.d("Match ID: " + mMatch!!.matchId)
//        Timber.d(bytesToString(mMatchData))
//        Timber.d("Match Status: " + mMatch!!.status)
//        Timber.d("Turn Status: " + mMatch!!.turnStatus)

        // Grab the appropriate segment from mMatchData based on player's color
        var startIndex = if (currentPlayer === lightPlayer) 0 else 100
        val playerData = Arrays.copyOfRange(mMatchData!!, startIndex, startIndex + 64)

        mBoard.restoreState(playerData)
        mMatchFragment.displayBoard(mBoard)
        dismissSpinner()

        // Commit opponent's moves to the deserialized Board object
        // 0 [Light's Board] 64 [Dark's Moves] 100 [Dark's Board] 164 [Light's Moves]
        startIndex += 64
        while (mMatchData!![startIndex].toInt() != 0) {
            val s = mBoard.getBoardSpaceFromNum(mMatchData!![startIndex++].toInt())
            mQueuedMoves.add(s!!)
        }

        mUpdatingMatch = false
        if (!mQueuedMoves.isEmpty()) {
            processReceivedTurns()
        }
        updateScore()

        // Check for inactive match states
        when (mMatch!!.status) {
            TurnBasedMatch.MATCH_STATUS_CANCELED -> {
                mMatchFragment.displayMessage(getString(R.string.match_canceled))
                return
            }
            TurnBasedMatch.MATCH_STATUS_EXPIRED -> {
                mMatchFragment.displayMessage(getString(R.string.match_expired))
                return
            }
            TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING -> {
                mMatchFragment.displayMessage(getString(R.string.match_finding_partner))
                return
            }
            TurnBasedMatch.MATCH_STATUS_COMPLETE -> {
                endMatch()
                return
            }
        }

        // OK, it's active. Check on turn status.
        when (mMatch!!.turnStatus) {
            TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN -> {
                mMatchFragment.dismissMessage()
//                autoplayIfEnabled()
            }
            TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN -> {
                mMatchFragment.displayMessage(getString(R.string.match_opponent_turn))
            }
            TurnBasedMatch.MATCH_TURN_STATUS_INVITED -> {
                mMatchFragment.displayMessage(getString(R.string.match_invite_pending))
            }
        }
    }

    private fun saveMatchData() {
        val playerBoard = mBoard.serialize()

        if (mMatchData == null) {
            mMatchData = ByteArray(256)
            System.arraycopy(playerBoard, 0, mMatchData!!, 0, playerBoard.size)
            System.arraycopy(playerBoard, 0, mMatchData!!, 100, playerBoard.size)
        } else {
            val startIndex = if (mPlayer === mLightPlayer) 0 else 100
            // Copy the serialized Board into the appropriate place in match data
            System.arraycopy(playerBoard, 0, mMatchData!!, startIndex, playerBoard.size)
            // Clear out the first 16 nodes following, which were the other player's previous moves
            for (clearIndex in startIndex + 64 until startIndex + 64 + 16)
                mMatchData!![clearIndex] = 0
        }
    }

    private fun processReceivedTurns() {
        mUpdatingMatch = true

        delay(CPU_TURN_DELAY_MS) {
            mBoard.commitPiece(mQueuedMoves.removeAt(0), opponentColor)

            // If there are not moves in the pending queue, update the score and save match data
            if (mQueuedMoves.isEmpty()) {
                mUpdatingMatch = false
                updateScore()
                saveMatchData()
//                autoplayIfEnabled()
            }
            // Otherwise, make a recursive call to this function to process them
            else processReceivedTurns()
        }
    }

    override fun onTurnBasedMatchReceived(match: TurnBasedMatch) {
        if (mMatch != null && mMatch!!.matchId == match.matchId) {
            if (match.turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN) {
                // Turn still belongs to opponent, wait for another update
                return
            }
            updateMatch(match)
        }
    }

    override fun onTurnBasedMatchRemoved(matchId: String) {
        if (mMatch != null && mMatch!!.matchId == matchId) {
            toast(R.string.match_removed)
            clearBoard()
        }
    }

    // For testing full end-to-end multiplayer flow
    private fun autoplayIfEnabled() {
        if (!mUpdatingMatch && AUTOMATED_MULTIPLAYER
                && mMatch!!.status == TurnBasedMatch.MATCH_STATUS_ACTIVE
                && mMatch!!.turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
            delay(500) {
                val color = if (mPlayer === mLightPlayer) ReversiColor.LIGHT else ReversiColor.DARK
                val bestMove = ComputerAI.getBestMove_d3(mBoard, color)
                if (bestMove != null) {
                    onSpaceClick(bestMove.y, bestMove.x)
                }
            }
        }
    }
}
