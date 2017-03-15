package com.ddiehl.android.reversi.game

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.support.annotation.DrawableRes
import android.support.annotation.LayoutRes
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils.loadAnimation
import android.widget.*
import butterknife.bindView
import com.ddiehl.android.reversi.*
import com.ddiehl.android.reversi.model.Board
import com.ddiehl.android.reversi.model.ReversiColor
import com.ddiehl.android.reversi.model.ReversiPlayer
import com.ddiehl.android.reversi.settings.SettingsActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.games.Games
import com.google.android.gms.games.GamesActivityResultCodes
import com.google.android.gms.games.multiplayer.Multiplayer
import com.google.android.gms.games.multiplayer.Participant
import com.google.android.gms.games.multiplayer.ParticipantResult
import com.google.android.gms.games.multiplayer.realtime.RoomConfig
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchUpdateReceivedListener
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer
import com.jakewharton.rxbinding.view.RxView
import java.util.*

class MatchFragment : FrameLayout, OnTurnBasedMatchUpdateReceivedListener {

    companion object {
        private @LayoutRes val LAYOUT_RES_ID = R.layout.match_fragment

        // Delay between animations for the waiting message
        val WAITING_MESSAGE_FADE_DELAY_MS = 2000L

        private val ARG_MULTI_PLAYER = "ARG_MULTI_PLAYER"
        private val PREF_AUTO_SIGN_IN = "PREF_AUTO_SIGN_IN"
    }

    internal val mMatchGridView by bindView<TableLayout>(R.id.match_grid)
    internal val mPlayerOneScore by bindView<TextView>(R.id.score_p1)
    internal val mPlayerTwoScore by bindView<TextView>(R.id.score_p2)
    internal val mPlayerOneLabel by bindView<TextView>(R.id.p1_label)
    internal val mPlayerTwoLabel by bindView<TextView>(R.id.p2_label)
    internal val mBoardPanelView by bindView<View>(R.id.board_panels)
    internal val mStartNewMatchButton by bindView<Button>(R.id.board_panel_new_game)
    internal val mSelectMatchButton by bindView<Button>(R.id.board_panel_select_game)
    internal val mMatchMessageView by bindView<View>(R.id.match_message)
    internal val mMatchMessageTextView by bindView<TextView>(R.id.match_message_text)
    internal val mMatchMessageIcon1 by bindView<ImageView>(R.id.match_message_icon_1)
    internal val mMatchMessageIcon2 by bindView<ImageView>(R.id.match_message_icon_2)

    private var mDisplayedDialog: Dialog? = null

    private val mLeftFadeOut: Animation by lazy { loadAnimation(context, R.anim.waitingmessage_fadeout) }
    private val mLeftFadeIn: Animation by lazy { loadAnimation(context, R.anim.waitingmessage_fadein) }
    private val mRightFadeOut: Animation by lazy { loadAnimation(context, R.anim.waitingmessage_fadeout) }
    private val mRightFadeIn: Animation by lazy { loadAnimation(context, R.anim.waitingmessage_fadein) }
    private var mMatchMessageIcon1Color = false
    private var mMatchMessageIcon2Color = true

    // Activity implements this
    private var mMatchView: MatchView

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr) {
        init(context, attrs, defStyleAttr)
        mMatchView = context as MatchView
    }

    private fun init(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) {
        LayoutInflater.from(context)
                .inflate(LAYOUT_RES_ID, parent as ViewGroup, true)

        mStartNewMatchButton.setOnClickListener { mMatchView.onStartNewMatchClicked() }
        mSelectMatchButton.setOnClickListener { mMatchView.onSelectMatchClicked() }

        mPlayerOneScore.text = 0.toString()
        mPlayerTwoScore.text = 0.toString()

        initMatchGrid(mMatchGridView)
        mMatchGridView.visibility = View.GONE

        initializeWaitingAnimations()
    }


    //region Public API

    fun updateBoardUi(board: Board, animate: Boolean = false) {
        for (i in 0..mMatchGridView.childCount - 1) {
            val row = mMatchGridView.getChildAt(i) as ViewGroup
            for (j in 0..row.childCount - 1) {
                val space = row.getChildAt(j)
                updateSpace(space, board, i, j, animate)
            }
        }
    }

    fun showMatchButtons(newGame: Boolean, selectGame: Boolean) {
        mSelectMatchButton.visibility = if (newGame) View.VISIBLE else View.GONE
        mSelectMatchButton.visibility = if (selectGame) View.VISIBLE else View.GONE
    }

    //endregion


    private fun initMatchGrid(grid: ViewGroup) {
        for (i in 0..grid.childCount - 1) {
            val row = grid.getChildAt(i) as ViewGroup
            for (j in 0..row.childCount - 1) {
                val space = row.getChildAt(j)

                RxView.clicks(space)
                        .subscribe({ mMatchView.handleSpaceClick(i, j) })
            }
        }
    }

    private fun updateSpace(view: View, board: Board, row: Int, col: Int, animate: Boolean) {
        val space = board.spaceAt(row, col)
        if (space.color == null) {
            if (view.tag == null || view.tag as Int != 0) {
                view.tag = 0
                if (animate) {
                    animateBackgroundChange(view, R.drawable.board_space_neutral)
                } else {
                    view.setBackgroundResource(R.drawable.board_space_neutral)
                }
            }
        } else {
            when (space.color) {
                ReversiColor.LIGHT ->
                    if (view.tag == null || view.tag as Int != 1) {
                        view.tag = 1
                        if (animate) {
                            animateBackgroundChange(view, R.drawable.board_space_p1)
                        } else {
                            view.setBackgroundResource(R.drawable.board_space_p1)
                        }
                    }
                ReversiColor.DARK ->
                    if (view.tag == null || view.tag as Int != 2) {
                        view.tag = 2
                        if (animate) {
                            animateBackgroundChange(view, R.drawable.board_space_p2)
                        } else {
                            view.setBackgroundResource(R.drawable.board_space_p2)
                        }
                    }
            }
        }
    }

    private fun animateBackgroundChange(view: View, @DrawableRes resId: Int) {
        val fadeOut = loadAnimation(context, R.anim.playermove_fadeout)
        val fadeIn = loadAnimation(context, R.anim.playermove_fadein)

        fadeOut.setListener(onEnd = {
            view.setBackgroundResource(resId)
            view.startAnimation(fadeIn)
        })

        view.startAnimation(fadeOut)
    }

    private fun switchFirstTurn() {
        if (mPlayerWithFirstTurn == null) {
            mPlayerWithFirstTurn = mP1
        } else {
            mPlayerWithFirstTurn = if (mPlayerWithFirstTurn === mP1) mP2 else mP1
        }
        mCurrentPlayer = mPlayerWithFirstTurn
    }

    fun updateScoreForPlayer(p: ReversiPlayer) {
        (if (p === mP1) mPlayerOneScore else mPlayerTwoScore).text = p.score.toString()
    }

    fun endMatch() {
        singlePlayer {
            val winner: ReversiPlayer
            if (mP1.score != mP2.score) {
                winner = if (mP1.score > mP2.score) mP1 else mP2
                showWinningToast(winner)
                val diff = 64 - mP1.score - mP2.score
                winner.score = winner.score + diff
                updateScoreForPlayer(winner)
            } else {
                showWinningToast(null)
            }
            switchFirstTurn()
            m1PSavedState.clear()
            mMatchInProgress = false
        }

        multiPlayer {
            updateScore()
            if (mMatch!!.status == TurnBasedMatch.MATCH_STATUS_COMPLETE) {
                when (mPlayer!!.result.result) {
                    ParticipantResult.MATCH_RESULT_WIN -> {
                        displayMessage(getString(R.string.winner_you))
                    }
                    ParticipantResult.MATCH_RESULT_TIE -> {
                        displayMessage(getString(R.string.winner_tie))
                    }
                    ParticipantResult.MATCH_RESULT_LOSS -> {
                        val msg = if (mPlayer === mLightPlayer) R.string.winner_dark else R.string.winner_light
                        displayMessage(getString(msg))
                    }
                    else -> {
                        displayMessage(getString(R.string.match_complete))
                    }
                }

                if (mMatch!!.turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
                    // Call finishMatch() to close out match for player
                    Games.TurnBasedMultiplayer.finishMatch(mGoogleApiClient, mMatch!!.matchId)
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
                        displayMessage(getString(msg))
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
                        displayMessage(getString(msg))
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
                    displayMessage(getString(R.string.winner_tie))
                }

                // Call finishMatch() with result parameters
                Games.TurnBasedMultiplayer.finishMatch(
                        mGoogleApiClient, mMatch!!.matchId, mMatchData,
                        winnerResult, loserResult
                )
                        .setResultCallback { updateMatchResult ->
                            processResultFinishMatch(updateMatchResult)
                        }
            }
        }
    }

    fun displayBoard() {
        mBoardPanelView.visibility = View.GONE
        mMatchGridView.visibility = View.VISIBLE
        updateBoardUi()
    }

    fun showWinningToast(winner: ReversiPlayer?) {
        val text =
                if (winner == null) {
                    context.getString(R.string.winner_none)
                } else if (winner === mP1) {
                    context.getString(R.string.winner_p1)
                } else {
                    context.getString(R.string.winner_cpu)
                }
        toast(text, Toast.LENGTH_LONG)
    }


    //region Options menu


    //endregion


    //region Multi Player fragment

    private fun connectGoogleApiClient() {
        multiPlayer {
            // Check if Google Play Services are available
            val result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity)
            if (result != ConnectionResult.SUCCESS) {
                showErrorDialog(result)
            } else {
                mMatchView.showSpinner()
                mGoogleApiClient.connect()
            }
        }
    }

    private fun registerMatchUpdateListener(shouldRegister: Boolean) {
        Games.TurnBasedMultiplayer.unregisterMatchUpdateListener(mGoogleApiClient)
        if (shouldRegister) {
            Games.TurnBasedMultiplayer.registerMatchUpdateListener(mGoogleApiClient, activity as OnTurnBasedMatchUpdateReceivedListener)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
        // Resolving error with Play Games
            RC_RESOLVE_ERROR -> handleError(resultCode)

        // Returned from the 'Select Match' dialog
            RC_VIEW_MATCHES -> handleSelectMatchResult(resultCode, data)

        // Returned from 'Select players to Invite' dialog
            RC_SELECT_PLAYERS -> handleSelectPlayersResult(resultCode, data)

        // Returned from achievements screen
            RC_SHOW_ACHIEVEMENTS -> handleShowAchievementsResult(resultCode)

        // Returned from settings screen
            RC_SETTINGS -> handleSettingsResult(resultCode)
        }
    }

    private fun handleError(resultCode: Int) {
        mResolvingError = false
        if (resultCode == Activity.RESULT_OK) {
            if (!mGoogleApiClient.isConnecting && !mGoogleApiClient.isConnected) {
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
            mIsSignedIn = false
            signOutFromGooglePlay()
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

            mMatchView?.showSpinner()
            Games.TurnBasedMultiplayer.createMatch(mGoogleApiClient, matchConfig)
                    .setResultCallback { processResult(it) }
        } else if (resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
            // User signed out
            mIsSignedIn = false
            signOutFromGooglePlay()
        } else {
            showErrorDialog(resultCode)
        }
    }

    private fun handleShowAchievementsResult(resultCode: Int) {
        if (resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
            // User signed out
            mIsSignedIn = false
            signOutFromGooglePlay()
        }
    }

    private fun handleSettingsResult(resultCode: Int) {
        when (resultCode) {
            SettingsActivity.RESULT_SIGN_IN -> connectGoogleApiClient()
            SettingsActivity.RESULT_SIGN_OUT -> mSignOutOnConnect = true
        }
    }

    private fun signOutFromGooglePlay() {
        toast(R.string.sign_out_confirmation)

        mSignOutOnConnect = false
        autoConnectPreference = false
        if (mGoogleApiClient.isConnected && mIsSignedIn) {
            Games.signOut(mGoogleApiClient)
        }
        mIsSignedIn = false
        mGoogleApiClient.disconnect()

        activity.setResult(SettingsActivity.RESULT_SIGN_OUT)
        activity.finish()
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
        displayBoard()
        updateScore()

        val participantId = mMatch!!.getParticipantId(Games.Players.getCurrentPlayerId(mGoogleApiClient))
        Games.TurnBasedMultiplayer.takeTurn(
                mGoogleApiClient, match.matchId, mMatchData, participantId
        ).setResultCallback { processResult(it) }
    }

    private fun processResult(result: TurnBasedMultiplayer.UpdateMatchResult) {
        mMatch = result.match
        mMatchView?.dismissSpinner()

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
        displayBoard()
        mMatchView?.dismissSpinner()

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
                displayMessage(getString(R.string.match_canceled))
                return
            }
            TurnBasedMatch.MATCH_STATUS_EXPIRED -> {
                displayMessage(getString(R.string.match_expired))
                return
            }
            TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING -> {
                displayMessage(getString(R.string.match_finding_partner))
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
                dismissMessage()
//                autoplayIfEnabled()
                return
            }
            TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN -> {
                displayMessage(getString(R.string.match_opponent_turn))
                return
            }
            TurnBasedMatch.MATCH_TURN_STATUS_INVITED -> displayMessage(getString(R.string.match_invite_pending))
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
            for (clearIndex in startIndex + 64..startIndex + 64 + 16 - 1)
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

    private fun updateMatchState() {
        if (mBoard.hasMove(opponentColor)) { // If opponent can make a move, it's his turn
            val pId = if (mOpponent == null) null else mOpponent!!.participantId
            Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, mMatch!!.matchId, mMatchData, pId)
                    .setResultCallback { updateMatchResult -> processResult(updateMatchResult) }
        } else if (mBoard.hasMove(currentPlayerColor)) { // Opponent has no move, keep turn
            val msg = getString(R.string.no_moves, mOpponent!!.displayName)
            toast(msg)
            Games.TurnBasedMultiplayer.takeTurn(
                    mGoogleApiClient, mMatch!!.matchId, mMatchData,
                    mPlayer!!.participantId
            )
                    .setResultCallback { updateMatchResult -> processResult(updateMatchResult) }
        } else { // No moves remaining, end of match
            endMatch()
            return
        }
        updateScore()
    }

    private val currentPlayer: Participant
        get() {
            val currentPlayerId = Games.Players.getCurrentPlayerId(mGoogleApiClient)
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

    fun clearBoard() {
        mPlayerTwoScore.text = ""
        mBoardPanelView.visibility = View.VISIBLE
    }

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

        mPlayerOneScore.text = mLightScore.toString()
        mPlayerTwoScore.text = mDarkScore.toString()
    }

    private fun displayMessage(matchMsg: String) {
        mMatchMessageTextView.text = matchMsg
        mMatchMessageView.visibility = View.VISIBLE

        // Start animations for side icons
        if (!mLeftFadeOut.hasStarted() && !mRightFadeOut.hasStarted()) {
            mMatchMessageIcon1.startAnimation(mLeftFadeOut)
            mMatchMessageIcon2.startAnimation(mRightFadeOut)
        }
    }

    fun dismissMessage() {
        mMatchMessageView.visibility = View.INVISIBLE
        mMatchMessageTextView.text = ""
        mLeftFadeOut.cancel()
        mRightFadeOut.cancel()
        mLeftFadeIn.cancel()
        mRightFadeIn.cancel()
    }

    private fun initializeWaitingAnimations() {
        val icon1 = mMatchMessageIcon1
        val icon2 = mMatchMessageIcon2

        icon1.setBackgroundResource(R.drawable.player_icon_p1)
        icon2.setBackgroundResource(R.drawable.player_icon_p2)

        mRightFadeOut.setListener(onEnd = {
            // Flip background resources & start animation
            icon2.setBackgroundResource(
                    if (mMatchMessageIcon2Color) R.drawable.player_icon_p1
                    else R.drawable.player_icon_p2
            )
            mMatchMessageIcon2Color = !mMatchMessageIcon2Color
            icon2.startAnimation(mRightFadeIn)
        })

        mLeftFadeOut.setListener(onEnd = {
            // Flip background resources & start animation
            icon1.setBackgroundResource(
                    if (mMatchMessageIcon1Color) R.drawable.player_icon_p1
                    else R.drawable.player_icon_p2
            )
            mMatchMessageIcon1Color = !mMatchMessageIcon1Color
            icon1.startAnimation(mLeftFadeIn)
        })

        mLeftFadeIn.setListener(onEnd = {
            delay(WAITING_MESSAGE_FADE_DELAY_MS) {
                icon1.startAnimation(mLeftFadeOut)
                icon2.startAnimation(mRightFadeOut)
            }
        })
    }

    //endregion
}
