package com.ddiehl.android.reversi.game

import android.app.Activity
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import com.ddiehl.android.reversi.BuildConfig
import com.ddiehl.android.reversi.R
import com.ddiehl.android.reversi.model.Board
import com.ddiehl.android.reversi.settings.SettingsActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.games.Games
import com.google.android.gms.games.GamesActivityResultCodes
import com.google.android.gms.games.multiplayer.Multiplayer
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch
import com.google.example.games.basegameutils.GameHelper

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

    private lateinit var mGameController: GameController
    private val mHelper: GameHelper by lazy {
        GameHelper(this, GameHelper.CLIENT_GAMES)
                .apply { if (BuildConfig.DEBUG) enableDebugLog(true) }
    }

    private var mSignOutOnConnect = false

    //region BaseGameActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LAYOUT_RES_ID)

        setSupportActionBar(mToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        mGameController = GameController(this)
        mHelper.setup(this)
        mHelper.setShowErrorDialogs(true)

        mMatchFragment.showScore(false)
    }

    override fun onStart() {
        super.onStart()
        mHelper.onStart(this)
    }

    override fun onStop() {
        if (getApiClient().isConnected) {
            mGameController.registerMatchUpdateListener(false) // FIXME: Why isn't this called elsewhere?
            getApiClient().disconnect()
        }

        mHelper.onStop()
        super.onStop()
    }

    override fun getGameHelper(): GameHelper = mHelper

    override fun clearBoard() {
        mMatchFragment.clearBoard()
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

    private fun getInvitationId(): String {
        return mHelper.invitationId
    }

    //endregion

    //region GameHelper.Listener

    override fun onSignInSucceeded() {
        dismissSpinner()

        if (mSignOutOnConnect) {
            mSignOutOnConnect = false
            mMatchFragment.clearBoard()
            signOut()
        }
    }

    override fun onSignInFailed() {
        dismissSpinner()
    }

    //endregion

    override fun displaySignInPrompt() {
        AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_sign_in_title))
                .setMessage(getString(R.string.dialog_sign_in_message))
                .setPositiveButton(getString(R.string.dialog_sign_in_confirm), onSignInConfirm())
                .setNegativeButton(getString(R.string.dialog_sign_in_cancel), { _, _ -> })
                .show()
    }

    fun onSignInConfirm() = DialogInterface.OnClickListener {
        _, _ -> beginUserInitiatedSignIn()
    }

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

    override fun showScore(show: Boolean) {
        mMatchFragment.showScore(show)
    }

    override fun showScore(light: Int, dark: Int) {
        mMatchFragment.showScore(light, dark)
    }

    override fun displayMessage(string: String) {
        mMatchFragment.displayMessage(string)
    }

    override fun displayMessage(resId: Int) {
        mMatchFragment.displayMessage(getString(resId))
    }

    override fun dismissMessage() {
        mMatchFragment.dismissMessage()
    }

    override fun onSpaceClick(row: Int, col: Int) {
        mGameController.onSpaceClick(row, col)
    }

    override fun displayBoard(board: Board) {
        mMatchFragment.displayBoard(board)
    }

    override fun toast(msg: String) {
        toast(msg)
    }

    override fun toast(resId: Int) {
        toast(getString(resId))
    }

    override fun toast(resId: Int, vararg args: Any) {
        toast(getString(resId, args))
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

    override fun onCloseMatchClicked() {
        mMatchFragment.clearBoard()
        mMatchFragment.showMatchButtons(true, true)
    }

    override fun onForfeitMatchClicked() {
        mGameController.forfeitMatch()
    }

    override fun onShowAchievementsClicked() {
        if (!getApiClient().isConnected) {
            displaySignInPrompt()
            return
        }

        val intent = Games.Achievements.getAchievementsIntent(getApiClient())
        startActivityForResult(intent, RC_SHOW_ACHIEVEMENTS)
    }

    override fun onSettingsClicked() {
        val settings = Intent(this, SettingsActivity::class.java)
        startActivityForResult(settings, RC_SETTINGS)
    }

    //endregion

    override fun showCancelMatchDialog() {
        AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_cancel_match_title))
                .setMessage(getString(R.string.dialog_cancel_match_message))
                .setPositiveButton(getString(R.string.dialog_cancel_match_confirm), onCancelMatchConfirm())
                .setNegativeButton(getString(R.string.dialog_cancel_match_cancel)) { _, _ -> }
                .setCancelable(true)
                .show()
    }

    private fun onCancelMatchConfirm() = DialogInterface.OnClickListener { _, _ ->
        mGameController.cancelMatch()
    }

    override fun showForfeitMatchDialog() {
        AlertDialog.Builder(this)
                .setTitle(R.string.dialog_forfeit_match_title)
                .setMessage(R.string.dialog_forfeit_match_message)
                .setPositiveButton(R.string.dialog_forfeit_match_confirm, onForfeitMatchConfirm())
                .setNegativeButton(R.string.dialog_forfeit_match_cancel) { _, _ -> }
                .setCancelable(true)
                .show()
    }

    private fun onForfeitMatchConfirm() = DialogInterface.OnClickListener { _, _ ->
        mGameController.doForfeitMatch()
    }

    override fun showForfeitMatchForbiddenAlert() {
        AlertDialog.Builder(this)
                .setTitle(R.string.dialog_forfeit_match_forbidden_title)
                .setMessage(R.string.dialog_forfeit_match_forbidden_message)
                .setPositiveButton(R.string.dialog_forfeit_match_forbidden_confirm) { _, _ -> }
                .setCancelable(true)
                .show()
    }

    override fun showLeaveMatchDialog() {
        AlertDialog.Builder(this)
                .setTitle(R.string.dialog_leave_match_title)
                .setMessage(R.string.dialog_leave_match_message)
                .setPositiveButton(R.string.dialog_leave_match_confirm, onLeaveMatchConfirm())
                .setNegativeButton(R.string.dialog_leave_match_cancel, { _, _ -> })
                .setCancelable(true)
                .show()
    }

    private fun onLeaveMatchConfirm() = DialogInterface.OnClickListener { _, _ ->
        mGameController.leaveMatch()
    }

    /* Generic warning/info dialog */
    override fun showAlertDialog(errorTitle: Int, errorMessage: Int) {
        AlertDialog.Builder(this)
                .setTitle(getString(errorTitle))
                .setMessage(getString(errorMessage))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.dialog_error_confirm)) { _, _ -> }
                .show()
    }

    private fun showErrorDialog(errorCode: Int) {
        GoogleApiAvailability.getInstance()
                .getErrorDialog(this, errorCode, RC_RESOLVE_ERROR)
                ?.show()
    }

    override fun askForRematch() {
        AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_rematch_title))
                .setMessage(getString(R.string.dialog_rematch_message))
                .setPositiveButton(getString(R.string.dialog_rematch_confirm), onRematchConfirm())
                .setNegativeButton(getString(R.string.dialog_rematch_cancel), onRematchCancel())
                .setIcon(ContextCompat.getDrawable(this, R.drawable.ic_av_replay_blue))
                .create()
    }

    private fun onRematchConfirm() =
            DialogInterface.OnClickListener { _, _ -> mGameController.rematch() }

    private fun onRematchCancel() = DialogInterface.OnClickListener { _, _ -> }

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

        // Returned from 'Start new Match' dialog
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
        when (resultCode) {
            Activity.RESULT_OK -> {
                val match = data?.getParcelableExtra<TurnBasedMatch>(Multiplayer.EXTRA_TURN_BASED_MATCH)
                if (match != null) {
                    mGameController.selectMatch(match)
                }
            }
            GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED -> {
                // User signed out
                signOut()
            }
            else -> showErrorDialog(resultCode)
        }
    }

    private fun handleSelectPlayersResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            mGameController.initiateMatch(data)
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
                // User signed out
                mSignOutOnConnect = true
            }
        }
    }
}
