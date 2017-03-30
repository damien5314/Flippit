package com.ddiehl.android.reversi.game

import android.content.Intent
import com.ddiehl.android.reversi.AUTOMATED_MULTIPLAYER
import com.ddiehl.android.reversi.CPU_TURN_DELAY_MS
import com.ddiehl.android.reversi.R
import com.ddiehl.android.reversi.delay
import com.ddiehl.android.reversi.model.*
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.games.Games
import com.google.android.gms.games.GamesStatusCodes
import com.google.android.gms.games.multiplayer.Multiplayer
import com.google.android.gms.games.multiplayer.Participant
import com.google.android.gms.games.multiplayer.ParticipantResult
import com.google.android.gms.games.multiplayer.realtime.RoomConfig
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchUpdateReceivedListener
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer
import timber.log.Timber
import java.util.*

class GameController(view: MatchView) : OnTurnBasedMatchUpdateReceivedListener {

    companion object {
        private val LIGHT_START_INDEX = 0
        private val DARK_START_INDEX = 100
    }

    private val mBoard: Board = Board(8, 8)
    private var mMatch: TurnBasedMatch? = null
    private var mPlayer: ReversiPlayer? = null
    private var mOpponent: ReversiPlayer? = null
    private var mLightPlayer: ReversiPlayer? = null
    private var mDarkPlayer: ReversiPlayer? = null
    private val mMatchView: MatchView = view
    private val mAchievementManager: AchievementManager by lazy {
        AchievementManager.get(getApiClient())
    }
    private var mUpdatingMatch = false
    private val mQueuedMoves: MutableList<BoardSpace> = ArrayList()

    private fun getApiClient(): GoogleApiClient = mMatchView.getGameHelper().apiClient

    private fun getCurrentPlayer(match: TurnBasedMatch): ReversiPlayer {
        val currentPlayerId: String? = Games.Players.getCurrentPlayerId(getApiClient())
        val participantId: String? = match.getParticipantId(currentPlayerId)

        val playerIsLight = match.getParticipant(participantId) === mLightPlayer!!.gpg
        return if (playerIsLight) mLightPlayer!! else mDarkPlayer!!
    }

    private fun getOpponent(match: TurnBasedMatch): ReversiPlayer {
        val opponent: Participant? = match.descriptionParticipant
        val opponentIsLight = mLightPlayer!!.gpg === opponent
        return if (opponentIsLight) mLightPlayer!! else mDarkPlayer!!
    }

    private fun getCurrentPlayerColor(): ReversiColor {
        if (mPlayer === mLightPlayer) return ReversiColor.LIGHT
        else return ReversiColor.DARK
    }

    private fun getOpponentColor(): ReversiColor {
        if (mOpponent === mLightPlayer) return ReversiColor.LIGHT
        else return ReversiColor.DARK
    }

    private fun getLightPlayer(match: TurnBasedMatch): Participant {
        return match.getParticipant(match.creatorId)
    }

    private fun getDarkPlayer(match: TurnBasedMatch): Participant? {
        val participantIds = match.participantIds
        val lightId = match.creatorId

        val darkId: String? = participantIds.lastOrNull { it != lightId }
        if (darkId == null) return null
        else return match.getParticipant(darkId)
    }

    private fun getScore(color: ReversiColor): Int = mBoard.count { it.color == color }

    private fun signOut() {
        mMatchView.getGameHelper().signOut()
    }

    fun onSpaceClick(row: Int, col: Int) {

        Timber.d("Piece clicked @ $row $col")
        if (mUpdatingMatch || !mQueuedMoves.isEmpty()) {
            Timber.d("Error: Still evaluating last move")
            return
        }

        if (mMatch!!.status != TurnBasedMatch.MATCH_STATUS_ACTIVE) {
            Timber.d("Error: Match is not active")
            return
        }

        if (mMatch!!.turnStatus != TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
            Timber.d("Error: Not player's turn")
            return
        }

        val space = mBoard.spaceAt(row, col)

        if (space.isOwned) {
            Timber.d("Error: Space is owned")
            return
        }

        if (!getApiClient().isConnected) {
            mMatchView.displaySignInPrompt()
            return
        }

        val playerColor = getCurrentPlayerColor()

        if (mBoard.spacesCapturedWithMove(space, playerColor) == 0) {
            Timber.d("Error: Invalid move")
            return
        }

        mUpdatingMatch = true
        mMatchView.showSpinner()
        mBoard.commitPiece(space, playerColor)
        val data = saveMatchData()

        // Add selected piece to the end of mMatchData array
        // 0 [Light's Board] 64 [Dark's Moves] 100 [Dark's Board] 164 [Light's Moves]
        var nextIndex = if (mPlayer == mLightPlayer) 164 else 64
        // Increase index til we run into an unfilled index
        while (data[nextIndex].toInt() != 0) {
            nextIndex += 1
        }
        data[nextIndex] = mBoard.getSpaceNumber(space)

        updateMatchState(mMatch!!, data)
    }

    private fun updateMatchState(match: TurnBasedMatch, data: ByteArray) {
        // If opponent can make a move, it's his turn
        if (mBoard.hasMove(mOpponent!!.color)) {
            val playerId = if (mOpponent == null) null else mOpponent!!.gpg?.participantId
            Games.TurnBasedMultiplayer.takeTurn(getApiClient(), match.matchId, data, playerId)
                    .setResultCallback { updateMatchResult -> processResult(updateMatchResult) }
        }
        // Opponent has no move, keep turn
        else if (mBoard.hasMove(mPlayer!!.color)) {
            mMatchView.toast(R.string.no_moves, mOpponent!!.gpg!!.displayName)
            Games.TurnBasedMultiplayer.takeTurn(
                    getApiClient(), match.matchId, data, mPlayer!!.gpg!!.participantId
            ).setResultCallback { updateMatchResult -> processResult(updateMatchResult) }
        }
        // No moves remaining, end of match
        else {
            endMatch()
            return
        }
        updateScore(match)
    }

    private fun updateMatch(match: TurnBasedMatch) {
        mUpdatingMatch = true

        val lightParticipant = getLightPlayer(match)
        val darkParticipant = getDarkPlayer(match)
        mLightPlayer = ReversiPlayer(ReversiColor.LIGHT, lightParticipant.displayName, lightParticipant)
        mDarkPlayer = ReversiPlayer(ReversiColor.LIGHT, darkParticipant?.displayName ?: "", darkParticipant)

        mPlayer = getCurrentPlayer(match)
        mOpponent = getOpponent(match)
        val matchData = match.data

        // Grab the appropriate segment from mMatchData based on player's color
        var startIndex = if (getCurrentPlayer(match) === mLightPlayer) 0 else 100
        val playerData = Arrays.copyOfRange(matchData, startIndex, startIndex + 64)

        mBoard.restoreState(playerData)
        mMatchView.showScore(true)
        mMatchView.displayBoard(mBoard)
        mMatchView.dismissSpinner()

        // Commit opponent's moves to the deserialized Board object
        // 0 [Light's Board] 64 [Dark's Moves] 100 [Dark's Board] 164 [Light's Moves]
        startIndex += 64
        while (matchData[startIndex].toInt() != 0) {
            val space = mBoard.getBoardSpaceFromNum(matchData[startIndex++].toInt())
            mQueuedMoves.add(space)
        }

        mUpdatingMatch = false
        if (!mQueuedMoves.isEmpty()) {
            processReceivedTurns(match)
        }
        updateScore(match)

        // Check for inactive match states
        when (match.status) {
            TurnBasedMatch.MATCH_STATUS_CANCELED -> {
                mMatchView.displayMessage(R.string.match_canceled)
                return
            }
            TurnBasedMatch.MATCH_STATUS_EXPIRED -> {
                mMatchView.displayMessage(R.string.match_expired)
                return
            }
            TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING -> {
                mMatchView.displayMessage(R.string.match_finding_partner)
                return
            }
            TurnBasedMatch.MATCH_STATUS_COMPLETE -> {
                endMatch()
                return
            }
        }

        // OK, it's active. Check on turn status.
        when (match.turnStatus) {
            TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN -> {
                mMatchView.dismissMessage()
//                autoplayIfEnabled()
            }
            TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN -> {
                mMatchView.displayMessage(R.string.match_opponent_turn)
            }
            TurnBasedMatch.MATCH_TURN_STATUS_INVITED -> {
                mMatchView.displayMessage(R.string.match_invite_pending)
            }
        }

        mMatch = match
    }

    private fun updateScore(match: TurnBasedMatch) {
        var light = getScore(ReversiColor.LIGHT)
        var dark = getScore(ReversiColor.DARK)

        if (match.status == TurnBasedMatch.MATCH_STATUS_COMPLETE && !mUpdatingMatch) {
            // Add remaining spaces to winning count as per Reversi rules
            if (mLightPlayer!!.gpg!!.result.result == ParticipantResult.MATCH_RESULT_WIN) {
                light += mBoard.numberOfEmptySpaces
            } else if (mDarkPlayer!!.gpg!!.result.result == ParticipantResult.MATCH_RESULT_WIN) {
                dark += mBoard.numberOfEmptySpaces
            }
        }

        mMatchView.showScore(light, dark)
    }

    private fun processResult(result: TurnBasedMultiplayer.InitiateMatchResult) {
        val match = result.match

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
        mBoard.reset()
        mMatchView.showScore(true)
        mMatchView.displayBoard(mBoard)
        updateScore(match)

        val playerId = Games.Players.getCurrentPlayerId(getApiClient())
        val participantId = mMatch!!.getParticipantId(playerId)
        Games.TurnBasedMultiplayer.takeTurn(
                getApiClient(), match.matchId, saveMatchData(), participantId
        ).setResultCallback { processResult(it) }
    }

    private fun processResult(result: TurnBasedMultiplayer.UpdateMatchResult) {
        val match = result.match
        mMatchView.dismissSpinner()

        if (checkStatusCode(result.status.statusCode)) {
            updateMatch(match)
        }

        mMatch = match
        mUpdatingMatch = false
    }

    private fun processReceivedTurns(match: TurnBasedMatch) {
        mUpdatingMatch = true

        delay(CPU_TURN_DELAY_MS) {
            mBoard.commitPiece(mQueuedMoves.removeAt(0), mOpponent!!.color)

            // If there are not moves in the pending queue, update the score and save match data
            if (mQueuedMoves.isEmpty()) {
                mUpdatingMatch = false
                updateScore(match)
                saveMatchData()
//                autoplayIfEnabled()
            }
            // Otherwise, make a recursive call to this function to process them
            else processReceivedTurns(match)
        }
    }

    fun endMatch() {
        updateScore(mMatch!!)

        if (mMatch!!.status == TurnBasedMatch.MATCH_STATUS_COMPLETE) {
            when (mPlayer!!.gpg!!.result.result) {
                ParticipantResult.MATCH_RESULT_WIN -> {
                    mMatchView.displayMessage(R.string.winner_you)
                }
                ParticipantResult.MATCH_RESULT_TIE -> {
                    mMatchView.displayMessage(R.string.winner_tie)
                }
                ParticipantResult.MATCH_RESULT_LOSS -> {
                    val msg = if (mPlayer === mLightPlayer) R.string.winner_dark else R.string.winner_light
                    mMatchView.displayMessage(msg)
                }
                else -> {
                    mMatchView.displayMessage(R.string.match_complete)
                }
            }

            if (mMatch!!.turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
                // Call finishMatch() to close out mMatch!! for player
                Games.TurnBasedMultiplayer.finishMatch(getApiClient(), mMatch!!.matchId)
                        .setResultCallback {
                            updateMatchResult -> processResultFinishMatch(updateMatchResult)
                        }
            }
        } else { // Match is not yet finished
            val winnerResult: ParticipantResult
            val loserResult: ParticipantResult
            if (getScore(ReversiColor.LIGHT) != getScore(ReversiColor.DARK)) {
                if (getScore(ReversiColor.LIGHT) > getScore(ReversiColor.DARK)) {
                    winnerResult = ParticipantResult(
                            mLightPlayer!!.gpg!!.participantId,
                            ParticipantResult.MATCH_RESULT_WIN,
                            ParticipantResult.PLACING_UNINITIALIZED
                    )
                    loserResult = ParticipantResult(
                            mDarkPlayer!!.gpg!!.participantId,
                            ParticipantResult.MATCH_RESULT_LOSS,
                            ParticipantResult.PLACING_UNINITIALIZED
                    )
                    val msg = if (mPlayer === mLightPlayer) R.string.winner_you else R.string.winner_light
                    mMatchView.displayMessage(msg)
                } else {
                    winnerResult = ParticipantResult(
                            mDarkPlayer!!.gpg!!.participantId,
                            ParticipantResult.MATCH_RESULT_WIN,
                            ParticipantResult.PLACING_UNINITIALIZED
                    )
                    loserResult = ParticipantResult(
                            mLightPlayer!!.gpg!!.participantId,
                            ParticipantResult.MATCH_RESULT_LOSS,
                            ParticipantResult.PLACING_UNINITIALIZED
                    )
                    val msg = if (mPlayer === mDarkPlayer) R.string.winner_you else R.string.winner_dark
                    mMatchView.displayMessage(msg)
                }
            } else {
                winnerResult = ParticipantResult(
                        mDarkPlayer!!.gpg!!.participantId,
                        ParticipantResult.MATCH_RESULT_TIE,
                        ParticipantResult.PLACING_UNINITIALIZED
                )
                loserResult = ParticipantResult(
                        mLightPlayer!!.gpg!!.participantId,
                        ParticipantResult.MATCH_RESULT_TIE,
                        ParticipantResult.PLACING_UNINITIALIZED
                )
                mMatchView.displayMessage(R.string.winner_tie)
            }

            // Call finishMatch() with result parameters
            Games.TurnBasedMultiplayer.finishMatch(
                    getApiClient(), mMatch!!.matchId, saveMatchData(), winnerResult, loserResult
            ).setResultCallback { updateMatchResult -> processResultFinishMatch(updateMatchResult) }
        }
    }

    private fun processResultFinishMatch(result: TurnBasedMultiplayer.UpdateMatchResult) {
        mUpdatingMatch = false
        mMatchView.dismissSpinner()
        if (checkStatusCode(result.status.statusCode)) {
            val match = result.match
            val player = getCurrentPlayer(match)
            mMatch = match

            // Update achievements
            if (player.gpg!!.result.result == ParticipantResult.MATCH_RESULT_WIN) {
                mAchievementManager.unlock(Achievements.FIRST_WIN)
                val maxScore = mBoard.width * mBoard.height
                val playerScore = if (mPlayer === mLightPlayer) getScore(ReversiColor.LIGHT) else getScore(ReversiColor.DARK)
                if (playerScore == maxScore) {
                    mAchievementManager.unlock(Achievements.PERFECT_WIN)
                }
            } else if (player.gpg.result.result == ParticipantResult.MATCH_RESULT_TIE) {
                mAchievementManager.unlock(Achievements.TIE_GAME)
            }
            mAchievementManager.increment(Achievements.TEN_MATCHES, 1)
            mAchievementManager.increment(Achievements.HUNDRED_MATCHES, 1)

            if (mMatch!!.canRematch()) {
                mMatchView.askForRematch()
            }
        }
    }

    fun forfeitMatch() {
        if (mMatch == null) {
            mMatchView.toast(R.string.no_match_selected)
            return
        }

        if (!getApiClient().isConnected) {
            mMatchView.displaySignInPrompt()
            return
        }

        when (mMatch!!.status) {
            TurnBasedMatch.MATCH_STATUS_COMPLETE,
            TurnBasedMatch.MATCH_STATUS_CANCELED,
            TurnBasedMatch.MATCH_STATUS_EXPIRED -> {
                mMatchView.toast(R.string.match_inactive)
            }
            TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING -> mMatchView.showLeaveMatchDialog()
            TurnBasedMatch.MATCH_STATUS_ACTIVE -> {
                if (mMatch!!.turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
                    if (mOpponent == null) {
                        mMatchView.showLeaveMatchDialog()
                    } else {
                        if (mOpponent!!.gpg!!.status == Participant.STATUS_JOINED) {
                            mMatchView.showForfeitMatchDialog()
                        } else {
                            mMatchView.showCancelMatchDialog()
                        }
                    }
                } else {
                    mMatchView.showForfeitMatchForbiddenAlert()
                }
            }
        }
    }

    fun doForfeitMatch() {
        if (!getApiClient().isConnected) {
            mMatchView.displaySignInPrompt()
            return
        }

        val winnerResult = ParticipantResult(
                mOpponent!!.gpg!!.participantId,
                ParticipantResult.MATCH_RESULT_WIN,
                ParticipantResult.PLACING_UNINITIALIZED
        )
        val loserResult = ParticipantResult(
                mPlayer!!.gpg!!.participantId,
                ParticipantResult.MATCH_RESULT_LOSS,
                ParticipantResult.PLACING_UNINITIALIZED
        )
        // Give win to other player
        Games.TurnBasedMultiplayer.finishMatch(
                getApiClient(), mMatch!!.matchId, saveMatchData(), winnerResult, loserResult
        ).setResultCallback { result ->
            if (result.status.isSuccess) {
                mMatchView.toast(R.string.forfeit_success)
                updateMatch(result.match)
            } else {
                mMatchView.toast(R.string.forfeit_fail)
            }
        }
    }

    fun leaveMatch() {
        if (!getApiClient().isConnected) {
            mMatchView.displaySignInPrompt()
            return
        }

        if (mMatch!!.turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
            Games.TurnBasedMultiplayer.leaveMatchDuringTurn(getApiClient(), mMatch!!.matchId, null)
                    .setResultCallback { processResultLeaveMatch(it) }
        } else {
            Games.TurnBasedMultiplayer.leaveMatch(getApiClient(), mMatch!!.matchId)
                    .setResultCallback { processResultLeaveMatch(it) }
        }
    }

    private fun processResultLeaveMatch(result: TurnBasedMultiplayer.LeaveMatchResult) {
        if (result.status.isSuccess) {
            mMatchView.toast(R.string.match_canceled_toast)
            mMatch = null
            mMatchView.clearBoard()
        } else {
            mMatchView.toast(R.string.cancel_fail)
        }
    }

    fun cancelMatch() {
        if (!getApiClient().isConnected) {
            mMatchView.displaySignInPrompt()
        } else {
            Games.TurnBasedMultiplayer.cancelMatch(getApiClient(), mMatch!!.matchId)
                    .setResultCallback {
                        result -> processResult(result)
                    }
        }
    }

    private fun processResult(result: TurnBasedMultiplayer.CancelMatchResult) {
        if (result.status.isSuccess) {
            mMatchView.toast(R.string.match_canceled_toast)
            mMatchView.clearBoard()
        } else {
            mMatchView.toast(R.string.cancel_fail)
        }
    }

    fun rematch() {
        if (!getApiClient().isConnected) {
            mMatchView.showSpinner()
            Games.TurnBasedMultiplayer.rematch(getApiClient(), mMatch!!.matchId)
                    .setResultCallback { result -> processResult(result) }
            mMatch = null
        } else {
            mMatchView.displaySignInPrompt()
        }
    }

    fun selectMatch(match: TurnBasedMatch) {
        mMatch = match
        if (match.data == null) {
            startMatch(match)
        } else {
            updateMatch(match)
        }
    }

    fun initiateMatch(data: Intent) {
        val invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS)

        val minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0)
        val maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0)

        val autoMatchCriteria =
                if (minAutoMatchPlayers > 0) {
                    RoomConfig.createAutoMatchCriteria(minAutoMatchPlayers, maxAutoMatchPlayers, 0)
                } else {
                    mAchievementManager.unlock(Achievements.PLAY_WITH_FRIEND)
                    null
                }

        val matchConfig = TurnBasedMatchConfig.builder()
                .addInvitedPlayers(invitees)
                .setAutoMatchCriteria(autoMatchCriteria)
                .build()

        mMatchView.showSpinner()
        Games.TurnBasedMultiplayer.createMatch(getApiClient(), matchConfig)
                .setResultCallback { processResult(it) }
    }

    fun registerMatchUpdateListener(shouldRegister: Boolean) {
        Games.TurnBasedMultiplayer.unregisterMatchUpdateListener(getApiClient())
        if (shouldRegister) {
            Games.TurnBasedMultiplayer.registerMatchUpdateListener(getApiClient(), this)
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
            mMatchView.toast(R.string.match_removed)
            mMatch = null
            mMatchView.clearBoard()
        }
    }

    private fun saveMatchData(): ByteArray {
        val board = mBoard.serialize()

        val data = mMatch!!.data ?: ByteArray(256).apply {
            System.arraycopy(board, 0, this, LIGHT_START_INDEX, board.size)
            System.arraycopy(board, 0, this, DARK_START_INDEX, board.size)
        }

        return data.apply {
            val startIndex = if (mPlayer === mLightPlayer) LIGHT_START_INDEX else DARK_START_INDEX

            // Copy the serialized Board into the appropriate place in match data
            System.arraycopy(board, 0, this, startIndex, board.size)

            // Clear out the first 16 nodes following, which were the other player's previous moves
            for (clearIndex in (startIndex + 64 until startIndex + 64 + 16)) {
                this[clearIndex] = 0
            }
        }
    }

    private fun checkStatusCode(statusCode: Int): Boolean {
        if (statusCode == GamesStatusCodes.STATUS_OK
                || statusCode == GamesStatusCodes.STATUS_NETWORK_ERROR_OPERATION_DEFERRED) {
            return true
        }

        mMatch = null
        mMatchView.clearBoard()
        mMatchView.dismissSpinner()

        when (statusCode) {
            GamesStatusCodes.STATUS_MULTIPLAYER_ERROR_NOT_TRUSTED_TESTER -> {
                mMatchView.showAlertDialog(
                        R.string.dialog_error_title,
                        R.string.dialog_error_tester_untrusted
                )
            }
            GamesStatusCodes.STATUS_MATCH_ERROR_ALREADY_REMATCHED -> {
                mMatchView.showAlertDialog(
                        R.string.dialog_error_title,
                        R.string.dialog_error_already_rematched
                )
            }
            GamesStatusCodes.STATUS_NETWORK_ERROR_OPERATION_FAILED -> {
                mMatchView.showAlertDialog(
                        R.string.dialog_error_title,
                        R.string.dialog_error_network_operation_failed
                )
            }
            GamesStatusCodes.STATUS_CLIENT_RECONNECT_REQUIRED -> {
                mMatchView.showAlertDialog(
                        R.string.dialog_error_title,
                        R.string.dialog_error_reconnect_required
                )
            }
            GamesStatusCodes.STATUS_INTERNAL_ERROR -> {
                mMatchView.showAlertDialog(
                        R.string.dialog_error_title,
                        R.string.dialog_error_internal_error
                )
            }
            GamesStatusCodes.STATUS_MATCH_ERROR_INACTIVE_MATCH -> {
                mMatchView.showAlertDialog(
                        R.string.dialog_error_title,
                        R.string.dialog_error_inactive_match
                )
            }
            GamesStatusCodes.STATUS_MATCH_ERROR_LOCALLY_MODIFIED -> {
                mMatchView.showAlertDialog(
                        R.string.dialog_error_title,
                        R.string.dialog_error_locally_modified
                )
            }
            else -> {
                mMatchView.showAlertDialog(
                        R.string.dialog_error_title,
                        R.string.dialog_error_message_default
                )
            }
        }

        return false
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
